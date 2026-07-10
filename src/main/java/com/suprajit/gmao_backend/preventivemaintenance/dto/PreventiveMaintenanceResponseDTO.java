package com.suprajit.gmao_backend.preventivemaintenance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreventiveMaintenanceResponseDTO {
    private Long id;

    // ── Équipement résolu ───────────────────────────────────
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String equipmentType;
    private String equipmentLocation;

    // ── Planification ───────────────────────────────────────
    private String maintenanceType;
    private Integer frequencyDays;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private LocalDate nextReminderDate;
    private MaintenanceStatus status;

    // ── Jours restants (calculé) ────────────────────────────
    private Long daysUntilNext;   // négatif = en retard

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private Long assignedByIdField;      // renomme si conflit avec un champ existant
    private String assignedByName;
    private String executionStatus;      // Pending, In_Progress, Completed, ou null
    private String problemFound;
    private String solution;
    private LocalDateTime technicianStartTime;
    private LocalDateTime technicianEndTime;
}