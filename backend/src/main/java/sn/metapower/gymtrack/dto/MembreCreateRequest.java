package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MembreCreateRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank String telephone,
        @Email String email,
        LocalDate dateNaissance,
        String adresse,
        Long planId
) {}
