package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangementMotDePasseRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire") String motDePasseActuel,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 100, message = "Le nouveau mot de passe doit contenir au moins 8 caractères")
        String nouveauMotDePasse
) {}
