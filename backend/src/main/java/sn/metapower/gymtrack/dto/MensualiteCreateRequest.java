package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotNull;
import sn.metapower.gymtrack.entities.ModePaiement;

import java.time.LocalDate;

public record MensualiteCreateRequest(
        @NotNull Long membreId,
        // Début de la période couverte ; vide = date du paiement, ou lendemain
        // de la fin de l'abonnement en cours s'il n'est pas encore terminé
        LocalDate dateDebut,
        @NotNull Integer montant,
        @NotNull ModePaiement modePaiement,
        String reference,
        LocalDate datePaiement
) {}
