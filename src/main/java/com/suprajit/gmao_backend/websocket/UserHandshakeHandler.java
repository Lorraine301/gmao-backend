package com.suprajit.gmao_backend.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

// ── Transforme l'userId stocké par l'intercepteur en Principal Spring,
// pour que convertAndSendToUser(userId, ...) fonctionne correctement ──
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");
        return new StompPrincipal(userId);
    }
}