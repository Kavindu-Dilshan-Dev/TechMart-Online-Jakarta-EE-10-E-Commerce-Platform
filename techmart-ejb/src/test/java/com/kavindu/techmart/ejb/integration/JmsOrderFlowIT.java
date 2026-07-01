package com.kavindu.techmart.ejb.integration;

import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.dto.OrderItemDTO;
import com.kavindu.techmart.common.dto.PaymentDTO;
import com.kavindu.techmart.common.interfaces.OrderServiceLocal;
import com.kavindu.techmart.common.interfaces.PaymentServiceLocal;
import com.kavindu.techmart.ejb.mdb.OrderProcessingMDB;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import com.kavindu.techmart.ejb.session.stateless.InventoryReservationHelper;
import com.kavindu.techmart.ejb.session.stateless.InventoryServiceBean;
import com.kavindu.techmart.ejb.session.stateless.NotificationServiceBean;
import com.kavindu.techmart.ejb.session.stateless.OrderServiceBean;
import com.kavindu.techmart.ejb.session.stateless.PaymentServiceBean;
import com.kavindu.techmart.ejb.util.JmsConstants;
import com.kavindu.techmart.ejb.util.JsonUtil;
import com.kavindu.techmart.ejb.util.Mappers;
import com.kavindu.techmart.ejb.websocket.WebSocketSessionRegistry;
import jakarta.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(ArquillianExtension.class)
class JmsOrderFlowIT {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "techmart-jms-it.war")
                .addPackages(true, "com.kavindu.techmart.common")
                .addClasses(OrderServiceBean.class, OrderProcessingMDB.class,
                        InventoryServiceBean.class, InventoryReservationHelper.class,
                        NotificationServiceBean.class, PaymentServiceBean.class,
                        SystemConfigBean.class, PerformanceMetricsBean.class,
                        CircuitBreakerBean.class, WebSocketSessionRegistry.class,
                        Mappers.class, JmsConstants.class, JsonUtil.class)
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @EJB
    private OrderServiceLocal orderService;

    @EJB
    private PaymentServiceLocal paymentService;

    @Test
    void orderReachesProcessingAfterPaymentViaMdb() throws Exception {
        OrderDTO order = orderService.placeOrder(2L,
                List.of(new OrderItemDTO(1L, 1)), "1 Test Road, Colombo");
        Assertions.assertEquals("PENDING", order.getStatus());

        PaymentDTO payment = paymentService.initiatePayment(order.getId(), "VISA");
        paymentService.handleCallback(order.getId(), "SUCCESS", payment.getPaymentReference());

        String status = pollStatus(order.getId(), "PROCESSING", 10_000);
        Assertions.assertEquals("PROCESSING", status, "MDB should advance the order to PROCESSING");
    }

    private String pollStatus(Long orderId, String target, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            status = orderService.findById(orderId).getStatus();
            if (target.equals(status)) {
                return status;
            }
            Thread.sleep(250);
        }
        return status;
    }
}
