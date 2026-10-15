package sn.metapower.gymtrack.dto;

import sn.metapower.gymtrack.entities.GerantSite;

import java.time.LocalTime;

public record GerantSiteDTO(
        Long id,
        Long userId,
        String nom,
        String prenom,
        String username,
        String email,
        Long siteId,
        String siteNom,
        LocalTime heureDebut,
        LocalTime heureFin,
        boolean actif
) {
    /** Fabrique un GerantSiteDTO depuis une entité. */
    public static GerantSiteDTO from(GerantSite gs) {
        return new GerantSiteDTO(
                gs.getId(),
                gs.getUser().getId(),
                gs.getUser().getNom(),
                gs.getUser().getPrenom(),
                gs.getUser().getUsername(),
                gs.getUser().getEmail(),
                gs.getSite().getId(),
                gs.getSite().getNom(),
                gs.getHeureDebut(),
                gs.getHeureFin(),
                gs.isActif()
        );
    }
}
