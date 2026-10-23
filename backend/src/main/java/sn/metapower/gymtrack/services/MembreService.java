package sn.metapower.gymtrack.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.MembreCreateRequest;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.PlanAbonnement;
import sn.metapower.gymtrack.entities.StatutMembre;
import sn.metapower.gymtrack.exceptions.BusinessException;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.PlanAbonnementRepository;

import java.util.UUID;

@Service
public class MembreService {

    private final MembreRepository membreRepository;
    private final PlanAbonnementRepository planRepository;
    private final QrCodeService qrCodeService;

    public MembreService(MembreRepository membreRepository,
                         PlanAbonnementRepository planRepository,
                         QrCodeService qrCodeService) {
        this.membreRepository = membreRepository;
        this.planRepository = planRepository;
        this.qrCodeService = qrCodeService;
    }

    @Transactional
    public Membre create(MembreCreateRequest req) {
        String email = emailOuNull(req.email());
        if (email != null && membreRepository.existsByEmail(email)) {
            throw new BusinessException("Un membre avec cet email existe déjà");
        }
        if (membreRepository.existsByTelephone(req.telephone())) {
            throw new BusinessException("Un membre avec ce numéro de téléphone existe déjà");
        }

        Membre m = new Membre();
        m.setQrCodeToken(UUID.randomUUID().toString());
        m.setStatut(StatutMembre.ACTIF);
        m.setNumeroCarte(generateNumeroCarte());
        appliquer(m, req, email);
        return membreRepository.save(m);
    }

    /** Modification d'un membre (numéro de carte, QR et statut inchangés). */
    @Transactional
    public Membre update(Long id, MembreCreateRequest req) {
        Membre m = findById(id);
        String email = emailOuNull(req.email());
        // Contrôles d'unicité seulement si la valeur change : des membres importés du
        // registre papier partagent un téléphone (famille) et doivent rester modifiables.
        if (email != null && !email.equals(m.getEmail()) && membreRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessException("Un autre membre a déjà cet email");
        }
        String tel = req.telephone().trim();
        if (!tel.equals(m.getTelephone()) && membreRepository.existsByTelephoneAndIdNot(tel, id)) {
            throw new BusinessException("Un autre membre a déjà ce numéro de téléphone");
        }
        appliquer(m, req, email);
        return membreRepository.save(m);
    }

    private void appliquer(Membre m, MembreCreateRequest req, String email) {
        m.setNom(req.nom().trim());
        m.setPrenom(req.prenom().trim());
        m.setTelephone(req.telephone().trim());
        m.setEmail(email);
        m.setDateNaissance(req.dateNaissance());
        m.setAdresse(req.adresse());
        if (req.planId() != null) {
            PlanAbonnement plan = planRepository.findById(req.planId())
                    .orElseThrow(() -> new RessourceNotFoundException("Plan introuvable : " + req.planId()));
            m.setPlan(plan);
        } else {
            m.setPlan(null);
        }
    }

    /**
     * Email vide du formulaire → null. Sinon le contrôle d'unicité comparait des
     * chaînes vides : le 1er membre sans email passait, tous les suivants étaient
     * refusés (« Un membre avec cet email existe déjà »).
     */
    private static String emailOuNull(String email) {
        return (email == null || email.isBlank()) ? null : email.trim().toLowerCase();
    }

    public Membre findById(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Membre introuvable : " + id));
    }

    public byte[] generateQrCodePng(Long membreId) {
        Membre m = findById(membreId);
        return qrCodeService.generatePng(m.getQrCodeToken());
    }

    /**
     * Numéro suivant dans la série du registre papier (MBR-0575, MBR-0797…) :
     * plus grand numéro existant + 1. (L'ancien comptage « MBR-année-count+1 »
     * redonnait un numéro déjà pris dès qu'un membre était supprimé.)
     */
    private String generateNumeroCarte() {
        return String.format("MBR-%04d", membreRepository.findMaxNumeroCarteSerie() + 1);
    }
}
