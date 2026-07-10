package com.suprajit.gmao_backend.preventivemaintenance.dto;

import java.util.List;

import com.suprajit.gmao_backend.sparepart.dto.ConsumeStockRequestDTO;

import lombok.Data;

@Data
public class CompletePreventiveMaintenanceDTO {
    private String problemFound;   // nullable — rien trouvé = OK
    private String solution;       // nullable
    private List<ConsumeStockRequestDTO> parts;  // nullable/optionnel
}