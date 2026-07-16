package com.suprajit.gmao_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.AiAnalysis;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByFailureId(Long failureId);
}