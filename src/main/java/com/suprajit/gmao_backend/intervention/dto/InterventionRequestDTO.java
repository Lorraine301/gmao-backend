package com.suprajit.gmao_backend.intervention.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterventionRequestDTO {

    @NotNull(message = "La panne est obligatoire")
    private Long failureId;

    @NotNull(message = "Le technicien est obligatoire")
    private Long technicianId;
}