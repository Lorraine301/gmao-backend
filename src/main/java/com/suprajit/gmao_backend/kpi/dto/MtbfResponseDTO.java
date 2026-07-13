package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MtbfResponseDTO {
    private Long equipmentId;
    private String equipmentCode;
    private Double mtbf;           // en heures, null si aucune panne (indéfini)
    private int periodDays;
}