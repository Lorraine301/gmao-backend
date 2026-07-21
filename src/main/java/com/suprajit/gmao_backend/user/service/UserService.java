package com.suprajit.gmao_backend.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suprajit.gmao_backend.entity.Role;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.RoleRepository;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.user.dto.UserRequestDTO;
import com.suprajit.gmao_backend.user.dto.UserResponseDTO;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private UserResponseDTO toDTO(User u) {
        return UserResponseDTO.builder()
                .id(u.getId())
                .employeeCode(u.getEmployeeCode())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .role(u.getRole().getName())
                .speciality(u.getSpeciality())
                .availabilityStatus(u.getAvailabilityStatus())
                .active(u.getActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }

    // ── READ ALL, filtrable par rôle ──────────────────────────
    public List<UserResponseDTO> findAll(String roleName) {
        List<User> users = (roleName != null)
                ? userRepository.findByRole_NameIn(List.of(roleName))
                : userRepository.findAll();
        return users.stream().map(this::toDTO).toList();
    }

    // ── READ ONE ────────────────────────────────────────────
    public UserResponseDTO findById(Long id) {
        return toDTO(userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'id : " + id)));
    }

    // ── CREATE ───────────────────────────────────────────────
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmployeeCode(dto.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec le code employé " + dto.getEmployeeCode() + " existe déjà");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire à la création");
        }

        Role role = roleRepository.findByName(dto.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Rôle non trouvé : " + dto.getRoleName()));

        User user = User.builder()
                .employeeCode(dto.getEmployeeCode())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(role)
                .speciality(dto.getSpeciality())
                .availabilityStatus(dto.getAvailabilityStatus())
                .active(true)
                .build();

        return toDTO(userRepository.save(user));
    }

    // ── UPDATE ───────────────────────────────────────────────
    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'id : " + id));

        // Vérifie l'unicité seulement si la valeur a changé
        if (!user.getEmployeeCode().equals(dto.getEmployeeCode())
                && userRepository.existsByEmployeeCode(dto.getEmployeeCode())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec le code employé " + dto.getEmployeeCode() + " existe déjà");
        }
        if (!user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec l'email " + dto.getEmail() + " existe déjà");
        }

        Role role = roleRepository.findByName(dto.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Rôle non trouvé : " + dto.getRoleName()));

        user.setEmployeeCode(dto.getEmployeeCode());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setSpeciality(dto.getSpeciality());
        user.setAvailabilityStatus(dto.getAvailabilityStatus());

        // Mot de passe changé uniquement s'il est fourni (non vide)
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        return toDTO(userRepository.save(user));
    }

    // ── DÉSACTIVER (suppression douce) ────────────────────────
    public void deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        user.setActive(false);
        userRepository.save(user);
    }

    // ── RÉACTIVER ────────────────────────────────────────────
    public void reactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        user.setActive(true);
        userRepository.save(user);
    }
}