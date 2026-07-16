package com.suprajit.gmao_backend.ruleengine.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleEngineSummaryDTO {
    private long rule1TriggeredCount;   // équipement critique + 3+ pannes en 7j
    private long rule2TriggeredCount;   // MTTR élevé
    private long rule3TriggeredCount;   // criticité élevée → priorité minimale High
    private long rule4RecommendationsCount; // technicien recommandé
    private long rule5EscalatedCount;   // escalade Critical via analyse LLM
    private long rule6MaintenancesCreatedCount; // maintenance urgente auto-créée
}