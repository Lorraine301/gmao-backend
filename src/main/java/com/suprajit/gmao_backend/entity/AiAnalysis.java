package com.suprajit.gmao_backend.entity;

import com.suprajit.gmao_backend.entity.enums.AiAnalysisStatus;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_id", nullable = false)
    private Failure failure;

    @Column(name = "predicted_cause", columnDefinition = "TEXT")
    private String predictedCause;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private FailurePriority riskLevel;   // réutilise l'enum existant (Low/Medium/High/Critical)

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AiAnalysisStatus status = AiAnalysisStatus.Pending;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}