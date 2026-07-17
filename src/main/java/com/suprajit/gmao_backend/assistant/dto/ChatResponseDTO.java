package com.suprajit.gmao_backend.assistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponseDTO {
    private String reply;
}