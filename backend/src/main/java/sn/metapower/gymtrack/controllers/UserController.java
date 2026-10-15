package sn.metapower.gymtrack.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.metapower.gymtrack.dto.CurrentUserDTO;
import sn.metapower.gymtrack.dto.GerantSiteDTO;
import sn.metapower.gymtrack.entities.AppRole;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.GerantSiteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository userRepository;
    private final GerantSiteRepository gerantSiteRepository;

    public UserController(AppUserRepository userRepository,
                          GerantSiteRepository gerantSiteRepository) {
        this.userRepository = userRepository;
        this.gerantSiteRepository = gerantSiteRepository;
    }

    /**
     * Retourne le profil complet de l'utilisateur authentifié :
     * rôles + affectations de salle (pour les gérants).
     * Sécurisé par la règle globale anyRequest().authenticated().
     */
    @GetMapping("/me")
    public CurrentUserDTO me(Authentication authentication) {
        String username = authentication.getName();
        AppUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new RessourceNotFoundException("Utilisateur introuvable : " + username);
        }

        List<String> roles = user.getAppRoles().stream()
                .map(AppRole::getRoleName)
                .toList();

        List<GerantSiteDTO> assignments = gerantSiteRepository
                .findByUserId(user.getId())
                .stream()
                .filter(gs -> gs.isActif())
                .map(GerantSiteDTO::from)
                .toList();

        return new CurrentUserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                roles,
                assignments
        );
    }
}
