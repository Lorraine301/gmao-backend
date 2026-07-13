package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TechnicianWorkloadDTO {
    private Long technicianId;
    private String technicianName;
    private String employeeCode;
    private long interventionCount;
}