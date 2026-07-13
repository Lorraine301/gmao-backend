package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyTrendDTO {
    private String month;          // format "yyyy-MM"
    private long failureCount;
    private long interventionCount;
}