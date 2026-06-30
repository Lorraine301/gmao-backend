package com.suprajit.gmao_backend.intervention.dto;

import com.suprajit.gmao_backend.entity.enums.InterventionStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateInterventionStatusDTO {
    @NotNull(message = "Le statut est obligatoire")
    private InterventionStatus status;
}