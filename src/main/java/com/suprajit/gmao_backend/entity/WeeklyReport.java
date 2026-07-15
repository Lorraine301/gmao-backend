package com.suprajit.gmao_backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weekly_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "total_failures", nullable = false)
    private Integer totalFailures;

    @Column(name = "resolved_failures", nullable = false)
    private Integer resolvedFailures;

    @Column(name = "average_repair_time")
    private Double averageRepairTime;   // MTTR de la semaine, en heures

    @Column(name = "critical_machines", columnDefinition = "TEXT")
    private String criticalMachines;    // codes équipements séparés par virgule

    @Column(name = "llm_summary", columnDefinition = "TEXT")
    private String llmSummary;          // rempli en Sprint 5

    @Column(columnDefinition = "TEXT")
    private String recommendations;     // rempli en Sprint 5

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "pdf_path", length = 255)
    private String pdfPath;             // rempli en Sprint 4 (carte 34, génération PDF)

    @Column(name = "generated_by", length = 30)
    private String generatedBy;   // "System" (auto) ou nom de l'utilisateur (déclenchement manuel) 

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}