package com.kavindu.techmart.web.security;

import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.exception.AccessDeniedException;
import com.kavindu.techmart.common.exception.AuthException;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestContext {

    private UserDTO currentUser;
    private String token;

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO currentUser) {
        this.currentUser = currentUser;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public Long getUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public String getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean hasRole(String role) {
        return currentUser != null && role != null && role.equals(currentUser.getRole());
    }

    public void requireAuthenticated() {
        if (!isAuthenticated()) {
            throw new AuthException("Authentication required");
        }
    }

    public void requireRole(String... roles) {
        requireAuthenticated();
        for (String role : roles) {
            if (hasRole(role)) {
                return;
            }
        }
        throw new AccessDeniedException("Access denied: requires one of role(s) " + String.join(", ", roles));
    }
}
