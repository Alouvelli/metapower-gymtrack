package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotNull;
import sn.metapower.gymtrack.entities.ModePaiement;

import java.time.LocalDate;

public record MensualiteCreateRequest(
        @NotNull Long membreId,
        @NotNull LocalDate moisConcerne,
        @NotNull Integer montant,
        @NotNull ModePaiement modePaiement,
        String reference,
        LocalDate datePaiement
) {}
