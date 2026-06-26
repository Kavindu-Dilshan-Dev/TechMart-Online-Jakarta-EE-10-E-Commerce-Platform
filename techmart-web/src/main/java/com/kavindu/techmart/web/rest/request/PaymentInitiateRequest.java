package com.kavindu.techmart.web.rest.request;

public class PaymentInitiateRequest {

    private Long orderId;
    private String method;

    public PaymentInitiateRequest() {
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
