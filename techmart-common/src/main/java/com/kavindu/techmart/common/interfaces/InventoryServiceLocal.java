package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.InventoryDTO;

import java.util.List;
import java.util.concurrent.Future;

public interface InventoryServiceLocal {

    boolean reserveStock(Long productId, int qty);

    void releaseStock(Long productId, int qty);

    void deductStock(Long productId, int qty);

    List<InventoryDTO> getInventoryForProduct(Long productId);

    int getTotalStock(Long productId);

    boolean isProductInStock(Long productId);

    InventoryDTO updateInventory(Long inventoryId, int newQty);

    Future<Void> syncInventoryAcrossWarehouses(Long productId);

    void rebalanceWarehouses(Long productId);

    List<InventoryDTO> getLowStockAlerts();

    List<InventoryDTO> getAllInventory();
}
