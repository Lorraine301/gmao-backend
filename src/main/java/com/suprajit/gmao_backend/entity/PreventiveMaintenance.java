package com.suprajit.gmao_backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.suprajit.gmao_backend.entity.enums.ExecutionStatus;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private User assignedTechnician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", length = 20)
    private ExecutionStatus executionStatus;   // Pending, In_Progress, Completed — null tant que non assignée

    @Column(name = "problem_found", columnDefinition = "TEXT")
    private String problemFound;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(name = "technician_start_time")
    private LocalDateTime technicianStartTime;

    @Column(name = "technician_end_time")
    private LocalDateTime technicianEndTime;
    
}