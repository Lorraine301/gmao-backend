package com.suprajit.gmao_backend.failure.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.ai.service.AiAnalysisService;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.failure.dto.FailureRequestDTO;
import com.suprajit.gmao_backend.failure.dto.FailureResponseDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.ruleengine.service.RuleEngineService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class FailureService {

    private final FailureRepository failureRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RuleEngineService ruleEngineService;
    private final AiAnalysisService aiAnalysisService;

    // ── Mapper entité → DTO (résout équipement + déclarant) ──
    private FailureResponseDTO toDTO(Failure f) {
        return FailureResponseDTO.builder()
                .id(f.getId())
                .failureCode(f.getFailureCode())
                .equipmentId(f.getEquipment().getId())
                .equipmentCode(f.getEquipment().getCode())
                .equipmentName(f.getEquipment().getName())
                .equipmentType(f.getEquipment().getType())
                .title(f.getTitle())
                .description(f.getDescription())
                .failureType(f.getFailureType())
                .priority(f.getPriority())
                .llmPriority(f.getLlmPriority())
                .status(f.getStatus())
                .reportedById(f.getReportedBy().getId())
                .reportedByName(f.getReportedBy().getFullName())
                .reportedByEmployeeCode(f.getReportedBy().getEmployeeCode())
                .reportedChannel(f.getReportedChannel())
                .reportedAt(f.getReportedAt())
                .resolvedAt(f.getResolvedAt())
                .llmProcessed(f.getLlmProcessed())
                .ruleEngineTriggered(f.getRuleEngineTriggered())
                        .recommendedTechnicianId(f.getRecommendedTechnicianId())
                        .recommendedTechnicianName(
                            f.getRecommendedTechnicianId() != null
                                ? userRepository.findById(f.getRecommendedTechnicianId())
                                    .map(User::getFullName).orElse(null)
                                : null
                        )         
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
                
    }

    // ── Génère le prochain code panne (FAIL-0001, FAIL-0002...) ──
    private String generateFailureCode() {
        long count = failureRepository.count() + 1;
        return String.format("FAIL-%04d", count);
    }

    // ── Récupère l'utilisateur connecté depuis le token JWT ──
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + email));
    }

    // ── DECLARE (création) ───────────────────────────────────
    public FailureResponseDTO declare(FailureRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Équipement non trouvé avec l'id : " + dto.getEquipmentId()));

        User currentUser = getCurrentUser(); // reportedBy automatique depuis JWT

        Failure failure = Failure.builder()
                .failureCode(generateFailureCode())
                .equipment(equipment)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .failureType(dto.getFailureType())
                .priority(FailurePriority.Medium) // valeur par défaut, sera affinée par Rule Engine (carte 20)
                .status(FailureStatus.Open)
                .reportedBy(currentUser)
                .reportedChannel(dto.getReportedChannel() != null ? dto.getReportedChannel() : "Web")
                .reportedAt(LocalDateTime.now())
                .llmProcessed(false)
                .build();

        Failure saved = failureRepository.save(failure);

        // ── Évaluation par le Rule Engine ──────────────────────
        var ruleResult = ruleEngineService.evaluateFailure(saved);
        saved.setPriority(ruleResult.getComputedPriority());

        // ← Stocker si une règle a été déclenchée
        saved.setRuleEngineTriggered(!ruleResult.getTriggeredRules().isEmpty());
        saved.setRecommendedTechnicianId(ruleResult.getRecommendedTechnicianId());
        saved = failureRepository.save(saved);

                if (!ruleResult.getTriggeredRules().isEmpty()) {
                    System.out.println("[RULE ENGINE] Panne " + saved.getFailureCode() + " :");
                    ruleResult.getTriggeredRules().forEach(r -> System.out.println("  → " + r));
                }

                return toDTO(saved);
    }

    // ── READ ALL avec filtres ─────────────────────────────────
    public List<FailureResponseDTO> findAll(FailureStatus status, FailurePriority priority, Long equipmentId) {
        return failureRepository.findWithFilters(status, priority, equipmentId)
                .stream().map(this::toDTO).toList();
    }

    // ── READ ONE ────────────────────────────────────────────
    public FailureResponseDTO findById(Long id) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Panne non trouvée avec l'id : " + id));
        return toDTO(failure);
    }

    // ── UPDATE STATUS ──────────────────────────────────────────
    public FailureResponseDTO updateStatus(Long id, FailureStatus newStatus) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Panne non trouvée avec l'id : " + id));

        failure.setStatus(newStatus);

        // Si on clôture la panne, enregistrer la date de résolution
        if (newStatus == FailureStatus.Resolved || newStatus == FailureStatus.Closed) {
            failure.setResolvedAt(LocalDateTime.now());
        }

        return toDTO(failureRepository.save(failure));
    }

    // ── UPDATE PRIORITY ─────────────────────────────────────────
    public FailureResponseDTO updatePriority(Long id, FailurePriority newPriority) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Panne non trouvée avec l'id : " + id));

        failure.setPriority(newPriority);
        return toDTO(failureRepository.save(failure));
    }
    // ── READ URGENT (Critical + non clôturées) ─────────────────────
    public List<FailureResponseDTO> findUrgent() {
    return failureRepository.findUrgent()
            .stream().map(this::toDTO).toList();
    }
    // ── Clôture définitive (Admin/Supervisor uniquement) ────
    public FailureResponseDTO closeFailure(Long id) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Panne non trouvée avec l'id : " + id));

        if (failure.getStatus() != FailureStatus.Resolved) {
            throw new IllegalStateException(
                "Seule une panne au statut 'Resolved' peut être clôturée définitivement. Statut actuel : "
                + failure.getStatus());
        }

        failure.setStatus(FailureStatus.Closed);

        return toDTO(failureRepository.save(failure));
    }
    // ── Réévaluation forcée du Rule Engine (V1 + V2 si analyse dispo) ──
    public FailureResponseDTO reevaluateWithRuleEngine(Long id) {
        Failure failure = failureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Panne non trouvée avec l'id : " + id));

        var ruleResult = ruleEngineService.evaluateFailure(failure);
        failure.setPriority(ruleResult.getComputedPriority());
        failure.setRuleEngineTriggered(!ruleResult.getTriggeredRules().isEmpty());
        failure.setRecommendedTechnicianId(ruleResult.getRecommendedTechnicianId());
        Failure saved = failureRepository.save(failure);

        // ── Applique aussi les règles 5/6 si une analyse LLM existe déjà ──
        ruleEngineService.applyPostLlmRules(saved);

        Failure refreshed = failureRepository.findById(saved.getId())
                .orElseThrow(() -> new EntityNotFoundException("Panne non trouvée avec l'id : " + id));

        return toDTO(refreshed);
    }

}