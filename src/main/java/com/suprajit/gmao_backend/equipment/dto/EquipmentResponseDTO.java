package com.suprajit.gmao_backend.equipment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipmentResponseDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private String type;
    private String category;
    private String plant;
    private LocalDate installationDate;
    private LocalDate commissioningDate;
    private EquipmentStatus status;
    private CriticalityLevel criticalityLevel;
    private String area;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}