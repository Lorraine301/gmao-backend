package com.suprajit.gmao_backend.pdf.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.pdf.service.PdfService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Rapports mensuels", description = "Génération à la volée des bilans mensuels PDF")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
public class ReportController {

    private final PdfService pdfService;

    // ── GET /api/reports/monthly?month=&year= ─────────────────
    @Operation(summary = "Télécharger le bilan mensuel PDF",
            description = "Généré à la volée, non persisté en base.")
    @GetMapping("/monthly")
    public ResponseEntity<byte[]> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year) throws IOException {
        byte[] pdfBytes = pdfService.generateMonthlyReportBytes(month, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=monthly_report_" + year + "_" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}