package com.kavindu.techmart.common.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true, inherited = true)
public class InsufficientStockException extends TechMartException {

    private static final long serialVersionUID = 1L;

    private final Long productId;

    public InsufficientStockException(Long productId, String message) {
        super(message);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
