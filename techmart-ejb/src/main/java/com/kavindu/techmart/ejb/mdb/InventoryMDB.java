package com.kavindu.techmart.ejb.mdb;

import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.ejb.util.JmsConstants;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "InventoryMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = JmsConstants.INVENTORY_QUEUE),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession",
                propertyValue = "10")
})
public class InventoryMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(InventoryMDB.class.getName());

    @EJB
    private InventoryServiceLocal inventoryService;

    @EJB
    private NotificationServiceLocal notificationService;

    @Override
    public void onMessage(Message message) {
        try {
            String action = message.getStringProperty(JmsConstants.PROP_ACTION);
            long productId = message.getLongProperty(JmsConstants.PROP_PRODUCT_ID);
            LOG.fine(() -> "InventoryMDB received action=" + action + " productId=" + productId);

            if (action == null) {
                return;
            }
            switch (action) {
                case JmsConstants.ACTION_SYNC -> {
                    inventoryService.rebalanceWarehouses(productId);
                    LOG.info("Synced/rebalanced warehouses for product " + productId);
                }
                case JmsConstants.ACTION_REORDER_CHECK -> handleReorderCheck(message, productId);
                case JmsConstants.ACTION_UPDATE ->
                        LOG.fine(() -> "Inventory update acknowledged for product " + productId);
                default -> LOG.warning("Unknown inventory action: " + action);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error processing inventory message", e);
        }
    }

    private void handleReorderCheck(Message message, long productId) throws Exception {
        boolean backInStock = message.propertyExists(JmsConstants.PROP_BACK_IN_STOCK)
                && message.getBooleanProperty(JmsConstants.PROP_BACK_IN_STOCK);
        if (backInStock) {
            notificationService.sendStockBackNotification(productId);
            LOG.info("Triggered back-in-stock notifications for product " + productId);
        }

        if (!inventoryService.isProductInStock(productId)) {
            LOG.info("Reorder check: product " + productId + " is OUT OF STOCK");
        }
    }
}
