package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.dto.OrderItemDTO;
import com.kavindu.techmart.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Future;

public interface OrderServiceLocal {

    OrderDTO placeOrder(Long userId, List<OrderItemDTO> items, String shippingAddress);

    Future<OrderDTO> processOrderAsync(Long orderId);

    OrderDTO cancelOrder(Long orderId, Long userId);

    OrderDTO updateOrderStatus(Long orderId, OrderStatus status);

    OrderDTO findById(Long orderId);

    OrderDTO findByOrderNumber(String orderNumber);

    List<OrderDTO> findOrdersByUser(Long userId);

    List<OrderDTO> findOrdersByStatus(OrderStatus status);

    List<OrderDTO> findRecentOrders(int limit);

    List<OrderDTO> findAllOrders();

    long countByStatus(OrderStatus status);

    BigDecimal getRevenueSince(LocalDateTime since);
}
