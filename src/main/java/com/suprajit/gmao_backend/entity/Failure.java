package com.suprajit.gmao_backend.entity;

import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "failures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Failure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "failure_code", unique = true, length = 30)
    private String failureCode;             // ex: FAIL-0001

    // ── Relation vers Equipment ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "failure_type", length = 50)
    private String failureType;             // ex: Mechanical, Electrical

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FailurePriority priority = FailurePriority.Medium;

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_priority")
    private FailurePriority llmPriority;    // calculé par le LLM (Sprint 5)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FailureStatus status = FailureStatus.Open;

    // ── Relation vers User (déclarant) ───────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @Column(name = "reported_channel", length = 30)
    @Builder.Default
    private String reportedChannel = "Web"; // ex: Web, Mobile, Terminal

    @Column(name = "reported_at")
    @Builder.Default
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "llm_processed")
    @Builder.Default
    private Boolean llmProcessed = false;

    // ── Relation inverse vers Intervention ───────────────────
    @OneToMany(mappedBy = "failure", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Intervention> interventions = new ArrayList<>();

    // ── Audit ─────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}