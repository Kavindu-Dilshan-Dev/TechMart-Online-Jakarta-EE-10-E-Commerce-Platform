package com.kavindu.techmart.web.rest;

import com.kavindu.techmart.common.dto.ApiResponse;
import jakarta.ws.rs.core.Response;

public final class RestSupport {

    private RestSupport() {
    }

    public static <T> Response ok(T data) {
        return Response.ok(ApiResponse.ok(data)).build();
    }

    public static <T> Response ok(T data, String message) {
        return Response.ok(ApiResponse.ok(data, message)).build();
    }

    public static <T> Response created(T data, String message) {
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.ok(data, message))
                .build();
    }

    public static Response message(String message) {
        return Response.ok(ApiResponse.ok(null, message)).build();
    }
}
