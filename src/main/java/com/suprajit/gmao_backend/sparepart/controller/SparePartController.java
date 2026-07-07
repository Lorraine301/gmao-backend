package com.suprajit.gmao_backend.sparepart.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.sparepart.dto.AddInterventionPartsRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.InterventionPartResponseDTO;
import com.suprajit.gmao_backend.sparepart.dto.SparePartRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.SparePartResponseDTO;
import com.suprajit.gmao_backend.sparepart.service.SparePartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Stock de pièces", description = "Gestion du stock de pièces de rechange")
@SecurityRequirement(name = "bearerAuth")
public class SparePartController {

    private final SparePartService sparePartService;

    // ── GET /api/spare-parts ──────────────────────────────────
    @Operation(
        summary = "Lister les pièces de rechange",
        description = "Retourne toutes les pièces. Avec ?lowStock=true, retourne uniquement celles dont quantity <= minimumStock."
    )
    @GetMapping("/api/spare-parts")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<List<SparePartResponseDTO>> findAll(
            @Parameter(description = "Filtrer uniquement les pièces en stock faible")
            @RequestParam(required = false) Boolean lowStock) {
        return ResponseEntity.ok(sparePartService.findAll(lowStock));
    }

    // ── GET /api/spare-parts/low-stock ───────────────────────
    @Operation(summary = "Pièces en stock faible", description = "Retourne les pièces dont quantity <= minimum_stock.")
    @GetMapping("/api/spare-parts/low-stock")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<SparePartResponseDTO>> findLowStock() {
        return ResponseEntity.ok(sparePartService.findLowStock());
    }

    // ── GET /api/spare-parts/{id} ─────────────────────────────
    @Operation(summary = "Détail d'une pièce")
    @GetMapping("/api/spare-parts/{id}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<SparePartResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(sparePartService.findById(id));
    }

    // ── POST /api/spare-parts ─────────────────────────────────
    @Operation(summary = "Créer une pièce de rechange", description = "Réservé à l'Admin.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pièce créée"),
        @ApiResponse(responseCode = "400", description = "Référence déjà existante ou données invalides")
    })
    @PostMapping("/api/spare-parts")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<SparePartResponseDTO> create(
            @Valid @RequestBody SparePartRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sparePartService.create(dto));
    }

    // ── PUT /api/spare-parts/{id} ─────────────────────────────
    @Operation(summary = "Modifier une pièce de rechange")
    @PutMapping("/api/spare-parts/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<SparePartResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SparePartRequestDTO dto) {
        return ResponseEntity.ok(sparePartService.update(id, dto));
    }

    // ── POST /api/interventions/{id}/parts ────────────────────
    @Operation(
        summary = "Déclarer les pièces utilisées dans une intervention",
        description = "Décrémente automatiquement le stock. Lance une alerte si stock faible après consommation."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pièces enregistrées"),
        @ApiResponse(responseCode = "400", description = "Stock insuffisant"),
        @ApiResponse(responseCode = "404", description = "Intervention ou pièce non trouvée")
    })
    @PostMapping("/api/interventions/{interventionId}/parts")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<List<InterventionPartResponseDTO>> addParts(
            @PathVariable Long interventionId,
            @Valid @RequestBody AddInterventionPartsRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sparePartService.addPartsToIntervention(interventionId, dto));
    }
}