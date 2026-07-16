package com.suprajit.gmao_backend.ai.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalysisResponseDTO {
    private Long id;
    private Long failureId;
    private String predictedCause;
    private String recommendedAction;
    private String riskLevel;
    private String summary;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}