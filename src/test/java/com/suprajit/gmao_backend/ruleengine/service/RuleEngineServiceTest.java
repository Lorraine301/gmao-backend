package com.suprajit.gmao_backend.ruleengine.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.suprajit.gmao_backend.entity.AiAnalysis;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.enums.AiAnalysisStatus;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.AiAnalysisRepository;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.ruleengine.dto.RuleEvaluationResult;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock private FailureRepository failureRepository;
    @Mock private InterventionRepository interventionRepository;
    @Mock private EquipmentRepository equipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AiAnalysisRepository aiAnalysisRepository;
    @Mock private PreventiveMaintenanceRepository preventiveMaintenanceRepository;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    private Equipment criticalEquipment;
    private Equipment mediumEquipment;
    private Failure failure;

    @BeforeEach
    void setUp() {
        criticalEquipment = Equipment.builder()
                .id(1L).code("WIN-0038").name("Winding Machine")
                .criticalityLevel(CriticalityLevel.High)
                .build();

        mediumEquipment = Equipment.builder()
                .id(2L).code("EXT-0006").name("Extrusion Machine")
                .criticalityLevel(CriticalityLevel.Medium)
                .build();

        failure = Failure.builder()
                .id(10L).failureCode("FAIL-0010")
                .equipment(criticalEquipment)
                .priority(FailurePriority.Medium)
                .failureType("Mechanical")
                .build();
    }

    // ── Règle 1 : équipement critique + 3+ pannes en 7 jours → Critical ──
    @Test
    void evaluateFailure_shouldTriggerRule1_whenCriticalEquipmentHasManyRecentFailures() {
        when(failureRepository.findRecentByEquipment(eq(criticalEquipment.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(new Failure(), new Failure(), new Failure())); // 3 pannes récentes
        when(interventionRepository.findAverageMttrByEquipment(anyLong())).thenReturn(null);
        when(userRepository.findBySpecialityAndAvailabilityStatus(any(), any())).thenReturn(List.of());
        when(userRepository.findByRole_NameAndAvailabilityStatus(any(), any())).thenReturn(List.of());

        RuleEvaluationResult result = ruleEngineService.evaluateFailure(failure);

        assertThat(result.getComputedPriority()).isEqualTo(FailurePriority.Critical);
        assertThat(result.getTriggeredRules()).isNotEmpty();
    }

    // ── Règle 2 : MTTR élevé → priorité relevée à High ──
    // NOTE : équipement Medium => la Règle 1 (protégée par "if criticalityLevel == High")
    // n'appelle jamais findRecentByEquipment. Aucun stub sur cette méthode ici.
    @Test
    void evaluateFailure_shouldTriggerRule2_whenMttrExceedsThreshold() {
        Failure mediumFailure = Failure.builder()
                .id(11L).failureCode("FAIL-0011")
                .equipment(mediumEquipment)
                .priority(FailurePriority.Medium)
                .failureType("Electrical")
                .build();

        when(interventionRepository.findAverageMttrByEquipment(mediumEquipment.getId()))
                .thenReturn(5.0); // > seuil de 4.0h
        when(userRepository.findBySpecialityAndAvailabilityStatus(any(), any())).thenReturn(List.of());
        when(userRepository.findByRole_NameAndAvailabilityStatus(any(), any())).thenReturn(List.of());

        RuleEvaluationResult result = ruleEngineService.evaluateFailure(mediumFailure);

        assertThat(result.getComputedPriority()).isEqualTo(FailurePriority.High);
    }

    // ── Règle 5 : LLM dit Critical + priorité actuelle < High → escalade ──
    // NOTE : une seule panne renvoyée par findTop2... => applyRule6AutoMaintenance()
    // s'arrête avant d'appeler preventiveMaintenanceRepository. Aucun stub dessus ici.
    @Test
    void applyPostLlmRules_shouldEscalateToCritical_whenLlmRiskIsCriticalAndPriorityBelowHigh() {
        AiAnalysis analysis = AiAnalysis.builder()
                .id(1L).failure(failure)
                .status(AiAnalysisStatus.Completed)
                .riskLevel(FailurePriority.Critical)
                .build();

        when(aiAnalysisRepository.findByFailureId(failure.getId())).thenReturn(Optional.of(analysis));
        when(failureRepository.findTop2ByEquipmentIdOrderByReportedAtDesc(criticalEquipment.getId()))
                .thenReturn(List.of(failure)); // une seule panne : règle 6 ne se déclenche pas

        ruleEngineService.applyPostLlmRules(failure);

        assertThat(failure.getPriority()).isEqualTo(FailurePriority.Critical);
        verify(failureRepository, times(1)).save(failure);
    }

    // ── Règle 6 : 2 pannes consécutives Critical → maintenance urgente créée ──
    @Test
    void applyPostLlmRules_shouldCreateUrgentMaintenance_whenLastTwoFailuresAreBothCritical() {
        Failure previousFailure = Failure.builder()
                .id(9L).failureCode("FAIL-0009")
                .equipment(criticalEquipment)
                .priority(FailurePriority.Critical)
                .llmPriority(FailurePriority.Critical)
                .build();

        Failure currentFailure = Failure.builder()
                .id(10L).failureCode("FAIL-0010")
                .equipment(criticalEquipment)
                .priority(FailurePriority.Critical)
                .llmPriority(FailurePriority.Critical)
                .build();

        AiAnalysis analysis = AiAnalysis.builder()
                .id(2L).failure(currentFailure)
                .status(AiAnalysisStatus.Completed)
                .riskLevel(FailurePriority.Critical)
                .build();

        when(aiAnalysisRepository.findByFailureId(currentFailure.getId())).thenReturn(Optional.of(analysis));
        when(failureRepository.findTop2ByEquipmentIdOrderByReportedAtDesc(criticalEquipment.getId()))
                .thenReturn(List.of(currentFailure, previousFailure)); // les 2 dernières, toutes Critical
        when(preventiveMaintenanceRepository.findByEquipmentId(criticalEquipment.getId()))
                .thenReturn(List.of()); // aucune maintenance urgente existante

        ruleEngineService.applyPostLlmRules(currentFailure);

        verify(preventiveMaintenanceRepository, times(1)).save(any(PreventiveMaintenance.class));
        verify(notificationService, times(1))
                .notifyAdminsAndSupervisors(eq("Critical"), any(), eq("PreventiveMaintenance"), any());
    }
}