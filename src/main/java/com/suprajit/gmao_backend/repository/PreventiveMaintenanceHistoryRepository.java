package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.PreventiveMaintenanceHistory;

public interface PreventiveMaintenanceHistoryRepository extends JpaRepository<PreventiveMaintenanceHistory, Long> {
    List<PreventiveMaintenanceHistory> findByEquipmentIdOrderByCompletedAtDesc(Long equipmentId);
    List<PreventiveMaintenanceHistory> findAllByOrderByCompletedAtDesc();
    List<PreventiveMaintenanceHistory> findByTechnicianIdOrderByCompletedAtDesc(Long technicianId);
}