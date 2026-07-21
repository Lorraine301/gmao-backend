package com.suprajit.gmao_backend.auth.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String speciality;
    private String availabilityStatus;
    private LocalDateTime lastLoginAt;  
}