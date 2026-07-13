package com.suprajit.gmao_backend.kpi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriorityCountDTO {
    private String priority;
    private long count;
}