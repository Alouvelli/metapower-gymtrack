package sn.metapower.gymtrack.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.GerantStatsDTO;
import sn.metapower.gymtrack.entities.GerantSite;
import sn.metapower.gymtrack.entities.StatutMembre;
import sn.metapower.gymtrack.entities.StatutPaiement;
import sn.metapower.gymtrack.repositories.GerantSiteRepository;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.MensualiteRepository;
import sn.metapower.gymtrack.repositories.TicketEntreeRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class GerantStatsService {

    private final GerantSiteRepository gerantSiteRepository;
    private final MensualiteRepository mensualiteRepository;
    private final TicketEntreeRepository ticketRepository;
    private final MembreRepository membreRepository;

    public GerantStatsService(GerantSiteRepository gerantSiteRepository,
                               MensualiteRepository mensualiteRepository,
                               TicketEntreeRepository ticketRepository,
                               MembreRepository membreRepository) {
        this.gerantSiteRepository = gerantSiteRepository;
        this.mensualiteRepository = mensualiteRepository;
        this.ticketRepository = ticketRepository;
        this.membreRepository = membreRepository;
    }

    /**
     * Calcule les stats de chaque gérant actif pour le mois donné.
     * Utilisé exclusivement par le super admin.
     */
    @Transactional(readOnly = true)
    public List<GerantStatsDTO> computeStats(YearMonth mois) {
        ZoneId zone = ZoneId.systemDefault();

        LocalDate from     = mois.atDay(1);
        LocalDate to       = mois.atEndOfMonth();
        Instant   fromInst = from.atStartOfDay(zone).toInstant();
        Instant   toInst   = to.plusDays(1).atStartOfDay(zone).toInstant();

        long membresActifsGlobal = membreRepository.countByStatut(StatutMembre.ACTIF);
        long membresPayesCeMois  = mensualiteRepository.countMembresCouverts(
                StatutPaiement.PAYE, from, to);

        List<GerantSite> gerants = gerantSiteRepository.findByActifTrue();

        return gerants.stream().map(gs -> {
            Long userId = gs.getUser().getId();

            long nbMens   = mensualiteRepository.countByUserAndPeriod(userId, from, to);
            long caMens   = mensualiteRepository.sumMontantByUserAndPeriod(userId, StatutPaiement.PAYE, from, to);
            long nbTkts   = ticketRepository.countByUserAndPeriod(userId, fromInst, toInst);
            long caTkts   = ticketRepository.sumCaByUserAndPeriod(userId, fromInst, toInst);
            long caTotal  = caMens + caTkts;

            double taux = membresActifsGlobal > 0
                    ? Math.round((membresPayesCeMois * 1000.0 / membresActifsGlobal)) / 10.0
                    : 0.0;

            return new GerantStatsDTO(
                    gs.getId(),
                    gs.getUser().getNom(),
                    gs.getUser().getPrenom(),
                    gs.getUser().getUsername(),
                    gs.getSite().getId(),
                    gs.getSite().getNom(),
                    gs.getHeureDebut(),
                    gs.getHeureFin(),
                    nbMens, caMens,
                    nbTkts, caTkts, caTotal,
                    membresActifsGlobal,
                    membresPayesCeMois,
                    taux
            );
        }).toList();
    }
}
