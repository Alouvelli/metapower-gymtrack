package sn.metapower.gymtrack.services;

import org.springframework.stereotype.Service;
import sn.metapower.gymtrack.dto.DashboardStats;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.entities.StatutMembre;
import sn.metapower.gymtrack.entities.StatutPaiement;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DashboardService {

    private final MembreRepository membreRepository;
    private final MensualiteRepository mensualiteRepository;
    private final TicketEntreeRepository ticketRepository;
    private final PointageRepository pointageRepository;
    private final SiteRepository siteRepository;

    public DashboardService(MembreRepository membreRepository,
                            MensualiteRepository mensualiteRepository,
                            TicketEntreeRepository ticketRepository,
                            PointageRepository pointageRepository,
                            SiteRepository siteRepository) {
        this.membreRepository = membreRepository;
        this.mensualiteRepository = mensualiteRepository;
        this.ticketRepository = ticketRepository;
        this.pointageRepository = pointageRepository;
        this.siteRepository = siteRepository;
    }

    /** Stats globales ou filtrées par site (admin). */
    public DashboardStats getStats(Long siteId) {
        return getStats(siteId, null);
    }

    /**
     * Stats filtrées par site et, optionnellement, par gérant (userId).
     * Quand gerantUserId != null : CA mensualités et tickets limités aux transactions
     * enregistrées par ce gérant.
     */
    public DashboardStats getStats(Long siteId, Long gerantUserId) {
        Site site = null;
        String siteNom = "Toutes les salles";
        if (siteId != null) {
            site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + siteId));
            siteNom = site.getNom();
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today      = LocalDate.now();
        Instant dayStart     = today.atStartOfDay(zone).toInstant();
        Instant dayEnd       = today.plusDays(1).atStartOfDay(zone).toInstant();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd   = monthStart.plusMonths(1).minusDays(1);
        Instant monthStartI  = monthStart.atStartOfDay(zone).toInstant();
        Instant monthEndI    = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        long actifs    = membreRepository.countByStatut(StatutMembre.ACTIF);
        long enRetard  = membreRepository.countByStatut(StatutMembre.EXPIRE);
        long suspendus = membreRepository.countByStatut(StatutMembre.SUSPENDU);

        long pointagesJour = (site != null)
                ? pointageRepository.countBySiteAndHeureEntreeBetween(site, dayStart, dayEnd)
                : pointageRepository.countByHeureEntreeBetween(dayStart, dayEnd);
        long ticketsJour = (site != null)
                ? ticketRepository.countBySiteAndDatePaiementBetween(site, dayStart, dayEnd)
                : ticketRepository.countByDatePaiementBetween(dayStart, dayEnd);

        // CA du jour et du mois — filtrés par gérant si renseigné
        long caMensualitesJour, caMensualites, caTicketsJour, caTickets;
        if (gerantUserId != null) {
            caMensualitesJour = mensualiteRepository.sumMontantByUserAndPeriod(
                    gerantUserId, StatutPaiement.PAYE, today, today);
            caMensualites = mensualiteRepository.sumMontantByUserAndPeriod(
                    gerantUserId, StatutPaiement.PAYE, monthStart, monthEnd);
            caTicketsJour = ticketRepository.sumCaByUserAndPeriod(gerantUserId, dayStart, dayEnd);
            caTickets = ticketRepository.sumCaByUserAndPeriod(gerantUserId, monthStartI, monthEndI);
        } else {
            caMensualitesJour = mensualiteRepository.sumMontantByStatutAndPeriod(
                    StatutPaiement.PAYE, today, today);
            caMensualites = mensualiteRepository.sumMontantByStatutAndPeriod(
                    StatutPaiement.PAYE, monthStart, monthEnd);
            caTicketsJour = ticketRepository.sumCaByPeriodAndSite(dayStart, dayEnd, siteId);
            caTickets = ticketRepository.sumCaByPeriodAndSite(monthStartI, monthEndI, siteId);
        }

        long caJournalier = caMensualitesJour + caTicketsJour;

        // Membres dont l'abonnement couvre aujourd'hui (période « de date à date »)
        long membresPayesCeMois = mensualiteRepository.countMembresCouverts(
                StatutPaiement.PAYE, today, today);
        double tauxRecouvrement = actifs > 0
                ? Math.round((membresPayesCeMois * 1000.0 / actifs)) / 10.0
                : 0.0;

        return new DashboardStats(
                siteId, siteNom,
                actifs, enRetard, suspendus,
                pointagesJour, ticketsJour,
                caJournalier,
                caMensualites, caTickets, caMensualites + caTickets,
                membresPayesCeMois, tauxRecouvrement
        );
    }
}
