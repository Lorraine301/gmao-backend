package com.suprajit.gmao_backend.notification.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponseDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private String type;
    private String message;
    private String entityType;
    private Long entityId;
    private String status;
    private LocalDateTime notificationDate;
    private LocalDateTime createdAt;
}