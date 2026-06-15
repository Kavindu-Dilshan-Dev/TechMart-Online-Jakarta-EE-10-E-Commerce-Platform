package com.kavindu.techmart.common.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false, inherited = true)
public class AccessDeniedException extends TechMartException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
