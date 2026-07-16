package com.suprajit.gmao_backend.groq.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.groq.service.GroqService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Groq Test (temporaire)", description = "Endpoint de test à supprimer une fois la carte 38 validée")
@SecurityRequirement(name = "bearerAuth")
public class GroqTestController {

    private final GroqService groqService;

    // ── GET /api/groq-test?prompt=... ────────────────────────
    @Operation(summary = "[TEST TEMPORAIRE] Tester un prompt basique sur Groq")
    @GetMapping("/api/groq-test")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> test(@RequestParam String prompt) {
        String response = groqService.callGroq(
            "Tu es un assistant utile et concis.",
            prompt
        );
        return ResponseEntity.ok(response);
    }
}