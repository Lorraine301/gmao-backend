package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.Intervention;
import com.suprajit.gmao_backend.entity.enums.InterventionStatus;

public interface InterventionRepository extends JpaRepository<Intervention, Long> {
    List<Intervention> findByTechnicianId(Long technicianId);
    List<Intervention> findByFailureId(Long failureId);
    List<Intervention> findByStatus(InterventionStatus status);
}