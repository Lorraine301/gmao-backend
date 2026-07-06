package com.suprajit.gmao_backend.entity;

import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "preventive_maintenances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreventiveMaintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relation vers Equipment ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // ── Informations de planification ────────────────────────
    @Column(name = "maintenance_type", length = 100)
    private String maintenanceType;         // ex: PT (Préventive Totale), PC (Partielle)

    @Column(name = "frequency_days")
    private Integer frequencyDays;          // ex: 90 → tous les 90 jours

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;  // calculée automatiquement

    @Column(name = "next_reminder_date")
    private LocalDate nextReminderDate;     // ex: 7 jours avant next_maintenance_date

    // ── État ────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.Scheduled;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}