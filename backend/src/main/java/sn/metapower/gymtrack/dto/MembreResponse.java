package sn.metapower.gymtrack.dto;

import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.StatutMembre;

import java.time.LocalDate;

public record MembreResponse(
        Long id,
        String numeroCarte,
        String nom,
        String prenom,
        String telephone,
        String email,
        LocalDate dateNaissance,
        String adresse,
        String photoUrl,
        String qrCodeToken,
        StatutMembre statut,
        Long planId,
        String planNom,
        Integer planPrix
) {
    public static MembreResponse from(Membre m) {
        return new MembreResponse(
                m.getId(),
                m.getNumeroCarte(),
                m.getNom(),
                m.getPrenom(),
                m.getTelephone(),
                m.getEmail(),
                m.getDateNaissance(),
                m.getAdresse(),
                m.getPhotoUrl(),
                m.getQrCodeToken(),
                m.getStatut(),
                m.getPlan() != null ? m.getPlan().getId() : null,
                m.getPlan() != null ? m.getPlan().getNom() : null,
                m.getPlan() != null ? m.getPlan().getPrix() : null
        );
    }
}
