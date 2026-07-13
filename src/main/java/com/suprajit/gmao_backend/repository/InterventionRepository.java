package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByTechnicianId(Long technicianId);
    List<Intervention> findByFailureId(Long failureId);
    List<Intervention> findByStatus(InterventionStatus status);
    List<Intervention> findByTechnicianIdAndStatus(Long technicianId, InterventionStatus status);
    List<Intervention> findByTechnicianIdAndStatusNot(Long technicianId, InterventionStatus status);

 @Query("""
           SELECT AVG(i.duration)
           FROM Intervention i
           WHERE i.failure.equipment.id = :equipmentId
             AND i.duration IS NOT NULL
           """)
    Double findAverageMttrByEquipment(@Param("equipmentId") Long equipmentId);
}