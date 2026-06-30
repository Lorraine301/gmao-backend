package com.suprajit.gmao_backend.failure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FailureRequestDTO {

    @NotNull(message = "L'équipement est obligatoire")
    private Long equipmentId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @Size(max = 50)
    private String failureType;

    private String reportedChannel; // optionnel, défaut "Web"
}