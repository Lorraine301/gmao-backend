package com.suprajit.gmao_backend.sparepart.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreventiveMaintenancePartResponseDTO {
    private Long id;
    private Long preventiveMaintenanceId;
    private Long sparePartId;
    private String sparePartName;
    private String sparePartReference;
    private Integer quantityUsed;
    private LocalDateTime createdAt;
}