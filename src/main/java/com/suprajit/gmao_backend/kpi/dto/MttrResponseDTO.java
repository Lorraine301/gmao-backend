package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MttrResponseDTO {
    private Long equipmentId;      // null si global
    private String equipmentCode;  // null si global
    private Double mttr;           // en heures, null si aucune donnée
    private int periodDays;
}