package com.suprajit.gmao_backend.websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.security.jwt.JwtService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

// ── Valide le JWT passé en query param (?token=...) AVANT d'accepter
// la connexion WebSocket, et stocke l'userId pour l'utiliser ensuite ──
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = httpRequest.getParameter("token");

        if (token == null || token.isBlank()) {
            return false; // refuse la connexion sans token
        }

        try {
            String email = jwtService.extractUsername(token);
            Long userId = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                    .getId();

            attributes.put("userId", userId.toString());
            return true;

        } catch (Exception e) {
            System.out.println("[WEBSOCKET] Connexion refusée : token invalide - " + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // rien à faire
    }
}