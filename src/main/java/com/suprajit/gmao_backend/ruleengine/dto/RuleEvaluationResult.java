package com.suprajit.gmao_backend.ruleengine.dto;

import java.util.List;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleEvaluationResult {
    private FailurePriority computedPriority;
    private List<String> triggeredRules;
    private Long recommendedTechnicianId;
    private String recommendedTechnicianName;
}