package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.dto.OrderItemDTO;
import com.kavindu.techmart.common.entity.Order;
import com.kavindu.techmart.common.entity.OrderItem;
import com.kavindu.techmart.common.entity.Product;
import com.kavindu.techmart.common.entity.User;
import com.kavindu.techmart.common.enums.OrderStatus;
import com.kavindu.techmart.common.exception.InsufficientStockException;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.exception.TechMartException;
import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.common.interfaces.OrderServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
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
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@Local(OrderServiceLocal.class)
public class OrderServiceBean implements OrderServiceLocal {

    private static final Logger LOG = Logger.getLogger(OrderServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private InventoryServiceLocal inventoryService;

    @EJB
    private NotificationServiceLocal notificationService;

    @EJB
    private SystemConfigBean systemConfig;

    @EJB
    private PerformanceMetricsBean metrics;

    @Inject
    @JMSConnectionFactory("java:/jms/cf/TechMart")
    private JMSContext jmsContext;

    @Resource(lookup = JmsConstants.ORDER_QUEUE)
    private Queue orderQueue;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO placeOrder(Long userId, List<OrderItemDTO> items, String shippingAddress) {
        if (items == null || items.isEmpty()) {
            throw new TechMartException("Cannot place an order with no items");
        }
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        List<Product> products = new ArrayList<>();
        List<int[]> reserved = new ArrayList<>();
        try {
            for (OrderItemDTO line : items) {
                Product product = em.find(Product.class, line.getProductId());
                if (product == null || !product.isActive()) {
                    throw new ResourceNotFoundException("Product not available: " + line.getProductId());
                }
                if (line.getQuantity() <= 0) {
                    throw new TechMartException("Invalid quantity for product " + line.getProductId());
                }
                boolean ok = inventoryService.reserveStock(product.getId(), line.getQuantity());
                if (!ok) {
                    throw new InsufficientStockException(product.getId(),
                            "Insufficient stock for '" + product.getName() + "'");
                }
                reserved.add(new int[]{product.getId().intValue(), line.getQuantity()});
                products.add(product);
            }
        } catch (RuntimeException ex) {

            for (int[] r : reserved) {
                safeRelease((long) r[0], r[1]);
            }
            throw ex;
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(shippingAddress);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < items.size(); i++) {
            OrderItemDTO line = items.get(i);
            Product product = products.get(i);
            BigDecimal unitPrice = product.getEffectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(lineTotal);
            item.setProductSnapshotName(product.getName());
            item.setProductSnapshotSku(product.getSku());
            order.addItem(item);
        }

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(systemConfig.getTaxRate()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = BigDecimal.valueOf(systemConfig.getShippingFlat())
                .setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTax(tax);
        order.setShippingCost(shipping);
        order.setTotalAmount(subtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP));

        em.persist(order);
        em.flush();

        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_AWAITING_PAYMENT)
                .setProperty(JmsConstants.PROP_ORDER_ID, order.getId().longValue())
                .send(orderQueue, "");

        LOG.info("Placed order " + order.getOrderNumber() + " for user " + userId
                + " total=" + order.getTotalAmount());
        return Mappers.toOrderDTO(order);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<OrderDTO> processOrderAsync(Long orderId) {
        try {
            Order order = em.find(Order.class, orderId);
            if (order == null) {
                throw new ResourceNotFoundException("Order not found: " + orderId);
            }

            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    inventoryService.deductStock(item.getProduct().getId(), item.getQuantity());
                }
            }
            order.setStatus(OrderStatus.PROCESSING);
            order.setProcessedAt(LocalDateTime.now());
            em.flush();

            notificationService.sendOrderNotification(order.getUser().getId(), orderId,
                    order.getOrderNumber(), OrderStatus.PROCESSING);
            metrics.recordOrderProcessed();
            LOG.info("Processed order " + order.getOrderNumber() + " -> PROCESSING");
            return new AsyncResult<>(Mappers.toOrderDTO(order));
        } catch (RuntimeException ex) {
            metrics.recordOrderFailed();
            LOG.warning("Failed to process order " + orderId + ": " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO cancelOrder(Long orderId, Long userId) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new TechMartException("Order cannot be cancelled in status " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                safeRelease(item.getProduct().getId(), item.getQuantity());
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        em.flush();

        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_CANCEL)
                .setProperty(JmsConstants.PROP_ORDER_ID, order.getId().longValue())
                .send(orderQueue, "");

        notificationService.sendOrderNotification(order.getUser().getId(), orderId,
                order.getOrderNumber(), OrderStatus.CANCELLED);
        LOG.info("Cancelled order " + order.getOrderNumber());
        return Mappers.toOrderDTO(order);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        order.setStatus(status);
        if (status == OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        em.flush();
        notificationService.sendOrderNotification(order.getUser().getId(), orderId,
                order.getOrderNumber(), status);
        LOG.info("Order " + order.getOrderNumber() + " status -> " + status);
        return Mappers.toOrderDTO(order);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO findById(Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return Mappers.toOrderDTO(order);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrderDTO findByOrderNumber(String orderNumber) {
        try {
            Order order = em.createNamedQuery("Order.findByNumber", Order.class)
                    .setParameter("orderNumber", orderNumber)
                    .getSingleResult();
            return Mappers.toOrderDTO(order);
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("Order not found: " + orderNumber);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<OrderDTO> findOrdersByUser(Long userId) {
        return em.createNamedQuery("Order.findByUser", Order.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream().map(Mappers::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<OrderDTO> findOrdersByStatus(OrderStatus status) {
        return em.createNamedQuery("Order.findByStatus", Order.class)
                .setParameter("status", status)
                .getResultList()
                .stream().map(Mappers::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<OrderDTO> findRecentOrders(int limit) {
        return em.createNamedQuery("Order.findRecent", Order.class)
                .setMaxResults(limit <= 0 ? 10 : limit)
                .getResultList()
                .stream().map(Mappers::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<OrderDTO> findAllOrders() {
        return em.createNamedQuery("Order.findRecent", Order.class)
                .getResultList()
                .stream().map(Mappers::toOrderDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long countByStatus(OrderStatus status) {
        return em.createNamedQuery("Order.countByStatus", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public BigDecimal getRevenueSince(LocalDateTime since) {
        BigDecimal revenue = em.createNamedQuery("Order.revenueSince", BigDecimal.class)
                .setParameter("since", since)
                .getSingleResult();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    private void safeRelease(Long productId, int qty) {
        try {
            inventoryService.releaseStock(productId, qty);
        } catch (RuntimeException ex) {
            LOG.warning("Failed to release stock for product " + productId + ": " + ex.getMessage());
        }
    }

    private String generateOrderNumber() {
        return "TM-" + System.currentTimeMillis() + "-"
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
