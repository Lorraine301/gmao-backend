package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;

public interface FailureRepository extends JpaRepository<Failure, Long> {
    Optional<Failure> findByFailureCode(String failureCode);
    List<Failure> findByStatus(FailureStatus status);
    List<Failure> findByPriority(FailurePriority priority);
    List<Failure> findByEquipmentId(Long equipmentId);
    boolean existsByFailureCode(String failureCode);
}