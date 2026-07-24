package com.suprajit.gmao_backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.auth.dto.ChangePasswordDTO;
import com.suprajit.gmao_backend.auth.dto.LoginRequest;
import com.suprajit.gmao_backend.auth.dto.LoginResponse;
import com.suprajit.gmao_backend.auth.dto.UpdateAvailabilityDTO;
import com.suprajit.gmao_backend.auth.dto.UserProfileResponse;
import com.suprajit.gmao_backend.auth.service.AuthService;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints de connexion et de profil utilisateur")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur avec email/password et retourne un token JWT valide 24h"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie – token JWT retourné",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
            content = @Content),
        @ApiResponse(responseCode = "403", description = "Accès refusé",
            content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
        summary = "Profil de l'utilisateur connecté",
        description = "Retourne les informations du compte associé au token JWT fourni",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil retourné avec succès",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token manquant ou invalide",
            content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        return ResponseEntity.ok(UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .speciality(user.getSpeciality())
                .availabilityStatus(user.getAvailabilityStatus())
                .lastLoginAt(user.getLastLoginAt())
                .build());
    }
    @Operation(
        summary = "Changer son propre mot de passe",
        description = "Nécessite l'ancien mot de passe pour confirmation.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mot de passe changé avec succès"),
        @ApiResponse(responseCode = "400", description = "Ancien mot de passe incorrect ou nouveau mot de passe invalide")
    })
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return ResponseEntity.noContent().build();
    } 
    @Operation(summary = "Changer ma propre disponibilité", description = "Réservé au technicien connecté.")
    @PutMapping("/me/availability")
    public ResponseEntity<Void> updateMyAvailability(@Valid @RequestBody UpdateAvailabilityDTO dto) {
        authService.updateMyAvailability(dto.getAvailabilityStatus());
        return ResponseEntity.noContent().build();
    }  
}