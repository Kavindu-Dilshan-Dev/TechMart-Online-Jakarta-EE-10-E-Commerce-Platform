package com.kavindu.techmart.web.rest.request;

import com.kavindu.techmart.common.dto.OrderItemDTO;

import java.util.List;

public class PlaceOrderRequest {

    private List<OrderItemDTO> items;
    private String shippingAddress;

    public PlaceOrderRequest() {
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
