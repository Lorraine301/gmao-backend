package com.suprajit.gmao_backend.assistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.assistant.dto.ChatRequestDTO;
import com.suprajit.gmao_backend.assistant.dto.ChatResponseDTO;
import com.suprajit.gmao_backend.assistant.service.AssistantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant IA", description = "Assistant conversationnel maintenance (bonus)")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private final AssistantService assistantService;

    @Operation(summary = "Poser une question à l'assistant GMAO")
    @PostMapping("/chat")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor') or hasRole('Technician')")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO dto) {
        String reply = assistantService.chat(dto.getMessage());
        return ResponseEntity.ok(ChatResponseDTO.builder().reply(reply).build());
    }
}