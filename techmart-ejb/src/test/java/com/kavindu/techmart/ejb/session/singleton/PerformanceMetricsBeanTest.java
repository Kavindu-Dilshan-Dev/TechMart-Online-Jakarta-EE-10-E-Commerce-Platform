package com.kavindu.techmart.ejb.session.singleton;

import com.kavindu.techmart.common.dto.MetricsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceMetricsBeanTest {

    private PerformanceMetricsBean metrics;

    @BeforeEach
    void setUp() {
        metrics = new PerformanceMetricsBean();
        metrics.init();
    }

    @Test
    @DisplayName("Active-user counter increments and decrements, never below zero")
    void incrementDecrement_activeUsers() {
        metrics.incrementActiveUsers();
        metrics.incrementActiveUsers();
        metrics.incrementActiveUsers();
        assertEquals(3, metrics.getActiveUsers());
        metrics.decrementActiveUsers();
        assertEquals(2, metrics.getActiveUsers());
        metrics.decrementActiveUsers();
        metrics.decrementActiveUsers();
        metrics.decrementActiveUsers();
        assertEquals(0, metrics.getActiveUsers());
    }

    @Test
    @DisplayName("recordRequest updates totals, averages and per-endpoint stats")
    void recordRequest_updatesStats() {
        metrics.recordRequest("GET /products", 100);
        metrics.recordRequest("GET /products", 200);
        assertEquals(2, metrics.getTotalRequests());
        assertEquals(150.0, metrics.getAverageResponseTimeMs(), 0.001);

        MetricsDTO dto = metrics.getMetrics();
        assertTrue(dto.getEndpointAverageMs().containsKey("GET /products"));
        assertEquals(2L, dto.getEndpointCounts().get("GET /products"));
    }

    @Test
    @DisplayName("Order counters are tracked")
    void orderCounters() {
        metrics.recordOrderProcessed();
        metrics.recordOrderProcessed();
        metrics.recordOrderFailed();
        MetricsDTO dto = metrics.getMetrics();
        assertEquals(2L, dto.getTotalOrdersProcessed());
        assertEquals(1L, dto.getTotalOrdersFailed());
    }

    @Test
    @DisplayName("getMetrics returns a populated JVM snapshot")
    void getMetrics_returnsData() {
        MetricsDTO dto = metrics.getMetrics();
        assertNotNull(dto);
        assertTrue(dto.getHeapMaxMb() > 0, "heap max should be positive");
        assertTrue(dto.getThreadCount() > 0, "thread count should be positive");
        assertTrue(dto.getUptimeMillis() >= 0);
        assertEquals(10000, dto.getMaxConcurrentUsers());
        assertNotNull(dto.getUptimeFormatted());
    }
}
