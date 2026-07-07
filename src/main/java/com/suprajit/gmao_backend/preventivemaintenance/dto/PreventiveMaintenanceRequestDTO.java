package com.suprajit.gmao_backend.preventivemaintenance.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PreventiveMaintenanceRequestDTO {

    @NotNull(message = "L'équipement est obligatoire")
    private Long equipmentId;

    @NotBlank(message = "Le type de maintenance est obligatoire")
    private String maintenanceType;

    @NotNull(message = "La fréquence est obligatoire")
    @Min(value = 1, message = "La fréquence doit être d'au moins 1 jour")
    private Integer frequencyDays;

    @NotNull(message = "La date de dernière maintenance est obligatoire")
    private LocalDate lastMaintenanceDate;
}