package com.suprajit.gmao_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAvailabilityDTO {

    @NotBlank(message = "La disponibilité est obligatoire")
    @Pattern(regexp = "Available|Unavailable", message = "Valeur invalide : Available ou Unavailable uniquement")
    private String availabilityStatus;
}