package com.suprajit.gmao_backend.groq.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroqService {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.model}")
    private String model;

// ── Appel générique à l'API Groq (chat completions) ──────
    public String callGroq(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "max_tokens", 1024,
            "temperature", 0.3
        );

        try {
            String rawResponse = groqWebClient.post()
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return extractContent(rawResponse);

        } catch (WebClientResponseException.TooManyRequests e) {
            throw new RuntimeException(
                "Limite de requêtes Groq atteinte (429 rate limit). Réessayez dans quelques instants.", e);

        } catch (WebClientResponseException e) {
            throw new RuntimeException(
                "Erreur API Groq (" + e.getStatusCode() + ") : " + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            // Regroupe timeout, erreurs réseau et toute autre erreur inattendue
            throw new RuntimeException("Erreur lors de l'appel à Groq : " + e.getMessage(), e);
        }
    }

    // ── Extrait choices[0].message.content de la réponse JSON brute ──
    private String extractContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de parser la réponse Groq : " + e.getMessage(), e);
        }
    }
}