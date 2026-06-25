package com.suprajit.gmao_backend.auth.service;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

    public LoginResponse login(LoginRequest request) {
        // Spring Security vérifie email + password (BCrypt) automatiquement
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        // Générer le token avec claims supplémentaires
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
}