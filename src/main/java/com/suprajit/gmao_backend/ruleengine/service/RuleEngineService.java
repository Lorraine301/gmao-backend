package com.suprajit.gmao_backend.ruleengine.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
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

    // Seuil MTTR au-delà duquel un équipement est considéré à risque (en heures)
    private static final double MTTR_THRESHOLD = 4.0;
    private static final int RECENT_FAILURES_WINDOW_DAYS = 7;
    private static final int RECENT_FAILURES_THRESHOLD = 3;

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
            triggeredRules.add(String.format(
                "Règle 2 : MTTR moyen de %.1fh dépasse le seuil de %.1fh → équipement à risque",
                avgMttr, MTTR_THRESHOLD));
        }

        // ── Règle 3 : criticité élevée de base → priorité minimale High ──
        if (equipment.getCriticalityLevel() == CriticalityLevel.High
                && computedPriority == FailurePriority.Medium) {
            computedPriority = FailurePriority.High;
            triggeredRules.add(String.format(
                "Règle 3 : équipement %s à criticité élevée → priorité minimale High",
                equipment.getCode()));
        }

        // ── Règle 4 : recommandation de technicien ──
        Optional<User> recommendedTechnician = findRecommendedTechnician(failure);

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
}