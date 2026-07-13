package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiSummaryDTO {
    private long totalFailures;
    private long resolvedFailures;
    private double resolutionRate;      // en %
    private Double averageMttr;         // en heures, null si aucune donnée
    private int periodDays;
}