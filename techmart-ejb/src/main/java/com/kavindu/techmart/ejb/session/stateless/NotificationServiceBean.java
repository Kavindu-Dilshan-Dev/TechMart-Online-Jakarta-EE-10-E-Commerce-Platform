package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.NotificationDTO;
import com.kavindu.techmart.common.entity.Notification;
import com.kavindu.techmart.common.entity.Product;
import com.kavindu.techmart.common.entity.StockAlert;
import com.kavindu.techmart.common.entity.User;
import com.kavindu.techmart.common.enums.NotificationType;
import com.kavindu.techmart.common.enums.OrderStatus;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.ejb.util.JmsConstants;
import com.kavindu.techmart.ejb.util.JsonUtil;
import com.kavindu.techmart.ejb.util.Mappers;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Stateless
@Local(NotificationServiceLocal.class)
public class NotificationServiceBean implements NotificationServiceLocal {

    private static final Logger LOG = Logger.getLogger(NotificationServiceBean.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @Inject
    @JMSConnectionFactory("java:/jms/cf/TechMart")
    private JMSContext jmsContext;

    @Resource(lookup = JmsConstants.NOTIFICATION_TOPIC)
    private Topic notificationTopic;

    @Resource(lookup = JmsConstants.EMAIL_QUEUE)
    private Queue emailQueue;

    @Override
    public void sendOrderNotification(Long userId, Long orderId, String orderNumber, OrderStatus status) {
        String title = "Order " + orderNumber + " " + humanize(status);
        String message = "Your order " + orderNumber + " is now " + humanize(status) + ".";

        Notification n = new Notification();
        n.setUser(em.getReference(User.class, userId));
        n.setType(NotificationType.ORDER_UPDATE);
        n.setTitle(title);
        n.setMessage(message);
        em.persist(n);

        String json = JsonUtil.notificationJson(userId, NotificationType.ORDER_UPDATE.name(),
                title, message, orderId, null);
        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_USER_ID, userId.longValue())
                .send(notificationTopic, json);
        LOG.fine(() -> "Order notification published for user " + userId + ", order " + orderNumber);
    }

    @Override
    public void sendStockBackNotification(Long productId) {
        List<StockAlert> alerts = em.createNamedQuery("StockAlert.findPendingByProduct", StockAlert.class)
                .setParameter("productId", productId)
                .getResultList();
        if (alerts.isEmpty()) {
            return;
        }
        Product product = em.find(Product.class, productId);
        String productName = product != null ? product.getName() : ("#" + productId);
        String title = "Back in stock: " + productName;
        String message = "Good news! '" + productName + "' is back in stock. Order now before it sells out.";

        for (StockAlert alert : alerts) {
            User user = alert.getUser();
            Notification n = new Notification();
            n.setUser(user);
            n.setType(NotificationType.STOCK_ALERT);
            n.setTitle(title);
            n.setMessage(message);
            em.persist(n);

            String json = JsonUtil.notificationJson(user.getId(), NotificationType.STOCK_ALERT.name(),
                    title, message, null, productId);
            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_USER_ID, user.getId().longValue())
                    .send(notificationTopic, json);

            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_EMAIL_TYPE, JmsConstants.EMAIL_STOCK_BACK)
                    .setProperty(JmsConstants.PROP_TO_EMAIL, user.getEmail())
                    .send(emailQueue, message);

            alert.setNotified(true);
            alert.setNotifiedAt(LocalDateTime.now());
        }
        LOG.info("Sent back-in-stock notifications to " + alerts.size()
                + " subscriber(s) for product " + productId);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return em.createNamedQuery("Notification.findUnreadByUser", Notification.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream().map(Mappers::toNotificationDTO).collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getUnreadCount(Long userId) {
        return em.createNamedQuery("Notification.countUnreadByUser", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = em.find(Notification.class, notificationId);
        if (n == null) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
        if (!n.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        em.createQuery("UPDATE Notification n SET n.isRead = true, n.readAt = :now "
                        + "WHERE n.user.id = :userId AND n.isRead = false")
                .setParameter("now", LocalDateTime.now())
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void createStockAlert(Long userId, Long productId) {
        if (findAlert(userId, productId) != null) {
            return;
        }
        StockAlert alert = new StockAlert();
        alert.setUser(em.getReference(User.class, userId));
        alert.setProduct(em.getReference(Product.class, productId));
        alert.setNotified(false);
        em.persist(alert);
        LOG.fine(() -> "Stock alert created: user " + userId + ", product " + productId);
    }

    @Override
    public void removeStockAlert(Long userId, Long productId) {
        StockAlert alert = findAlert(userId, productId);
        if (alert != null) {
            em.remove(alert);
        }
    }

    @Override
    public void publishSystemBroadcast(String title, String message) {
        String json = JsonUtil.notificationJson(null, NotificationType.SYSTEM.name(),
                title, message, null, null);
        jmsContext.createProducer()
                .setProperty(JmsConstants.PROP_BROADCAST, true)
                .send(notificationTopic, json);
        LOG.info("System broadcast published: " + title);
    }

    private StockAlert findAlert(Long userId, Long productId) {
        try {
            return em.createNamedQuery("StockAlert.findByUserAndProduct", StockAlert.class)
                    .setParameter("userId", userId)
                    .setParameter("productId", productId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static String humanize(OrderStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }
}
