package com.kavindu.techmart.ejb.session.stateful;

import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.exception.InsufficientStockException;
import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.common.interfaces.ProductServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartBeanTest {

    @Mock
    private InventoryServiceLocal inventoryService;
    @Mock
    private ProductServiceLocal productService;
    @Mock
    private PerformanceMetricsBean metrics;

    @InjectMocks
    private ShoppingCartBean cart;

    private ProductDTO product(long id, String name, String price) {
        ProductDTO p = new ProductDTO();
        p.setId(id);
        p.setName(name);
        p.setSku("SKU-" + id);
        p.setEffectivePrice(new BigDecimal(price));
        return p;
    }

    @BeforeEach
    void setUp() {
        cart.onCreate();
        cart.initCart(7L);
        lenient().when(productService.findById(1L)).thenReturn(product(1L, "Phone", "1000.00"));
        lenient().when(inventoryService.getTotalStock(1L)).thenReturn(10);
    }

    @Test
    @DisplayName("addItem adds a new line with computed line total")
    void addItem_new() {
        cart.addItem(1L, 2);
        assertEquals(1, cart.getCartItems().size());
        assertEquals(2, cart.getItemCount());
        assertEquals(new BigDecimal("2000.00"), cart.getCartTotal());
    }

    @Test
    @DisplayName("addItem on an existing product increments the quantity")
    void addItem_existingIncrements() {
        cart.addItem(1L, 2);
        cart.addItem(1L, 3);
        assertEquals(5, cart.getItemCount());
        assertEquals(new BigDecimal("5000.00"), cart.getCartTotal());
    }

    @Test
    @DisplayName("addItem beyond available stock is rejected")
    void addItem_insufficientStock() {
        assertThrows(InsufficientStockException.class, () -> cart.addItem(1L, 99));
    }

    @Test
    @DisplayName("updateQuantity to zero removes the line")
    void updateQuantity_zeroRemoves() {
        cart.addItem(1L, 2);
        cart.updateQuantity(1L, 0);
        assertTrue(cart.getCartItems().isEmpty());
        assertEquals(0, cart.getItemCount());
    }

    @Test
    @DisplayName("clearCart and checkout empty the cart")
    void clearCart_empties() {
        cart.addItem(1L, 2);
        cart.clearCart();
        assertTrue(cart.getCartItems().isEmpty());
        cart.addItem(1L, 1);
        cart.checkout();
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    @DisplayName("initCart records the user id")
    void initCart_setsUser() {
        assertEquals(7L, cart.getUserId());
    }
}
