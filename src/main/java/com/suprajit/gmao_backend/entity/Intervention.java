package com.suprajit.gmao_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;

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
@Table(name = "interventions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relation vers Failure ────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_id", nullable = false)
    private Failure failure;

    // ── Relation vers User (technicien assigné) ──────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    // ── Relation vers User (qui a assigné, ex: Supervisor) ───
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    private Double duration;                // en heures, calculé à la complétion

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FailurePriority priority = FailurePriority.Medium;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterventionStatus status = InterventionStatus.Pending;

    @Column(columnDefinition = "TEXT")
    private String solution;

    // ── Relation vers User (qui a clôturé) ───────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    // ── Audit ─────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}