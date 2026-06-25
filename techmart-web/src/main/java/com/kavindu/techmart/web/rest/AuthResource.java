package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.interfaces.AuthServiceLocal;
import com.kavindu.techmart.web.rest.request.ChangePasswordRequest;
import com.kavindu.techmart.web.rest.request.LoginRequest;
import com.kavindu.techmart.web.rest.request.RegisterRequest;
import com.kavindu.techmart.web.security.RequestContext;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Auth")
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @EJB
    private AuthServiceLocal authService;

    @Inject
    private RequestContext requestContext;

    @POST
    @Path("/login")
    @Operation(summary = "Login and obtain a session token")
    public Response login(LoginRequest request) {
        UserDTO user = authService.login(request.getUsername(), request.getPassword());
        return RestSupport.ok(user, "Login successful");
    }

    @POST
    @Path("/register")
    @Operation(summary = "Register a new customer account")
    public Response register(RegisterRequest request) {
        UserDTO dto = new UserDTO();
        dto.setUsername(request.getUsername());
        dto.setEmail(request.getEmail());
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setPhone(request.getPhone());
        UserDTO created = authService.register(dto, request.getPassword());
        return RestSupport.created(created, "Registration successful");
    }

    @POST
    @Path("/logout")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Invalidate the current session token")
    public Response logout() {
        if (requestContext.getToken() != null) {
            authService.logout(requestContext.getToken());
        }
        return RestSupport.message("Logged out");
    }

    @GET
    @Path("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get the currently authenticated user's profile")
    public Response me() {
        requestContext.requireAuthenticated();
        return RestSupport.ok(requestContext.getCurrentUser());
    }

    @PUT
    @Path("/me/password")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Change the authenticated user's password")
    public Response changePassword(ChangePasswordRequest request) {
        requestContext.requireAuthenticated();
        authService.changePassword(requestContext.getUserId(),
                request.getOldPassword(), request.getNewPassword());
        return RestSupport.message("Password updated");
    }
}
