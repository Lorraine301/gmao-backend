package com.suprajit.gmao_backend.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.user.dto.TechnicianDto;
import com.suprajit.gmao_backend.user.dto.UserRequestDTO;
import com.suprajit.gmao_backend.user.dto.UserResponseDTO;
import com.suprajit.gmao_backend.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    // ── Endpoint existant, inchangé ──────────────────────────
    @GetMapping("/technicians")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    @Transactional
    public ResponseEntity<List<TechnicianDto>> getTechnicians() {
        List<TechnicianDto> result = userRepository
                .findByRole_NameAndAvailabilityStatus("Technician", "Available")
                .stream()
                .map(u -> new TechnicianDto(
                        u.getId(), u.getFullName(), u.getEmail(), u.getEmployeeCode(),
                        u.getSpeciality(), u.getAvailabilityStatus()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    // ── GET /api/users?role= ──────────────────────────────────
    @Operation(summary = "Lister les utilisateurs", description = "Filtre optionnel par rôle (?role=Technician)")
    @GetMapping
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<List<UserResponseDTO>> findAll(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.findAll(role));
    }

    // ── GET /api/users/{id} ────────────────────────────────────
    @Operation(summary = "Détail d'un utilisateur")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Admin') or hasRole('Supervisor')")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // ── POST /api/users ────────────────────────────────────────
    @Operation(summary = "Créer un utilisateur")
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    // ── PUT /api/users/{id} ────────────────────────────────────
    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<UserResponseDTO> update(@PathVariable Long id, @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    // ── PUT /api/users/{id}/deactivate ────────────────────────
    @Operation(summary = "Désactiver un utilisateur (suppression douce)")
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ── PUT /api/users/{id}/reactivate ────────────────────────
    @Operation(summary = "Réactiver un utilisateur")
    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) {
        userService.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}