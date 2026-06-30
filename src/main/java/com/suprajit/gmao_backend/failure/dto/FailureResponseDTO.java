package com.suprajit.gmao_backend.failure.dto;

import java.time.LocalDateTime;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailureResponseDTO {
    private Long id;
    private String failureCode;

    // ── Équipement résolu (pas juste l'id) ──────────────────
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;

    private String title;
    private String description;
    private String failureType;
    private FailurePriority priority;
    private FailurePriority llmPriority;
    private FailureStatus status;

    // ── Déclarant résolu (pas juste l'id) ────────────────────
    private Long reportedById;
    private String reportedByName;
    private String reportedByEmployeeCode;

    private String reportedChannel;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private Boolean llmProcessed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}