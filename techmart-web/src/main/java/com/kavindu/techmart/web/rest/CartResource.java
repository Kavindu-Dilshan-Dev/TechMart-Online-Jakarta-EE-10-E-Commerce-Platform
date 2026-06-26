package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.web.rest.request.AddCartItemRequest;
import com.kavindu.techmart.web.rest.request.UpdateCartItemRequest;
import com.kavindu.techmart.web.security.RequestContext;
import com.kavindu.techmart.web.session.CartSession;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Cart")
@SecurityRequirement(name = "BearerAuth")
@Path("/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartResource {

    @Inject
    private CartSession cart;

    @Inject
    private RequestContext requestContext;

    @GET
    @Operation(summary = "Get the current user's cart")
    public Response getCart() {
        ensureCart();
        return cartView();
    }

    @POST
    @Path("/items")
    @Operation(summary = "Add a product to the cart")
    public Response addItem(AddCartItemRequest request) {
        ensureCart();
        cart.addItem(request.getProductId(), request.getQuantity());
        return cartView();
    }

    @PUT
    @Path("/items/{productId}")
    @Operation(summary = "Update quantity of a cart item")
    public Response updateItem(@PathParam("productId") Long productId, UpdateCartItemRequest request) {
        ensureCart();
        cart.updateQuantity(productId, request.getQuantity());
        return cartView();
    }

    @DELETE
    @Path("/items/{productId}")
    @Operation(summary = "Remove a specific item from the cart")
    public Response removeItem(@PathParam("productId") Long productId) {
        ensureCart();
        cart.removeItem(productId);
        return cartView();
    }

    @DELETE
    @Operation(summary = "Clear all items from the cart")
    public Response clearCart() {
        ensureCart();
        cart.clearCart();
        return RestSupport.message("Cart cleared");
    }

    private void ensureCart() {
        requestContext.requireAuthenticated();
        if (cart.getUserId() == null) {
            cart.initCart(requestContext.getUserId());
        }
    }

    private Response cartView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("items", cart.getCartItems());
        view.put("total", cart.getCartTotal());
        view.put("itemCount", cart.getItemCount());
        return RestSupport.ok(view);
    }
}
