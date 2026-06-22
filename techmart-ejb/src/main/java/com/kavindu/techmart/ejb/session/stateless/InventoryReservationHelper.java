package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.entity.Inventory;
import com.kavindu.techmart.ejb.exception.ConcurrencyConflictException;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;

import java.util.Comparator;
import java.util.List;

@Stateless
public class InventoryReservationHelper {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean tryReserve(Long productId, int qty) {
        List<Inventory> rows = rowsForProduct(productId);
        int totalAvailable = rows.stream().mapToInt(Inventory::getQuantityAvailable).sum();
        if (totalAvailable < qty) {
            return false;
        }
        int remaining = qty;
        rows.sort(Comparator.comparingInt(Inventory::getQuantityAvailable).reversed());
        for (Inventory inv : rows) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(inv.getQuantityAvailable(), remaining);
            if (take > 0) {
                inv.setQuantityAvailable(inv.getQuantityAvailable() - take);
                inv.setQuantityReserved(inv.getQuantityReserved() + take);
                remaining -= take;
            }
        }
        flushOrConflict();
        return remaining <= 0;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean tryDeduct(Long productId, int qty) {
        List<Inventory> rows = rowsForProduct(productId);
        int remaining = qty;
        rows.sort(Comparator.comparingInt(Inventory::getQuantityReserved).reversed());
        for (Inventory inv : rows) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(inv.getQuantityReserved(), remaining);
            if (take > 0) {
                inv.setQuantityReserved(inv.getQuantityReserved() - take);
                remaining -= take;
            }
        }
        flushOrConflict();
        return remaining <= 0;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean tryRelease(Long productId, int qty) {
        List<Inventory> rows = rowsForProduct(productId);
        int remaining = qty;
        rows.sort(Comparator.comparingInt(Inventory::getQuantityReserved).reversed());
        for (Inventory inv : rows) {
            if (remaining <= 0) {
                break;
            }
            int give = Math.min(inv.getQuantityReserved(), remaining);
            if (give > 0) {
                inv.setQuantityReserved(inv.getQuantityReserved() - give);
                inv.setQuantityAvailable(inv.getQuantityAvailable() + give);
                remaining -= give;
            }
        }
        flushOrConflict();
        return true;
    }

    private List<Inventory> rowsForProduct(Long productId) {
        return em.createNamedQuery("Inventory.findByProduct", Inventory.class)
                .setParameter("productId", productId)
                .getResultList();
    }

    private void flushOrConflict() {
        try {
            em.flush();
        } catch (OptimisticLockException e) {
            throw new ConcurrencyConflictException("Optimistic lock conflict on inventory", e);
        }
    }
}
