package com.kavindu.techmart.ejb.websocket;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;
import jakarta.websocket.Session;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton(name = "WebSocketSessionRegistry")
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class WebSocketSessionRegistry {

    private static final Logger LOG = Logger.getLogger(WebSocketSessionRegistry.class.getName());

    private final Map<Long, Set<Session>> userSessions = new ConcurrentHashMap<>();

    public void register(Long userId, Session session) {
        if (userId == null || session == null) {
            return;
        }
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        LOG.fine(() -> "WebSocket registered for user " + userId
                + " (total connections=" + getConnectionCount() + ")");
    }

    public void unregister(Long userId, Session session) {
        if (userId == null || session == null) {
            return;
        }
        Set<Session> set = userSessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    public void unregister(Session session) {
        if (session == null) {
            return;
        }
        userSessions.forEach((userId, set) -> set.remove(session));
        userSessions.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void sendToUser(Long userId, String jsonPayload) {
        if (userId == null) {
            return;
        }
        Set<Session> set = userSessions.get(userId);
        if (set == null || set.isEmpty()) {
            LOG.fine(() -> "No live WebSocket session for user " + userId + "; push skipped");
            return;
        }
        for (Session session : set) {
            sendQuietly(session, jsonPayload);
        }
    }

    public void broadcast(String jsonPayload) {
        for (Set<Session> set : userSessions.values()) {
            for (Session session : set) {
                sendQuietly(session, jsonPayload);
            }
        }
    }

    private void sendQuietly(Session session, String payload) {
        try {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(payload);
            }
        } catch (RuntimeException ex) {
            LOG.log(Level.FINE, "Failed to push to WebSocket session; dropping", ex);
            unregister(session);
        }
    }

    public int getConnectionCount() {
        return userSessions.values().stream().mapToInt(Set::size).sum();
    }

    public int getUserCount() {
        return userSessions.size();
    }

    public Map<Long, Set<Session>> getUserSessions() {
        return Collections.unmodifiableMap(userSessions);
    }
}
