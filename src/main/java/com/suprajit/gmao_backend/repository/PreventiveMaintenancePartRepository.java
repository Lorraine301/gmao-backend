// repository/PreventiveMaintenancePartRepository.java
package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.PreventiveMaintenancePart;

public interface PreventiveMaintenancePartRepository
        extends JpaRepository<PreventiveMaintenancePart, Long> {
                List<PreventiveMaintenancePart> findByPreventiveMaintenanceId(Long preventiveMaintenanceId);
                List<PreventiveMaintenancePart> findByHistoryId(Long historyId);
}