package com.kavindu.techmart.common.interfaces;

import com.kavindu.techmart.common.dto.PayHereCheckoutDTO;
import com.kavindu.techmart.common.dto.PaymentDTO;

import java.util.Map;
import java.util.concurrent.Future;

public interface PaymentServiceLocal {

    PayHereCheckoutDTO preparePayHerePayment(Long orderId);

    void handleNotify(Map<String, String> params);

    PaymentDTO initiatePayment(Long orderId, String method);

    PaymentDTO handleCallback(Long orderId, String status, String reference);

    Future<PaymentDTO> verifyPaymentAsync(String reference);

    PaymentDTO getPaymentForOrder(Long orderId);

    PaymentDTO findByReference(String reference);
}
