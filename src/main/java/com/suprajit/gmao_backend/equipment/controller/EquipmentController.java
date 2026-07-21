package com.suprajit.gmao_backend.equipment.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.equipment.dto.EquipmentImportResultDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentRequestDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentResponseDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentStatusUpdateDTO;
import com.suprajit.gmao_backend.equipment.service.EquipmentImportExportService;
import com.suprajit.gmao_backend.equipment.service.EquipmentService;
import com.suprajit.gmao_backend.pdf.service.PdfService;

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
    private final EquipmentImportExportService importExportService;
    private final PdfService pdfService;


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
            @RequestParam(name = "criticality",required = false) CriticalityLevel criticality,
            @Parameter(description = "Recherche textuelle")
            @RequestParam(required = false) String search) {

        return ResponseEntity.ok(equipmentService.findAll(status, type, criticality, search));
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
     

    // ── GET /api/equipments/export?format=excel|json ────────
    @Operation(summary = "Exporter les équipements", description = "Format excel (défaut) ou json.")
    @GetMapping("/export")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "excel") String format) throws IOException {

        byte[] content;
        String filename;
        MediaType mediaType;

        if ("json".equalsIgnoreCase(format)) {
            content = importExportService.exportToJson();
            filename = "equipements.json";
            mediaType = MediaType.APPLICATION_JSON;
        } else {
            content = importExportService.exportToExcel();
            filename = "equipements.xlsx";
            mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(content);
    }

    // ── POST /api/equipments/import ──────────────────────────
    @Operation(
        summary = "Importer des équipements en masse",
        description = "Fichier .xlsx ou .json contenant plusieurs équipements. " +
                      "Les lignes invalides ou en doublon (code déjà existant) sont ignorées, pas bloquantes."
    )
    @PostMapping("/import")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<EquipmentImportResultDTO> importFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(importExportService.importFile(file));
    }
    // ── PATCH /api/equipments/{id}/status ───────────────────
    @Operation(
        summary = "Changer rapidement le statut d'un équipement",
        description = "Modifie uniquement le statut, sans passer par le formulaire complet."
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<EquipmentResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EquipmentStatusUpdateDTO dto) {
        return ResponseEntity.ok(equipmentService.updateStatus(id, dto.getStatus()));
    } 
    
    // ── GET /api/equipments/{id}/datasheet ───────────────────
    @Operation(
        summary = "Télécharger la fiche technique PDF d'un équipement",
        description = "Inclut les informations générales, l'état, et l'historique des pannes des 90 derniers jours."
    )
    @GetMapping("/{id}/datasheet")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<byte[]> getDatasheet(@PathVariable Long id) throws IOException {
        byte[] pdfBytes = pdfService.generateEquipmentDatasheet(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fiche_equipement_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}