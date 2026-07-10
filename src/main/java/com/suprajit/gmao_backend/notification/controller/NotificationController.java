package com.suprajit.gmao_backend.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.notification.dto.NotificationResponseDTO;
import com.suprajit.gmao_backend.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications utilisateur")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET /api/notifications ────────────────────────────────
    @Operation(
        summary = "Mes notifications",
        description = "Retourne toutes les notifications de l'utilisateur connecté, triées par date décroissante."
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationResponseDTO>> findMine() {
        return ResponseEntity.ok(notificationService.findForCurrentUser());
    }

    // ── GET /api/notifications/count ─────────────────────────
    @Operation(summary = "Nombre de notifications non lues (pour le badge navbar)")
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> countUnread() {
        return ResponseEntity.ok(
            Map.of("unreadCount", notificationService.countUnreadForCurrentUser())
        );
    }

    // ── PUT /api/notifications/{id}/read ─────────────────────
    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // ── PUT /api/notifications/read-all ──────────────────────
    @Operation(summary = "Marquer toutes les notifications comme lues")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }
}