package com.kavindu.techmart.common.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true, inherited = true)
public class TechMartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TechMartException(String message) {
        super(message);
    }

    public TechMartException(String message, Throwable cause) {
        super(message, cause);
    }
}
