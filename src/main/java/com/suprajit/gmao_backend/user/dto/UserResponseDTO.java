package com.suprajit.gmao_backend.user.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private Long id;
    private String employeeCode;
    private String fullName;
    private String email;
    private String role;
    private String speciality;
    private String availabilityStatus;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}