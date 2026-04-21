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

    public DashboardStats getStats(Long siteId) {
        Site site = null;
        String siteNom = "Toutes les salles";
        if (siteId != null) {
            site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + siteId));
            siteNom = site.getNom();
        }

        // Bornes "aujourd'hui" et "ce mois-ci"
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now();
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        Instant monthStartI = monthStart.atStartOfDay(zone).toInstant();
        Instant monthEndI = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        // Membres : agrégats globaux (les membres ne sont pas rattachés à un site)
        long actifs = membreRepository.countByStatut(StatutMembre.ACTIF);
        long enRetard = membreRepository.countByStatut(StatutMembre.EXPIRE);
        long suspendus = membreRepository.countByStatut(StatutMembre.SUSPENDU);

        // Activité du jour
        long pointagesJour = (site != null)
                ? pointageRepository.countBySiteAndHeureEntreeBetween(site, dayStart, dayEnd)
                : pointageRepository.countByHeureEntreeBetween(dayStart, dayEnd);
        long ticketsJour = (site != null)
                ? ticketRepository.countBySiteAndDatePaiementBetween(site, dayStart, dayEnd)
                : ticketRepository.countByDatePaiementBetween(dayStart, dayEnd);

        // CA du jour
        long caMensualitesJour = mensualiteRepository.sumMontantByStatutAndPeriod(
                StatutPaiement.PAYE, today, today);
        long caTicketsJour = ticketRepository.sumCaByPeriodAndSite(dayStart, dayEnd, siteId);
        long caJournalier = caMensualitesJour + caTicketsJour;

        // CA du mois — mensualités globales (pas de notion de site sur Mensualite)
        long caMensualites = mensualiteRepository.sumMontantByStatutAndPeriod(
                StatutPaiement.PAYE, monthStart, monthEnd);
        long caTickets = ticketRepository.sumCaByPeriodAndSite(monthStartI, monthEndI, siteId);

        // Taux de recouvrement mensuel
        LocalDate premierDuMois = today.withDayOfMonth(1);
        long membresPayesCeMois = mensualiteRepository.countMembresPayesPourMois(
                StatutPaiement.PAYE, premierDuMois);
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
