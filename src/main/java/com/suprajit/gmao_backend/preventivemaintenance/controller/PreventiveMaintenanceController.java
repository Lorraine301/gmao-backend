package com.suprajit.gmao_backend.preventivemaintenance.controller;

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

import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceRequestDTO;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceResponseDTO;
import com.suprajit.gmao_backend.preventivemaintenance.service.PreventiveMaintenanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preventive-maintenances")
@RequiredArgsConstructor
@Tag(name = "Maintenance Préventive",
     description = "Planification et suivi des maintenances préventives")
@SecurityRequirement(name = "bearerAuth")
public class PreventiveMaintenanceController {

    private final PreventiveMaintenanceService pmService;

    // ── POST /api/preventive-maintenances ────────────────────
    @Operation(
        summary = "Planifier une maintenance préventive",
        description = "Crée une nouvelle maintenance périodique. " +
                      "next_maintenance_date et next_reminder_date sont calculées automatiquement."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Maintenance planifiée"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Équipement non trouvé")
    })
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<PreventiveMaintenanceResponseDTO> schedule(
            @Valid @RequestBody PreventiveMaintenanceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pmService.schedule(dto));
    }

    // ── GET /api/preventive-maintenances ─────────────────────
    @Operation(
        summary = "Lister toutes les maintenances préventives",
        description = "Retourne toutes les maintenances avec le champ daysUntilNext " +
                      "(négatif = en retard)."
    )
    @GetMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<PreventiveMaintenanceResponseDTO>> findAll() {
        return ResponseEntity.ok(pmService.findAll());
    }

    // ── GET /api/preventive-maintenances/overdue ─────────────
    @Operation(
        summary = "Maintenances en retard",
        description = "Retourne les maintenances dont next_maintenance_date < aujourd'hui " +
                      "et statut non Completed/Cancelled."
    )
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<PreventiveMaintenanceResponseDTO>> findOverdue() {
        return ResponseEntity.ok(pmService.findOverdue());
    }

    // ── GET /api/preventive-maintenances/equipment/{id} ──────
    @Operation(
        summary = "Maintenances par équipement",
        description = "Retourne toutes les maintenances planifiées pour un équipement donné."
    )
    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<PreventiveMaintenanceResponseDTO>> findByEquipment(
            @PathVariable Long equipmentId) {
        return ResponseEntity.ok(pmService.findByEquipment(equipmentId));
    }

    // ── PUT /api/preventive-maintenances/{id}/complete ────────
    @Operation(
        summary = "Marquer une maintenance comme terminée",
        description = "Met à jour last_maintenance_date = aujourd'hui, " +
                      "recalcule next_maintenance_date = aujourd'hui + frequency_days, " +
                      "status → Completed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Maintenance complétée"),
        @ApiResponse(responseCode = "404", description = "Maintenance non trouvée")
    })
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<PreventiveMaintenanceResponseDTO> complete(@PathVariable Long id) {
        return ResponseEntity.ok(pmService.complete(id));
    }
}