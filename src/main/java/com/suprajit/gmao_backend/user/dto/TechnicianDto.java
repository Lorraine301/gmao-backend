package com.suprajit.gmao_backend.user.dto;


 
public record TechnicianDto(
    Long id,
    String fullName,
    String email,
    String employeeCode,
    String speciality,
    String availabilityStatus
) {}