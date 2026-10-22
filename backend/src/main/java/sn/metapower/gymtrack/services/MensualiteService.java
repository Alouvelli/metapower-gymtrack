package sn.metapower.gymtrack.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.MensualiteCreateRequest;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.entities.StatutPaiement;
import sn.metapower.gymtrack.exceptions.BusinessException;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.MensualiteRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MensualiteService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MensualiteRepository mensualiteRepository;
    private final MembreRepository membreRepository;
    private final AppUserRepository userRepository;

    public MensualiteService(MensualiteRepository mensualiteRepository,
                             MembreRepository membreRepository,
                             AppUserRepository userRepository) {
        this.mensualiteRepository = mensualiteRepository;
        this.membreRepository = membreRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Mensualite enregistrerPaiement(MensualiteCreateRequest req) {
        Membre membre = membreRepository.findById(req.membreId())
                .orElseThrow(() -> new RessourceNotFoundException("Membre introuvable : " + req.membreId()));

        LocalDate datePaiement = req.datePaiement() != null ? req.datePaiement() : LocalDate.now();

        // Un plan Trimestriel / Semestriel / Annuel couvre plusieurs mois d'un coup
        int nbMois = (membre.getPlan() != null && membre.getPlan().getDureeEnMois() != null)
                ? Math.max(1, membre.getPlan().getDureeEnMois())
                : 1;

        // Période « de date à date » : démarre au paiement, ou au lendemain de la fin de
        // l'abonnement en cours (renouvellement anticipé : aucun jour perdu).
        LocalDate debut;
        if (req.dateDebut() != null) {
            debut = req.dateDebut();
        } else {
            LocalDate lendemainFin = mensualiteRepository.findDerniereFin(membre, StatutPaiement.PAYE)
                    .map(fin -> fin.plusDays(1))
                    .orElse(datePaiement);
            debut = lendemainFin.isAfter(datePaiement) ? lendemainFin : datePaiement;
        }
        LocalDate fin = debut.plusMonths(nbMois).minusDays(1);   // payé le 29/09 → 28/10

        if (!mensualiteRepository.findChevauchant(membre, StatutPaiement.PAYE, debut, fin).isEmpty()) {
            throw new BusinessException("La période du " + DATE_FR.format(debut) + " au "
                    + DATE_FR.format(fin) + " est déjà (en partie) payée");
        }

        Mensualite mensualite = new Mensualite();
        mensualite.setMembre(membre);
        mensualite.setDateDebut(debut);
        mensualite.setDateFin(fin);
        mensualite.setMoisConcerne(debut.withDayOfMonth(1));
        mensualite.setMoisFin(fin.withDayOfMonth(1));
        mensualite.setNbMois(nbMois);
        mensualite.setMontant(req.montant());
        mensualite.setModePaiement(req.modePaiement());
        mensualite.setReference(req.reference());
        mensualite.setDatePaiement(datePaiement);
        mensualite.setStatut(StatutPaiement.PAYE);

        // Enregistre qui a saisi ce paiement (même pattern que TicketService.vendre)
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            AppUser recorder = userRepository.findByUsername(username);
            mensualite.setEnregistrePar(recorder);
        } catch (Exception ignored) {
            // Pas de contexte d'auth (ex. tests), on continue sans enregistrePar
        }

        return mensualiteRepository.save(mensualite);
    }

    public List<Mensualite> findByMembre(Long membreId) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new RessourceNotFoundException("Membre introuvable : " + membreId));
        return mensualiteRepository.findByMembre(membre);
    }

    /**
     * Vrai si un paiement du membre couvre aujourd'hui. Utilisé par PointageService.
     */
    public boolean estAJour(Membre membre) {
        return mensualiteRepository.existsCouvrant(membre, StatutPaiement.PAYE, LocalDate.now());
    }
}
