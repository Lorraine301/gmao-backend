package com.suprajit.gmao_backend.intervention.dto;

import java.util.List;

import com.suprajit.gmao_backend.sparepart.dto.ConsumeStockRequestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteInterventionDTO {

    @NotBlank(message = "La solution est obligatoire pour clôturer une intervention")
    private String solution;

    private List<ConsumeStockRequestDTO> parts;   // optionnel — pièces utilisées à la clôture
}