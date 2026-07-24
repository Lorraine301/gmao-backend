package com.suprajit.gmao_backend.weeklyreport.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyReportResponseDTO {
    private Long id;
    private Integer month;
    private Integer year;
    private Integer totalFailures;
    private Integer totalInterventions;
    private Double averageMttr;
    private String topEquipment;
    private String llmSummary;
    private String recommendations;
    private String generatedBy;
    private LocalDateTime generatedAt;
    private String pdfPath;
}