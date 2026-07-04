package com.kavindu.techmart.web.metrics;

import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MicrometerRegistryProducer {

    @EJB
    private PerformanceMetricsBean performanceMetrics;

    private PrometheusMeterRegistry registry;

    @PostConstruct
    public void init() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Bridge existing PerformanceMetricsBean counters as Prometheus gauges.
        // Lambdas evaluate lazily at scrape time so startup ordering doesn't matter.
        Gauge.builder("techmart.requests.total",
                      () -> (double) performanceMetrics.getTotalRequests())
             .description("Total HTTP API requests handled since startup")
             .register(registry);

        Gauge.builder("techmart.active.users",
                      () -> (double) performanceMetrics.getActiveUsers())
             .description("Currently active authenticated users")
             .register(registry);

        Gauge.builder("techmart.response.time.avg.ms",
                      () -> performanceMetrics.getAverageResponseTimeMs())
             .description("Cumulative average API response time in milliseconds")
             .register(registry);
    }

    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }
}
