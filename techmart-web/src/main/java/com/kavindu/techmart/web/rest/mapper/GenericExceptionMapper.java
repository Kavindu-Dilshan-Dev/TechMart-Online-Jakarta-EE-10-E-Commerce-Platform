package com.kavindu.techmart.web.rest.mapper;

import com.kavindu.techmart.common.dto.ApiResponse;
import com.kavindu.techmart.common.exception.TechMartException;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        Throwable cause = unwrap(ex);

        if (cause instanceof TechMartException tme) {
            return TechMartExceptionMapper.build(tme);
        }
        if (cause instanceof WebApplicationException wae) {
            return Response.fromResponse(wae.getResponse())
                    .entity(ApiResponse.fail(wae.getMessage(), "HTTP_" + wae.getResponse().getStatus()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        LOG.log(Level.SEVERE, "Unhandled exception in REST layer", ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ApiResponse.fail("An unexpected error occurred", "INTERNAL_ERROR"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        int guard = 0;
        while (guard++ < 10 && current != null) {
            if (current instanceof TechMartException || current instanceof WebApplicationException) {
                return current;
            }
            Throwable next = (current instanceof EJBException ejb && ejb.getCausedByException() != null)
                    ? ejb.getCausedByException()
                    : current.getCause();
            if (next == null || next == current) {
                break;
            }
            current = next;
        }
        return current != null ? current : ex;
    }
}
