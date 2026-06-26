package com.kavindu.techmart.web.rest.request;

public class UpdateCartItemRequest {

    private int quantity;

    public UpdateCartItemRequest() {
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
