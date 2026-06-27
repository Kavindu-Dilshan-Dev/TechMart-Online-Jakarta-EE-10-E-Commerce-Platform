package com.kavindu.techmart.web.rest.mapper;

import com.kavindu.techmart.common.dto.ApiResponse;
import com.kavindu.techmart.common.exception.AccessDeniedException;
import com.kavindu.techmart.common.exception.AuthException;
import com.kavindu.techmart.common.exception.CircuitOpenException;
import com.kavindu.techmart.common.exception.InsufficientStockException;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.exception.TechMartException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TechMartExceptionMapper implements ExceptionMapper<TechMartException> {

    @Override
    public Response toResponse(TechMartException ex) {
        return build(ex);
    }

    public static Response build(TechMartException ex) {
        Response.Status status;
        String code;
        if (ex instanceof AuthException) {
            status = Response.Status.UNAUTHORIZED;
            code = "UNAUTHORIZED";
        } else if (ex instanceof AccessDeniedException) {
            status = Response.Status.FORBIDDEN;
            code = "FORBIDDEN";
        } else if (ex instanceof ResourceNotFoundException) {
            status = Response.Status.NOT_FOUND;
            code = "NOT_FOUND";
        } else if (ex instanceof InsufficientStockException) {
            status = Response.Status.CONFLICT;
            code = "INSUFFICIENT_STOCK";
        } else if (ex instanceof CircuitOpenException) {
            status = Response.Status.SERVICE_UNAVAILABLE;
            code = "SERVICE_UNAVAILABLE";
        } else {
            status = Response.Status.BAD_REQUEST;
            code = "BAD_REQUEST";
        }
        return Response.status(status)
                .entity(ApiResponse.fail(ex.getMessage(), code))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
