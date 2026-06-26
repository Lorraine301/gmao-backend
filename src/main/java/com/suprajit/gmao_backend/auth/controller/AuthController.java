package com.suprajit.gmao_backend.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suprajit.gmao_backend.auth.dto.LoginRequest;
import com.suprajit.gmao_backend.auth.dto.LoginResponse;
import com.suprajit.gmao_backend.auth.dto.UserProfileResponse;
import com.suprajit.gmao_backend.auth.service.AuthService;
import com.suprajit.gmao_backend.entity.User;
import com.suprajit.gmao_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(
             @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

    return ResponseEntity.ok(UserProfileResponse.builder()
            .userId(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .speciality(user.getSpeciality())
            .availabilityStatus(user.getAvailabilityStatus())
            .build());
}
}