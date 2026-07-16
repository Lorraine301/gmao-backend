package com.suprajit.gmao_backend.ai.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suprajit.gmao_backend.ai.dto.AiAnalysisResponseDTO;
import com.suprajit.gmao_backend.entity.AiAnalysis;
import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.WeeklyReport;
import com.suprajit.gmao_backend.entity.enums.AiAnalysisStatus;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.groq.service.GroqService;
import com.suprajit.gmao_backend.pdf.service.PdfService;
import com.suprajit.gmao_backend.repository.AiAnalysisRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.WeeklyReportRepository;
import com.suprajit.gmao_backend.ruleengine.dto.AtRiskEquipmentDTO;
import com.suprajit.gmao_backend.ruleengine.service.RuleEngineService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiAnalysisRepository aiAnalysisRepository;
    private final FailureRepository failureRepository;
    private final GroqService groqService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeeklyReportRepository weeklyReportRepository;
    private final PdfService pdfService;
    private final RuleEngineService ruleEngineService;

    private static final int RECENT_FAILURES_WINDOW_DAYS = 7;

    // ── Point d'entrée asynchrone : analyse une panne via le LLM ──
    @Async
    @Transactional
    public void analyzeFailure(Long failureId) {
        Failure failure = failureRepository.findById(failureId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Panne non trouvée avec l'id : " + failureId));

        Equipment equipment = failure.getEquipment();

        // ── Récupère ou crée l'entrée AiAnalysis pour cette panne ──
        AiAnalysis analysis = aiAnalysisRepository.findByFailureId(failureId)
                .orElse(AiAnalysis.builder().failure(failure).build());

        try {
            String systemPrompt = "Tu es un expert en maintenance industrielle. "
                    + "Analyse la panne suivante et retourne UNIQUEMENT un JSON valide sans markdown "
                    + "avec les champs : predicted_cause, recommended_action, "
                    + "risk_level (Low/Medium/High/Critical), summary";

            String userPrompt = buildUserPrompt(failure, equipment);

            String rawResponse = groqService.callGroq(systemPrompt, userPrompt);
            JsonNode json = parseJsonResponse(rawResponse);

            String predictedCause = json.path("predicted_cause").asText(null);
            String recommendedAction = json.path("recommended_action").asText(null);
            String riskLevelStr = json.path("risk_level").asText(null);
            String summary = json.path("summary").asText(null);

            FailurePriority riskLevel = parseRiskLevel(riskLevelStr);

            analysis.setPredictedCause(predictedCause);
            analysis.setRecommendedAction(recommendedAction);
            analysis.setRiskLevel(riskLevel);
            analysis.setSummary(summary);
            analysis.setStatus(AiAnalysisStatus.Completed);
            analysis.setErrorMessage(null);
            aiAnalysisRepository.save(analysis);

            // ── Mettre à jour la panne avec le résultat LLM ──
            failure.setLlmPriority(riskLevel);
            failure.setLlmProcessed(true);
            failureRepository.save(failure);
             // ── Rule Engine V2 : applique les règles 5 et 6 avec les données LLM ──
            ruleEngineService.applyPostLlmRules(failure);

        } catch (Exception e) {
            analysis.setStatus(AiAnalysisStatus.Failed);
            analysis.setErrorMessage(e.getMessage());
            aiAnalysisRepository.save(analysis);

            failure.setLlmProcessed(true);
            failureRepository.save(failure);

            System.out.println("[AI ANALYSIS] Échec de l'analyse pour la panne "
                    + failureId + " : " + e.getMessage());
        }
    }

    // ── Construction du prompt utilisateur ────────────────────
    private String buildUserPrompt(Failure failure, Equipment equipment) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_FAILURES_WINDOW_DAYS);
        long recentFailuresCount = failureRepository
                .findRecentByEquipment(equipment.getId(), since).size();

        return String.format("""
                Équipement :
                - Code : %s
                - Type : %s
                - Criticité : %s

                Panne :
                - Titre : %s
                - Description : %s
                - Type de panne : %s

                Historique : %d panne(s) signalée(s) sur cet équipement dans les %d derniers jours.
                """,
                equipment.getCode(),
                equipment.getType() != null ? equipment.getType() : "Non spécifié",
                equipment.getCriticalityLevel() != null ? equipment.getCriticalityLevel().name() : "Non spécifié",
                failure.getTitle(),
                failure.getDescription() != null ? failure.getDescription() : "Non spécifiée",
                failure.getFailureType() != null ? failure.getFailureType() : "Non spécifié",
                recentFailuresCount,
                RECENT_FAILURES_WINDOW_DAYS
        );
    }

    // ── Parse le JSON retourné, en gérant les backticks markdown éventuels ──
    private JsonNode parseJsonResponse(String rawResponse) throws Exception {
        String cleaned = rawResponse.trim();

        // Retire les blocs ```json ... ``` ou ``` ... ``` si le LLM les ajoute
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(json)?", "").trim();
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }

        return objectMapper.readTree(cleaned);
    }

    // ── Convertit le texte risk_level en enum FailurePriority ──
    private FailurePriority parseRiskLevel(String riskLevelStr) {
        if (riskLevelStr == null) return null;
        try {
            return FailurePriority.valueOf(riskLevelStr.trim());
        } catch (IllegalArgumentException e) {
            return null; // valeur inattendue du LLM, on n'échoue pas toute l'analyse pour ça
        }
    }

    // ── Mapper ──────────────────────────────────────────────
    private AiAnalysisResponseDTO toDTO(AiAnalysis a) {
        return AiAnalysisResponseDTO.builder()
                .id(a.getId())
                .failureId(a.getFailure().getId())
                .predictedCause(a.getPredictedCause())
                .recommendedAction(a.getRecommendedAction())
                .riskLevel(a.getRiskLevel() != null ? a.getRiskLevel().name() : null)
                .summary(a.getSummary())
                .status(a.getStatus().name())
                .errorMessage(a.getErrorMessage())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    // ── GET : récupérer l'analyse d'une panne ─────────────────
    public AiAnalysisResponseDTO getByFailureId(Long failureId) {
        AiAnalysis analysis = aiAnalysisRepository.findByFailureId(failureId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune analyse IA trouvée pour la panne : " + failureId));
        return toDTO(analysis);
    }

    // ── Relancer l'analyse si le statut est Failed ────────────
    public void retryAnalysis(Long failureId) {
        AiAnalysis analysis = aiAnalysisRepository.findByFailureId(failureId).orElse(null);

        if (analysis != null && analysis.getStatus() != AiAnalysisStatus.Failed) {
            throw new IllegalStateException(
                "Seule une analyse au statut 'Failed' peut être relancée. Statut actuel : "
                + analysis.getStatus());
        }

        analyzeFailure(failureId);
    }

    // ── Génère la synthèse IA + recommandations d'un bilan hebdomadaire ──
    // NOTE : prend l'ID (pas l'entité) et la refetch en interne, exactement
    // comme analyzeFailure() — évite tout souci de session/transaction
    // entre le thread appelant et ce thread asynchrone.
    @Async
    @Transactional
    public void generateLlmSummary(Long weeklyReportId) {
        WeeklyReport report = weeklyReportRepository.findById(weeklyReportId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Bilan hebdomadaire non trouvé avec l'id : " + weeklyReportId));

        try {
            String systemPrompt = "Tu es un expert en maintenance industrielle. "
                    + "Analyse les métriques hebdomadaires suivantes et retourne UNIQUEMENT un JSON valide "
                    + "sans markdown avec les champs : summary (synthèse en 2-3 paragraphes) et "
                    + "recommendations (liste de 3 à 5 recommandations concrètes, sous forme de texte "
                    + "avec des tirets ou numérotée dans une seule chaîne)";

            String userPrompt = buildWeeklySummaryPrompt(report);

            String rawResponse = groqService.callGroq(systemPrompt, userPrompt);
            JsonNode json = parseJsonResponse(rawResponse);

            report.setLlmSummary(json.path("summary").asText(null));
            report.setRecommendations(json.path("recommendations").asText(null));
            weeklyReportRepository.save(report);

            // ── Régénère le PDF pour remplacer les placeholders par le contenu réel ──
            try {
                String pdfPath = pdfService.generateWeeklyReportPdf(report);
                report.setPdfPath(pdfPath);
                weeklyReportRepository.save(report);
            } catch (Exception e) {
                System.out.println("[AI SUMMARY] Synthèse générée mais échec régénération PDF : "
                        + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("[AI SUMMARY] Échec de la génération de synthèse pour le bilan "
                    + weeklyReportId + " : " + e.getMessage());
        }
    }

    // ── Construction du prompt à partir des métriques du bilan ──
    private String buildWeeklySummaryPrompt(WeeklyReport report) {
        String topFailingEquipments = findTopFailingEquipmentsForWeek(report);

        return String.format("""
                Bilan hebdomadaire — Semaine %d :
                - Total pannes : %d
                - Pannes résolues : %d
                - MTTR moyen : %s
                - Équipements critiques (plus de 2 pannes) : %s
                - Équipements les plus touchés cette semaine : %s
                """,
                report.getWeekNumber(),
                report.getTotalFailures(),
                report.getResolvedFailures(),
                report.getAverageRepairTime() != null ? report.getAverageRepairTime() + " h" : "non disponible",
                report.getCriticalMachines() != null ? report.getCriticalMachines() : "aucun",
                topFailingEquipments
        );
    }

    // ── Recalcule les équipements les plus touchés dans la semaine du rapport ──
    private String findTopFailingEquipmentsForWeek(WeeklyReport report) {
        int year = report.getGeneratedAt().getYear();

        LocalDate approxDate = LocalDate.ofYearDay(year, 1)
                .with(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR, report.getWeekNumber());
        LocalDate weekStart = approxDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = approxDate.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        List<Failure> weekFailures = failureRepository.findByReportedAtBetween(
                weekStart.atStartOfDay(), weekEnd.atTime(23, 59, 59));

        Map<String, Long> countByEquipment = weekFailures.stream()
                .collect(Collectors.groupingBy(f -> f.getEquipment().getCode(), Collectors.counting()));

        if (countByEquipment.isEmpty()) return "aucun";

        return countByEquipment.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> e.getKey() + " (" + e.getValue() + " panne(s))")
                .collect(Collectors.joining(", "));
    }

    // ── Interprétation narrative des KPI (mise en cache 1h) ───
    @Cacheable("kpiInterpretation")
    public String interpretKpis(Map<String, Object> kpiData) {
        String systemPrompt = "Tu es un expert en maintenance industrielle. "
                + "Analyse les indicateurs de performance (KPI) suivants et rédige une interprétation "
                + "narrative en 2-3 paragraphes : identifie les tendances, les équipements préoccupants, "
                + "et suggère 2-3 actions prioritaires. Réponds en texte simple, sans JSON, sans markdown.";

        StringBuilder userPrompt = new StringBuilder("Indicateurs actuels :\n");
        kpiData.forEach((key, value) -> userPrompt.append("- ")
                .append(key).append(" : ").append(value).append("\n"));

        try {
            return groqService.callGroq(systemPrompt, userPrompt.toString());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'interprétation des KPI : " + e.getMessage(), e);
        }
    }

    // ── Enrichit chaque équipement à risque d'une explication LLM courte ──
    public List<AtRiskEquipmentDTO> enrichAtRiskEquipments(List<AtRiskEquipmentDTO> atRiskEquipments) {
        return atRiskEquipments.stream()
                .map(eq -> {
                    try {
                        String explanation = generateShortRiskExplanation(eq);
                        eq.setLlmExplanation(explanation);
                    } catch (Exception e) {
                        eq.setLlmExplanation(null); // silencieux : n'échoue pas tout l'affichage pour un équipement
                        System.out.println("[AI ENRICH] Échec explication pour " + eq.getEquipmentCode()
                                + " : " + e.getMessage());
                    }
                    return eq;
                })
                .toList();
    }

    private String generateShortRiskExplanation(AtRiskEquipmentDTO eq) {
        String systemPrompt = "Tu es un expert en maintenance industrielle. "
                + "Explique en 1 à 2 phrases maximum, de façon simple et directe, pourquoi cet équipement "
                + "est considéré à risque. Ne retourne que le texte de l'explication, sans introduction.";

        String userPrompt = String.format(
                "Équipement %s (%s) — Raison technique détectée : %s",
                eq.getEquipmentCode(), eq.getEquipmentName(), eq.getRiskReason()
        );

        return groqService.callGroq(systemPrompt, userPrompt);
    }
}