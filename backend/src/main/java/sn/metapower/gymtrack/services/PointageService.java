package sn.metapower.gymtrack.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.config.CacheConfig;
import sn.metapower.gymtrack.dto.ScanRequest;
import sn.metapower.gymtrack.dto.ScanResponse;
import sn.metapower.gymtrack.entities.*;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.PointageRepository;
import sn.metapower.gymtrack.repositories.SiteRepository;
import sn.metapower.gymtrack.repositories.TicketEntreeRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Cœur du système : valide un scan QR à l'accueil d'une salle.
 * Flux unifié membre + ticket. Anti-doublon via Caffeine cache.
 */
@Service
public class PointageService {

    private static final Logger log = LoggerFactory.getLogger(PointageService.class);

    private final MembreRepository membreRepository;
    private final TicketEntreeRepository ticketRepository;
    private final SiteRepository siteRepository;
    private final PointageRepository pointageRepository;
    private final MensualiteService mensualiteService;
    private final CacheManager cacheManager;

    public PointageService(MembreRepository membreRepository,
                           TicketEntreeRepository ticketRepository,
                           SiteRepository siteRepository,
                           PointageRepository pointageRepository,
                           MensualiteService mensualiteService,
                           CacheManager cacheManager) {
        this.membreRepository = membreRepository;
        this.ticketRepository = ticketRepository;
        this.siteRepository = siteRepository;
        this.pointageRepository = pointageRepository;
        this.mensualiteService = mensualiteService;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public ScanResponse scan(ScanRequest req) {
        Site site = siteRepository.findById(req.siteId())
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + req.siteId()));

        // 1. Anti-doublon : même token + même site dans les 5 minutes suivant une entrée AUTORISÉE.
        //    Un refus ne verrouille pas le QR (ex. le membre paie au comptoir puis re-scanne).
        String dedupKey = req.qrToken() + ":" + site.getId();
        Cache cache = cacheManager.getCache(CacheConfig.SCAN_DEDUP_CACHE);
        if (cache != null && cache.get(dedupKey) != null) {
            log.info("Scan ignoré (doublon < 5 min) : token={}, site={}", req.qrToken(), site.getId());
            return ScanResponse.refus("DOUBLON_RECENT");
        }

        ScanResponse response = scanSansDedup(req, site);
        if (response.autorise() && cache != null) {
            cache.put(dedupKey, Boolean.TRUE);
        }
        return response;
    }

    private ScanResponse scanSansDedup(ScanRequest req, Site site) {
        // 2. Tenter membre d'abord
        Optional<Membre> membreOpt = membreRepository.findByQrCodeToken(req.qrToken());
        if (membreOpt.isPresent()) {
            return scanMembre(membreOpt.get(), site, req);
        }

        // 3. Sinon tenter ticket
        Optional<TicketEntree> ticketOpt = ticketRepository.findByQrCodeToken(req.qrToken());
        if (ticketOpt.isPresent()) {
            return scanTicket(ticketOpt.get(), site, req);
        }

        // 4. QR inconnu
        log.warn("Scan refusé : QR_INVALIDE token={}", req.qrToken());
        enregistrerRefus(null, null, site, "QR_INVALIDE", req);
        return ScanResponse.refus("QR_INVALIDE");
    }

    // ─── Flux MEMBRE ──────────────────────────────────────────
    private ScanResponse scanMembre(Membre membre, Site site, ScanRequest req) {
        if (membre.getStatut() == StatutMembre.SUSPENDU) {
            enregistrerRefus(membre, null, site, "MEMBRE_SUSPENDU", req);
            return ScanResponse.refus("MEMBRE_SUSPENDU");
        }
        if (!mensualiteService.estAJour(membre)) {
            enregistrerRefus(membre, null, site, "PAIEMENT_EN_RETARD", req);
            return ScanResponse.refus("PAIEMENT_EN_RETARD");
        }

        Pointage pointage = new Pointage();
        pointage.setMembre(membre);
        pointage.setSite(site);
        pointage.setStatut(StatutPointage.AUTORISE);
        pointage.setTypeAcces(TypeAcces.MEMBRE);
        pointage.setScanSource(req.scanSource() != null ? req.scanSource() : ScanSource.TABLETTE);
        pointage.setHeureEntree(Instant.now());
        pointageRepository.save(pointage);

        log.info("Pointage MEMBRE autorisé : membre={} site={}", membre.getNumeroCarte(), site.getNom());
        return ScanResponse.autoriseMembre(
                membre.getNom(),
                membre.getPrenom(),
                membre.getNumeroCarte(),
                site.getNom(),
                membre.getPlan() != null ? membre.getPlan().getNom() : null,
                pointage.getHeureEntree()
        );
    }

    // ─── Flux TICKET ──────────────────────────────────────────
    private ScanResponse scanTicket(TicketEntree ticket, Site site, ScanRequest req) {
        if (ticket.getStatut() == StatutTicket.UTILISE) {
            enregistrerRefus(null, ticket, site, "TICKET_DEJA_UTILISE", req);
            return ScanResponse.refus("TICKET_DEJA_UTILISE");
        }
        if (ticket.getStatut() == StatutTicket.ANNULE) {
            enregistrerRefus(null, ticket, site, "TICKET_ANNULE", req);
            return ScanResponse.refus("TICKET_ANNULE");
        }
        if (ticket.getDateValidite().isBefore(Instant.now())) {
            ticket.setStatut(StatutTicket.EXPIRE);
            ticketRepository.save(ticket);
            enregistrerRefus(null, ticket, site, "TICKET_EXPIRE", req);
            return ScanResponse.refus("TICKET_EXPIRE");
        }

        // Marquer le ticket consommé
        ticket.setStatut(StatutTicket.UTILISE);
        ticket.setDateUtilisation(Instant.now());
        ticketRepository.save(ticket);

        Pointage pointage = new Pointage();
        pointage.setTicket(ticket);
        pointage.setSite(site);
        pointage.setStatut(StatutPointage.AUTORISE);
        pointage.setTypeAcces(TypeAcces.TICKET);
        pointage.setScanSource(req.scanSource() != null ? req.scanSource() : ScanSource.TABLETTE);
        pointage.setHeureEntree(Instant.now());
        pointageRepository.save(pointage);

        log.info("Pointage TICKET autorisé : ticket={} site={}", ticket.getNumero(), site.getNom());
        return ScanResponse.autoriseTicket(
                ticket.getNomClient(),
                ticket.getNumero(),
                site.getNom(),
                pointage.getHeureEntree(),
                ticket.getDateValidite()
        );
    }

    private void enregistrerRefus(Membre membre, TicketEntree ticket, Site site,
                                   String motif, ScanRequest req) {
        // On enregistre uniquement les refus liés à un acteur connu (membre OU ticket).
        // Les QR_INVALIDE purs ne sont pas persistés (contrainte chk_pointage_acteur).
        if (membre == null && ticket == null) {
            return;
        }
        Pointage refus = new Pointage();
        refus.setMembre(membre);
        refus.setTicket(ticket);
        refus.setSite(site);
        refus.setStatut(StatutPointage.REFUSE);
        refus.setMotifRefus(motif);
        refus.setTypeAcces(membre != null ? TypeAcces.MEMBRE : TypeAcces.TICKET);
        refus.setScanSource(req.scanSource() != null ? req.scanSource() : ScanSource.TABLETTE);
        refus.setHeureEntree(Instant.now());
        pointageRepository.save(refus);
    }
}
