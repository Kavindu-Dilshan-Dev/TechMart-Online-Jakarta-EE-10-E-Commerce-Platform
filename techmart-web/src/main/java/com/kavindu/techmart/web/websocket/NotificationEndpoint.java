package com.kavindu.techmart.web.websocket;

import com.kavindu.techmart.common.dto.UserDTO;
import com.kavindu.techmart.common.interfaces.AuthServiceLocal;
import com.kavindu.techmart.common.interfaces.NotificationServiceLocal;
import com.kavindu.techmart.ejb.util.JsonUtil;
import com.kavindu.techmart.ejb.websocket.WebSocketSessionRegistry;
import jakarta.ejb.EJB;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint("/ws/notifications/{token}")
public class NotificationEndpoint {

    private static final Logger LOG = Logger.getLogger(NotificationEndpoint.class.getName());

    @EJB
    private AuthServiceLocal authService;

    @EJB
    private NotificationServiceLocal notificationService;

    @EJB
    private WebSocketSessionRegistry registry;

    private Long userId;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        UserDTO user = authService.validateToken(token);
        if (user == null) {
            closeQuietly(session, "Invalid or expired session");
            return;
        }
        this.userId = user.getId();
        registry.register(userId, session);

        long unread = notificationService.getUnreadCount(userId);
        session.getAsyncRemote().sendText(JsonUtil.unreadCountJson(unread));
        LOG.fine(() -> "WebSocket opened for user " + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equalsIgnoreCase(message)) {
            session.getAsyncRemote().sendText("{\"type\":\"PONG\"}");
        }
    }

    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            registry.unregister(userId, session);
            LOG.fine(() -> "WebSocket closed for user " + userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOG.log(Level.FINE, "WebSocket error", error);
        registry.unregister(session);
    }

    private void closeQuietly(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception ignored) {

        }
    }
}
