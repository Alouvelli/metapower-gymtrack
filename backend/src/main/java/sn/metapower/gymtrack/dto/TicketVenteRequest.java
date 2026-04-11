package sn.metapower.gymtrack.dto;

import jakarta.validation.constraints.NotNull;
import sn.metapower.gymtrack.entities.ModePaiement;

public record TicketVenteRequest(
        @NotNull Long tarifId,
        @NotNull Long siteId,
        @NotNull ModePaiement modePaiement,
        String nomClient,
        String telephoneClient
) {}
