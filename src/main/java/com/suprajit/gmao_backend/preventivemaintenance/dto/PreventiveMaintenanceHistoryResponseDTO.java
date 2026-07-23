package com.suprajit.gmao_backend.preventivemaintenance.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreventiveMaintenanceHistoryResponseDTO {
    private Long id;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long technicianId;
    private String technicianName;
    private String maintenanceType;
    private LocalDateTime completedAt;
    private String problemFound;
    private String solution;
}