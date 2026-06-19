package com.kavindu.techmart.ejb.util;

import com.kavindu.techmart.common.dto.CategoryDTO;
import com.kavindu.techmart.common.dto.InventoryDTO;
import com.kavindu.techmart.common.dto.NotificationDTO;
import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.dto.OrderItemDTO;
import com.kavindu.techmart.common.dto.PaymentDTO;
import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.entity.Category;
import com.kavindu.techmart.common.entity.Inventory;
import com.kavindu.techmart.common.entity.Notification;
import com.kavindu.techmart.common.entity.Order;
import com.kavindu.techmart.common.entity.OrderItem;
import com.kavindu.techmart.common.entity.Payment;
import com.kavindu.techmart.common.entity.Product;
import com.kavindu.techmart.common.entity.User;

import java.util.stream.Collectors;

public final class Mappers {

    private Mappers() {
    }

    public static ProductDTO toProductDTO(Product p, int totalStock) {
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setSku(p.getSku());
        dto.setPrice(p.getPrice());
        dto.setDiscountedPrice(p.getDiscountedPrice());
        dto.setEffectivePrice(p.getEffectivePrice());
        dto.setBrand(p.getBrand());
        dto.setImageUrl(p.getImageUrl());
        dto.setActive(p.isActive());
        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
        }
        dto.setTotalStock(totalStock);
        dto.setInStock(totalStock > 0);
        return dto;
    }

    public static CategoryDTO toCategoryDTO(Category c, long productCount) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setImageUrl(c.getImageUrl());
        if (c.getParent() != null) {
            dto.setParentId(c.getParent().getId());
        }
        dto.setProductCount(productCount);
        return dto;
    }

    public static UserDTO toUserDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setPhone(u.getPhone());
        dto.setRole(u.getRole() != null ? u.getRole().name() : null);
        dto.setActive(u.isActive());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    public static OrderItemDTO toOrderItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
        }
        dto.setProductName(item.getProductSnapshotName());
        dto.setProductSku(item.getProductSnapshotSku());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        return dto;
    }

    public static OrderDTO toOrderDTO(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());
        if (o.getUser() != null) {
            dto.setUserId(o.getUser().getId());
            dto.setUsername(o.getUser().getUsername());
        }
        dto.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
        dto.setSubtotal(o.getSubtotal());
        dto.setTax(o.getTax());
        dto.setShippingCost(o.getShippingCost());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setShippingAddress(o.getShippingAddress());
        dto.setOrderDate(o.getOrderDate());
        dto.setProcessedAt(o.getProcessedAt());
        dto.setShippedAt(o.getShippedAt());
        dto.setDeliveredAt(o.getDeliveredAt());
        dto.setItems(o.getItems().stream().map(Mappers::toOrderItemDTO).collect(Collectors.toList()));
        Payment payment = o.getPayment();
        if (payment != null) {
            dto.setPaymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
            dto.setPaymentReference(payment.getPaymentReference());
        }
        return dto;
    }

    public static InventoryDTO toInventoryDTO(Inventory i) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(i.getId());
        if (i.getProduct() != null) {
            dto.setProductId(i.getProduct().getId());
            dto.setProductName(i.getProduct().getName());
            dto.setProductSku(i.getProduct().getSku());
        }
        if (i.getWarehouse() != null) {
            dto.setWarehouseId(i.getWarehouse().getId());
            dto.setWarehouseName(i.getWarehouse().getName());
        }
        dto.setQuantityAvailable(i.getQuantityAvailable());
        dto.setQuantityReserved(i.getQuantityReserved());
        dto.setReorderThreshold(i.getReorderThreshold());
        dto.setReorderQuantity(i.getReorderQuantity());
        dto.setLastUpdated(i.getLastUpdated());
        dto.setLowStock(i.getQuantityAvailable() <= i.getReorderThreshold());
        return dto;
    }

    public static NotificationDTO toNotificationDTO(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        if (n.getUser() != null) {
            dto.setUserId(n.getUser().getId());
        }
        dto.setType(n.getType() != null ? n.getType().name() : null);
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }

    public static PaymentDTO toPaymentDTO(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        if (p.getOrder() != null) {
            dto.setOrderId(p.getOrder().getId());
            dto.setOrderNumber(p.getOrder().getOrderNumber());
        }
        dto.setPaymentReference(p.getPaymentReference());
        dto.setGatewayTransactionId(p.getGatewayTransactionId());
        dto.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        dto.setAmount(p.getAmount());
        dto.setCurrency(p.getCurrency());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setInitiatedAt(p.getInitiatedAt());
        dto.setCompletedAt(p.getCompletedAt());
        return dto;
    }
}
