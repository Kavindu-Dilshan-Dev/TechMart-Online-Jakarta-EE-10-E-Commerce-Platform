package com.kavindu.techmart.web.session;

import com.kavindu.techmart.common.dto.CartItemDTO;
import com.kavindu.techmart.common.interfaces.CartServiceLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@SessionScoped
public class CartSession implements CartServiceLocal, Serializable {

    @EJB
    private CartServiceLocal cart;

    @Override public void initCart(Long userId)                       { cart.initCart(userId); }
    @Override public Long getUserId()                                 { return cart.getUserId(); }
    @Override public CartItemDTO addItem(Long productId, int qty)     { return cart.addItem(productId, qty); }
    @Override public void removeItem(Long productId)                  { cart.removeItem(productId); }
    @Override public void updateQuantity(Long productId, int qty)     { cart.updateQuantity(productId, qty); }
    @Override public List<CartItemDTO> getCartItems()                 { return cart.getCartItems(); }
    @Override public BigDecimal getCartTotal()                        { return cart.getCartTotal(); }
    @Override public int getItemCount()                               { return cart.getItemCount(); }
    @Override public void clearCart()                                 { cart.clearCart(); }
    @Override public void checkout()                                  { cart.checkout(); }
}
