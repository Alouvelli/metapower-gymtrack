package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import sn.metapower.gymtrack.entities.ModePaiement;

public record TicketVenteRequest(
        @NotNull Long tarifId,
        @NotNull Long siteId,
        @NotNull ModePaiement modePaiement,
        String nomClient,
        @NotBlank(message = "Le numéro WhatsApp du client est obligatoire") String telephoneClient
) {}
