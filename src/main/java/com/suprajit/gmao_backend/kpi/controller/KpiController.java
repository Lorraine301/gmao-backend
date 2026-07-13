package com.suprajit.gmao_backend.kpi.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.kpi.dto.AvailabilityResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.EquipmentFailureCountDTO;
import com.suprajit.gmao_backend.kpi.dto.KpiSummaryDTO;
import com.suprajit.gmao_backend.kpi.dto.MonthlyTrendDTO;
import com.suprajit.gmao_backend.kpi.dto.MtbfResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.MttrResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.PriorityCountDTO;
import com.suprajit.gmao_backend.kpi.dto.StatusCountDTO;
import com.suprajit.gmao_backend.kpi.dto.TechnicianWorkloadDTO;
import com.suprajit.gmao_backend.kpi.service.KpiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
@Tag(name = "KPI & Statistiques", description = "Indicateurs de performance pour le dashboard")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
public class KpiController {

    private final KpiService kpiService;

    // ── GET /api/kpi/summary ─────────────────────────────────
    @Operation(summary = "Résumé global des KPI",
            description = "Total pannes, taux de résolution, MTTR moyen global sur la période.")
    @GetMapping("/summary")
    public ResponseEntity<KpiSummaryDTO> getSummary(
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getSummary(period, from, to));
    }

    // ── GET /api/kpi/failures-by-equipment ───────────────────
    @Operation(summary = "Top équipements les plus défaillants")
    @GetMapping("/failures-by-equipment")
    public ResponseEntity<List<EquipmentFailureCountDTO>> getFailuresByEquipment(
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getFailuresByEquipment(period, from, to));
    }

    // ── GET /api/kpi/failures-by-status ──────────────────────
    @Operation(summary = "Répartition des pannes par statut")
    @GetMapping("/failures-by-status")
    public ResponseEntity<List<StatusCountDTO>> getFailuresByStatus(
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getFailuresByStatus(period, from, to));
    }

    // ── GET /api/kpi/failures-by-priority ────────────────────
    @Operation(summary = "Répartition des pannes par priorité")
    @GetMapping("/failures-by-priority")
    public ResponseEntity<List<PriorityCountDTO>> getFailuresByPriority(
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getFailuresByPriority(period, from, to));
    }

    // ── GET /api/kpi/mttr ─────────────────────────────────────
    @Operation(summary = "MTTR (temps moyen de réparation)",
            description = "Global si equipmentId omis, sinon spécifique à l'équipement.")
    @GetMapping("/mttr")
    public ResponseEntity<MttrResponseDTO> getMttr(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.calculateMTTR(equipmentId, period, from, to));
    }

    // ── GET /api/kpi/mtbf ─────────────────────────────────────
    @Operation(summary = "MTBF (temps moyen entre pannes)")
    @GetMapping("/mtbf")
    public ResponseEntity<MtbfResponseDTO> getMtbf(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.calculateMTBF(equipmentId, period, from, to));
    }

    // ── GET /api/kpi/availability ─────────────────────────────
    @Operation(summary = "Taux de disponibilité")
    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponseDTO> getAvailability(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.calculateAvailabilityRate(equipmentId, period, from, to));
    }

    // ── GET /api/kpi/monthly-trends ───────────────────────────
    @Operation(summary = "Évolution mensuelle des pannes et interventions")
    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(kpiService.getMonthlyTrends(months));
    }

    // ── GET /api/kpi/technician-workload ──────────────────────
    @Operation(summary = "Charge de travail par technicien")
    @GetMapping("/technician-workload")
    public ResponseEntity<List<TechnicianWorkloadDTO>> getTechnicianWorkload(
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(kpiService.getInterventionsByTechnician(period, from, to));
    }
}