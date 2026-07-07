package com.suprajit.gmao_backend.sparepart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SparePartResponseDTO {
    private Long id;
    private String name;
    private String reference;
    private String supplier;
    private String warehouseLocation;
    private Integer quantity;
    private Integer minimumStock;
    private String unit;
    private BigDecimal unitPrice;
    private boolean lowStock;             // calculé : quantity <= minimumStock
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}