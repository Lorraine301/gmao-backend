package com.suprajit.gmao_backend.failure.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.ai.dto.AiAnalysisResponseDTO;
import com.suprajit.gmao_backend.ai.service.AiAnalysisService;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.failure.dto.FailureRequestDTO;
import com.suprajit.gmao_backend.failure.dto.FailureResponseDTO;
import com.suprajit.gmao_backend.failure.dto.UpdatePriorityDTO;
import com.suprajit.gmao_backend.failure.dto.UpdateStatusDTO;
import com.suprajit.gmao_backend.failure.service.FailureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/failures")
@RequiredArgsConstructor
@Tag(name = "Pannes", description = "Déclaration et suivi des pannes machines")
@SecurityRequirement(name = "bearerAuth")
public class FailureController {

    private final FailureService failureService;

    // ── POST /api/failures ────────────────────────────────────
    @Operation(
        summary = "Déclarer une panne",
        description = "Crée une nouvelle panne sur un équipement. Le déclarant (reportedBy) est automatiquement extrait du token JWT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Panne déclarée"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "404", description = "Équipement non trouvé")
    })
   @PostMapping
    @PreAuthorize("hasRole('Technician') or hasRole('Supervisor') or hasRole('Admin')")
    public ResponseEntity<FailureResponseDTO> declare(
            @Valid @RequestBody FailureRequestDTO dto) {
        FailureResponseDTO created = failureService.declare(dto);

        // ── Déclenche l'analyse IA en asynchrone, APRÈS que la transaction
        // de declare() soit déjà validée (commit), pour que le thread async
        // puisse bien retrouver la panne en base ──
        aiAnalysisService.analyzeFailure(created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── GET /api/failures ──────────────────────────────────────
    @Operation(
        summary = "Lister les pannes",
        description = "Retourne toutes les pannes avec filtres optionnels par statut, priorité et équipement, triées par date décroissante."
    )
    @GetMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<List<FailureResponseDTO>> findAll(
            @Parameter(description = "Filtrer par statut (Open, In_Progress, Resolved, Closed)")
            @RequestParam(required = false) FailureStatus status,

            @Parameter(description = "Filtrer par priorité (Low, Medium, High, Critical)")
            @RequestParam(required = false) FailurePriority priority,

            @Parameter(description = "Filtrer par équipement")
            @RequestParam(required = false) Long equipmentId) {

        return ResponseEntity.ok(failureService.findAll(status, priority, equipmentId));
    }

    // ── GET /api/failures/{id} ─────────────────────────────────
    @Operation(summary = "Détail d'une panne")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Panne trouvée"),
        @ApiResponse(responseCode = "404", description = "Panne non trouvée")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<FailureResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(failureService.findById(id));
    }

    // ── PUT /api/failures/{id}/status ──────────────────────────
    @Operation(
        summary = "Changer le statut d'une panne",
        description = "Met à jour le statut du workflow (Open → In_Progress → Resolved → Closed). Enregistre automatiquement resolvedAt si Resolved/Closed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statut mis à jour"),
        @ApiResponse(responseCode = "404", description = "Panne non trouvée")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<FailureResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusDTO dto) {
        return ResponseEntity.ok(failureService.updateStatus(id, dto.getStatus()));
    }

    // ── PUT /api/failures/{id}/priority ────────────────────────
    @Operation(
        summary = "Changer la priorité d'une panne",
        description = "Réservé au Superviseur et à l'Admin pour ajuster manuellement la priorité calculée."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Priorité mise à jour"),
        @ApiResponse(responseCode = "404", description = "Panne non trouvée")
    })
    @PutMapping("/{id}/priority")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<FailureResponseDTO> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityDTO dto) {
        return ResponseEntity.ok(failureService.updatePriority(id, dto.getPriority()));
    }

    // ── PUT /api/failures/{id}/close ─────────────────────────
    @Operation(
        summary = "Clôturer définitivement une panne",
        description = "Réservé à Admin/Supervisor. Ne fonctionne que si la panne est au statut Resolved."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Panne clôturée"),
        @ApiResponse(responseCode = "400", description = "La panne n'est pas au statut Resolved"),
        @ApiResponse(responseCode = "404", description = "Panne non trouvée")
    })
    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<FailureResponseDTO> closeFailure(@PathVariable Long id) {
        return ResponseEntity.ok(failureService.closeFailure(id));
    }
    private final AiAnalysisService aiAnalysisService;

    // ── GET /api/failures/{id}/analysis ───────────────────────
    @Operation(summary = "Récupérer l'analyse IA d'une panne")
    @GetMapping("/{id}/analysis")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<AiAnalysisResponseDTO> getAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.getByFailureId(id));
    }

    // ── POST /api/failures/{id}/analysis/retry ────────────────
    @Operation(summary = "Relancer l'analyse IA d'une panne (si Failed)")
    @PostMapping("/{id}/analysis/retry")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<Void> retryAnalysis(@PathVariable Long id) {
        aiAnalysisService.retryAnalysis(id);
        return ResponseEntity.accepted().build();
    }


}