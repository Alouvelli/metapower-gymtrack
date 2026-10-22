package sn.metapower.gymtrack.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.TicketVenteRequest;
import sn.metapower.gymtrack.entities.*;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class TicketService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketEntreeRepository ticketRepository;
    private final TarifTicketRepository tarifRepository;
    private final SiteRepository siteRepository;
    private final AppUserRepository userRepository;
    private final WhatsAppService whatsAppService;

    public TicketService(TicketEntreeRepository ticketRepository,
                         TarifTicketRepository tarifRepository,
                         SiteRepository siteRepository,
                         AppUserRepository userRepository,
                         WhatsAppService whatsAppService) {
        this.ticketRepository = ticketRepository;
        this.tarifRepository = tarifRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.whatsAppService = whatsAppService;
    }

    @Transactional
    public TicketEntree vendre(TicketVenteRequest req) {
        // Numéro WhatsApp obligatoire : le ticket est envoyé au client par WhatsApp
        String telephone = whatsAppService.normaliserNumero(req.telephoneClient());

        TarifTicket tarif = tarifRepository.findById(req.tarifId())
                .orElseThrow(() -> new RessourceNotFoundException("Tarif introuvable : " + req.tarifId()));
        Site site = siteRepository.findById(req.siteId())
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + req.siteId()));

        Instant now = Instant.now();

        TicketEntree ticket = new TicketEntree();
        ticket.setNumero(generateNumero());
        ticket.setQrCodeToken(UUID.randomUUID().toString());
        ticket.setSite(site);
        ticket.setTarif(tarif);
        ticket.setPrix(tarif.getPrix());
        ticket.setModePaiement(req.modePaiement());
        ticket.setDatePaiement(now);
        ticket.setDateValidite(now.plus(tarif.getDureeValiditeHeures(), ChronoUnit.HOURS));
        ticket.setStatut(StatutTicket.VALIDE);
        ticket.setNomClient(req.nomClient());
        ticket.setTelephoneClient(telephone);

        // Vendeur (utilisateur courant authentifié)
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            AppUser vendeur = userRepository.findByUsername(username);
            ticket.setVendeur(vendeur);
        } catch (Exception ignored) {
            // Pas de contexte d'auth (ex. test), on continue sans vendeur
        }

        return ticketRepository.save(ticket);
    }

    public TicketEntree findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Ticket introuvable : " + id));
    }

    /**
     * Format : TKT-YYYYMMDD-XXXX où XXXX est le rang dans la journée.
     * Calcul approximatif basé sur le timestamp pour éviter les collisions concurrentes.
     */
    private String generateNumero() {
        String prefix = "TKT-" + DAY_FMT.format(LocalDate.now().atStartOfDay(ZoneId.systemDefault())) + "-";
        // Compteur journalier : dernier numéro du jour + 1. (L'ancien suffixe basé sur
        // l'heure % 10000 se répétait dans la journée → violation d'unicité.)
        long suivant = ticketRepository.findMaxNumeroByPrefix(prefix)
                .map(n -> Long.parseLong(n.substring(prefix.length())) + 1)
                .orElse(1L);
        return prefix + String.format("%04d", suivant);
    }
}
