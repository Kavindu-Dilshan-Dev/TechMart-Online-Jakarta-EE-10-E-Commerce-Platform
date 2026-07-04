package com.kavindu.techmart.web.filter;

import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.web.metrics.MicrometerRegistryProducer;
import io.micrometer.core.instrument.Timer;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.concurrent.TimeUnit;

@Provider
public class RequestMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME = "techmart.req.start";

    @EJB
    private PerformanceMetricsBean metrics;

    @Inject
    private MicrometerRegistryProducer registryProducer;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_TIME, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object start = requestContext.getProperty(START_TIME);
        if (start instanceof Long startNanos) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String uri    = normalise(requestContext.getUriInfo().getPath());
            String method = requestContext.getMethod();
            String endpoint = method + " /" + uri;

            metrics.recordRequest(endpoint, elapsedMs);

            Timer.builder("techmart.http.request.duration")
                 .tag("method", method)
                 .tag("uri", uri)
                 .tag("status", String.valueOf(responseContext.getStatus()))
                 .description("HTTP API request duration")
                 .register(registryProducer.getRegistry())
                 .record(elapsedMs, TimeUnit.MILLISECONDS);
        }
    }

    private static String normalise(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.replaceAll("(?<=/|^)\\d+(?=/|$)", "{id}");
    }
}
