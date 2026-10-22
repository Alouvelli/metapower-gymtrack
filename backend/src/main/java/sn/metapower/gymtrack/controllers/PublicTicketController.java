package sn.metapower.gymtrack.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.entities.TicketEntree;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.TicketEntreeRepository;
import sn.metapower.gymtrack.services.QrCodeService;
import sn.metapower.gymtrack.services.RecuPdfService;

import java.time.Instant;

/**
 * Accès public au ticket via son token QR (UUID non devinable), pour le lien
 * envoyé au client par WhatsApp. Jamais par id : les id sont séquentiels et
 * permettraient de récupérer les QR valides d'autres clients.
 */
@RestController
@RequestMapping("/api/public/tickets")
public class PublicTicketController {

    private final TicketEntreeRepository ticketRepository;
    private final QrCodeService qrCodeService;
    private final RecuPdfService recuPdfService;

    public PublicTicketController(TicketEntreeRepository ticketRepository,
                                  QrCodeService qrCodeService,
                                  RecuPdfService recuPdfService) {
        this.ticketRepository = ticketRepository;
        this.qrCodeService = qrCodeService;
        this.recuPdfService = recuPdfService;
    }

    /** Infos affichées sur la page /t/{token} (pas de téléphone ni de données vendeur). */
    public record TicketPublic(String numero, String siteNom, String tarif, Integer prix,
                               Instant dateValidite, String statut, String nomClient) {}

    @GetMapping("/{token}")
    public TicketPublic get(@PathVariable String token) {
        TicketEntree t = find(token);
        return new TicketPublic(t.getNumero(), t.getSite().getNom(), t.getTarif().getLibelle(),
                t.getPrix(), t.getDateValidite(), t.getStatut().name(), t.getNomClient());
    }

    @GetMapping(value = "/{token}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrCode(@PathVariable String token) {
        return qrCodeService.generatePng(find(token).getQrCodeToken());
    }

    @GetMapping(value = "/{token}/recu", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> recu(@PathVariable String token) {
        TicketEntree ticket = find(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recu-" + ticket.getNumero() + ".pdf\"")
                .body(recuPdfService.genererRecu(ticket));
    }

    private TicketEntree find(String token) {
        return ticketRepository.findByQrCodeToken(token)
                .orElseThrow(() -> new RessourceNotFoundException("Ticket introuvable"));
    }
}
