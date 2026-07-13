package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailabilityResponseDTO {
    private Long equipmentId;
    private String equipmentCode;
    private double availabilityRate;  // en %, entre 0 et 100
    private int periodDays;
}