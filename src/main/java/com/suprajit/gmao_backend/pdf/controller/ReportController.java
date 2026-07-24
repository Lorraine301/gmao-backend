package com.suprajit.gmao_backend.pdf.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.ai.service.AiAnalysisService;
import com.suprajit.gmao_backend.pdf.service.PdfService;
import com.suprajit.gmao_backend.repository.MonthlyReportRepository;
import com.suprajit.gmao_backend.weeklyreport.dto.MonthlyReportResponseDTO;
import com.suprajit.gmao_backend.weeklyreport.service.MonthlyReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Rapports mensuels", description = "Bilans mensuels PDF, clôturés automatiquement chaque 1er du mois")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
public class ReportController {

    private final PdfService pdfService;
    private final MonthlyReportService monthlyReportService;
    private final MonthlyReportRepository monthlyReportRepository;
    private final AiAnalysisService aiAnalysisService;

    // ── GET /api/reports/monthly?month=&year= ─────────────────
    @Operation(
        summary = "Télécharger le bilan mensuel PDF",
        description = "Le bilan doit déjà avoir été clôturé (automatiquement ou manuellement)."
    )
    @GetMapping("/monthly")
    public ResponseEntity<byte[]> getMonthlyReport(
            @RequestParam int month,
            @RequestParam int year) throws IOException {

        MonthlyReportResponseDTO report = monthlyReportService.findByMonthYear(month, year);

        byte[] pdfBytes = pdfService.getOrGenerateMonthlyReportBytes(
                monthlyReportRepository.findById(report.getId()).orElseThrow());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=monthly_report_" + year + "_" + month + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ── POST /api/reports/monthly/generate?month=&year= ──────
    // Endpoint de TEST pour forcer manuellement la clôture (à garder ou retirer en prod)
    @Operation(
            summary = "[TEST] Clôturer manuellement un bilan mensuel",
            description = "Utile pour ne pas attendre le 1er du mois suivant. " +
                        "Avec ?force=true, régénère même si déjà existant (usage test uniquement)."
        )
        @PostMapping("/monthly/generate")
        @PreAuthorize("hasRole('Admin')")
        public ResponseEntity<MonthlyReportResponseDTO> generateMonthlyReport(
                @RequestParam int month,
                @RequestParam int year,
                @RequestParam(defaultValue = "false") boolean force) {

            MonthlyReportResponseDTO report = force
                    ? monthlyReportService.forceRegenerate(month, year, "Admin")
                    : monthlyReportService.generateIfAbsent(month, year, "Admin");

            aiAnalysisService.generateMonthlyLlmSummary(report.getId());
            return ResponseEntity.ok(report);
        }
}