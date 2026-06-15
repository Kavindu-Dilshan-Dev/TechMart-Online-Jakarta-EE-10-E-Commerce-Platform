package com.kavindu.techmart.common.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false, inherited = true)
public class CircuitOpenException extends TechMartException {

    private static final long serialVersionUID = 1L;

    private final String service;

    public CircuitOpenException(String service, String message) {
        super(message);
        this.service = service;
    }

    public String getService() {
        return service;
    }
}
