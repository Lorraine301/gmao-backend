package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.suprajit.gmao_backend.entity.Failure;
import com.suprajit.gmao_backend.entity.enums.FailurePriority;
import com.suprajit.gmao_backend.entity.enums.FailureStatus;

public interface FailureRepository extends JpaRepository<Failure, Long> {

    Optional<Failure> findByFailureCode(String failureCode);
    boolean existsByFailureCode(String failureCode);

    @Query("SELECT f FROM Failure f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:priority IS NULL OR f.priority = :priority) AND " +
           "(:equipmentId IS NULL OR f.equipment.id = :equipmentId) " +
           "ORDER BY f.reportedAt DESC")
    List<Failure> findWithFilters(
        @Param("status") FailureStatus status,
        @Param("priority") FailurePriority priority,
        @Param("equipmentId") Long equipmentId);

    long countByFailureCodeStartingWith(String prefix);
}