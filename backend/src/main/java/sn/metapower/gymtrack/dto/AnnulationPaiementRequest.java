package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnulationPaiementRequest(
        @NotBlank(message = "Le motif d'annulation est obligatoire")
        @Size(min = 3, max = 255, message = "Le motif doit contenir entre 3 et 255 caractères")
        String motif
) {}
