package sn.metapower.gymtrack.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.metapower.gymtrack.repositories.SiteRepository;

import java.util.List;

/**
 * Liste publique des salles actives, pour l'écran de configuration du scanner
 * tablette (qui n'a pas de session utilisateur, seulement la clé device).
 * Seuls l'id et le nom sont exposés.
 */
@RestController
@RequestMapping("/api/public/sites")
public class PublicSiteController {

    private final SiteRepository siteRepository;

    public PublicSiteController(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public record SitePublic(Long id, String nom) {}

    @GetMapping
    public List<SitePublic> sitesActifs() {
        return siteRepository.findByActifTrue().stream()
                .map(s -> new SitePublic(s.getId(), s.getNom()))
                .toList();
    }
}
