package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.enums.OrderStatus;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.common.interfaces.OrderServiceLocal;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import com.kavindu.techmart.ejb.session.singleton.PerformanceMetricsBean;
import com.kavindu.techmart.ejb.session.singleton.SystemConfigBean;
import com.kavindu.techmart.ejb.websocket.WebSocketSessionRegistry;
import com.kavindu.techmart.web.rest.request.BroadcastRequest;
import com.kavindu.techmart.web.rest.request.ToggleRequest;
import com.kavindu.techmart.web.security.RequestContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Metrics")
@SecurityRequirement(name = "BearerAuth")
@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource {

    @EJB
    private PerformanceMetricsBean metrics;

    @EJB
    private CircuitBreakerBean circuitBreaker;

    @EJB
    private OrderServiceLocal orderService;

    @EJB
    private NotificationServiceLocal notificationService;

    @EJB
    private SystemConfigBean systemConfig;

    @EJB
    private WebSocketSessionRegistry registry;

    @Inject
    private RequestContext requestContext;

    @GET
    @Operation(summary = "Get application performance metrics (Admin/Developer)")
    public Response getMetrics() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        return RestSupport.ok(metrics.getMetrics());
    }

    @GET
    @Path("/jvm")
    @Operation(summary = "Get JVM memory and thread metrics (Admin/Developer)")
    public Response getJvmMetrics() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        return RestSupport.ok(metrics.getJvmMetrics());
    }

    @GET
    @Path("/samples")
    @Operation(summary = "Get recent performance sample history (Admin/Developer)")
    public Response getRecentSamples() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        return RestSupport.ok(metrics.getRecentSamples());
    }

    @GET
    @Path("/uptime")
    @Operation(summary = "Get server uptime and active user count (Admin/Developer)")
    public Response getUptime() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uptime", metrics.getMetrics().getUptimeFormatted());
        data.put("uptimeMillis", metrics.getUptimeMillis());
        data.put("activeUsers", metrics.getActiveUsers());
        return RestSupport.ok(data);
    }

    @GET
    @Path("/circuit-breakers")
    @Operation(summary = "Get state of all circuit breakers (Admin/Developer)")
    public Response getCircuitBreakers() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        return RestSupport.ok(circuitBreaker.getAllStatesAsString());
    }

    @GET
    @Path("/orders")
    @Operation(summary = "Get order counts by status and today's revenue (Admin/Developer)")
    public Response getOrderMetrics() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending", orderService.countByStatus(OrderStatus.PENDING));
        data.put("processing", orderService.countByStatus(OrderStatus.PROCESSING));
        data.put("shipped", orderService.countByStatus(OrderStatus.SHIPPED));
        data.put("delivered", orderService.countByStatus(OrderStatus.DELIVERED));
        data.put("cancelled", orderService.countByStatus(OrderStatus.CANCELLED));
        data.put("revenueToday", orderService.getRevenueSince(LocalDate.now().atStartOfDay()));
        return RestSupport.ok(data);
    }

    @GET
    @Path("/websocket")
    @Operation(summary = "Get active WebSocket connection and user counts (Admin/Developer)")
    public Response getWebSocketMetrics() {
        requestContext.requireRole("ADMIN", "DEVELOPER");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("connections", registry.getConnectionCount());
        data.put("users", registry.getUserCount());
        return RestSupport.ok(data);
    }

    @POST
    @Path("/circuit-breakers/{service}/reset")
    @Operation(summary = "Manually reset a circuit breaker to CLOSED state (Admin)")
    public Response resetCircuitBreaker(@PathParam("service") String service) {
        requestContext.requireRole("ADMIN");
        circuitBreaker.reset(service);
        return RestSupport.message("Circuit breaker '" + service + "' reset");
    }

    @POST
    @Path("/broadcast")
    @Operation(summary = "Publish a system broadcast notification to all connected users (Admin)")
    public Response broadcast(BroadcastRequest request) {
        requestContext.requireRole("ADMIN");
        notificationService.publishSystemBroadcast(request.getTitle(), request.getMessage());
        return RestSupport.message("Broadcast published");
    }

    @POST
    @Path("/simulate/payment-failure")
    @Operation(summary = "Toggle payment failure simulation for testing (Admin)")
    public Response simulatePaymentFailure(ToggleRequest request) {
        requestContext.requireRole("ADMIN");
        systemConfig.setConfig("payhere.simulate.failure", String.valueOf(request.isEnabled()));
        return RestSupport.message("Payment failure simulation set to " + request.isEnabled());
    }
}
