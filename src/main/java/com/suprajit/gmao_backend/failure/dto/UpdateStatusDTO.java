package com.suprajit.gmao_backend.failure.dto;

import com.suprajit.gmao_backend.entity.enums.FailureStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusDTO {
    @NotNull(message = "Le statut est obligatoire")
    private FailureStatus status;
}