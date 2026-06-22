package com.kavindu.techmart.ejb.session.stateless;

import com.kavindu.techmart.common.dto.PayHereCheckoutDTO;
import com.kavindu.techmart.common.dto.PaymentDTO;
import com.kavindu.techmart.common.entity.Order;
import com.kavindu.techmart.common.entity.Payment;
import com.kavindu.techmart.common.entity.User;
import com.kavindu.techmart.common.enums.PaymentStatus;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.exception.TechMartException;
import com.kavindu.techmart.common.interfaces.PaymentServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import com.kavindu.techmart.ejb.util.JmsConstants;
import com.kavindu.techmart.ejb.util.Mappers;
import com.kavindu.techmart.ejb.util.PayHereUtil;
import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

@Stateless
@Local(PaymentServiceLocal.class)
public class PaymentServiceBean implements PaymentServiceLocal {

    private static final Logger LOG = Logger.getLogger(PaymentServiceBean.class.getName());
    private static final String SERVICE = "payhere";

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private CircuitBreakerBean circuitBreaker;

    @EJB
    private SystemConfigBean systemConfig;

    @Inject
    @JMSConnectionFactory("java:/jms/cf/TechMart")
    private JMSContext jmsContext;

    @Resource(lookup = JmsConstants.ORDER_QUEUE)
    private Queue orderQueue;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PayHereCheckoutDTO preparePayHerePayment(Long orderId) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        Payment payment = existingPaymentFor(orderId);
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentReference(generateReference());
            payment.setAmount(order.getTotalAmount());
            payment.setCurrency("LKR");
        }
        payment.setPaymentMethod("PAYHERE");
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setGatewayResponse("PayHere checkout prepared (sandbox=" + systemConfig.isPayhereSandbox() + ")");
        em.persist(payment);
        em.flush();

        String merchantId = systemConfig.getPayhereMerchantId();
        String currency = payment.getCurrency();
        String amount = order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String orderNo = order.getOrderNumber();
        String hash = PayHereUtil.checkoutHash(merchantId, orderNo, amount, currency,
                systemConfig.getPayhereMerchantSecret());
        String base = systemConfig.getPublicBaseUrl();

        User user = order.getUser();
        PayHereCheckoutDTO dto = new PayHereCheckoutDTO();
        dto.setSandbox(systemConfig.isPayhereSandbox());
        dto.setMerchantId(merchantId);
        dto.setOrderId(orderNo);
        dto.setPaymentReference(payment.getPaymentReference());
        dto.setItems("TechMart Order " + orderNo);
        dto.setAmount(amount);
        dto.setCurrency(currency);
        dto.setHash(hash);
        dto.setReturnUrl(base + "/order-confirmation.html?id=" + order.getId());
        dto.setCancelUrl(base + "/checkout.html");
        dto.setNotifyUrl(base + "/api/payments/notify");
        dto.setFirstName(user != null && user.getFirstName() != null ? user.getFirstName() : "Customer");
        dto.setLastName(user != null && user.getLastName() != null ? user.getLastName() : "TechMart");
        dto.setEmail(user != null ? user.getEmail() : "customer@techmart.lk");
        dto.setPhone(user != null && user.getPhone() != null ? user.getPhone() : "0770000000");
        dto.setAddress(order.getShippingAddress() != null ? order.getShippingAddress() : "N/A");
        dto.setCity("Colombo");
        dto.setCountry("Sri Lanka");

        LOG.info("Prepared PayHere checkout for order " + orderNo + " (ref " + payment.getPaymentReference() + ")");
        return dto;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void handleNotify(Map<String, String> params) {
        String merchantId = params.get("merchant_id");
        String orderNo = params.get("order_id");
        String payhereAmount = params.get("payhere_amount");
        String payhereCurrency = params.get("payhere_currency");
        String statusCode = params.get("status_code");
        String md5sig = params.get("md5sig");
        String paymentId = params.get("payment_id");

        boolean valid = PayHereUtil.verifyNotifySig(merchantId, orderNo, payhereAmount,
                payhereCurrency, statusCode, systemConfig.getPayhereMerchantSecret(), md5sig);
        if (!valid) {
            LOG.warning("PayHere notify REJECTED: md5sig mismatch for order " + orderNo);
            return;
        }

        Payment payment = paymentByOrderNumber(orderNo);
        if (payment == null) {
            LOG.warning("PayHere notify for unknown order " + orderNo);
            return;
        }

        if ("2".equals(statusCode)) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setGatewayTransactionId(paymentId);
            payment.setGatewayResponse("PayHere notify: success");
            em.flush();
            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_PAYMENT_SUCCESS)
                    .setProperty(JmsConstants.PROP_ORDER_ID, payment.getOrder().getId().longValue())
                    .send(orderQueue, "");
            LOG.info("PayHere notify: order " + orderNo + " PAID (payment_id=" + paymentId + ")");
        } else if ("0".equals(statusCode)) {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setGatewayResponse("PayHere notify: pending");
            em.flush();
            LOG.info("PayHere notify: order " + orderNo + " pending");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("PayHere notify: status_code=" + statusCode);
            em.flush();
            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_PAYMENT_FAILED)
                    .setProperty(JmsConstants.PROP_ORDER_ID, payment.getOrder().getId().longValue())
                    .send(orderQueue, "");
            LOG.warning("PayHere notify: order " + orderNo + " FAILED/cancelled (status_code=" + statusCode + ")");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PaymentDTO initiatePayment(Long orderId, String method) {
        Order order = em.find(Order.class, orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }

        Payment payment = existingPaymentFor(orderId);
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentReference(generateReference());
            payment.setAmount(order.getTotalAmount());
            payment.setCurrency("LKR");
            payment.setStatus(PaymentStatus.PENDING);
        }
        payment.setPaymentMethod(method);
        final Payment current = payment;

        String paymentUrl = circuitBreaker.callWithBreaker(SERVICE,
                () -> simulatePayHereInit(order, current));

        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setGatewayResponse("Checkout session created (sandbox="
                + systemConfig.isPayhereSandbox() + ")");
        em.persist(payment);
        em.flush();

        PaymentDTO dto = Mappers.toPaymentDTO(payment);
        dto.setPaymentUrl(paymentUrl);
        LOG.info("Initiated payment " + payment.getPaymentReference() + " for order " + orderId);
        return dto;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PaymentDTO handleCallback(Long orderId, String status, String reference) {
        Payment payment = reference != null ? findByReferenceEntity(reference) : existingPaymentFor(orderId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found for order " + orderId);
        }
        boolean success = status != null
                && (status.equalsIgnoreCase("SUCCESS") || status.equalsIgnoreCase("COMPLETED"));

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setGatewayTransactionId("PH" + System.currentTimeMillis());
            em.flush();
            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_PAYMENT_SUCCESS)
                    .setProperty(JmsConstants.PROP_ORDER_ID, payment.getOrder().getId().longValue())
                    .send(orderQueue, "");
            LOG.info("Payment SUCCESS for order " + payment.getOrder().getOrderNumber());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse("Gateway reported failure");
            em.flush();

            jmsContext.createProducer()
                    .setProperty(JmsConstants.PROP_ACTION, JmsConstants.ACTION_PAYMENT_FAILED)
                    .setProperty(JmsConstants.PROP_ORDER_ID, payment.getOrder().getId().longValue())
                    .send(orderQueue, "");
            LOG.warning("Payment FAILED for order " + payment.getOrder().getOrderNumber());
        }
        return Mappers.toPaymentDTO(payment);
    }

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Future<PaymentDTO> verifyPaymentAsync(String reference) {
        Payment payment = findByReferenceEntity(reference);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found: " + reference);
        }

        String verified = circuitBreaker.callWithBreaker(SERVICE,
                () -> "VERIFIED:" + payment.getStatus());
        LOG.fine(() -> "Async verification for " + reference + " -> " + verified);
        return new AsyncResult<>(Mappers.toPaymentDTO(payment));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PaymentDTO getPaymentForOrder(Long orderId) {
        Payment payment = existingPaymentFor(orderId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found for order " + orderId);
        }
        return Mappers.toPaymentDTO(payment);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PaymentDTO findByReference(String reference) {
        Payment payment = findByReferenceEntity(reference);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found: " + reference);
        }
        return Mappers.toPaymentDTO(payment);
    }

    private String simulatePayHereInit(Order order, Payment payment) {
        if (systemConfig.getBooleanConfig("payhere.simulate.failure", false)) {
            throw new TechMartException("Simulated PayHere gateway timeout");
        }
        String base = systemConfig.isPayhereSandbox()
                ? "https://sandbox.payhere.lk/pay/checkout"
                : "https://www.payhere.lk/pay/checkout";
        return base + "?merchant_id=" + systemConfig.getPayhereMerchantId()
                + "&order_id=" + order.getOrderNumber()
                + "&ref=" + payment.getPaymentReference()
                + "&amount=" + order.getTotalAmount()
                + "&currency=" + payment.getCurrency();
    }

    private Payment existingPaymentFor(Long orderId) {
        try {
            return em.createNamedQuery("Payment.findByOrder", Payment.class)
                    .setParameter("orderId", orderId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private Payment paymentByOrderNumber(String orderNumber) {
        try {
            return em.createQuery(
                            "SELECT p FROM Payment p WHERE p.order.orderNumber = :no", Payment.class)
                    .setParameter("no", orderNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private Payment findByReferenceEntity(String reference) {
        try {
            return em.createNamedQuery("Payment.findByReference", Payment.class)
                    .setParameter("reference", reference)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private String generateReference() {
        return "PAY-" + System.currentTimeMillis() + "-"
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
