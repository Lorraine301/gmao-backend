package com.suprajit.gmao_backend.sparepart.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AddInterventionPartsRequestDTO {

    @NotEmpty(message = "La liste des pièces ne peut pas être vide")
    private List<ConsumeStockRequestDTO> parts;
}