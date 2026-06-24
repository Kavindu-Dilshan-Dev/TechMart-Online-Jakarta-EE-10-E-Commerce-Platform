package com.kavindu.techmart.ejb.mdb;

import com.kavindu.techmart.ejb.util.JmsConstants;
import com.kavindu.techmart.ejb.websocket.WebSocketSessionRegistry;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "NotificationMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = JmsConstants.NOTIFICATION_TOPIC),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability",
                propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId",
                propertyValue = "techmart-notification-mdb"),
        @ActivationConfigProperty(propertyName = "subscriptionName",
                propertyValue = "TechMartNotificationSubscription"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession",
                propertyValue = "1")
})
public class NotificationMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(NotificationMDB.class.getName());

    @EJB
    private WebSocketSessionRegistry registry;

    @Override
    public void onMessage(Message message) {
        try {
            String payload = (message instanceof TextMessage tm) ? tm.getText() : "";
            boolean broadcast = message.propertyExists(JmsConstants.PROP_BROADCAST)
                    && message.getBooleanProperty(JmsConstants.PROP_BROADCAST);

            if (broadcast) {
                registry.broadcast(payload);
                LOG.fine(() -> "Broadcast pushed to " + registry.getConnectionCount() + " connection(s)");
            } else if (message.propertyExists(JmsConstants.PROP_USER_ID)) {
                long userId = message.getLongProperty(JmsConstants.PROP_USER_ID);
                registry.sendToUser(userId, payload);
                LOG.fine(() -> "Notification pushed to user " + userId);
            } else {
                LOG.warning("Notification message had neither userId nor broadcast flag");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error delivering notification", e);
        }
    }
}
