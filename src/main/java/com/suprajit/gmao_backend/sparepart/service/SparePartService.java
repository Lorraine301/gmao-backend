package com.suprajit.gmao_backend.sparepart.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.InterventionPart;
import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.PreventiveMaintenancePart;
import com.suprajit.gmao_backend.entity.SparePart;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.InterventionPartRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenancePartRepository;
import com.suprajit.gmao_backend.repository.PreventiveMaintenanceRepository;
import com.suprajit.gmao_backend.repository.SparePartRepository;
import com.suprajit.gmao_backend.sparepart.dto.AddInterventionPartsRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.ConsumeStockRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.InterventionPartResponseDTO;
import com.suprajit.gmao_backend.sparepart.dto.PartConsumptionResponseDTO;
import com.suprajit.gmao_backend.sparepart.dto.PreventiveMaintenancePartResponseDTO;
import com.suprajit.gmao_backend.sparepart.dto.SparePartRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.SparePartResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SparePartService {

    private final SparePartRepository sparePartRepository;
    private final InterventionRepository interventionRepository;
    private final InterventionPartRepository interventionPartRepository;
    private final NotificationService notificationService;

    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final PreventiveMaintenancePartRepository preventiveMaintenancePartRepository;

    // ── Mapper ──────────────────────────────────────────────
    private SparePartResponseDTO toDTO(SparePart s) {
        return SparePartResponseDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .reference(s.getReference())
                .supplier(s.getSupplier())
                .warehouseLocation(s.getWarehouseLocation())
                .quantity(s.getQuantity())
                .minimumStock(s.getMinimumStock())
                .unit(s.getUnit())
                .unitPrice(s.getUnitPrice())
                .lowStock(s.getQuantity() <= s.getMinimumStock())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    // ── FIND ALL (avec filtre lowStock optionnel) ────────────
    public List<SparePartResponseDTO> findAll(Boolean lowStock) {
        List<SparePart> parts = (lowStock != null && lowStock)
                ? sparePartRepository.findLowStock()
                : sparePartRepository.findAll();
        return parts.stream().map(this::toDTO).toList();
    }

    // ── FIND BY ID ───────────────────────────────────────────
    public SparePartResponseDTO findById(Long id) {
        return toDTO(sparePartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pièce non trouvée avec l'id : " + id)));
    }

    // ── FIND LOW STOCK ───────────────────────────────────────
    public List<SparePartResponseDTO> findLowStock() {
        return sparePartRepository.findLowStock()
                .stream().map(this::toDTO).toList();
    }

    // ── CREATE ───────────────────────────────────────────────
    public SparePartResponseDTO create(SparePartRequestDTO dto) {
        if (sparePartRepository.existsByReference(dto.getReference())) {
            throw new IllegalArgumentException(
                    "Une pièce avec la référence " + dto.getReference() + " existe déjà");
        }

        SparePart part = SparePart.builder()
                .name(dto.getName())
                .reference(dto.getReference())
                .supplier(dto.getSupplier())
                .warehouseLocation(dto.getWarehouseLocation())
                .quantity(dto.getQuantity())
                .minimumStock(dto.getMinimumStock())
                .unit(dto.getUnit())
                .unitPrice(dto.getUnitPrice())
                .build();

        return toDTO(sparePartRepository.save(part));
    }

    // ── UPDATE ───────────────────────────────────────────────
    public SparePartResponseDTO update(Long id, SparePartRequestDTO dto) {
        SparePart existing = sparePartRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pièce non trouvée avec l'id : " + id));

        existing.setName(dto.getName());
        existing.setReference(dto.getReference());
        existing.setSupplier(dto.getSupplier());
        existing.setWarehouseLocation(dto.getWarehouseLocation());
        existing.setQuantity(dto.getQuantity());
        existing.setMinimumStock(dto.getMinimumStock());
        existing.setUnit(dto.getUnit());
        existing.setUnitPrice(dto.getUnitPrice());

        return toDTO(sparePartRepository.save(existing));
    }

    // ── CONSUME STOCK (via intervention) ────────────────────
    public List<InterventionPartResponseDTO> addPartsToIntervention(
            Long interventionId,
            AddInterventionPartsRequestDTO request) {

        Intervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Intervention non trouvée avec l'id : " + interventionId));

        return request.getParts().stream().map(partRequest -> {
            SparePart sparePart = sparePartRepository.findById(partRequest.getSparePartId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Pièce non trouvée avec l'id : " + partRequest.getSparePartId()));

            // ── Vérification stock suffisant ──────────────────
            if (sparePart.getQuantity() < partRequest.getQuantityUsed()) {
                throw new IllegalStateException(String.format(
                    "Stock insuffisant pour '%s' : disponible %d, demandé %d",
                    sparePart.getName(), sparePart.getQuantity(), partRequest.getQuantityUsed()
                ));
            }

            // ── Décrémentation du stock ───────────────────────
            int newQuantity = sparePart.getQuantity() - partRequest.getQuantityUsed();
            sparePart.setQuantity(newQuantity);
            sparePartRepository.save(sparePart);

            // ── Alerte si stock faible après consommation ─────
            if (newQuantity <= sparePart.getMinimumStock()) {
                String alertMessage = String.format(
                    "🔧 Stock faible : '%s' (réf: %s) — quantité restante : %d (minimum: %d)",
                    sparePart.getName(), sparePart.getReference(),
                    newQuantity, sparePart.getMinimumStock()
                );
                notificationService.notifyAdminsAndSupervisors(
                    "Warning", alertMessage, "SparePart", sparePart.getId()
                );
                System.out.println("[STOCK] Alerte stock faible : " + sparePart.getReference());
            }

            // ── Enregistrer la pièce utilisée ────────────────
            InterventionPart ip = InterventionPart.builder()
                    .intervention(intervention)
                    .sparePart(sparePart)
                    .quantityUsed(partRequest.getQuantityUsed())
                    .build();

            InterventionPart saved = interventionPartRepository.save(ip);

            return InterventionPartResponseDTO.builder()
                    .id(saved.getId())
                    .interventionId(interventionId)
                    .sparePartId(sparePart.getId())
                    .sparePartName(sparePart.getName())
                    .sparePartReference(sparePart.getReference())
                    .quantityUsed(saved.getQuantityUsed())
                    .createdAt(saved.getCreatedAt())
                    .build();

        }).toList();
    }

    // ── CONSUME STOCK (via maintenance préventive) ──────────
    // Duplication volontaire de addPartsToIntervention() : isolation totale
    // du flux panne (intervention_parts), zéro risque de régression.
    public void addPartsToPreventiveMaintenance(Long preventiveMaintenanceId,
            List<ConsumeStockRequestDTO> parts) {

        if (parts == null || parts.isEmpty()) return;

        PreventiveMaintenance pm = preventiveMaintenanceRepository.findById(preventiveMaintenanceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Maintenance non trouvée avec l'id : " + preventiveMaintenanceId));

        for (ConsumeStockRequestDTO partRequest : parts) {
            SparePart sparePart = sparePartRepository.findById(partRequest.getSparePartId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Pièce non trouvée avec l'id : " + partRequest.getSparePartId()));

            if (sparePart.getQuantity() < partRequest.getQuantityUsed()) {
                throw new IllegalStateException(String.format(
                    "Stock insuffisant pour '%s' : disponible %d, demandé %d",
                    sparePart.getName(), sparePart.getQuantity(), partRequest.getQuantityUsed()
                ));
            }

            int newQuantity = sparePart.getQuantity() - partRequest.getQuantityUsed();
            sparePart.setQuantity(newQuantity);
            sparePartRepository.save(sparePart);

            if (newQuantity <= sparePart.getMinimumStock()) {
                String alertMessage = String.format(
                    "🔧 Stock faible : '%s' (réf: %s) — quantité restante : %d (minimum: %d)",
                    sparePart.getName(), sparePart.getReference(),
                    newQuantity, sparePart.getMinimumStock()
                );
                notificationService.notifyAdminsAndSupervisors(
                    "Warning", alertMessage, "SparePart", sparePart.getId()
                );
                System.out.println("[STOCK] Alerte stock faible : " + sparePart.getReference());
            }

            preventiveMaintenancePartRepository.save(
                PreventiveMaintenancePart.builder()
                    .preventiveMaintenance(pm)
                    .sparePart(sparePart)
                    .quantityUsed(partRequest.getQuantityUsed())
                    .build()
            );
        }
    }
    // ── HISTORIQUE DE CONSOMMATION (pannes + maintenance préventive) ──
    public List<PartConsumptionResponseDTO> getConsumptionHistory(String type) {
        List<PartConsumptionResponseDTO> result = new java.util.ArrayList<>();

        // ── Consommation via interventions de pannes ──
        if (type == null || type.equalsIgnoreCase("CORRECTIVE")) {
            interventionPartRepository.findAll().forEach(ip -> {
                Intervention intervention = ip.getIntervention();
                SparePart sp = ip.getSparePart();
                BigDecimal unitPrice = sp.getUnitPrice();
                BigDecimal total = unitPrice != null
                        ? unitPrice.multiply(BigDecimal.valueOf(ip.getQuantityUsed()))
                        : null;

                result.add(PartConsumptionResponseDTO.builder()
                        .id(ip.getId())
                        .consumptionDate(ip.getCreatedAt())
                        .sparePartName(sp.getName())
                        .sparePartReference(sp.getReference())
                        .quantityUsed(ip.getQuantityUsed())
                        .unitPrice(unitPrice)
                        .totalPrice(total)
                        .consumptionType("CORRECTIVE")
                        .technicianName(intervention.getTechnician() != null
                                ? intervention.getTechnician().getFullName() : null)
                        .equipmentCode(intervention.getFailure() != null
                                ? intervention.getFailure().getEquipment().getCode() : null)
                        .equipmentName(intervention.getFailure() != null
                                ? intervention.getFailure().getEquipment().getName() : null)
                        .build());
            });
        }

        // ── Consommation via maintenance préventive ──
        if (type == null || type.equalsIgnoreCase("PREVENTIVE")) {
            preventiveMaintenancePartRepository.findAll().forEach(pmp -> {
                PreventiveMaintenance pm = pmp.getPreventiveMaintenance();
                SparePart sp = pmp.getSparePart();
                BigDecimal unitPrice = sp.getUnitPrice();
                BigDecimal total = unitPrice != null
                        ? unitPrice.multiply(BigDecimal.valueOf(pmp.getQuantityUsed()))
                        : null;

                result.add(PartConsumptionResponseDTO.builder()
                        .id(pmp.getId())
                        .consumptionDate(pmp.getCreatedAt())
                        .sparePartName(sp.getName())
                        .sparePartReference(sp.getReference())
                        .quantityUsed(pmp.getQuantityUsed())
                        .unitPrice(unitPrice)
                        .totalPrice(total)
                        .consumptionType("PREVENTIVE")
                        .technicianName(pm.getAssignedTechnician() != null
                                ? pm.getAssignedTechnician().getFullName() : null)
                        .equipmentCode(pm.getEquipment().getCode())
                        .equipmentName(pm.getEquipment().getName())
                        .build());
            });
        }

        // ── Tri par date décroissante (plus récent en premier) ──
        result.sort((a, b) -> b.getConsumptionDate().compareTo(a.getConsumptionDate()));

        return result;
    }
    // ── Pièces utilisées pour une intervention donnée ────────
    public List<InterventionPartResponseDTO> findPartsByIntervention(Long interventionId) {
        return interventionPartRepository.findByInterventionId(interventionId).stream()
                .map(ip -> InterventionPartResponseDTO.builder()
                        .id(ip.getId())
                        .interventionId(interventionId)
                        .sparePartId(ip.getSparePart().getId())
                        .sparePartName(ip.getSparePart().getName())
                        .sparePartReference(ip.getSparePart().getReference())
                        .quantityUsed(ip.getQuantityUsed())
                        .createdAt(ip.getCreatedAt())
                        .build())
                .toList();
    }

    // ── Pièces utilisées pour une maintenance préventive donnée ──
    public List<PreventiveMaintenancePartResponseDTO> findPartsByPreventiveMaintenance(Long pmId) {
        return preventiveMaintenancePartRepository.findByPreventiveMaintenanceId(pmId).stream()
                .map(pmp -> PreventiveMaintenancePartResponseDTO.builder()
                        .id(pmp.getId())
                        .preventiveMaintenanceId(pmId)
                        .sparePartId(pmp.getSparePart().getId())
                        .sparePartName(pmp.getSparePart().getName())
                        .sparePartReference(pmp.getSparePart().getReference())
                        .quantityUsed(pmp.getQuantityUsed())
                        .createdAt(pmp.getCreatedAt())
                        .build())
                .toList();
    }
}