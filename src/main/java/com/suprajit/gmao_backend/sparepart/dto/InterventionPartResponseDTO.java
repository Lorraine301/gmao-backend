package com.suprajit.gmao_backend.sparepart.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterventionPartResponseDTO {
    private Long id;
    private Long interventionId;
    private Long sparePartId;
    private String sparePartName;
    private String sparePartReference;
    private Integer quantityUsed;
    private LocalDateTime createdAt;
}