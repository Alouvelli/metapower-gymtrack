package sn.metapower.gymtrack.dto;

public record DashboardStats(
        // Périmètre
        Long siteId,           // null = consolidé toutes salles
        String siteNom,
        // Membres
        long membresActifs,
        long membresEnRetard,
        long membresSuspendus,
        // Activité du jour
        long pointagesAujourdhui,
        long ticketsVendusAujourdhui,
        // Chiffre d'affaires du mois
        long caMensualitesMois,
        long caTicketsMois,
        long caTotalMois
) {}
