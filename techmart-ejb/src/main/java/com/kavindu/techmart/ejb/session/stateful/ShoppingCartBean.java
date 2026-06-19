package com.kavindu.techmart.ejb.session.stateful;

import com.kavindu.techmart.common.dto.CartItemDTO;
import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.exception.InsufficientStockException;
import com.kavindu.techmart.common.interfaces.CartServiceLocal;
import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.common.interfaces.ProductServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Stateful
@SessionScoped
@StatefulTimeout(value = 30, unit = TimeUnit.MINUTES)
public class ShoppingCartBean implements CartServiceLocal, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ShoppingCartBean.class.getName());

    @EJB
    private transient InventoryServiceLocal inventoryService;

    @EJB
    private transient ProductServiceLocal productService;

    @EJB
    private transient PerformanceMetricsBean metrics;

    private Long userId;
    private final Map<Long, CartItemDTO> cartItems = new LinkedHashMap<>();
    private LocalDateTime sessionStartTime;

    @PostConstruct
    public void onCreate() {
        sessionStartTime = LocalDateTime.now();
        LOG.fine("ShoppingCartBean created (stateful @PostConstruct)");
    }

    @Override
    public void initCart(Long userId) {
        this.userId = userId;
        if (sessionStartTime == null) {
            sessionStartTime = LocalDateTime.now();
        }
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public CartItemDTO addItem(Long productId, int qty) {
        if (qty <= 0) {
            throw new InsufficientStockException(productId, "Quantity must be positive");
        }
        ProductDTO product = productService.findById(productId);
        int available = inventoryService.getTotalStock(productId);

        CartItemDTO existing = cartItems.get(productId);
        int desired = qty + (existing != null ? existing.getQuantity() : 0);
        if (desired > available) {
            throw new InsufficientStockException(productId,
                    "Only " + available + " unit(s) of '" + product.getName() + "' available");
        }

        CartItemDTO item = existing != null ? existing : new CartItemDTO();
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setSku(product.getSku());
        item.setImageUrl(product.getImageUrl());
        item.setUnitPrice(product.getEffectivePrice());
        item.setQuantity(desired);
        item.setAvailableStock(available);
        item.setLineTotal(product.getEffectivePrice().multiply(BigDecimal.valueOf(desired)));
        cartItems.put(productId, item);
        LOG.fine(() -> "Cart add: product " + productId + " qty " + desired);
        return item;
    }

    @Override
    public void removeItem(Long productId) {
        cartItems.remove(productId);
    }

    @Override
    public void updateQuantity(Long productId, int qty) {
        if (qty <= 0) {
            cartItems.remove(productId);
            return;
        }
        CartItemDTO item = cartItems.get(productId);
        if (item == null) {
            addItem(productId, qty);
            return;
        }
        int available = inventoryService.getTotalStock(productId);
        if (qty > available) {
            throw new InsufficientStockException(productId, "Only " + available + " unit(s) available");
        }
        item.setQuantity(qty);
        item.setAvailableStock(available);
        item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
    }

    @Override
    public List<CartItemDTO> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    @Override
    public BigDecimal getCartTotal() {
        return cartItems.values().stream()
                .map(CartItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int getItemCount() {
        return cartItems.values().stream().mapToInt(CartItemDTO::getQuantity).sum();
    }

    @Override
    public void clearCart() {
        cartItems.clear();
    }

    @Override
    public void checkout() {
        LOG.fine(() -> "Cart checkout: clearing " + cartItems.size() + " line(s)");
        cartItems.clear();
    }

    @PrePassivate
    public void onPassivate() {
        LOG.fine("ShoppingCartBean passivating (state persisted to disk)");
    }

    @PostActivate
    public void onActivate() {

        LOG.fine("ShoppingCartBean activating; refreshing prices");
        for (CartItemDTO item : cartItems.values()) {
            try {
                ProductDTO fresh = productService.findById(item.getProductId());
                item.setUnitPrice(fresh.getEffectivePrice());
                item.setAvailableStock(inventoryService.getTotalStock(item.getProductId()));
                item.setLineTotal(fresh.getEffectivePrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())));
            } catch (RuntimeException ex) {
                LOG.warning("Could not refresh cart item " + item.getProductId() + ": " + ex.getMessage());
            }
        }
    }

    @PreDestroy
    public void onDestroy() {
        if (sessionStartTime != null && metrics != null) {
            long seconds = Duration.between(sessionStartTime, LocalDateTime.now()).getSeconds();
            metrics.recordRequest("cart.session.seconds", seconds);
            LOG.fine(() -> "ShoppingCartBean destroyed after " + seconds + "s");
        }
    }
}
