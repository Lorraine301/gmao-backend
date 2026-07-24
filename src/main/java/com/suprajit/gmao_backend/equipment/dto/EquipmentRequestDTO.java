package com.suprajit.gmao_backend.equipment.dto;

import java.time.LocalDate;

import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentRequestDTO {

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères")
    private String code;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
    private String name;

    private String description;

    @Size(max = 100)
    private String serialNumber;

    @Size(max = 100)
    private String manufacturer;

    @Size(max = 100)
    private String model;

    @Size(max = 50)
    private String type;

    @Size(max = 50)
    private String category;

    @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
    private String plant;

    @Size(max = 50)
    private String area;


    private LocalDate installationDate;
    private LocalDate commissioningDate;

    @NotNull(message = "Le statut est obligatoire")
    private EquipmentStatus status;

    @NotNull(message = "Le niveau de criticité est obligatoire")
    private CriticalityLevel criticalityLevel;

    private String notes;
}