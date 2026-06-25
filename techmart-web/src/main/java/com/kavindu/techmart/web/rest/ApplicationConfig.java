package com.kavindu.techmart.web.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@OpenAPIDefinition(
        info = @Info(
                title = "TechMart Online API",
                version = "1.0.0",
                description = "Jakarta EE 10 e-commerce REST API. " +
                        "Click **Authorize** and enter your token (obtained from POST /api/auth/login) " +
                        "to access secured endpoints.",
                contact = @Contact(name = "TechMart", email = "kavindu4543@gmail.com")
        ),
        tags = {
                @Tag(name = "Auth",      description = "Authentication and user profile"),
                @Tag(name = "Products",  description = "Product catalog, search and categories"),
                @Tag(name = "Cart",      description = "Session-based shopping cart"),
                @Tag(name = "Orders",    description = "Order placement and tracking"),
                @Tag(name = "Payments",  description = "Payment initiation and gateway callbacks"),
                @Tag(name = "Inventory", description = "Warehouse inventory management (Admin)"),
                @Tag(name = "Metrics",   description = "System metrics and admin controls (Admin/Developer)")
        },
        components = @Components(
                securitySchemes = @SecurityScheme(
                        securitySchemeName = "BearerAuth",
                        type = SecuritySchemeType.HTTP,
                        scheme = "bearer",
                        bearerFormat = "token",
                        description = "Enter the token received from POST /api/auth/login"
                )
        )
)
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
}
