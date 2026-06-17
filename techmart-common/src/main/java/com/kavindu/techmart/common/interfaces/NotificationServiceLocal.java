package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.NotificationDTO;
import com.kavindu.techmart.common.enums.OrderStatus;

import java.util.List;

public interface NotificationServiceLocal {

    void sendOrderNotification(Long userId, Long orderId, String orderNumber, OrderStatus status);

    void sendStockBackNotification(Long productId);

    List<NotificationDTO> getUnreadNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void createStockAlert(Long userId, Long productId);

    void removeStockAlert(Long userId, Long productId);

    void publishSystemBroadcast(String title, String message);
}
