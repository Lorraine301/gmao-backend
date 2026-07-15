package com.suprajit.gmao_backend.weeklyreport.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.WeeklyReport;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.pdf.service.PdfService;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.repository.WeeklyReportRepository;
import com.suprajit.gmao_backend.weeklyreport.dto.WeeklyReportResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;

    private static final int CRITICAL_THRESHOLD = 2; // "plus de 2 pannes"

    // ── Mapper ──────────────────────────────────────────────
    private WeeklyReportResponseDTO toDTO(WeeklyReport r) {
        return WeeklyReportResponseDTO.builder()
                .id(r.getId())
                .weekNumber(r.getWeekNumber())
                .totalFailures(r.getTotalFailures())
                .resolvedFailures(r.getResolvedFailures())
                .averageRepairTime(r.getAverageRepairTime())
                .criticalMachines(r.getCriticalMachines())
                .llmSummary(r.getLlmSummary())
                .recommendations(r.getRecommendations())
                .generatedAt(r.getGeneratedAt())
                .pdfPath(r.getPdfPath())
                .generatedBy(r.getGeneratedBy())
                .build();
    }

// ── Génération automatique (appelée par le scheduler chaque dimanche) ──
    public WeeklyReportResponseDTO generateWeeklyReport() {
        return generateWeeklyReport("System");
    }

    // ── Génération avec traçabilité de qui a déclenché le rapport ──
    public WeeklyReportResponseDTO generateWeeklyReport(String generatedBy) {
        LocalDate today = LocalDate.now();

        // Semaine ISO en cours : lundi → dimanche
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekEnd.atTime(23, 59, 59);

        int weekNumber = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        // ── Pannes de la semaine ──
        List<Failure> weekFailures = failureRepository.findByReportedAtBetween(start, end);
        int total = weekFailures.size();
        int resolved = (int) weekFailures.stream()
                .filter(f -> f.getStatus() == FailureStatus.Resolved || f.getStatus() == FailureStatus.Closed)
                .count();

        // ── MTTR de la semaine (interventions complétées dans la période) ──
        List<Intervention> weekInterventions = interventionRepository
                .findByStatusAndStartTimeBetween(InterventionStatus.Completed, start, end);

        List<Double> durations = weekInterventions.stream()
                .map(Intervention::getDuration)
                .filter(Objects::nonNull)
                .toList();

        Double avgRepairTime = durations.isEmpty() ? null
                : durations.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        // ── Équipements critiques : plus de 2 pannes cette semaine ──
        Map<Long, Long> failureCountByEquipmentId = weekFailures.stream()
                .collect(Collectors.groupingBy(f -> f.getEquipment().getId(), Collectors.counting()));

        Map<Long, Equipment> equipmentById = weekFailures.stream()
                .map(Failure::getEquipment)
                .collect(Collectors.toMap(Equipment::getId, eq -> eq, (a, b) -> a));

        String criticalMachines = failureCountByEquipmentId.entrySet().stream()
                .filter(e -> e.getValue() > CRITICAL_THRESHOLD)
                .map(e -> equipmentById.get(e.getKey()).getCode())
                .collect(Collectors.joining(", "));

        WeeklyReport report = WeeklyReport.builder()
                .weekNumber(weekNumber)
                .totalFailures(total)
                .resolvedFailures(resolved)
                .averageRepairTime(avgRepairTime != null ? Math.round(avgRepairTime * 100.0) / 100.0 : null)
                .criticalMachines(criticalMachines.isEmpty() ? null : criticalMachines)
                .llmSummary(null)
                .recommendations(null)
                .generatedBy(generatedBy)
                .generatedAt(LocalDateTime.now())
                .pdfPath(null)
                .build();

        WeeklyReport saved = weeklyReportRepository.save(report);

                try {
                String pdfPath = pdfService.generateWeeklyReportPdf(saved);
                saved.setPdfPath(pdfPath);
                saved = weeklyReportRepository.save(saved);
                } catch (IOException e) {
                System.out.println("[WEEKLY REPORT] Erreur lors de la génération du PDF : " + e.getMessage());
                }

                return toDTO(saved);
    }
    // ── Résoudre le nom de l'utilisateur connecté (pour generatedBy) ──
    public String getCurrentUserName() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getFullName)
                .orElse(email);
}

    // ── READ ALL (plus récent en premier) ───────────────────
    public List<WeeklyReportResponseDTO> findAll() {
        return weeklyReportRepository.findAllByOrderByGeneratedAtDesc()
                .stream().map(this::toDTO).toList();
    }

    // ── READ BY ID ────────────────────────────────────────────
    public WeeklyReportResponseDTO findById(Long id) {
        return toDTO(weeklyReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Rapport hebdomadaire non trouvé avec l'id : " + id)));
    }
}