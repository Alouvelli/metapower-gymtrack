package sn.metapower.gymtrack.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.MensualiteCreateRequest;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.entities.StatutPaiement;
import sn.metapower.gymtrack.exceptions.BusinessException;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.MensualiteRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class MensualiteService {

    private final MensualiteRepository mensualiteRepository;
    private final MembreRepository membreRepository;

    public MensualiteService(MensualiteRepository mensualiteRepository,
                             MembreRepository membreRepository) {
        this.mensualiteRepository = mensualiteRepository;
        this.membreRepository = membreRepository;
    }

    @Transactional
    public Mensualite enregistrerPaiement(MensualiteCreateRequest req) {
        Membre membre = membreRepository.findById(req.membreId())
                .orElseThrow(() -> new RessourceNotFoundException("Membre introuvable : " + req.membreId()));

        LocalDate mois = req.moisConcerne().withDayOfMonth(1);

        Mensualite mensualite = mensualiteRepository
                .findFirstByMembreAndMoisConcerne(membre, mois)
                .orElseGet(() -> {
                    Mensualite m = new Mensualite();
                    m.setMembre(membre);
                    m.setMoisConcerne(mois);
                    return m;
                });

        if (mensualite.getStatut() == StatutPaiement.PAYE) {
            throw new BusinessException("Mensualité déjà payée pour ce mois");
        }

        mensualite.setMontant(req.montant());
        mensualite.setModePaiement(req.modePaiement());
        mensualite.setReference(req.reference());
        mensualite.setDatePaiement(req.datePaiement() != null ? req.datePaiement() : LocalDate.now());
        mensualite.setStatut(StatutPaiement.PAYE);

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
        return mensualiteRepository.findFirstByMembreAndMoisConcerne(membre, moisCourant)
                .map(m -> m.getStatut() == StatutPaiement.PAYE)
                .orElse(false);
    }
}
