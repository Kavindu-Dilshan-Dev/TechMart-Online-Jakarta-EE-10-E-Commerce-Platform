package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.PayHereCheckoutDTO;
import com.kavindu.techmart.common.dto.PaymentDTO;
import com.kavindu.techmart.common.interfaces.PaymentServiceLocal;
import com.kavindu.techmart.web.metrics.BusinessMetrics;
import com.kavindu.techmart.web.rest.request.PaymentCallbackRequest;
import com.kavindu.techmart.web.rest.request.PaymentInitiateRequest;
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
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Tag(name = "Payments")
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @EJB
    private PaymentServiceLocal paymentService;

    @Inject
    private RequestContext requestContext;

    @Inject
    private BusinessMetrics businessMetrics;

    @POST
    @Path("/payhere/start")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Prepare a PayHere checkout session for an order")
    public Response startPayHere(PaymentInitiateRequest request) {
        requestContext.requireAuthenticated();
        PayHereCheckoutDTO dto = paymentService.preparePayHerePayment(request.getOrderId());
        return RestSupport.ok(dto);
    }

    @POST
    @Path("/notify")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "PayHere IPN webhook callback (called by gateway)")
    public Response notify(MultivaluedMap<String, String> form) {
        Map<String, String> params = new HashMap<>();
        if (form != null) {
            form.forEach((k, v) -> params.put(k, (v != null && !v.isEmpty()) ? v.get(0) : null));
        }
        paymentService.handleNotify(params);
        // PayHere status_code: 2=success, -1=cancelled, -2=failed, -3=chargedback
        if ("2".equals(params.get("status_code"))) {
            businessMetrics.recordPaymentSucceeded();
        } else {
            businessMetrics.recordPaymentFailed();
        }
        return Response.ok("OK").build();
    }

    @POST
    @Path("/initiate")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Initiate a payment for an order")
    public Response initiate(PaymentInitiateRequest request) {
        requestContext.requireAuthenticated();
        PaymentDTO dto = paymentService.initiatePayment(request.getOrderId(), request.getMethod());
        businessMetrics.recordPaymentInitiated();
        return RestSupport.ok(dto);
    }

    @POST
    @Path("/callback")
    @Operation(summary = "Handle a payment gateway status callback")
    public Response callback(PaymentCallbackRequest request) {
        PaymentDTO dto = paymentService.handleCallback(
                request.getOrderId(), request.getStatus(), request.getReference());
        return RestSupport.ok(dto);
    }

    @GET
    @Path("/verify/{reference}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Verify payment status by reference (async, 15s timeout)")
    public Response verify(@PathParam("reference") String reference)
            throws ExecutionException, InterruptedException, TimeoutException {
        requestContext.requireAuthenticated();
        Future<PaymentDTO> f = paymentService.verifyPaymentAsync(reference);
        PaymentDTO dto = f.get(15, TimeUnit.SECONDS);
        return RestSupport.ok(dto);
    }

    @GET
    @Path("/order/{orderId}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get the payment record for a given order")
    public Response forOrder(@PathParam("orderId") Long orderId) {
        requestContext.requireAuthenticated();
        PaymentDTO dto = paymentService.getPaymentForOrder(orderId);
        return RestSupport.ok(dto);
    }
}
