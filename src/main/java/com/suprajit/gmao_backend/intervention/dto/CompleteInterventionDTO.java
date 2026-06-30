package com.suprajit.gmao_backend.intervention.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteInterventionDTO {
    @NotBlank(message = "La solution est obligatoire pour clôturer une intervention")
    private String solution;
}