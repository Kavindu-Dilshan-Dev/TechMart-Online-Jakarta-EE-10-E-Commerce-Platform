package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.OrderDTO;
import com.kavindu.techmart.common.enums.OrderStatus;
import com.kavindu.techmart.common.exception.ResourceNotFoundException;
import com.kavindu.techmart.common.interfaces.OrderServiceLocal;
import com.kavindu.techmart.web.metrics.BusinessMetrics;
import com.kavindu.techmart.web.session.CartSession;
import com.kavindu.techmart.web.rest.request.OrderStatusUpdateRequest;
import com.kavindu.techmart.web.rest.request.PlaceOrderRequest;
import com.kavindu.techmart.web.security.RequestContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Orders")
@SecurityRequirement(name = "BearerAuth")
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @EJB
    private OrderServiceLocal orderService;

    @Inject
    private CartSession cart;

    @Inject
    private RequestContext requestContext;

    @Inject
    private BusinessMetrics businessMetrics;

    @POST
    @Operation(summary = "Place a new order")
    public Response placeOrder(PlaceOrderRequest request) {
        requestContext.requireAuthenticated();
        Long userId = requestContext.getUserId();
        OrderDTO order = orderService.placeOrder(userId, request.getItems(), request.getShippingAddress());
        businessMetrics.recordOrderPlaced();
        try {
            cart.checkout();
        } catch (RuntimeException ignored) {

        }
        return RestSupport.created(order, "Order placed");
    }

    @GET
    @Operation(summary = "List the current user's orders")
    public Response getMyOrders() {
        requestContext.requireAuthenticated();
        return RestSupport.ok(orderService.findOrdersByUser(requestContext.getUserId()));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a specific order by ID")
    public Response getOrder(@PathParam("id") Long id) {
        requestContext.requireAuthenticated();
        Long userId = requestContext.getUserId();
        OrderDTO order = orderService.findById(id);
        if (!requestContext.hasRole("ADMIN") && !order.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        return RestSupport.ok(order);
    }

    @POST
    @Path("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public Response cancelOrder(@PathParam("id") Long id) {
        requestContext.requireAuthenticated();
        OrderDTO order = orderService.cancelOrder(id, requestContext.getUserId());
        businessMetrics.recordOrderCancelled();
        return RestSupport.ok(order, "Order cancelled");
    }

    @GET
    @Path("/admin/all")
    @Operation(summary = "List all orders (Admin)")
    public Response getAllOrders() {
        requestContext.requireRole("ADMIN");
        return RestSupport.ok(orderService.findAllOrders());
    }

    @GET
    @Path("/admin/status/{status}")
    @Operation(summary = "List orders by status (Admin)",
               description = "Valid statuses: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED")
    public Response getOrdersByStatus(@PathParam("status") String status) {
        requestContext.requireRole("ADMIN");
        return RestSupport.ok(orderService.findOrdersByStatus(OrderStatus.valueOf(status.toUpperCase())));
    }

    @PUT
    @Path("/admin/{id}/status")
    @Operation(summary = "Update an order's status (Admin)")
    public Response updateOrderStatus(@PathParam("id") Long id, OrderStatusUpdateRequest request) {
        requestContext.requireRole("ADMIN");
        OrderDTO order = orderService.updateOrderStatus(id, OrderStatus.valueOf(request.getStatus().toUpperCase()));
        return RestSupport.ok(order, "Order status updated");
    }
}
