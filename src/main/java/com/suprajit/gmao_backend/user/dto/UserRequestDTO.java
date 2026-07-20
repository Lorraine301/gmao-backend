package com.suprajit.gmao_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "Le code employé est obligatoire")
    private String employeeCode;

    @NotBlank(message = "Le nom complet est obligatoire")
    private String fullName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    // Obligatoire à la création, optionnel à la modification (laisser vide = ne pas changer)
    private String password;

    @NotBlank(message = "Le rôle est obligatoire")
    private String roleName;   // "Admin", "Supervisor", "Technician"

    private String speciality;
    private String availabilityStatus;
}