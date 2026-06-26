package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.InventoryDTO;
import com.kavindu.techmart.common.interfaces.InventoryServiceLocal;
import com.kavindu.techmart.web.rest.request.InventoryUpdateRequest;
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

import java.util.List;

@Tag(name = "Inventory")
@SecurityRequirement(name = "BearerAuth")
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @EJB
    private InventoryServiceLocal inventoryService;

    @Inject
    private RequestContext requestContext;

    @GET
    @Path("/all")
    @Operation(summary = "List all inventory records across warehouses (Admin)")
    public Response getAllInventory() {
        requestContext.requireRole("ADMIN");
        List<InventoryDTO> inventory = inventoryService.getAllInventory();
        return RestSupport.ok(inventory);
    }

    @GET
    @Path("/product/{productId}")
    @Operation(summary = "Get inventory breakdown by warehouse for a product (Admin)")
    public Response getInventoryForProduct(@PathParam("productId") Long productId) {
        requestContext.requireRole("ADMIN");
        List<InventoryDTO> inventory = inventoryService.getInventoryForProduct(productId);
        return RestSupport.ok(inventory);
    }

    @GET
    @Path("/low-stock")
    @Operation(summary = "List products with stock below the alert threshold (Admin)")
    public Response getLowStockAlerts() {
        requestContext.requireRole("ADMIN");
        List<InventoryDTO> alerts = inventoryService.getLowStockAlerts();
        return RestSupport.ok(alerts);
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update the quantity for an inventory record (Admin)")
    public Response updateInventory(@PathParam("id") Long id, InventoryUpdateRequest request) {
        requestContext.requireRole("ADMIN");
        InventoryDTO updated = inventoryService.updateInventory(id, request.getQuantity());
        return RestSupport.ok(updated);
    }

    @POST
    @Path("/sync/{productId}")
    @Operation(summary = "Trigger async inventory sync across all warehouses for a product (Admin)")
    public Response syncInventory(@PathParam("productId") Long productId) {
        requestContext.requireRole("ADMIN");
        inventoryService.syncInventoryAcrossWarehouses(productId);
        return RestSupport.message("Sync queued");
    }
}
