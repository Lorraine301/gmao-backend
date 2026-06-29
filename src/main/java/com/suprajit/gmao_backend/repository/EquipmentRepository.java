package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.Equipment;
import com.suprajit.gmao_backend.entity.enums.CriticalityLevel;
import com.suprajit.gmao_backend.entity.enums.EquipmentStatus;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByCode(String code);

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByType(String type);

    List<Equipment> findByCriticalityLevel(CriticalityLevel criticalityLevel);

    List<Equipment> findByStatusAndCriticalityLevel(
        EquipmentStatus status,
        CriticalityLevel criticalityLevel
    );

    boolean existsByCode(String code);
}