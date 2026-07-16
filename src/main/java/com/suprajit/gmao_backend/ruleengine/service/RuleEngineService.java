package com.suprajit.gmao_backend.ruleengine.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.AiAnalysis;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.AiAnalysisStatus;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.AiAnalysisRepository;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
import com.suprajit.gmao_backend.ruleengine.dto.RuleEngineSummaryDTO;
import com.suprajit.gmao_backend.ruleengine.dto.RuleEvaluationResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RuleEngineService {

    private final FailureRepository failureRepository;
    private final InterventionRepository interventionRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;

    // Seuil MTTR au-delà duquel un équipement est considéré à risque (en heures)
    private static final double MTTR_THRESHOLD = 4.0;
    private static final int RECENT_FAILURES_WINDOW_DAYS = 7;
    private static final int RECENT_FAILURES_THRESHOLD = 3;

    // ── Compteurs en mémoire (repartent à zéro au redémarrage du serveur) ──
    private final AtomicLong rule1Count = new AtomicLong();
    private final AtomicLong rule2Count = new AtomicLong();
    private final AtomicLong rule3Count = new AtomicLong();
    private final AtomicLong rule4Count = new AtomicLong();
    private final AtomicLong rule5Count = new AtomicLong();
    private final AtomicLong rule6Count = new AtomicLong();

    // ── Point d'entrée principal : évalue une panne et calcule sa priorité ──
    public RuleEvaluationResult evaluateFailure(Failure failure) {
        List<String> triggeredRules = new ArrayList<>();
        FailurePriority computedPriority = failure.getPriority(); // priorité de départ (Medium)

        Equipment equipment = failure.getEquipment();

        // ── Règle 1 : équipement critique + plus de 3 pannes en 7 jours → urgence ──
        if (equipment.getCriticalityLevel() == CriticalityLevel.High) {
            LocalDateTime since = LocalDateTime.now().minusDays(RECENT_FAILURES_WINDOW_DAYS);
            long recentCount = failureRepository.findRecentByEquipment(equipment.getId(), since).size();

            if (recentCount >= RECENT_FAILURES_THRESHOLD) {
                computedPriority = FailurePriority.Critical;
                rule1Count.incrementAndGet();
                triggeredRules.add(String.format(
                    "Règle 1 : équipement critique (%s) avec %d pannes en %d jours → priorité Critical",
                    equipment.getCode(), recentCount, RECENT_FAILURES_WINDOW_DAYS));
            }
        }

        // ── Règle 2 : panne répétitive + MTTR dépasse le seuil → risque ──
        Double avgMttr = interventionRepository.findAverageMttrByEquipment(equipment.getId());
        if (avgMttr != null && avgMttr > MTTR_THRESHOLD) {
            if (computedPriority != FailurePriority.Critical) {
                computedPriority = FailurePriority.High;
            }
            rule2Count.incrementAndGet();
            triggeredRules.add(String.format(
                "Règle 2 : MTTR moyen de %.1fh dépasse le seuil de %.1fh → équipement à risque",
                avgMttr, MTTR_THRESHOLD));
        }

        // ── Règle 3 : criticité élevée de base → priorité minimale High ──
        if (equipment.getCriticalityLevel() == CriticalityLevel.High
                && computedPriority == FailurePriority.Medium) {
            computedPriority = FailurePriority.High;
            rule3Count.incrementAndGet();
            triggeredRules.add(String.format(
                "Règle 3 : équipement %s à criticité élevée → priorité minimale High",
                equipment.getCode()));
        }

        // ── Règle 4 : recommandation de technicien ──
        Optional<User> recommendedTechnician = findRecommendedTechnician(failure);
        if (recommendedTechnician.isPresent()) {
            rule4Count.incrementAndGet();
        }

        // Dans evaluateFailure(), après avoir calculé computedPriority :
        if (computedPriority == FailurePriority.Critical && !triggeredRules.isEmpty()) {
            notificationService.notifyAdminsAndSupervisors(
                "Critical",
                String.format("🚨 Panne urgente détectée sur %s : %s — Règle déclenchée",
                    failure.getEquipment().getCode(), failure.getTitle()),
                "Failure",
                failure.getId()
            );
        }
        return RuleEvaluationResult.builder()
                .computedPriority(computedPriority)
                .triggeredRules(triggeredRules)
                .recommendedTechnicianId(recommendedTechnician.map(User::getId).orElse(null))
                .recommendedTechnicianName(recommendedTechnician.map(User::getFullName).orElse(null))
                .build();
    }

    // ── Règle 4 : technicien disponible avec spécialité correspondante ──
    private Optional<User> findRecommendedTechnician(Failure failure) {
        String requiredSpeciality = mapFailureTypeToSpeciality(failure.getFailureType());

        List<User> candidates = userRepository
                .findBySpecialityAndAvailabilityStatus(requiredSpeciality, "Available");

        if (!candidates.isEmpty()) {
            return Optional.of(candidates.get(0));
        }

        // Fallback : n'importe quel technicien disponible
        List<User> anyTechnician = userRepository
                .findByRole_NameAndAvailabilityStatus("Technician", "Available");

        return anyTechnician.stream().findFirst();
    }

    private String mapFailureTypeToSpeciality(String failureType) {
        if (failureType == null) return "Électromécanique";
        return switch (failureType) {
            case "Electrical" -> "Électrique";
            case "Mechanical" -> "Mécanique";
            default -> "Électromécanique";
        };
    }

    // ── GET /api/equipments/at-risk ──────────────────────────
    public List<AtRiskEquipmentDTO> findAtRiskEquipments() {
        List<Equipment> criticalEquipments = equipmentRepository
                .findByCriticalityLevel(CriticalityLevel.High);

        List<AtRiskEquipmentDTO> atRisk = new ArrayList<>();
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_FAILURES_WINDOW_DAYS);

        for (Equipment eq : criticalEquipments) {
            long recentFailures = failureRepository.findRecentByEquipment(eq.getId(), since).size();
            Double avgMttr = interventionRepository.findAverageMttrByEquipment(eq.getId());
            double mttr = avgMttr != null ? avgMttr : 0.0;

            boolean isAtRisk = recentFailures >= RECENT_FAILURES_THRESHOLD || mttr > MTTR_THRESHOLD;

            if (isAtRisk) {
                String reason = recentFailures >= RECENT_FAILURES_THRESHOLD
                        ? String.format("%d pannes signalées dans les %d derniers jours",
                                recentFailures, RECENT_FAILURES_WINDOW_DAYS)
                        : String.format("Temps moyen de réparation élevé (%.1fh)", mttr);

                atRisk.add(AtRiskEquipmentDTO.builder()
                        .equipmentId(eq.getId())
                        .equipmentCode(eq.getCode())
                        .equipmentName(eq.getName())
                        .criticalityLevel(eq.getCriticalityLevel().name())
                        .recentFailuresCount(recentFailures)
                        .averageMttr(mttr)
                        .riskReason(reason)
                        .build());
            }
        }

        return atRisk;
    }

    // ══════════════════════════════════════════════════════════
    // Règles 5 & 6 — appliquées APRÈS que l'analyse LLM soit disponible
    // ══════════════════════════════════════════════════════════

    // ── Point d'entrée : vérifie si une analyse LLM existe, applique les règles 5/6 ──
    @Transactional
    public void applyPostLlmRules(Failure failure) {
        AiAnalysis analysis = aiAnalysisRepository.findByFailureId(failure.getId()).orElse(null);

        if (analysis == null || analysis.getStatus() != AiAnalysisStatus.Completed) {
            return; // pas d'analyse disponible ou échouée, rien à faire
        }

        applyRule5Escalation(failure, analysis);
        applyRule6AutoMaintenance(failure);
    }

    // ── Règle 5 : escalade si risk_level LLM = Critical et priorité actuelle < High ──
    private void applyRule5Escalation(Failure failure, AiAnalysis analysis) {
        if (analysis.getRiskLevel() == FailurePriority.Critical
                && failure.getPriority().ordinal() < FailurePriority.High.ordinal()) {

            failure.setPriority(FailurePriority.Critical);
            failureRepository.save(failure);
            rule5Count.incrementAndGet();

            System.out.println("[RULE ENGINE V2] Règle 5 déclenchée : panne "
                    + failure.getFailureCode() + " escaladée à Critical (analyse LLM)");
        }
    }

    // ── Règle 6 : 2 pannes consécutives llm_priority=Critical → maintenance urgente ──
    private void applyRule6AutoMaintenance(Failure failure) {
        Equipment equipment = failure.getEquipment();

        List<Failure> lastTwo = failureRepository
                .findTop2ByEquipmentIdOrderByReportedAtDesc(equipment.getId());

        if (lastTwo.size() < 2) return;

        boolean bothCritical = lastTwo.stream()
                .allMatch(f -> f.getLlmPriority() == FailurePriority.Critical);

        if (!bothCritical) return;

        // ── Évite les doublons : ne pas créer si une maintenance urgente
        // Scheduled/Overdue existe déjà pour cet équipement ──
        boolean alreadyExists = preventiveMaintenanceRepository
                .findByEquipmentId(equipment.getId()).stream()
                .anyMatch(pm -> "Urgente".equals(pm.getMaintenanceType())
                        && (pm.getStatus() == MaintenanceStatus.Scheduled
                            || pm.getStatus() == MaintenanceStatus.Overdue));

        if (alreadyExists) return;

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        PreventiveMaintenance urgentMaintenance = PreventiveMaintenance.builder()
                .equipment(equipment)
                .maintenanceType("Urgente")
                .frequencyDays(1)
                .lastMaintenanceDate(today)
                .nextMaintenanceDate(tomorrow)
                .nextReminderDate(today)
                .status(MaintenanceStatus.Scheduled)
                .build();

        preventiveMaintenanceRepository.save(urgentMaintenance);
        rule6Count.incrementAndGet();

        notificationService.notifyAdminsAndSupervisors(
            "Critical",
            String.format("🚨 Maintenance urgente créée automatiquement pour %s (%s) : "
                    + "2 pannes critiques consécutives détectées par l'IA",
                    equipment.getCode(), equipment.getName()),
            "PreventiveMaintenance",
            urgentMaintenance.getId()
        );

        System.out.println("[RULE ENGINE V2] Règle 6 déclenchée : maintenance urgente créée pour "
                + equipment.getCode());
    }

    // ── Statistiques du Rule Engine ──────────────────────────
    public RuleEngineSummaryDTO getSummary() {
        return RuleEngineSummaryDTO.builder()
                .rule1TriggeredCount(rule1Count.get())
                .rule2TriggeredCount(rule2Count.get())
                .rule3TriggeredCount(rule3Count.get())
                .rule4RecommendationsCount(rule4Count.get())
                .rule5EscalatedCount(rule5Count.get())
                .rule6MaintenancesCreatedCount(rule6Count.get())
                .build();
    }
}