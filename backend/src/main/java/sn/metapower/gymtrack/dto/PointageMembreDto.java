package sn.metapower.gymtrack.dto;

import sn.metapower.gymtrack.entities.Pointage;

import java.time.Instant;

public record PointageMembreDto(
        Long id,
        Instant heureEntree,
        Instant heureSortie,
        String statut,
        String motifRefus,
        String typeAcces,
        String scanSource,
        String siteNom
) {
    public static PointageMembreDto from(Pointage p) {
        return new PointageMembreDto(
                p.getId(),
                p.getHeureEntree(),
                p.getHeureSortie(),
                p.getStatut() != null ? p.getStatut().name() : null,
                p.getMotifRefus(),
                p.getTypeAcces() != null ? p.getTypeAcces().name() : null,
                p.getScanSource() != null ? p.getScanSource().name() : null,
                p.getSite() != null ? p.getSite().getNom() : null
        );
    }
}
