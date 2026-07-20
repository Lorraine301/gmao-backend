package com.suprajit.gmao_backend.equipment.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipmentImportResultDTO {
    private int createdCount;
    private int skippedCount;
    private List<String> errors;
}