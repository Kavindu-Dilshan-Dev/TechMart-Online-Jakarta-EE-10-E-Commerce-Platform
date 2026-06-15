package com.kavindu.techmart.ejb.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class ConcurrencyConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConcurrencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
