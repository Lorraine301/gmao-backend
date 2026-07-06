package com.suprajit.gmao_backend.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.user.dto.TechnicianDto;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/technicians")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    @Transactional
    public ResponseEntity<List<TechnicianDto>> getTechnicians() {
        List<TechnicianDto> result = userRepository
                .findByRole_NameAndAvailabilityStatus("Technician", "Available")
                .stream()
                .map(u -> new TechnicianDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getEmployeeCode(),
                        u.getSpeciality(),
                        u.getAvailabilityStatus()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}