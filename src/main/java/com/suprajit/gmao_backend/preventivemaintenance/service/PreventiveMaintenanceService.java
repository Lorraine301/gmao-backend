package com.suprajit.gmao_backend.preventivemaintenance.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceRequestDTO;
import com.suprajit.gmao_backend.preventivemaintenance.dto.PreventiveMaintenanceResponseDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PreventiveMaintenanceService {

    private final PreventiveMaintenanceRepository pmRepository;
    private final EquipmentRepository equipmentRepository;

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
}