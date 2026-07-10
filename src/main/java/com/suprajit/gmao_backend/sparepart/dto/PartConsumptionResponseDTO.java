package com.suprajit.gmao_backend.sparepart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartConsumptionResponseDTO {
    private Long id;
    private LocalDateTime consumptionDate;
    private String sparePartName;
    private String sparePartReference;
    private Integer quantityUsed;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String consumptionType;   // "CORRECTIVE" ou "PREVENTIVE"
    private String technicianName;
    private String equipmentCode;
    private String equipmentName;
}