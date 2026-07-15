package com.suprajit.gmao_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.WeeklyReport;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {
    List<WeeklyReport> findAllByOrderByGeneratedAtDesc();
}