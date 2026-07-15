package com.suprajit.gmao_backend.weeklyreport.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.pdf.service.PdfService;
import com.suprajit.gmao_backend.weeklyreport.dto.WeeklyReportResponseDTO;
import com.suprajit.gmao_backend.weeklyreport.service.WeeklyReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/weekly-reports")
@RequiredArgsConstructor
@Tag(name = "Bilans hebdomadaires", description = "Consultation des rapports de synthèse hebdomadaires")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final PdfService pdfService;

    // ── GET /api/weekly-reports ───────────────────────────────
    @Operation(summary = "Lister les bilans hebdomadaires", description = "Ordre décroissant (plus récent en premier).")
    @GetMapping
    public ResponseEntity<List<WeeklyReportResponseDTO>> findAll() {
        return ResponseEntity.ok(weeklyReportService.findAll());
    }

    // ── GET /api/weekly-reports/{id} ──────────────────────────
    @Operation(summary = "Détail d'un bilan hebdomadaire")
    @GetMapping("/{id}")
    public ResponseEntity<WeeklyReportResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(weeklyReportService.findById(id));
    }

    // ── POST /api/weekly-reports/generate ─────────────────────
    // Endpoint de TEST pour forcer manuellement la génération (à retirer en prod)
    @Operation(
        summary = "[TEST] Déclencher manuellement la génération du bilan de la semaine",
        description = "À supprimer en production — sert uniquement à valider le scheduler sans attendre dimanche 18h."
    )

    @PostMapping("/generate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<WeeklyReportResponseDTO> generateManually() {
        return ResponseEntity.ok(weeklyReportService.generateWeeklyReport(
            weeklyReportService.getCurrentUserName()));
    }
       // ── GET /api/weekly-reports/{id}/pdf ──────────────────────
    @Operation(summary = "Télécharger le PDF du bilan hebdomadaire")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@PathVariable Long id) throws IOException {
        byte[] pdfBytes = pdfService.getOrGenerateWeeklyReportBytes(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=weekly_report_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}