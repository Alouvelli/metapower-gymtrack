package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record GerantCreateDTO(
        @NotBlank String username,
        @NotBlank String password,
        @Email    String email,
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotNull  Long siteId,
        @NotNull  LocalTime heureDebut,
        @NotNull  LocalTime heureFin
) {}
