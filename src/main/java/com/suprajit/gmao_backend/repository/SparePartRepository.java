package com.suprajit.gmao_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.suprajit.gmao_backend.entity.SparePart;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    Optional<SparePart> findByReference(String reference);
    boolean existsByReference(String reference);

    // Pièces dont le stock est sous le minimum
    @Query("SELECT s FROM SparePart s WHERE s.quantity <= s.minimumStock")
    List<SparePart> findLowStock();
}