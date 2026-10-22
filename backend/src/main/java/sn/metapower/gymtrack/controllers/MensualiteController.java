package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.MensualiteCreateRequest;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.MensualiteRepository;
import sn.metapower.gymtrack.services.MensualiteService;

import java.util.List;

@RestController
@RequestMapping("/api/mensualites")
public class MensualiteController {

    private final MensualiteService mensualiteService;
    private final MensualiteRepository mensualiteRepository;
    private final AppUserRepository userRepository;

    public MensualiteController(MensualiteService mensualiteService,
                                MensualiteRepository mensualiteRepository,
                                AppUserRepository userRepository) {
        this.mensualiteService = mensualiteService;
        this.mensualiteRepository = mensualiteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Liste toutes les mensualités.
     * Pour un ROLE_GERANT : filtrées sur celles qu'il a lui-même enregistrées.
     */
    @GetMapping
    public List<Mensualite> findAll(Authentication authentication) {
        boolean isGerant = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GERANT"));
        if (isGerant) {
            AppUser user = userRepository.findByUsername(authentication.getName());
            return mensualiteRepository.findByEnregistreParId(user.getId());
        }
        return mensualiteRepository.findAll();
    }

    @GetMapping("/membre/{membreId}")
    public List<Mensualite> findByMembre(@PathVariable Long membreId) {
        return mensualiteService.findByMembre(membreId);
    }

    @PostMapping
    public Mensualite enregistrer(@Valid @RequestBody MensualiteCreateRequest request) {
        return mensualiteService.enregistrerPaiement(request);
    }
}
