package com.kavindu.techmart.ejb.mdb;

import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.interfaces.OrderServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.util.JmsConstants;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "OrderProcessingMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup",
                propertyValue = JmsConstants.ORDER_QUEUE),
        @ActivationConfigProperty(propertyName = "destinationType",
                propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode",
                propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession",
                propertyValue = "20")
})
public class OrderProcessingMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(OrderProcessingMDB.class.getName());
    private static final long PROCESS_TIMEOUT_SECONDS = 30;

    @EJB
    private OrderServiceLocal orderService;

    @EJB
    private PerformanceMetricsBean metrics;

    @Override
    public void onMessage(Message message) {
        try {
            String action = message.getStringProperty(JmsConstants.PROP_ACTION);
            long orderId = message.getLongProperty(JmsConstants.PROP_ORDER_ID);
            LOG.fine(() -> "OrderProcessingMDB received action=" + action + " orderId=" + orderId);

            if (action == null) {
                return;
            }
            switch (action) {
                case JmsConstants.ACTION_AWAITING_PAYMENT ->

                        LOG.fine(() -> "Order " + orderId + " awaiting payment");
                case JmsConstants.ACTION_PAYMENT_SUCCESS -> processPaidOrder(orderId);
                case JmsConstants.ACTION_PAYMENT_FAILED -> {
                    orderService.cancelOrder(orderId, null);
                    LOG.info("Order " + orderId + " cancelled after payment failure");
                }
                case JmsConstants.ACTION_CANCEL ->

                        LOG.fine(() -> "Order " + orderId + " cancellation acknowledged");
                default -> LOG.warning("Unknown order action: " + action);
            }
        } catch (Exception e) {

            metrics.recordOrderFailed();
            LOG.log(Level.SEVERE, "Error processing order message", e);
        }
    }

    private void processPaidOrder(long orderId) throws Exception {
        Future<OrderDTO> future = orderService.processOrderAsync(orderId);
        OrderDTO dto = future.get(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        LOG.info("Order " + dto.getOrderNumber() + " processed asynchronously after payment");
    }
}
