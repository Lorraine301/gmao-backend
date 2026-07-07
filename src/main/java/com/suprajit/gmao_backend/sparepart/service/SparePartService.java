package com.suprajit.gmao_backend.sparepart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.InterventionPart;
import com.suprajit.gmao_backend.entity.SparePart;
import com.suprajit.gmao_backend.notification.service.NotificationService;
import com.suprajit.gmao_backend.repository.InterventionPartRepository;
import com.suprajit.gmao_backend.repository.InterventionRepository;
import com.suprajit.gmao_backend.repository.SparePartRepository;
import com.suprajit.gmao_backend.sparepart.dto.AddInterventionPartsRequestDTO;
import com.suprajit.gmao_backend.sparepart.dto.InterventionPartResponseDTO;
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
}