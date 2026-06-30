package com.suprajit.gmao_backend.failure.dto;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePriorityDTO {
    @NotNull(message = "La priorité est obligatoire")
    private FailurePriority priority;
}