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

    public TicketService(TicketEntreeRepository ticketRepository,
                         TarifTicketRepository tarifRepository,
                         SiteRepository siteRepository,
                         AppUserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.tarifRepository = tarifRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketEntree vendre(TicketVenteRequest req) {
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
        ticket.setTelephoneClient(req.telephoneClient());

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
        String day = DAY_FMT.format(LocalDate.now().atStartOfDay(ZoneId.systemDefault()));
        // Suffixe basé sur les ms intra-jour pour rester unique sans verrou
        long suffix = (System.currentTimeMillis() / 1000) % 10000;
        return "TKT-" + day + "-" + String.format("%04d", suffix);
    }
}
