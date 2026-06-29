package com.suprajit.gmao_backend.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "equipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identification ──────────────────────────────────────
    @Column(nullable = false, unique = true, length = 30)
    private String code;                    // ex: EXT-0006, WIN-0038

    @Column(nullable = false, length = 150)
    private String name;                    // ex: Extrusion Machine

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;            // ex: EXT-0006

    // ── Fabricant & Modèle ──────────────────────────────────
    @Column(length = 100)
    private String manufacturer;            // ex: Maillefer, ARBURG

    @Column(length = 100)
    private String model;                   // ex: MX120, ARBURG 470S

    // ── Classification ──────────────────────────────────────
    @Column(length = 50)
    private String type;                    // ex: Extrusion, Winding, Molding

    @Column(length = 50)
    private String category;               // ex: Production, Support

    // ── Localisation ────────────────────────────────────────
    @Column(length = 50)
    private String plant;                   // ex: Suprajit Morocco

    @Column(name = "production_line", length = 50)
    private String productionLine;          // ex: Line A

    @Column(length = 100)
    private String location;               // ex: Workshop A

    // ── Dates ───────────────────────────────────────────────
    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "commissioning_date")
    private LocalDate commissioningDate;   // date de mise en service

    // ── État & Criticité ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EquipmentStatus status = EquipmentStatus.Active;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality_level", nullable = false)
    @Builder.Default
    private CriticalityLevel criticalityLevel = CriticalityLevel.Medium;

    // ── Maintenance ─────────────────────────────────────────
    @Column(name = "maintenance_team", length = 100)
    private String maintenanceTeam;        // ex: Mechanical Team

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ── Audit ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}