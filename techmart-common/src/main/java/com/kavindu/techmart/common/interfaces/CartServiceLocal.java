package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.CartItemDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CartServiceLocal {

    void initCart(Long userId);

    Long getUserId();

    CartItemDTO addItem(Long productId, int qty);

    void removeItem(Long productId);

    void updateQuantity(Long productId, int qty);

    List<CartItemDTO> getCartItems();

    BigDecimal getCartTotal();

    int getItemCount();

    void clearCart();

    void checkout();
}
