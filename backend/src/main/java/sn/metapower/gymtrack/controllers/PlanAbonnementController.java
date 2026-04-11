package sn.metapower.gymtrack.controllers;

import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.entities.PlanAbonnement;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.PlanAbonnementRepository;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanAbonnementController {

    private final PlanAbonnementRepository planRepository;

    public PlanAbonnementController(PlanAbonnementRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<PlanAbonnement> findAll() {
        return planRepository.findAll();
    }

    @GetMapping("/actifs")
    public List<PlanAbonnement> findActifs() {
        return planRepository.findByActifTrue();
    }

    @GetMapping("/{id}")
    public PlanAbonnement findById(@PathVariable Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Plan introuvable : " + id));
    }

    @PostMapping
    public PlanAbonnement create(@RequestBody PlanAbonnement plan) {
        plan.setId(null);
        return planRepository.save(plan);
    }

    @PutMapping("/{id}")
    public PlanAbonnement update(@PathVariable Long id, @RequestBody PlanAbonnement plan) {
        PlanAbonnement existing = planRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Plan introuvable : " + id));
        existing.setNom(plan.getNom());
        existing.setDureeEnMois(plan.getDureeEnMois());
        existing.setPrix(plan.getPrix());
        existing.setDescription(plan.getDescription());
        existing.setActif(plan.isActif());
        return planRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        planRepository.deleteById(id);
    }
}
