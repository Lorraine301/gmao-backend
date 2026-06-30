package com.suprajit.gmao_backend.intervention.dto;

import java.time.LocalDateTime;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterventionResponseDTO {
    private Long id;

    // ── Panne résolue ─────────────────────────────────────
    private Long failureId;
    private String failureCode;
    private String failureTitle;
    private String equipmentCode;
    private String equipmentName;

    // ── Technicien résolu ──────────────────────────────────
    private Long technicianId;
    private String technicianName;
    private String technicianEmployeeCode;

    // ── Assigné par résolu ───────────────────────────────────
    private Long assignedById;
    private String assignedByName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double duration;
    private FailurePriority priority;
    private InterventionStatus status;
    private String solution;

    private Long closedById;
    private String closedByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}