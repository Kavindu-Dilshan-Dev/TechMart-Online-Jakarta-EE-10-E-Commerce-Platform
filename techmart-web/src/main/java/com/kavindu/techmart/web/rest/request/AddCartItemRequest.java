package com.kavindu.techmart.web.rest.request;

public class AddCartItemRequest {

    private Long productId;
    private int quantity;

    public AddCartItemRequest() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
