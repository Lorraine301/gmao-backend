package com.suprajit.gmao_backend.sparepart.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SparePartRequestDTO {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "La référence est obligatoire")
    private String reference;

    private String supplier;
    private String warehouseLocation;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 0, message = "La quantité ne peut pas être négative")
    private Integer quantity;

    @NotNull(message = "Le stock minimum est obligatoire")
    @Min(value = 0)
    private Integer minimumStock;

    private String unit;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal unitPrice;
}