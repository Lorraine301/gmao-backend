package com.suprajit.gmao_backend.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.ai.service.AiAnalysisService;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.weeklyreport.service.WeeklyReportService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {

    private final PreventiveMaintenanceRepository pmRepository;
    private final NotificationService notificationService;
    private final WeeklyReportService weeklyReportService;
    private final AiAnalysisService aiAnalysisService;


    // ── Tâche principale : chaque jour à 8h30 ───────────────
    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void checkOverdueMaintenances() {
        System.out.println("[SCHEDULER] Vérification des maintenances en retard - "
                + LocalDate.now());

        List<PreventiveMaintenance> overdueList = pmRepository.findOverdue(LocalDate.now());

        int count = 0;
        for (PreventiveMaintenance pm : overdueList) {
            // Passer le statut à Overdue
            if (pm.getStatus() == MaintenanceStatus.Scheduled) {
                pm.setStatus(MaintenanceStatus.Overdue);
                pmRepository.save(pm);

                // Créer une notification pour Admins et Superviseurs
                String message = String.format(
                    "⚠ Maintenance en retard : %s sur l'équipement %s (%s). " +
                    "Date prévue : %s",
                    pm.getMaintenanceType(),
                    pm.getEquipment().getCode(),
                    pm.getEquipment().getName(),
                    pm.getNextMaintenanceDate()
                );

                notificationService.notifyAdminsAndSupervisors(
                    "Warning",
                    message,
                    "PreventiveMaintenance",
                    pm.getId()
                );

                count++;
                System.out.println("[SCHEDULER] → Maintenance en retard détectée : "
                        + pm.getEquipment().getCode()
                        + " | " + pm.getMaintenanceType());
            }
        }

        if (count == 0) {
            System.out.println("[SCHEDULER] Aucune nouvelle maintenance en retard.");
        } else {
            System.out.println("[SCHEDULER] " + count + " maintenance(s) passée(s) à Overdue.");
        }
    }

    // ── Tâche de rappel : 7 jours avant la date prévue ──────
    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void checkUpcomingReminders() {
        System.out.println("[SCHEDULER] Vérification des rappels de maintenance - "
                + LocalDate.now());

        List<PreventiveMaintenance> upcoming = pmRepository
                .findDueForReminder(LocalDate.now());

        for (PreventiveMaintenance pm : upcoming) {
            String message = String.format(
                "📅 Rappel : maintenance %s prévue dans 7 jours pour %s (%s). Date : %s",
                pm.getMaintenanceType(),
                pm.getEquipment().getCode(),
                pm.getEquipment().getName(),
                pm.getNextMaintenanceDate()
            );

            notificationService.notifyAdminsAndSupervisors(
                "Info",
                message,
                "PreventiveMaintenance",
                pm.getId()
            );

            System.out.println("[SCHEDULER] → Rappel envoyé pour : "
                    + pm.getEquipment().getCode());
        }
    }

    // ── TODO Sprint 5 : relancer les analyses LLM pending ───
    @Scheduled(cron = "0 0 9 * * *")
    public void retryPendingLlmAnalyses() {
        // TODO Sprint 5 : récupérer les AiAnalysis en statut Pending
        // et relancer l'appel Groq API
        System.out.println("[SCHEDULER] TODO Sprint 5 : retry LLM analyses pending");
    }
    @Scheduled(cron = "0 0 18 * * SUN")
    @Transactional
    public void generateWeeklyReportScheduled() {
        System.out.println("[SCHEDULER] Génération du bilan hebdomadaire...");
        var report = weeklyReportService.generateWeeklyReport();
        System.out.println("[SCHEDULER] Bilan hebdomadaire généré avec succès.");

        // ── Déclenche la synthèse IA APRÈS que le rapport soit déjà commité ──
        aiAnalysisService.generateLlmSummary(report.getId());
    }
    
}