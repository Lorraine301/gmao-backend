package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.suprajit.gmao_backend.entity.MonthlyReport;

public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    Optional<MonthlyReport> findByMonthAndYear(Integer month, Integer year);
    List<MonthlyReport> findAllByOrderByYearDescMonthDesc();
}