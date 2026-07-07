package com.suprajit.gmao_backend.scheduler;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@Tag(name = "Scheduler", description = "Endpoints de test manuel du scheduler (dev only)")
@SecurityRequirement(name = "bearerAuth")
public class SchedulerController {

    private final MaintenanceScheduler maintenanceScheduler;

    @Operation(summary = "Déclencher manuellement la vérification des maintenances en retard")
    @PostMapping("/check-overdue")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> triggerOverdueCheck() {
        maintenanceScheduler.checkOverdueMaintenances();
        return ResponseEntity.ok("Vérification déclenchée - voir les logs");
    }

    @Operation(summary = "Déclencher manuellement les rappels de maintenance")
    @PostMapping("/check-reminders")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<String> triggerReminderCheck() {
        maintenanceScheduler.checkUpcomingReminders();
        return ResponseEntity.ok("Rappels vérifiés - voir les logs");
    }
}