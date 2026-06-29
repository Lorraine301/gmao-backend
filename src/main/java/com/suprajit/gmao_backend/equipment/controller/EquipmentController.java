package com.suprajit.gmao_backend.equipment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.equipment.dto.EquipmentRequestDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentResponseDTO;
import com.suprajit.gmao_backend.equipment.service.EquipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
@Tag(name = "Équipements", description = "Gestion du parc machines de Suprajit Maroc")
@SecurityRequirement(name = "bearerAuth")
public class EquipmentController {

    private final EquipmentService equipmentService;

    // ── POST /api/equipments ────────────────────────────────
    @Operation(
        summary = "Créer un équipement",
        description = "Crée un nouvel équipement dans le parc machines. Réservé à l'Admin."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Équipement créé"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "409", description = "Code équipement déjà existant")
    })
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<EquipmentResponseDTO> create(
            @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(equipmentService.create(dto));
    }

    // ── GET /api/equipments ─────────────────────────────────
    @Operation(
        summary = "Lister les équipements",
        description = "Retourne tous les équipements avec filtres optionnels par statut, type et criticité."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste retournée")
    })
    @GetMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<EquipmentResponseDTO>> findAll(
            @Parameter(description = "Filtrer par statut (Active, Inactive, Under_Maintenance)")
            @RequestParam(required = false) EquipmentStatus status,

            @Parameter(description = "Filtrer par type (Extrusion, Winding, Molding...)")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filtrer par criticité (Low, Medium, High)")
            @RequestParam(required = false) CriticalityLevel criticality) {

        return ResponseEntity.ok(equipmentService.findAll(status, type, criticality));
    }

    // ── GET /api/equipments/{id} ────────────────────────────
    @Operation(
        summary = "Détail d'un équipement",
        description = "Retourne les détails complets d'un équipement par son ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Équipement trouvé"),
        @ApiResponse(responseCode = "404", description = "Équipement non trouvé")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<EquipmentResponseDTO> findById(
            @PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.findById(id));
    }

    // ── PUT /api/equipments/{id} ────────────────────────────
    @Operation(
        summary = "Modifier un équipement",
        description = "Met à jour les informations d'un équipement existant. Réservé à l'Admin."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Équipement mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Équipement non trouvé")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<EquipmentResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.ok(equipmentService.update(id, dto));
    }

    // ── DELETE /api/equipments/{id} ─────────────────────────
    @Operation(
        summary = "Supprimer un équipement",
        description = "Supprime définitivement un équipement. Réservé à l'Admin."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Équipement supprimé"),
        @ApiResponse(responseCode = "404", description = "Équipement non trouvé")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}