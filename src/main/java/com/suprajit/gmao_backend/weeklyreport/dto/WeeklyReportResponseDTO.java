package com.suprajit.gmao_backend.weeklyreport.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeeklyReportResponseDTO {
    private Long id;
    private Integer weekNumber;
    private Integer totalFailures;
    private Integer resolvedFailures;
    private Double averageRepairTime;
    private String criticalMachines;
    private String llmSummary;
    private String recommendations;
    private LocalDateTime generatedAt;
    private String pdfPath;
    private String generatedBy;
}