package com.suprajit.gmao_backend.sparepart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumeStockRequestDTO {

    @NotNull(message = "La pièce est obligatoire")
    private Long sparePartId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être d'au moins 1")
    private Integer quantityUsed;
}