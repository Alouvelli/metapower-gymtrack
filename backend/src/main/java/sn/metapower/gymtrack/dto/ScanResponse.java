package sn.metapower.gymtrack.dto;

import sn.metapower.gymtrack.entities.TypeAcces;

import java.time.Instant;

public record ScanResponse(
        boolean autorise,
        TypeAcces type,
        String motifRefus,
        // Infos affichées sur la tablette
        String nom,
        String prenom,
        String numero,        // numeroCarte ou numeroTicket
        String siteNom,
        Instant heureEntree,
        // Pour membres : infos plan
        String planNom,
        // Pour tickets : durée de validité restante
        Instant dateValidite
) {

    public static ScanResponse refus(String motif) {
        return new ScanResponse(false, null, motif, null, null, null, null, null, null, null);
    }

    public static ScanResponse autoriseMembre(String nom, String prenom, String numeroCarte,
                                              String siteNom, String planNom, Instant heureEntree) {
        return new ScanResponse(true, TypeAcces.MEMBRE, null,
                nom, prenom, numeroCarte, siteNom, heureEntree, planNom, null);
    }

    public static ScanResponse autoriseTicket(String nomClient, String numero, String siteNom,
                                              Instant heureEntree, Instant dateValidite) {
        return new ScanResponse(true, TypeAcces.TICKET, null,
                nomClient, null, numero, siteNom, heureEntree, null, dateValidite);
    }
}
