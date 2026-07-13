package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipmentFailureCountDTO {
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private long failureCount;
}