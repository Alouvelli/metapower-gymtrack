package sn.metapower.gymtrack.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.metapower.gymtrack.dto.GerantCreateDTO;
import sn.metapower.gymtrack.dto.GerantSiteDTO;
import sn.metapower.gymtrack.entities.AppRole;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.entities.GerantSite;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.exceptions.BusinessException;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.AppRoleRepository;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.GerantSiteRepository;
import sn.metapower.gymtrack.repositories.SiteRepository;

import java.time.LocalTime;
import java.util.List;

@Service
public class GerantService {

    private final GerantSiteRepository gerantSiteRepository;
    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;

    public GerantService(GerantSiteRepository gerantSiteRepository,
                         AppUserRepository userRepository,
                         AppRoleRepository roleRepository,
                         SiteRepository siteRepository,
                         PasswordEncoder passwordEncoder) {
        this.gerantSiteRepository = gerantSiteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Crée un compte gérant ET l'affecte à un site avec un créneau horaire. */
    @Transactional
    public GerantSiteDTO createGerantAccount(GerantCreateDTO dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new BusinessException("Nom d'utilisateur déjà utilisé : " + dto.username());
        }
        if (dto.email() != null && userRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Email déjà utilisé : " + dto.email());
        }

        Site site = siteRepository.findById(dto.siteId())
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + dto.siteId()));

        AppRole roleGerant = roleRepository.findByRoleName("ROLE_GERANT")
                .orElseThrow(() -> new RessourceNotFoundException("Rôle ROLE_GERANT introuvable"));

        // Création du compte utilisateur
        AppUser user = new AppUser();
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setEmail(dto.email());
        user.setNom(dto.nom());
        user.setPrenom(dto.prenom());
        user.setEnabled(true);
        user.getAppRoles().add(roleGerant);
        user = userRepository.save(user);

        // Affectation au site avec créneau
        GerantSite gs = new GerantSite();
        gs.setUser(user);
        gs.setSite(site);
        gs.setHeureDebut(dto.heureDebut());
        gs.setHeureFin(dto.heureFin());
        gs.setActif(true);
        gs = gerantSiteRepository.save(gs);

        return GerantSiteDTO.from(gs);
    }

    @Transactional(readOnly = true)
    public List<GerantSiteDTO> listGerants() {
        return gerantSiteRepository.findAll()
                .stream().map(GerantSiteDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<GerantSiteDTO> listGerantsBySite(Long siteId) {
        return gerantSiteRepository.findBySiteId(siteId)
                .stream().map(GerantSiteDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public GerantSiteDTO getGerant(Long id) {
        GerantSite gs = gerantSiteRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Gérant introuvable : " + id));
        return GerantSiteDTO.from(gs);
    }

    @Transactional
    public GerantSiteDTO updateSchedule(Long id, LocalTime heureDebut, LocalTime heureFin) {
        GerantSite gs = gerantSiteRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Gérant introuvable : " + id));
        gs.setHeureDebut(heureDebut);
        gs.setHeureFin(heureFin);
        return GerantSiteDTO.from(gerantSiteRepository.save(gs));
    }

    @Transactional
    public void deactivate(Long id) {
        GerantSite gs = gerantSiteRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Gérant introuvable : " + id));
        gs.setActif(false);
        gs.getUser().setEnabled(false);
        gerantSiteRepository.save(gs);
        userRepository.save(gs.getUser());
    }
}
