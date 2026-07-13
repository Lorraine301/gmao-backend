package com.suprajit.gmao_backend.kpi.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.kpi.dto.AvailabilityResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.EquipmentFailureCountDTO;
import com.suprajit.gmao_backend.kpi.dto.KpiSummaryDTO;
import com.suprajit.gmao_backend.kpi.dto.MonthlyTrendDTO;
import com.suprajit.gmao_backend.kpi.dto.MtbfResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.MttrResponseDTO;
import com.suprajit.gmao_backend.kpi.dto.PriorityCountDTO;
import com.suprajit.gmao_backend.kpi.dto.StatusCountDTO;
import com.suprajit.gmao_backend.kpi.dto.TechnicianWorkloadDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiService {

    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final EquipmentRepository equipmentRepository;

    private static final int DEFAULT_PERIOD_DAYS = 30;

    // ── Résolution de la période (from/to prioritaire, sinon period en jours) ──
    private LocalDateTime[] resolvePeriod(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime;
        LocalDateTime toDateTime;

        if (from != null && to != null) {
            fromDateTime = from.atStartOfDay();
            toDateTime = to.atTime(23, 59, 59);
        } else {
            int days = (period != null) ? period : DEFAULT_PERIOD_DAYS;
            toDateTime = LocalDateTime.now();
            fromDateTime = toDateTime.minusDays(days);
        }
        return new LocalDateTime[]{fromDateTime, toDateTime};
    }

    private int daysBetween(LocalDateTime from, LocalDateTime to) {
        return (int) Math.max(1, Duration.between(from, to).toDays());
    }

    // ── MTTR : moyenne des durées des interventions complétées ──
    public MttrResponseDTO calculateMTTR(Long equipmentId, Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);

        List<Intervention> completed = interventionRepository
                .findByStatusAndStartTimeBetween(InterventionStatus.Completed, range[0], range[1]);

        if (equipmentId != null) {
            completed = completed.stream()
                    .filter(i -> i.getFailure().getEquipment().getId().equals(equipmentId))
                    .toList();
        }

        List<Double> durations = completed.stream()
                .map(Intervention::getDuration)
                .filter(Objects::nonNull)
                .toList();

        Double mttr = durations.isEmpty() ? null
                : durations.stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        String equipmentCode = null;
        if (equipmentId != null) {
            equipmentCode = equipmentRepository.findById(equipmentId)
                    .map(Equipment::getCode).orElse(null);
        }

        return MttrResponseDTO.builder()
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .mttr(mttr != null ? Math.round(mttr * 100.0) / 100.0 : null)
                .periodDays(daysBetween(range[0], range[1]))
                .build();
    }

    // ── MTBF : (période en heures) / nombre de pannes ──
    public MtbfResponseDTO calculateMTBF(Long equipmentId, Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        long periodHours = Duration.between(range[0], range[1]).toHours();

        List<Failure> failures = failureRepository.findByReportedAtBetween(range[0], range[1]);
        if (equipmentId != null) {
            failures = failures.stream()
                    .filter(f -> f.getEquipment().getId().equals(equipmentId))
                    .toList();
        }

        Double mtbf = failures.isEmpty() ? null : (double) periodHours / failures.size();

        String equipmentCode = null;
        if (equipmentId != null) {
            equipmentCode = equipmentRepository.findById(equipmentId)
                    .map(Equipment::getCode).orElse(null);
        }

        return MtbfResponseDTO.builder()
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .mtbf(mtbf != null ? Math.round(mtbf * 100.0) / 100.0 : null)
                .periodDays(daysBetween(range[0], range[1]))
                .build();
    }

    // ── Taux de disponibilité : (période - temps d'arrêt) / période * 100 ──
    public AvailabilityResponseDTO calculateAvailabilityRate(Long equipmentId, Integer period,
            LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        double periodHours = Duration.between(range[0], range[1]).toHours();

        List<Intervention> completed = interventionRepository
                .findByStatusAndStartTimeBetween(InterventionStatus.Completed, range[0], range[1]);

        if (equipmentId != null) {
            completed = completed.stream()
                    .filter(i -> i.getFailure().getEquipment().getId().equals(equipmentId))
                    .toList();
        }

        double totalDowntime = completed.stream()
                .map(Intervention::getDuration)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        double availability = periodHours > 0
                ? Math.max(0, Math.min(100, ((periodHours - totalDowntime) / periodHours) * 100))
                : 100;

        String equipmentCode = null;
        if (equipmentId != null) {
            equipmentCode = equipmentRepository.findById(equipmentId)
                    .map(Equipment::getCode).orElse(null);
        }

        return AvailabilityResponseDTO.builder()
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .availabilityRate(Math.round(availability * 100.0) / 100.0)
                .periodDays(daysBetween(range[0], range[1]))
                .build();
    }

    // ── Top équipements les plus défaillants ──
    // NOTE : on groupe par equipmentId (Long), PAS par l'entité Equipment elle-même.
    // Grouper par une entité JPA en lazy loading (Failure.equipment) provoque un
    // LazyInitializationException, car Collectors.groupingBy appelle hashCode()/equals()
    // sur la clé, ce qui peut arriver après la fermeture de la session Hibernate.
    public List<EquipmentFailureCountDTO> getFailuresByEquipment(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        List<Failure> failures = failureRepository.findByReportedAtBetween(range[0], range[1]);

        Map<Long, Long> countsByEquipmentId = failures.stream()
                .collect(Collectors.groupingBy(f -> f.getEquipment().getId(), Collectors.counting()));

        Map<Long, Equipment> equipmentById = failures.stream()
                .map(Failure::getEquipment)
                .collect(Collectors.toMap(Equipment::getId, eq -> eq, (a, b) -> a));

        return countsByEquipmentId.entrySet().stream()
                .map(e -> {
                    Equipment eq = equipmentById.get(e.getKey());
                    return EquipmentFailureCountDTO.builder()
                            .equipmentId(eq.getId())
                            .equipmentCode(eq.getCode())
                            .equipmentName(eq.getName())
                            .failureCount(e.getValue())
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getFailureCount(), a.getFailureCount()))
                .toList();
    }

    // ── Répartition des pannes par statut ──
    public List<StatusCountDTO> getFailuresByStatus(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        List<Failure> failures = failureRepository.findByReportedAtBetween(range[0], range[1]);

        Map<FailureStatus, Long> counts = failures.stream()
                .collect(Collectors.groupingBy(Failure::getStatus, Collectors.counting()));

        return Arrays.stream(FailureStatus.values())
                .map(status -> StatusCountDTO.builder()
                        .status(status.name())
                        .count(counts.getOrDefault(status, 0L))
                        .build())
                .toList();
    }

    // ── Répartition des pannes par priorité ──
    public List<PriorityCountDTO> getFailuresByPriority(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        List<Failure> failures = failureRepository.findByReportedAtBetween(range[0], range[1]);

        Map<FailurePriority, Long> counts = failures.stream()
                .collect(Collectors.groupingBy(Failure::getPriority, Collectors.counting()));

        return Arrays.stream(FailurePriority.values())
                .map(priority -> PriorityCountDTO.builder()
                        .priority(priority.name())
                        .count(counts.getOrDefault(priority, 0L))
                        .build())
                .toList();
    }

    // ── Charge de travail par technicien ──
    // NOTE : même précaution que pour getFailuresByEquipment() — on groupe par
    // technicianId (Long), pas par l'entité User (relation lazy Intervention.technician).
    public List<TechnicianWorkloadDTO> getInterventionsByTechnician(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);
        List<Intervention> interventions = interventionRepository.findByStartTimeBetween(range[0], range[1]);

        Map<Long, Long> countsByTechnicianId = interventions.stream()
                .collect(Collectors.groupingBy(i -> i.getTechnician().getId(), Collectors.counting()));

        Map<Long, User> technicianById = interventions.stream()
                .map(Intervention::getTechnician)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        return countsByTechnicianId.entrySet().stream()
                .map(e -> {
                    User tech = technicianById.get(e.getKey());
                    return TechnicianWorkloadDTO.builder()
                            .technicianId(tech.getId())
                            .technicianName(tech.getFullName())
                            .employeeCode(tech.getEmployeeCode())
                            .interventionCount(e.getValue())
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getInterventionCount(), a.getInterventionCount()))
                .toList();
    }

    // ── Évolution mensuelle des pannes et interventions ──
    public List<MonthlyTrendDTO> getMonthlyTrends(int months) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth currentMonth = YearMonth.now();

        List<MonthlyTrendDTO> trends = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

            long failureCount = failureRepository.findByReportedAtBetween(start, end).size();
            long interventionCount = interventionRepository.findByStartTimeBetween(start, end).size();

            trends.add(MonthlyTrendDTO.builder()
                    .month(ym.format(formatter))
                    .failureCount(failureCount)
                    .interventionCount(interventionCount)
                    .build());
        }

        return trends;
    }

    // ── Résumé global (cartes KPI du dashboard) ──
    public KpiSummaryDTO getSummary(Integer period, LocalDate from, LocalDate to) {
        LocalDateTime[] range = resolvePeriod(period, from, to);

        List<Failure> failures = failureRepository.findByReportedAtBetween(range[0], range[1]);
        long total = failures.size();
        long resolved = failures.stream()
                .filter(f -> f.getStatus() == FailureStatus.Resolved || f.getStatus() == FailureStatus.Closed)
                .count();

        double resolutionRate = total > 0 ? (resolved * 100.0) / total : 0;

        MttrResponseDTO globalMttr = calculateMTTR(null, period, from, to);

        return KpiSummaryDTO.builder()
                .totalFailures(total)
                .resolvedFailures(resolved)
                .resolutionRate(Math.round(resolutionRate * 100.0) / 100.0)
                .averageMttr(globalMttr.getMttr())
                .periodDays(daysBetween(range[0], range[1]))
                .build();
    }
}