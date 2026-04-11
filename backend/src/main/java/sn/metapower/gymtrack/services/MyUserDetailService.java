package sn.metapower.gymtrack.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.repositories.AppUserRepository;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class MyUserDetailService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(MyUserDetailService.class);

    private final AppUserRepository appUserRepository;

    public MyUserDetailService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Authentification demandée pour : {}", username);
        AppUser appUser = appUserRepository.findByUsername(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("Utilisateur introuvable : " + username);
        }
        if (!appUser.isEnabled()) {
            throw new UsernameNotFoundException("Compte désactivé : " + username);
        }
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        appUser.getAppRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority(role.getRoleName())));
        return new User(appUser.getUsername(), appUser.getPassword(), authorities);
    }
}
