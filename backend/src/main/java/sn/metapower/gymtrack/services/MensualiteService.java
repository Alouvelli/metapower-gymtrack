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
import java.util.List;

@Service
public class MensualiteService {

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

        LocalDate mois = req.moisConcerne().withDayOfMonth(1);

        // Un plan Trimestriel / Semestriel / Annuel couvre plusieurs mois d'un coup
        int nbMois = (membre.getPlan() != null && membre.getPlan().getDureeEnMois() != null)
                ? Math.max(1, membre.getPlan().getDureeEnMois())
                : 1;
        LocalDate moisFin = mois.plusMonths(nbMois - 1);

        if (!mensualiteRepository.findChevauchant(membre, StatutPaiement.PAYE, mois, moisFin).isEmpty()) {
            throw new BusinessException(nbMois == 1
                    ? "Mensualité déjà payée pour ce mois"
                    : "Une partie de la période " + mois + " → " + moisFin + " est déjà payée");
        }

        Mensualite mensualite = mensualiteRepository
                .findFirstByMembreAndMoisConcerne(membre, mois)
                .orElseGet(() -> {
                    Mensualite m = new Mensualite();
                    m.setMembre(membre);
                    m.setMoisConcerne(mois);
                    return m;
                });

        mensualite.setNbMois(nbMois);
        mensualite.setMoisFin(moisFin);
        mensualite.setMontant(req.montant());
        mensualite.setModePaiement(req.modePaiement());
        mensualite.setReference(req.reference());
        mensualite.setDatePaiement(req.datePaiement() != null ? req.datePaiement() : LocalDate.now());
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
     * Vérifie si un membre est à jour de sa mensualité pour le mois en cours.
     * Utilisé par PointageService.
     */
    public boolean estAJour(Membre membre) {
        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        return mensualiteRepository.existsCouvrant(membre, StatutPaiement.PAYE, moisCourant);
    }
}
