package com.kavindu.techmart.web.filter;

import com.kavindu.techmart.common.dto.ApiResponse;
import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.interfaces.AuthServiceLocal;
import com.kavindu.techmart.web.security.RequestContext;
import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private static final String BEARER = "Bearer ";

    @EJB
    private AuthServiceLocal authService;

    @Inject
    private RequestContext requestContext;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String header = ctx.getHeaderString("Authorization");
        if (header == null || !header.startsWith(BEARER)) {
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        UserDTO user = authService.validateToken(token);
        if (user == null) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiResponse.fail("Invalid or expired session", "UNAUTHORIZED"))
                    .type(MediaType.APPLICATION_JSON)
                    .build());
            return;
        }
        requestContext.setCurrentUser(user);
        requestContext.setToken(token);
    }
}
