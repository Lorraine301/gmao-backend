package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.InterventionPart;

public interface InterventionPartRepository extends JpaRepository<InterventionPart, Long> {
    List<InterventionPart> findByInterventionId(Long interventionId);
    List<InterventionPart> findBySparePartId(Long sparePartId);
}