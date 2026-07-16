package com.suprajit.gmao_backend.ruleengine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AtRiskEquipmentDTO {
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String criticalityLevel;
    private long recentFailuresCount;
    private double averageMttr;
    private String riskReason;
    private String llmExplanation;

}