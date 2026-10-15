package sn.metapower.gymtrack.dto;

import java.time.LocalTime;

public record GerantStatsDTO(
        Long gerantId,
        String nom,
        String prenom,
        String username,
        Long siteId,
        String siteNom,
        LocalTime heureDebut,
        LocalTime heureFin,
        long nbMensualitesEnregistrees,
        long caMensualites,
        long nbTicketsVendus,
        long caTickets,
        long caTotal,
        long membresActifsSite,
        long membresPayesCeMois,
        double tauxRecouvrement
) {}
