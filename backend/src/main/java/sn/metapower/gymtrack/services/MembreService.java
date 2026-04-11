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

import java.time.Year;
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
        if (req.email() != null && membreRepository.existsByEmail(req.email())) {
            throw new BusinessException("Un membre avec cet email existe déjà");
        }
        if (membreRepository.existsByTelephone(req.telephone())) {
            throw new BusinessException("Un membre avec ce numéro de téléphone existe déjà");
        }

        Membre m = new Membre();
        m.setNom(req.nom());
        m.setPrenom(req.prenom());
        m.setTelephone(req.telephone());
        m.setEmail(req.email());
        m.setDateNaissance(req.dateNaissance());
        m.setAdresse(req.adresse());
        m.setQrCodeToken(UUID.randomUUID().toString());
        m.setStatut(StatutMembre.ACTIF);
        m.setNumeroCarte(generateNumeroCarte());

        if (req.planId() != null) {
            PlanAbonnement plan = planRepository.findById(req.planId())
                    .orElseThrow(() -> new RessourceNotFoundException("Plan introuvable : " + req.planId()));
            m.setPlan(plan);
        }
        return membreRepository.save(m);
    }

    public Membre findById(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Membre introuvable : " + id));
    }

    public byte[] generateQrCodePng(Long membreId) {
        Membre m = findById(membreId);
        return qrCodeService.generatePng(m.getQrCodeToken());
    }

    private String generateNumeroCarte() {
        int year = Year.now().getValue();
        String prefix = "MBR-" + year + "-";
        long count = membreRepository.countByNumeroCarteStartingWith(prefix) + 1;
        return prefix + String.format("%04d", count);
    }
}
