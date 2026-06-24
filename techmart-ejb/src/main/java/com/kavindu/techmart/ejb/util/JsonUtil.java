package com.kavindu.techmart.ejb.util;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String notificationJson(Long userId, String type, String title,
                                          String message, Long orderId, Long productId) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("type", type == null ? "SYSTEM" : type)
                .add("title", title == null ? "" : title)
                .add("message", message == null ? "" : message)
                .add("timestamp", System.currentTimeMillis());
        if (userId != null) {
            b.add("userId", userId);
        }
        if (orderId != null) {
            b.add("orderId", orderId);
        }
        if (productId != null) {
            b.add("productId", productId);
        }
        return b.build().toString();
    }

    public static String unreadCountJson(long count) {
        return Json.createObjectBuilder()
                .add("type", "UNREAD_COUNT")
                .add("count", count)
                .add("timestamp", System.currentTimeMillis())
                .build()
                .toString();
    }
}
