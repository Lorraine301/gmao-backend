package com.suprajit.gmao_backend.weeklyreport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.MonthlyReport;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.MonthlyReportRepository;
import com.suprajit.gmao_backend.weeklyreport.dto.MonthlyReportResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;

    private MonthlyReportResponseDTO toDTO(MonthlyReport r) {
        return MonthlyReportResponseDTO.builder()
                .id(r.getId())
                .month(r.getMonth())
                .year(r.getYear())
                .totalFailures(r.getTotalFailures())
                .totalInterventions(r.getTotalInterventions())
                .averageMttr(r.getAverageMttr())
                .topEquipment(r.getTopEquipment())
                .llmSummary(r.getLlmSummary())
                .recommendations(r.getRecommendations())
                .generatedBy(r.getGeneratedBy())
                .generatedAt(r.getGeneratedAt())
                .pdfPath(r.getPdfPath())
                .build();
    }

     // ── Lecture seule : retourne le bilan s'il a déjà été généré ──
    public MonthlyReportResponseDTO findByMonthYear(int month, int year) {
        return monthlyReportRepository.findByMonthAndYear(month, year)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Le bilan de " + month + "/" + year + " n'a pas encore été généré."));
    }
    // ── Force la régénération (usage TEST uniquement, écrase l'existant) ──
    public MonthlyReportResponseDTO forceRegenerate(int month, int year, String generatedBy) {
        monthlyReportRepository.findByMonthAndYear(month, year)
                .ifPresent(monthlyReportRepository::delete);
        return generate(month, year, generatedBy);
    }

    // ── Génération manuelle ou automatique (une seule fois par mois) ──
    public MonthlyReportResponseDTO generateIfAbsent(int month, int year, String generatedBy) {
        return monthlyReportRepository.findByMonthAndYear(month, year)
                .map(this::toDTO)
                .orElseGet(() -> generate(month, year, generatedBy));
    }

    private MonthlyReportResponseDTO generate(int month, int year, String generatedBy) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Failure> monthFailures = failureRepository.findByReportedAtBetween(start, end);
        List<Intervention> monthInterventions = interventionRepository.findByStartTimeBetween(start, end);

        List<Double> durations = monthInterventions.stream()
                .filter(i -> i.getStatus() == InterventionStatus.Completed)
                .map(Intervention::getDuration)
                .filter(Objects::nonNull)
                .toList();

        Double avgMttr = durations.isEmpty() ? null
                : durations.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        Map<String, Long> countByEquipmentCode = monthFailures.stream()
                .collect(Collectors.groupingBy(f -> f.getEquipment().getCode(), Collectors.counting()));

        String topEquipment = countByEquipmentCode.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + " pannes)")
                .collect(Collectors.joining(", "));

        MonthlyReport report = MonthlyReport.builder()
                .month(month)
                .year(year)
                .totalFailures(monthFailures.size())
                .totalInterventions(monthInterventions.size())
                .averageMttr(avgMttr != null ? Math.round(avgMttr * 100.0) / 100.0 : null)
                .topEquipment(topEquipment.isEmpty() ? null : topEquipment)
                .llmSummary(null)
                .recommendations(null)
                .generatedBy(generatedBy)
                .generatedAt(LocalDateTime.now())
                .pdfPath(null)
                .build();

        return toDTO(monthlyReportRepository.save(report));
    }

    public MonthlyReportResponseDTO findById(Long id) {
        return toDTO(monthlyReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bilan mensuel non trouvé : " + id)));
    }

    public List<MonthlyReportResponseDTO> findAll() {
        return monthlyReportRepository.findAllByOrderByYearDescMonthDesc()
                .stream().map(this::toDTO).toList();
    }
}