package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByCode(String code);
    boolean existsByCode(String code);

    // Filtres simples
    List<Equipment> findByStatus(EquipmentStatus status);
    List<Equipment> findByType(String type);
    List<Equipment> findByCriticalityLevel(CriticalityLevel criticalityLevel);

    // Filtres combinés
    List<Equipment> findByStatusAndCriticalityLevel(
        EquipmentStatus status, CriticalityLevel criticalityLevel);

    List<Equipment> findByStatusAndType(
        EquipmentStatus status, String type);

    List<Equipment> findByStatusAndTypeAndCriticalityLevel(
        EquipmentStatus status, String type, CriticalityLevel criticalityLevel);
    
    // Recherche textuelle sur le code et le type
    @Query("SELECT e FROM Equipment e WHERE " +
       "(:search IS NULL OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.type) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Equipment> searchEquipments(@Param("search") String search);

    @Query("SELECT e FROM Equipment e WHERE " +
       "(:status IS NULL OR e.status = :status) AND " +
       "(:type IS NULL OR e.type = :type) AND " +
       "(:criticality IS NULL OR e.criticalityLevel = :criticality) AND " +
       "(:search IS NULL OR LOWER(e.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(e.type) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Equipment> findWithFilters(
    @Param("status") EquipmentStatus status,
    @Param("type") String type,
    @Param("criticality") CriticalityLevel criticality,
    @Param("search") String search);
}