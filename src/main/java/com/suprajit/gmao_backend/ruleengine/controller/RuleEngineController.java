package com.suprajit.gmao_backend.ruleengine.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.ai.service.AiAnalysisService;
import com.suprajit.gmao_backend.failure.dto.FailureResponseDTO;
import com.suprajit.gmao_backend.failure.service.FailureService;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
import com.suprajit.gmao_backend.ruleengine.dto.RuleEngineSummaryDTO;
import com.suprajit.gmao_backend.ruleengine.service.RuleEngineService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rule Engine", description = "Moteur de règles métier - priorisation et détection des risques")
@SecurityRequirement(name = "bearerAuth")
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;
    private final FailureService failureService;
    private final AiAnalysisService aiAnalysisService;

    @Operation(
        summary = "Équipements à risque",
        description = "Retourne les équipements critiques présentant un risque élevé (pannes fréquentes ou MTTR élevé), "
                    + "enrichis d'une explication en langage naturel générée par le LLM."
    )
    @GetMapping("/api/equipments/at-risk")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<AtRiskEquipmentDTO>> getAtRiskEquipments() {
        List<AtRiskEquipmentDTO> atRisk = ruleEngineService.findAtRiskEquipments();
        List<AtRiskEquipmentDTO> enriched = aiAnalysisService.enrichAtRiskEquipments(atRisk);
        return ResponseEntity.ok(enriched);
    }

    @Operation(
        summary = "Pannes urgentes",
        description = "Retourne toutes les pannes ayant une priorité Critical et non clôturées."
    )
    @GetMapping("/api/failures/urgent")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<List<FailureResponseDTO>> getUrgentFailures() {
        return ResponseEntity.ok(failureService.findUrgent());
    }
    
    @Operation(
        summary = "Forcer la réévaluation du Rule Engine",
        description = "Utile après l'arrivée du résultat LLM : réapplique les règles 1-4, puis 5-6 si l'analyse IA existe."
    )
    @GetMapping("/api/rule-engine/evaluate/{failureId}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<FailureResponseDTO> forceEvaluate(@PathVariable Long failureId) {
        return ResponseEntity.ok(failureService.reevaluateWithRuleEngine(failureId));
    }

    @Operation(
        summary = "Statistiques du Rule Engine",
        description = "Compteurs en mémoire (remis à zéro au redémarrage du serveur)."
    )
    @GetMapping("/api/rule-engine/summary")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<RuleEngineSummaryDTO> getSummary() {
        return ResponseEntity.ok(ruleEngineService.getSummary());
    }
}