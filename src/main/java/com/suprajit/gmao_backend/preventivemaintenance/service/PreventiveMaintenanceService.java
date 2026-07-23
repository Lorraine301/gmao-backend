package com.suprajit.gmao_backend.preventivemaintenance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.PreventiveMaintenanceHistory;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.ExecutionStatus;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceHistoryResponseDTO;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceRequestDTO;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceResponseDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceHistoryRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.sparepart.dto.ConsumeStockRequestDTO;
import com.suprajit.gmao_backend.sparepart.service.SparePartService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PreventiveMaintenanceService {

    private final PreventiveMaintenanceRepository pmRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final SparePartService sparePartService;
    private final NotificationService notificationService;
    private final PreventiveMaintenanceHistoryRepository historyRepository;     

    // ── Mapper entité → DTO ─────────────────────────────────
    private PreventiveMaintenanceResponseDTO toDTO(PreventiveMaintenance pm) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), pm.getNextMaintenanceDate());
        return PreventiveMaintenanceResponseDTO.builder()
                .id(pm.getId())
                .equipmentId(pm.getEquipment().getId())
                .equipmentCode(pm.getEquipment().getCode())
                .equipmentName(pm.getEquipment().getName())
                .equipmentType(pm.getEquipment().getType())
                .equipmentLocation(pm.getEquipment().getLocation())
                .maintenanceType(pm.getMaintenanceType())
                .frequencyDays(pm.getFrequencyDays())
                .lastMaintenanceDate(pm.getLastMaintenanceDate())
                .nextMaintenanceDate(pm.getNextMaintenanceDate())
                .nextReminderDate(pm.getNextReminderDate())
                .status(pm.getStatus())
                .daysUntilNext(daysUntil) // négatif si en retard
                .createdAt(pm.getCreatedAt())
                .updatedAt(pm.getUpdatedAt())
                .assignedTechnicianId(pm.getAssignedTechnician() != null ? pm.getAssignedTechnician().getId() : null)
                .assignedTechnicianName(pm.getAssignedTechnician() != null ? pm.getAssignedTechnician().getFullName() : null)
                .assignedByName(pm.getAssignedBy() != null ? pm.getAssignedBy().getFullName() : null)
                .executionStatus(pm.getExecutionStatus() != null ? pm.getExecutionStatus().name() : null)
                .problemFound(pm.getProblemFound())
                .solution(pm.getSolution())
                .technicianStartTime(pm.getTechnicianStartTime())
                .technicianEndTime(pm.getTechnicianEndTime())
                .build();
    }

    // ── SCHEDULE (création) ─────────────────────────────────
    public PreventiveMaintenanceResponseDTO schedule(PreventiveMaintenanceRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.getEquipmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Équipement non trouvé avec l'id : " + dto.getEquipmentId()));

        // Calcul automatique des dates
        LocalDate nextDate     = dto.getLastMaintenanceDate().plusDays(dto.getFrequencyDays());
        LocalDate reminderDate = nextDate.minusDays(7); // rappel 7 jours avant

        // Statut initial : Overdue si la date calculée est déjà dépassée
        MaintenanceStatus initialStatus = nextDate.isBefore(LocalDate.now())
                ? MaintenanceStatus.Overdue
                : MaintenanceStatus.Scheduled;

        PreventiveMaintenance pm = PreventiveMaintenance.builder()
                .equipment(equipment)
                .maintenanceType(dto.getMaintenanceType())
                .frequencyDays(dto.getFrequencyDays())
                .lastMaintenanceDate(dto.getLastMaintenanceDate())
                .nextMaintenanceDate(nextDate)
                .nextReminderDate(reminderDate)
                .status(initialStatus)
                .build();

        return toDTO(pmRepository.save(pm));
    }

    // ── READ ALL ────────────────────────────────────────────
    public List<PreventiveMaintenanceResponseDTO> findAll() {
        return pmRepository.findAll()
                .stream().map(this::toDTO).toList();
    }

    // ── READ BY EQUIPMENT ────────────────────────────────────
    public List<PreventiveMaintenanceResponseDTO> findByEquipment(Long equipmentId) {
        return pmRepository.findByEquipmentId(equipmentId)
                .stream().map(this::toDTO).toList();
    }

    // ── READ OVERDUE ─────────────────────────────────────────
    public List<PreventiveMaintenanceResponseDTO> findOverdue() {
        return pmRepository.findOverdue(LocalDate.now())
                .stream().map(this::toDTO).toList();
    }

    // ── COMPLETE ─────────────────────────────────────────────
    public PreventiveMaintenanceResponseDTO complete(Long id) {
        PreventiveMaintenance pm = pmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance non trouvée avec l'id : " + id));

        LocalDate today       = LocalDate.now();
        LocalDate nextDate    = today.plusDays(pm.getFrequencyDays());
        LocalDate reminderDate = nextDate.minusDays(7);

        pm.setLastMaintenanceDate(today);
        pm.setNextMaintenanceDate(nextDate);
        pm.setNextReminderDate(reminderDate);
        pm.setStatus(MaintenanceStatus.Completed);

        return toDTO(pmRepository.save(pm));
    }

    // ── Résoudre l'utilisateur connecté ─────────────────────
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + email));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    // ── Affecter un technicien ──────────────────────────────
    public PreventiveMaintenanceResponseDTO assignTechnician(Long id, Long technicianId) {
        PreventiveMaintenance pm = pmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance non trouvée avec l'id : " + id));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Technicien non trouvé avec l'id : " + technicianId));

        pm.setAssignedTechnician(technician);
        pm.setAssignedBy(getCurrentUser());
        pm.setExecutionStatus(ExecutionStatus.Pending);

        PreventiveMaintenance saved = pmRepository.save(pm);

        // ── Notifier le technicien de sa nouvelle maintenance ──
        notificationService.create(
                technician.getId(),
                "Info",
                String.format("🔧 Nouvelle maintenance préventive assignée : %s sur %s (%s)",
                pm.getMaintenanceType(), pm.getEquipment().getCode(), pm.getEquipment().getName()),
                "PreventiveMaintenance",
                saved.getId()
        );

        return toDTO(saved);
}
    // ── Mes maintenances (technicien connecté) ──────────────
    public List<PreventiveMaintenanceResponseDTO> findMy(Long technicianId) {
        return pmRepository
                .findByAssignedTechnicianIdAndExecutionStatusNot(technicianId, ExecutionStatus.Completed)
                .stream().map(this::toDTO).toList();
    }

    // ── Démarrer (technicien) ───────────────────────────────
    public PreventiveMaintenanceResponseDTO startExecution(Long id) {
        PreventiveMaintenance pm = pmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance non trouvée avec l'id : " + id));

        pm.setExecutionStatus(ExecutionStatus.In_Progress);
        pm.setTechnicianStartTime(LocalDateTime.now());

        return toDTO(pmRepository.save(pm));
    }

 // ── Clôturer (technicien) : archive le cycle + réinitialise pour le prochain
    // SAUF si maintenance "Urgente" (créée par la Règle 6) : ponctuelle, jamais récurrente ──
    public PreventiveMaintenanceResponseDTO completeByTechnician(Long id,
            String problemFound, String solution, List<ConsumeStockRequestDTO> parts) {

        PreventiveMaintenance pm = pmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance non trouvée avec l'id : " + id));

        LocalDateTime now = LocalDateTime.now();

        // ── 1. Archive ce cycle dans l'historique (dans tous les cas) ──
        PreventiveMaintenanceHistory history = PreventiveMaintenanceHistory.builder()
                .equipment(pm.getEquipment())
                .technician(pm.getAssignedTechnician())
                .maintenanceType(pm.getMaintenanceType())
                .completedAt(now)
                .problemFound(problemFound)
                .solution(solution)
                .build();
        history = historyRepository.save(history);

        // ── 2. Enregistre les pièces utilisées, liées à ce cycle précis ──
        if (parts != null && !parts.isEmpty()) {
            sparePartService.addPartsToPreventiveMaintenance(pm.getId(), history.getId(), parts);
        }

        boolean isOneOffUrgent = "Urgente".equals(pm.getMaintenanceType());

        if (isOneOffUrgent) {
            // ── Maintenance ponctuelle (Règle 6) : clôture définitive, pas de récurrence ──
            pm.setExecutionStatus(ExecutionStatus.Completed);
            pm.setStatus(MaintenanceStatus.Completed);
            pm.setTechnicianEndTime(now);
            // On garde le technicien, les dates et le problème/solution tels quels :
            // c'est un événement isolé, son historique complet reste visible sur cette ligne aussi.

        } else {
            // ── Cycle récurrent normal : réinitialise pour la prochaine échéance ──
            LocalDate today = LocalDate.now();

            pm.setLastMaintenanceDate(today);
            pm.setNextMaintenanceDate(today.plusDays(pm.getFrequencyDays()));
            pm.setNextReminderDate(today.plusDays(pm.getFrequencyDays()).minusDays(7));
            pm.setStatus(MaintenanceStatus.Scheduled);

            pm.setAssignedTechnician(null);
            pm.setAssignedBy(null);
            pm.setExecutionStatus(null);
            pm.setProblemFound(null);
            pm.setSolution(null);
            pm.setTechnicianStartTime(null);
            pm.setTechnicianEndTime(null);
        }

        return toDTO(pmRepository.save(pm));
    }

    // ── Historique des maintenances, filtrable par équipement ──
    public List<PreventiveMaintenanceHistoryResponseDTO> findHistory(Long equipmentId) {
        List<PreventiveMaintenanceHistory> history = equipmentId != null
                ? historyRepository.findByEquipmentIdOrderByCompletedAtDesc(equipmentId)
                : historyRepository.findAllByOrderByCompletedAtDesc();

        return history.stream()
                .map(h -> PreventiveMaintenanceHistoryResponseDTO.builder()
                        .id(h.getId())
                        .equipmentId(h.getEquipment().getId())
                        .equipmentCode(h.getEquipment().getCode())
                        .equipmentName(h.getEquipment().getName())
                        .technicianId(h.getTechnician() != null ? h.getTechnician().getId() : null)
                        .technicianName(h.getTechnician() != null ? h.getTechnician().getFullName() : null)
                        .maintenanceType(h.getMaintenanceType())
                        .completedAt(h.getCompletedAt())
                        .problemFound(h.getProblemFound())
                        .solution(h.getSolution())
                        .build())
                .toList();
    } 
    
    // ── Archives du technicien connecté (basé sur l'historique) ──
    public List<PreventiveMaintenanceHistoryResponseDTO> findMyArchive(Long technicianId) {
        return historyRepository.findByTechnicianIdOrderByCompletedAtDesc(technicianId)
                .stream()
                .map(h -> PreventiveMaintenanceHistoryResponseDTO.builder()
                        .id(h.getId())
                        .equipmentId(h.getEquipment().getId())
                        .equipmentCode(h.getEquipment().getCode())
                        .equipmentName(h.getEquipment().getName())
                        .technicianId(h.getTechnician() != null ? h.getTechnician().getId() : null)
                        .technicianName(h.getTechnician() != null ? h.getTechnician().getFullName() : null)
                        .maintenanceType(h.getMaintenanceType())
                        .completedAt(h.getCompletedAt())
                        .problemFound(h.getProblemFound())
                        .solution(h.getSolution())
                        .build())
                .toList();
    }
}