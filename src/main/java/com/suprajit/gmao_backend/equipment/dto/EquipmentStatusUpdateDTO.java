package com.suprajit.gmao_backend.equipment.dto;

import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EquipmentStatusUpdateDTO {

    @NotNull(message = "Le statut est obligatoire")
    private EquipmentStatus status;
}