package com.suprajit.gmao_backend.intervention.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.intervention.dto.CompleteInterventionDTO;
import com.suprajit.gmao_backend.intervention.dto.InterventionRequestDTO;
import com.suprajit.gmao_backend.intervention.dto.InterventionResponseDTO;
import com.suprajit.gmao_backend.intervention.dto.UpdateInterventionStatusDTO;
import com.suprajit.gmao_backend.intervention.service.InterventionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
@Tag(name = "Interventions", description = "Affectation et suivi des interventions de maintenance")
@SecurityRequirement(name = "bearerAuth")
public class InterventionController {

    private final InterventionService interventionService;

    // ── POST /api/interventions ────────────────────────────
    @Operation(
        summary = "Créer une intervention",
        description = "Affecte un technicien à une panne. Met automatiquement la panne en statut In_Progress."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Intervention créée"),
        @ApiResponse(responseCode = "404", description = "Panne ou technicien non trouvé")
    })
    @PostMapping
    @PreAuthorize("hasRole('Supervisor') or hasRole('Admin')")
    public ResponseEntity<InterventionResponseDTO> create(
            @Valid @RequestBody InterventionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interventionService.create(dto));
    }

    // ── GET /api/interventions ───────────────────────────────
    @Operation(summary = "Lister toutes les interventions")
    @GetMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<InterventionResponseDTO>> findAll() {
        return ResponseEntity.ok(interventionService.findAll());
    }

    // ── GET /api/interventions/my ───────────────────────────
    @Operation(
        summary = "Mes interventions",
        description = "Retourne les interventions assignées au technicien actuellement connecté."
    )
    @GetMapping("/my")
    @PreAuthorize("hasRole('Technician')")
    public ResponseEntity<List<InterventionResponseDTO>> findMy() {
        Long currentUserId = interventionService.getCurrentUserId();
        return ResponseEntity.ok(interventionService.findByTechnician(currentUserId));
    }

    // ── PUT /api/interventions/{id}/status ──────────────────
    @Operation(
        summary = "Changer le statut d'une intervention",
        description = "Workflow : Pending → In_Progress → Completed (utiliser /complete pour clôturer avec solution)."
    )
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<InterventionResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInterventionStatusDTO dto) {
        return ResponseEntity.ok(interventionService.updateStatus(id, dto.getStatus()));
    }

    // ── PUT /api/interventions/{id}/complete ────────────────
    @Operation(
        summary = "Clôturer une intervention",
        description = "Termine l'intervention : calcule automatiquement la durée, enregistre la solution, " +
                      "passe le statut à Completed et la panne liée à Resolved. " +
                      "Enregistre également les pièces utilisées si fournies (décrémente le stock)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Intervention clôturée"),
        @ApiResponse(responseCode = "400", description = "Solution manquante ou stock insuffisant"),
        @ApiResponse(responseCode = "404", description = "Intervention non trouvée")
    })
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<InterventionResponseDTO> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteInterventionDTO dto) {
        return ResponseEntity.ok(
            interventionService.complete(id, dto.getSolution(), dto.getParts()));
    }

    // ── GET /api/interventions/my/archive ───────────────────
    @Operation(summary = "Mes interventions terminées (archives)")
    @GetMapping("/my/archive")
    @PreAuthorize("hasRole('Technician')")
    public ResponseEntity<List<InterventionResponseDTO>> findMyArchive() {
        Long currentUserId = interventionService.getCurrentUserId();
        return ResponseEntity.ok(interventionService.findMyArchive(currentUserId));
    }
}