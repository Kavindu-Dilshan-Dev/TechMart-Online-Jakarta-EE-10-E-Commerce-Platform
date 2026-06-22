package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.InventoryDTO;
import com.kavindu.techmart.common.entity.Inventory;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.exception.TechMartException;
import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.ejb.exception.ConcurrencyConflictException;
import com.kavindu.techmart.ejb.util.JmsConstants;
import com.kavindu.techmart.ejb.util.Mappers;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@Local(InventoryServiceLocal.class)
public class InventoryServiceBean implements InventoryServiceLocal {

    private static final Logger LOG = Logger.getLogger(InventoryServiceBean.class.getName());
    private static final int MAX_RETRIES = 5;

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private InventoryReservationHelper reservationHelper;

    @Inject
    @JMSConnectionFactory("java:/jms/cf/TechMart")
    private JMSContext jmsContext;

    @Resource(lookup = JmsConstants.INVENTORY_QUEUE)
    private Queue inventoryQueue;

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean reserveStock(Long productId, int qty) {
        if (qty <= 0) {
            return true;
        }
        boolean reserved = withRetry(() -> reservationHelper.tryReserve(productId, qty));
        if (reserved) {
            LOG.fine(() -> "Reserved " + qty + " of product " + productId);
        } else {
            LOG.info("Insufficient stock to reserve " + qty + " of product " + productId);
        }
        return reserved;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void releaseStock(Long productId, int qty) {
        if (qty <= 0) {
            return;
        }
        withRetry(() -> reservationHelper.tryRelease(productId, qty));
        LOG.fine(() -> "Released " + qty + " of product " + productId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void deductStock(Long productId, int qty) {
        if (qty <= 0) {
            return;
        }
        boolean ok = withRetry(() -> reservationHelper.tryDeduct(productId, qty));
        if (!ok) {
            LOG.warning("Could not fully deduct " + qty + " of product " + productId
                    + " (reserved pool smaller than expected)");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<InventoryDTO> getInventoryForProduct(Long productId) {
        return em.createNamedQuery("Inventory.findByProduct", Inventory.class)
                .setParameter("productId", productId)
                .getResultList()
                .stream().map(Mappers::toInventoryDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int getTotalStock(Long productId) {
        Long sum = em.createNamedQuery("Inventory.totalAvailableForProduct", Long.class)
                .setParameter("productId", productId)
                .getSingleResult();
        return sum == null ? 0 : sum.intValue();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean isProductInStock(Long productId) {
        return getTotalStock(productId) > 0;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryDTO updateInventory(Long inventoryId, int newQty) {
        Inventory inv = em.find(Inventory.class, inventoryId);
        if (inv == null) {
            throw new ResourceNotFoundException("Inventory row not found: " + inventoryId);
        }
        Long productId = inv.getProduct().getId();
        int oldProductTotal = getTotalStock(productId);
        int oldRowQty = inv.getQuantityAvailable();
        inv.setQuantityAvailable(Math.max(0, newQty));
        em.flush();

        int newProductTotal = oldProductTotal - oldRowQty + inv.getQuantityAvailable();
        boolean backInStock = oldProductTotal == 0 && newProductTotal > 0;

        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_REORDER_CHECK)
                .setProperty(JmsConstants.PROP_PRODUCT_ID, productId.longValue())
                .setProperty(JmsConstants.PROP_BACK_IN_STOCK, backInStock)
                .send(inventoryQueue, "");

        LOG.info("Updated inventory " + inventoryId + " to " + newQty
                + (backInStock ? " (product back in stock)" : ""));
        return Mappers.toInventoryDTO(inv);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Void> syncInventoryAcrossWarehouses(Long productId) {
        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_SYNC)
                .setProperty(JmsConstants.PROP_PRODUCT_ID, productId.longValue())
                .send(inventoryQueue, "");
        LOG.info("Queued cross-warehouse SYNC for product " + productId);
        return new AsyncResult<>(null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rebalanceWarehouses(Long productId) {
        List<Inventory> rows = em.createNamedQuery("Inventory.findByProduct", Inventory.class)
                .setParameter("productId", productId)
                .getResultList();
        if (rows.isEmpty()) {
            return;
        }
        int totalAvailable = rows.stream().mapToInt(Inventory::getQuantityAvailable).sum();
        int perWarehouse = totalAvailable / rows.size();
        int remainder = totalAvailable % rows.size();
        for (int i = 0; i < rows.size(); i++) {
            int share = perWarehouse + (i < remainder ? 1 : 0);
            rows.get(i).setQuantityAvailable(share);
        }
        LOG.info("Rebalanced product " + productId + " across " + rows.size()
                + " warehouses (" + totalAvailable + " units)");
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<InventoryDTO> getLowStockAlerts() {
        return em.createNamedQuery("Inventory.findLowStock", Inventory.class)
                .getResultList()
                .stream().map(Mappers::toInventoryDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<InventoryDTO> getAllInventory() {
        return em.createQuery(
                        "SELECT i FROM Inventory i ORDER BY i.product.id, i.warehouse.id", Inventory.class)
                .getResultList()
                .stream().map(Mappers::toInventoryDTO).collect(Collectors.toList());
    }

    private boolean withRetry(BooleanSupplier op) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return op.getAsBoolean();
            } catch (ConcurrencyConflictException conflict) {
                backoff(attempt);
            }
        }
        throw new TechMartException("Inventory update failed after " + MAX_RETRIES
                + " retries due to sustained concurrent modification");
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(Math.min(10L * (attempt + 1), 60L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
