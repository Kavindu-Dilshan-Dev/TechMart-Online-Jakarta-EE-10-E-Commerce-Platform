package com.kavindu.techmart.common.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true, inherited = true)
public class AuthException extends TechMartException {

    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }
}
