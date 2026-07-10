package com.suprajit.gmao_backend.intervention.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;
import com.suprajit.gmao_backend.intervention.dto.InterventionRequestDTO;
import com.suprajit.gmao_backend.intervention.dto.InterventionResponseDTO;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.sparepart.dto.AddInterventionPartsRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.ConsumeStockRequestDTO;
import com.suprajit.gmao_backend.sparepart.service.SparePartService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final FailureRepository failureRepository;
    private final UserRepository userRepository;
    private final SparePartService sparePartService;   // ← INDISPENSABLE

    // ── Mapper entité → DTO (résout toutes les relations) ──
    private InterventionResponseDTO toDTO(Intervention i) {
        return InterventionResponseDTO.builder()
                .id(i.getId())
                .failureId(i.getFailure().getId())
                .failureCode(i.getFailure().getFailureCode())
                .failureTitle(i.getFailure().getTitle())
                .equipmentCode(i.getFailure().getEquipment().getCode())
                .equipmentName(i.getFailure().getEquipment().getName())
                .technicianId(i.getTechnician().getId())
                .technicianName(i.getTechnician().getFullName())
                .technicianEmployeeCode(i.getTechnician().getEmployeeCode())
                .assignedById(i.getAssignedBy() != null ? i.getAssignedBy().getId() : null)
                .assignedByName(i.getAssignedBy() != null ? i.getAssignedBy().getFullName() : null)
                .startTime(i.getStartTime())
                .endTime(i.getEndTime())
                .duration(i.getDuration())
                .priority(i.getPriority())
                .status(i.getStatus())
                .solution(i.getSolution())
                .closedById(i.getClosedBy() != null ? i.getClosedBy().getId() : null)
                .closedByName(i.getClosedBy() != null ? i.getClosedBy().getFullName() : null)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + email));
    }

    // ── CREATE (affectation) ──────────────────────────────
    public InterventionResponseDTO create(InterventionRequestDTO dto) {
        Failure failure = failureRepository.findById(dto.getFailureId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Panne non trouvée avec l'id : " + dto.getFailureId()));

        User technician = userRepository.findById(dto.getTechnicianId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Technicien non trouvé avec l'id : " + dto.getTechnicianId()));

        User assignedBy = getCurrentUser();

        Intervention intervention = Intervention.builder()
                .failure(failure)
                .technician(technician)
                .assignedBy(assignedBy)
                .startTime(LocalDateTime.now())
                .priority(failure.getPriority())
                .status(InterventionStatus.Pending)
                .build();

        Intervention saved = interventionRepository.save(intervention);

        failure.setStatus(FailureStatus.In_Progress);
        failureRepository.save(failure);

        return toDTO(saved);
    }

    // ── READ ALL ────────────────────────────────────────────
    public List<InterventionResponseDTO> findAll() {
        return interventionRepository.findAll()
                .stream().map(this::toDTO).toList();
    }

    // ── READ BY TECHNICIAN (mes interventions) ────────────
    public List<InterventionResponseDTO> findByTechnician(Long technicianId) {
        return interventionRepository.findByTechnicianId(technicianId)
                .stream().map(this::toDTO).toList();
    }

    // ── UPDATE STATUS ───────────────────────────────────────
    public InterventionResponseDTO updateStatus(Long id, InterventionStatus newStatus) {
        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Intervention non trouvée avec l'id : " + id));

        intervention.setStatus(newStatus);
        return toDTO(interventionRepository.save(intervention));
    }

    // ── COMPLETE (clôture + calcul durée + pièces utilisées) ──
    public InterventionResponseDTO complete(Long id, String solution,
            List<ConsumeStockRequestDTO> parts) {

        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Intervention non trouvée avec l'id : " + id));

        LocalDateTime now = LocalDateTime.now();
        double durationHours = ChronoUnit.MINUTES.between(intervention.getStartTime(), now) / 60.0;

        intervention.setEndTime(now);
        intervention.setDuration(Math.round(durationHours * 100.0) / 100.0);
        intervention.setSolution(solution);
        intervention.setStatus(InterventionStatus.Completed);
        intervention.setClosedBy(getCurrentUser());

        Intervention saved = interventionRepository.save(intervention);

        // ── Enregistrer les pièces utilisées si fourni ────────
        if (parts != null && !parts.isEmpty()) {
            AddInterventionPartsRequestDTO partsRequest = new AddInterventionPartsRequestDTO();
            partsRequest.setParts(parts);
            sparePartService.addPartsToIntervention(saved.getId(), partsRequest);
        }

        // Mettre à jour le statut de la panne liée à Resolved
        Failure failure = intervention.getFailure();
        failure.setStatus(FailureStatus.Resolved);
        failure.setResolvedAt(now);
        failureRepository.save(failure);

        return toDTO(saved);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}