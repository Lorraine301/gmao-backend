package com.suprajit.gmao_backend.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Notification;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.notification.dto.NotificationResponseDTO;
import com.suprajit.gmao_backend.repository.NotificationRepository;
import com.suprajit.gmao_backend.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;      

    // ── Mapper ──────────────────────────────────────────────
    private NotificationResponseDTO toDTO(Notification n) {
        return NotificationResponseDTO.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .userFullName(n.getUser().getFullName())
                .type(n.getType())
                .message(n.getMessage())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .status(n.getStatus())
                .notificationDate(n.getNotificationDate())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ── Créer une notification pour un utilisateur ──────────
 // ── Créer une notification pour un utilisateur ──────────
    public void create(Long userId, String type, String message,
                       String entityType, Long entityId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification saved = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .status("Unread")
                .notificationDate(LocalDateTime.now())
                .build());

        // ── Push en temps réel via WebSocket ──
        messagingTemplate.convertAndSendToUser(
                userId.toString(), "/queue/notifications", toDTO(saved));
    }

    
    // ── Notifier tous les Admins et Superviseurs ─────────────
// ── Notifier tous les Admins et Superviseurs ─────────────
    public void notifyAdminsAndSupervisors(String type, String message,
                                            String entityType, Long entityId) {
        List<User> targets = userRepository.findByRole_NameIn(
                List.of("Admin", "Supervisor"));
        for (User user : targets) {
            Notification saved = notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(type)
                    .message(message)
                    .entityType(entityType)
                    .entityId(entityId)
                    .status("Unread")
                    .notificationDate(LocalDateTime.now())
                    .build());

            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(), "/queue/notifications", toDTO(saved));
        }
    }
    // ── Notifications de l'utilisateur connecté ──────────────
    public List<NotificationResponseDTO> findForCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDTO).toList();
    }

    // ── Compter les non lues (pour le badge) ─────────────────
    public long countUnreadForCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return notificationRepository.countByUserIdAndStatus(user.getId(), "Unread");
    }

    // ── Marquer une notification comme lue ───────────────────
    public NotificationResponseDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification non trouvée : " + id));
        notification.setStatus("Read");
        return toDTO(notificationRepository.save(notification));
    }

    // ── Marquer toutes comme lues ────────────────────────────
    public void markAllAsRead() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));

        List<Notification> unread = notificationRepository
                .findByUserIdAndStatus(user.getId(), "Unread");
        unread.forEach(n -> n.setStatus("Read"));
        notificationRepository.saveAll(unread);
    }
}