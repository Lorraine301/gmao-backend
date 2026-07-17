package com.suprajit.gmao_backend.assistant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.SparePart;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.groq.service.GroqService;
import com.suprajit.gmao_backend.repository.EquipmentRepository;
import com.suprajit.gmao_backend.repository.FailureRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.SparePartRepository;
import com.suprajit.gmao_backend.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) 
public class AssistantService {

    private final GroqService groqService;
    private final FailureRepository failureRepository;
    private final UserRepository userRepository;
    private final EquipmentRepository equipmentRepository;
    private final InterventionRepository interventionRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final SparePartRepository sparePartRepository;

    private static final int RECENT_FAILURES_WINDOW_DAYS = 30;
    private static final int RECENT_FAILURES_LIMIT = 10;

    public String chat(String message) {
        String context = buildContext();

        String systemPrompt = "Tu es un assistant GMAO expert en maintenance industrielle. "
                + "Réponds de façon claire et concise aux questions de l'utilisateur, "
                + "en te basant sur le contexte fourni ci-dessous. "
                + "Si l'information demandée n'est pas dans le contexte, dis-le clairement "
                + "plutôt que d'inventer une réponse.\n\n"
                + "Contexte : " + context;

        return groqService.callGroq(systemPrompt, message);
    }

    // ── Construit le contexte : rôle de l'utilisateur + pannes récentes + équipements + charge de travail + maintenance préventive + stock de pièces
    private String buildContext() {
        String role = getCurrentUserRole();

        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_FAILURES_WINDOW_DAYS);
        List<Failure> recentFailures = failureRepository.findByReportedAtBetween(since, LocalDateTime.now());

        String failuresSummary = recentFailures.stream()
                .limit(RECENT_FAILURES_LIMIT)
                .map(f -> String.format("- %s sur %s (%s) : priorité %s, statut %s",
                        f.getFailureCode(), f.getEquipment().getCode(), f.getEquipment().getName(),
                        f.getPriority(), f.getStatus()))
                .collect(Collectors.joining("\n"));

        // ── Équipements ──
        List<Equipment> allEquipments = equipmentRepository.findAll();
        String equipmentsSummary = allEquipments.stream()
                .map(e -> String.format("%s (%s, criticité %s, statut %s)",
                        e.getCode(), e.getType(), e.getCriticalityLevel(), e.getStatus()))
                .collect(Collectors.joining(", "));

        // ── Charge de travail par technicien (30 derniers jours) ──
        List<Intervention> recentInterventions = interventionRepository.findByStartTimeBetween(since, LocalDateTime.now());
        Map<String, Long> workloadByTechnician = recentInterventions.stream()
                .collect(Collectors.groupingBy(i -> i.getTechnician().getFullName(), Collectors.counting()));
        String workloadSummary = workloadByTechnician.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> e.getKey() + " : " + e.getValue() + " intervention(s)")
                .collect(Collectors.joining("\n"));

        // ── Maintenance préventive ──
        List<PreventiveMaintenance> allMaintenances = preventiveMaintenanceRepository.findAll();
        long overdueCount = allMaintenances.stream()
                .filter(pm -> pm.getStatus() == MaintenanceStatus.Overdue)
                .count();
        long scheduledCount = allMaintenances.stream()
                .filter(pm -> pm.getStatus() == MaintenanceStatus.Scheduled)
                .count();
        String upcomingMaintenances = allMaintenances.stream()
                .filter(pm -> pm.getStatus() == MaintenanceStatus.Scheduled
                        && pm.getNextMaintenanceDate() != null
                        && !pm.getNextMaintenanceDate().isAfter(LocalDate.now().plusDays(14)))
                .map(pm -> pm.getEquipment().getCode() + " (" + pm.getMaintenanceType()
                        + ", prévue le " + pm.getNextMaintenanceDate() + ")")
                .collect(Collectors.joining(", "));

        // ── Stock de pièces ──
        List<SparePart> allParts = sparePartRepository.findAll();
        long lowStockCount = allParts.stream()
                .filter(p -> p.getQuantity() <= p.getMinimumStock())
                .count();
        String lowStockParts = allParts.stream()
                .filter(p -> p.getQuantity() <= p.getMinimumStock())
                .map(p -> p.getReference() + " (" + p.getName() + ") : " + p.getQuantity() + " restant(s)")
                .collect(Collectors.joining(", "));

        return String.format("""
                Rôle de l'utilisateur : %s

                === ÉQUIPEMENTS ===
                Total : %d équipements
                Liste : %s

                === PANNES (%d derniers jours) ===
                Nombre de pannes : %d
                Détail :
                %s

                === CHARGE DE TRAVAIL TECHNICIENS (%d derniers jours) ===
                %s

                === MAINTENANCE PRÉVENTIVE ===
                Total planifiées : %d
                En retard (Overdue) : %d
                Prochaines (14 jours) : %s

                === STOCK DE PIÈCES ===
                Pièces en stock faible : %d
                Détail : %s
                """,
                role,
                allEquipments.size(),
                equipmentsSummary.isEmpty() ? "aucun équipement" : equipmentsSummary,
                RECENT_FAILURES_WINDOW_DAYS,
                recentFailures.size(),
                failuresSummary.isEmpty() ? "Aucune panne récente" : failuresSummary,
                RECENT_FAILURES_WINDOW_DAYS,
                workloadSummary.isEmpty() ? "Aucune intervention récente" : workloadSummary,
                scheduledCount,
                overdueCount,
                upcomingMaintenances.isEmpty() ? "aucune dans les 14 prochains jours" : upcomingMaintenances,
                lowStockCount,
                lowStockParts.isEmpty() ? "aucune pièce en stock faible" : lowStockParts
        );
    }

    private String getCurrentUserRole() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé : " + email));
        return user.getRole().getName();
    }
}