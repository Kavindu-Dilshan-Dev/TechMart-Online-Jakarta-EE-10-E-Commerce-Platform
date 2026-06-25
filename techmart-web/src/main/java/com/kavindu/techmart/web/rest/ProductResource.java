package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.ProductDTO;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.common.interfaces.ProductServiceLocal;
import com.kavindu.techmart.web.security.RequestContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Products")
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @EJB
    private ProductServiceLocal productService;

    @EJB
    private NotificationServiceLocal notificationService;

    @Inject
    private RequestContext requestContext;

    @GET
    @Operation(summary = "List or search active products",
               description = "Returns all active products. Provide any filter parameter to activate search mode.")
    public Response list(
            @Parameter(description = "Page number (0-based)") @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size") @QueryParam("size") @DefaultValue("20") int size,
            @Parameter(description = "Full-text search keyword") @QueryParam("keyword") String keyword,
            @Parameter(description = "Filter by category ID") @QueryParam("category") Long category,
            @Parameter(description = "Minimum price filter") @QueryParam("minPrice") BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @QueryParam("maxPrice") BigDecimal maxPrice) {
        List<ProductDTO> products;
        if (keyword != null || category != null || minPrice != null || maxPrice != null) {
            products = productService.searchProducts(keyword, category, minPrice, maxPrice, page, size);
        } else {
            products = productService.findAllActive(page, size);
        }
        return RestSupport.ok(products);
    }

    @GET
    @Path("/count")
    @Operation(summary = "Total count of active products")
    public Response count() {
        return RestSupport.ok(productService.countActive());
    }

    @GET
    @Path("/categories")
    @Operation(summary = "List all product categories")
    public Response categories() {
        return RestSupport.ok(productService.getAllCategories());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get a product by ID")
    public Response findById(@PathParam("id") Long id) {
        return RestSupport.ok(productService.findById(id));
    }

    @GET
    @Path("/category/{categoryId}")
    @Operation(summary = "List products by category")
    public Response findByCategory(@PathParam("categoryId") Long categoryId) {
        return RestSupport.ok(productService.findByCategory(categoryId));
    }

    @POST
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new product (Admin)")
    public Response create(ProductDTO dto) {
        requestContext.requireRole("ADMIN");
        ProductDTO created = productService.createProduct(dto);
        return RestSupport.created(created, "Product created");
    }

    @PUT
    @Path("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update an existing product (Admin)")
    public Response update(@PathParam("id") Long id, ProductDTO dto) {
        requestContext.requireRole("ADMIN");
        ProductDTO updated = productService.updateProduct(id, dto);
        return RestSupport.ok(updated, "Product updated");
    }

    @DELETE
    @Path("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Soft-delete a product (Admin)")
    public Response delete(@PathParam("id") Long id) {
        requestContext.requireRole("ADMIN");
        productService.deleteProduct(id);
        return RestSupport.message("Product deleted");
    }

    @POST
    @Path("/{id}/stock-alert")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Subscribe to a back-in-stock alert for a product")
    public Response createStockAlert(@PathParam("id") Long id) {
        requestContext.requireRole("CUSTOMER");
        notificationService.createStockAlert(requestContext.getUserId(), id);
        return RestSupport.message("Stock alert created");
    }

    @DELETE
    @Path("/{id}/stock-alert")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Cancel a stock alert subscription")
    public Response removeStockAlert(@PathParam("id") Long id) {
        requestContext.requireRole("CUSTOMER");
        notificationService.removeStockAlert(requestContext.getUserId(), id);
        return RestSupport.message("Stock alert removed");
    }
}
