package com.suprajit.gmao_backend.preventivemaintenance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTechnicianRequestDTO {
    @NotNull(message = "Le technicien est obligatoire")
    private Long technicianId;
}