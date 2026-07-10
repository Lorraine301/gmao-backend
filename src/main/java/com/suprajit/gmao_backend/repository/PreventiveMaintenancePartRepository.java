// repository/PreventiveMaintenancePartRepository.java
package com.suprajit.gmao_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.PreventiveMaintenancePart;

public interface PreventiveMaintenancePartRepository
        extends JpaRepository<PreventiveMaintenancePart, Long> {
}