package com.suprajit.gmao_backend.ruleengine.controller;

import com.suprajit.gmao_backend.failure.dto.FailureResponseDTO;
import com.suprajit.gmao_backend.failure.service.FailureService;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
import com.suprajit.gmao_backend.ruleengine.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rule Engine", description = "Moteur de règles métier - priorisation et détection des risques")
@SecurityRequirement(name = "bearerAuth")
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;
    private final FailureService failureService;

    @Operation(
        summary = "Équipements à risque",
        description = "Retourne les équipements critiques présentant un risque élevé (pannes fréquentes ou MTTR élevé)."
    )
    @GetMapping("/api/equipments/at-risk")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<AtRiskEquipmentDTO>> getAtRiskEquipments() {
        return ResponseEntity.ok(ruleEngineService.findAtRiskEquipments());
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
}