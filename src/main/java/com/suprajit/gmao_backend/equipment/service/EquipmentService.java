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
            CriticalityLevel criticality) {

        List<Equipment> equipments;

        if (status != null && type != null && criticality != null) {
            equipments = equipmentRepository
                .findByStatusAndTypeAndCriticalityLevel(status, type, criticality);
        } else if (status != null && criticality != null) {
            equipments = equipmentRepository
                .findByStatusAndCriticalityLevel(status, criticality);
        } else if (status != null && type != null) {
            equipments = equipmentRepository
                .findByStatusAndType(status, type);
        } else if (status != null) {
            equipments = equipmentRepository.findByStatus(status);
        } else if (type != null) {
            equipments = equipmentRepository.findByType(type);
        } else if (criticality != null) {
            equipments = equipmentRepository.findByCriticalityLevel(criticality);
        } else {
            equipments = equipmentRepository.findAll();
        }

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
}