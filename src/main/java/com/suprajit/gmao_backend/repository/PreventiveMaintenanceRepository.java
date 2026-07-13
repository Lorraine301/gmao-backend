package com.suprajit.gmao_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.suprajit.gmao_backend.entity.PreventiveMaintenance;
import com.suprajit.gmao_backend.entity.enums.ExecutionStatus;
import com.suprajit.gmao_backend.entity.enums.MaintenanceStatus;

public interface PreventiveMaintenanceRepository
        extends JpaRepository<PreventiveMaintenance, Long> {

    List<PreventiveMaintenance> findByEquipmentId(Long equipmentId);

    List<PreventiveMaintenance> findByStatus(MaintenanceStatus status);

    // Maintenances en retard : next_maintenance_date < aujourd'hui et pas encore Completed/Cancelled
    @Query("SELECT pm FROM PreventiveMaintenance pm WHERE " +
           "pm.nextMaintenanceDate < :today AND " +
           "pm.status NOT IN ('Completed', 'Cancelled')")
    List<PreventiveMaintenance> findOverdue(
        @org.springframework.data.repository.query.Param("today") LocalDate today);

    // Maintenances dont le rappel arrive (next_reminder_date <= aujourd'hui)
    @Query("SELECT pm FROM PreventiveMaintenance pm WHERE " +
           "pm.nextReminderDate <= :today AND pm.status = 'Scheduled'")
    List<PreventiveMaintenance> findDueForReminder(
        @org.springframework.data.repository.query.Param("today") LocalDate today);

    List<PreventiveMaintenance> findByAssignedTechnicianIdAndExecutionStatusNot(
    Long technicianId, ExecutionStatus excludedStatus);

    List<PreventiveMaintenance> findByAssignedTechnicianIdAndExecutionStatus(
    Long technicianId, ExecutionStatus status);
}