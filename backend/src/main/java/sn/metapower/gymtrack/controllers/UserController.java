package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.ChangementMotDePasseRequest;
import sn.metapower.gymtrack.dto.CurrentUserDTO;
import sn.metapower.gymtrack.dto.GerantSiteDTO;
import sn.metapower.gymtrack.entities.AppRole;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.exceptions.BusinessException;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.GerantSiteRepository;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository userRepository;
    private final GerantSiteRepository gerantSiteRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(AppUserRepository userRepository,
                          GerantSiteRepository gerantSiteRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.gerantSiteRepository = gerantSiteRepository;
        this.passwordEncoder = passwordEncoder;
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

    /**
     * Changement de son propre mot de passe (admin comme gérant).
     * Le mot de passe actuel est exigé : un jeton volé ne suffit pas à prendre le compte.
     */
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerMotDePasse(Authentication authentication,
                                  @Valid @RequestBody ChangementMotDePasseRequest req) {
        AppUser user = userRepository.findByUsername(authentication.getName());
        if (user == null) {
            throw new RessourceNotFoundException("Utilisateur introuvable : " + authentication.getName());
        }
        if (!passwordEncoder.matches(req.motDePasseActuel(), user.getPassword())) {
            throw new BusinessException("Mot de passe actuel incorrect");
        }
        if (passwordEncoder.matches(req.nouveauMotDePasse(), user.getPassword())) {
            throw new BusinessException("Le nouveau mot de passe doit être différent de l'actuel");
        }
        user.setPassword(passwordEncoder.encode(req.nouveauMotDePasse()));
        userRepository.save(user);
    }
}
