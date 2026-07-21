package com.suprajit.gmao_backend.equipment.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;
import com.suprajit.gmao_backend.equipment.dto.EquipmentRequestDTO;
import com.suprajit.gmao_backend.equipment.dto.EquipmentResponseDTO;
import com.suprajit.gmao_backend.repository.EquipmentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    // ── Mapper entité → DTO ─────────────────────────────────
    private EquipmentResponseDTO toDTO(Equipment eq) {
        return EquipmentResponseDTO.builder()
                .id(eq.getId())
                .code(eq.getCode())
                .name(eq.getName())
                .description(eq.getDescription())
                .serialNumber(eq.getSerialNumber())
                .manufacturer(eq.getManufacturer())
                .model(eq.getModel())
                .type(eq.getType())
                .category(eq.getCategory())
                .plant(eq.getPlant())
                .productionLine(eq.getProductionLine())
                .location(eq.getLocation())
                .installationDate(eq.getInstallationDate())
                .commissioningDate(eq.getCommissioningDate())
                .status(eq.getStatus())
                .criticalityLevel(eq.getCriticalityLevel())
                .maintenanceTeam(eq.getMaintenanceTeam())
                .notes(eq.getNotes())
                .createdAt(eq.getCreatedAt())
                .updatedAt(eq.getUpdatedAt())
                .build();
    }

    // ── Mapper DTO → entité ─────────────────────────────────
    private Equipment toEntity(EquipmentRequestDTO dto) {
        return Equipment.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .serialNumber(dto.getSerialNumber())
                .manufacturer(dto.getManufacturer())
                .model(dto.getModel())
                .type(dto.getType())
                .category(dto.getCategory())
                .plant(dto.getPlant())
                .productionLine(dto.getProductionLine())
                .location(dto.getLocation())
                .installationDate(dto.getInstallationDate())
                .commissioningDate(dto.getCommissioningDate())
                .status(dto.getStatus())
                .criticalityLevel(dto.getCriticalityLevel())
                .maintenanceTeam(dto.getMaintenanceTeam())
                .notes(dto.getNotes())
                .build();
    }

    // ── CREATE ──────────────────────────────────────────────
    public EquipmentResponseDTO create(EquipmentRequestDTO dto) {
        Equipment equipment = toEntity(dto);
        return toDTO(equipmentRepository.save(equipment));
    }

    // ── READ ALL (avec filtres optionnels) ──────────────────
    public List<EquipmentResponseDTO> findAll(
            EquipmentStatus status,
            String type,
            CriticalityLevel criticality,
            String search) {

        // Normaliser les valeurs vides
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String typeParam = (type != null && !type.isBlank()) ? type : null;

        List<Equipment> equipments = equipmentRepository
            .findWithFilters(status, typeParam, criticality, searchParam);

        return equipments.stream().map(this::toDTO).toList();
    }

    // ── READ ONE ────────────────────────────────────────────
    public EquipmentResponseDTO findById(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Équipement non trouvé avec l'id : " + id));
        return toDTO(equipment);
    }

    // ── UPDATE ──────────────────────────────────────────────
    public EquipmentResponseDTO update(Long id, EquipmentRequestDTO dto) {
        Equipment existing = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Équipement non trouvé avec l'id : " + id));

        existing.setCode(dto.getCode());
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setSerialNumber(dto.getSerialNumber());
        existing.setManufacturer(dto.getManufacturer());
        existing.setModel(dto.getModel());
        existing.setType(dto.getType());
        existing.setCategory(dto.getCategory());
        existing.setPlant(dto.getPlant());
        existing.setProductionLine(dto.getProductionLine());
        existing.setLocation(dto.getLocation());
        existing.setInstallationDate(dto.getInstallationDate());
        existing.setCommissioningDate(dto.getCommissioningDate());
        existing.setStatus(dto.getStatus());
        existing.setCriticalityLevel(dto.getCriticalityLevel());
        existing.setMaintenanceTeam(dto.getMaintenanceTeam());
        existing.setNotes(dto.getNotes());

        return toDTO(equipmentRepository.save(existing));
    }

    // ── DELETE ──────────────────────────────────────────────
    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new EntityNotFoundException(
                "Équipement non trouvé avec l'id : " + id);
        }
        equipmentRepository.deleteById(id);
    }
    // ── Changement de statut rapide (sans repasser par le formulaire complet) ──
    public EquipmentResponseDTO updateStatus(Long id, EquipmentStatus status) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Équipement non trouvé avec l'id : " + id));

        equipment.setStatus(status);
        return toDTO(equipmentRepository.save(equipment));
    }
}