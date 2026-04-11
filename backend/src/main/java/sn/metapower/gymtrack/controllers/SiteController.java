package sn.metapower.gymtrack.controllers;

import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.SiteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteRepository siteRepository;

    public SiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @GetMapping
    public List<Site> findAll() {
        return siteRepository.findAll();
    }

    @GetMapping("/actifs")
    public List<Site> findActifs() {
        return siteRepository.findByActifTrue();
    }

    @GetMapping("/{id}")
    public Site findById(@PathVariable Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + id));
    }

    @PostMapping
    public Site create(@RequestBody Site site) {
        site.setId(null);
        return siteRepository.save(site);
    }

    @PutMapping("/{id}")
    public Site update(@PathVariable Long id, @RequestBody Site site) {
        Site existing = siteRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + id));
        existing.setNom(site.getNom());
        existing.setAdresse(site.getAdresse());
        existing.setTelephone(site.getTelephone());
        existing.setEmail(site.getEmail());
        existing.setActif(site.isActif());
        return siteRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        siteRepository.deleteById(id);
    }
}
