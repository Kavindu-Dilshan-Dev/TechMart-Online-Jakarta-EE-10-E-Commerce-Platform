package com.kavindu.techmart.web.filter;

import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import jakarta.ejb.EJB;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RequestMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME = "techmart.req.start";

    @EJB
    private PerformanceMetricsBean metrics;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object start = requestContext.getProperty(START_TIME);
        if (start instanceof Long startNanos) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String endpoint = requestContext.getMethod() + " /" + normalise(requestContext.getUriInfo().getPath());
            metrics.recordRequest(endpoint, elapsedMs);
        }
    }

    private static String normalise(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        return path.replaceAll("(?<=/|^)\\d+(?=/|$)", "{id}");
    }
}
