package com.suprajit.gmao_backend.auth.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.suprajit.gmao_backend.auth.dto.ChangePasswordDTO;
import com.suprajit.gmao_backend.auth.dto.LoginRequest;
import com.suprajit.gmao_backend.auth.dto.LoginResponse;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.UserRepository;
import com.suprajit.gmao_backend.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;      

   public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // ── Mise à jour de la dernière connexion ──
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(), user.getPassword(), java.util.List.of()),
                Map.of("role", user.getRole().getName(), "userId", user.getId())
        );

        return LoginResponse.builder()
                .token(token)
                .role(user.getRole().getName())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }   
    // ── Changer son propre mot de passe (utilisateur connecté) ──
    public void changePassword(ChangePasswordDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("L'ancien mot de passe est incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }  
       
    // ── Le technicien change sa propre disponibilité ──────────
    public void updateMyAvailability(String availabilityStatus) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        user.setAvailabilityStatus(availabilityStatus);
        userRepository.save(user);
    }
}