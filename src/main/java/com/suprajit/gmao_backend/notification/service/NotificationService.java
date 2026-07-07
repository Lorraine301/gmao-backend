package com.suprajit.gmao_backend.notification.service;

import com.suprajit.gmao_backend.entity.Notification;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.NotificationRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Créer une notification pour un utilisateur ──────────
    public void create(Long userId, String type, String message,
                       String entityType, Long entityId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .status("Unread")
                .notificationDate(LocalDateTime.now())
                .build());
    }

    // ── Notifier tous les Admins et Superviseurs ─────────────
    public void notifyAdminsAndSupervisors(String type, String message,
                                            String entityType, Long entityId) {
        List<User> targets = userRepository.findByRole_NameIn(
                List.of("Admin", "Supervisor"));
        for (User user : targets) {
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(type)
                    .message(message)
                    .entityType(entityType)
                    .entityId(entityId)
                    .status("Unread")
                    .notificationDate(LocalDateTime.now())
                    .build());
        }
    }
}