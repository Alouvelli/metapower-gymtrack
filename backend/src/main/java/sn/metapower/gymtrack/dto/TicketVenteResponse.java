package sn.metapower.gymtrack.dto;

import sn.metapower.gymtrack.entities.TicketEntree;

import java.time.Instant;

public record TicketVenteResponse(
        Long id,
        String numero,
        String qrCodeToken,
        String qrCodeBase64,
        Long siteId,
        String siteNom,
        Integer prix,
        String modePaiement,
        Instant dateValidite,
        String nomClient,
        String telephoneClient,
        String lienTicket,
        String lienWhatsApp
) {
    public static TicketVenteResponse from(TicketEntree t, String qrBase64,
                                           String lienTicket, String lienWhatsApp) {
        return new TicketVenteResponse(
                t.getId(),
                t.getNumero(),
                t.getQrCodeToken(),
                qrBase64,
                t.getSite().getId(),
                t.getSite().getNom(),
                t.getPrix(),
                t.getModePaiement().name(),
                t.getDateValidite(),
                t.getNomClient(),
                t.getTelephoneClient(),
                lienTicket,
                lienWhatsApp
        );
    }
}
