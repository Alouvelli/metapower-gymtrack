package sn.metapower.gymtrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sn.metapower.gymtrack.entities.AppRole;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.repositories.AppRoleRepository;
import sn.metapower.gymtrack.repositories.AppUserRepository;

import java.util.List;

/**
 * Initialise les rôles par défaut et un compte admin si la base est vide.
 * Mot de passe admin pris dans {@code app.admin-default-password} (env var).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppRoleRepository roleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-default-password}")
    private String adminDefaultPassword;

    public DataInitializer(AppRoleRepository roleRepository,
                           AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List.of("ROLE_ADMIN", "ROLE_RECEPTIONNISTE", "ROLE_CAISSIER", "ROLE_GERANT").forEach(name ->
                roleRepository.findByRoleName(name)
                        .orElseGet(() -> roleRepository.save(new AppRole(name))));

        if (!userRepository.existsByUsername("admin")) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setEmail("admin@metapower.sn");
            admin.setNom("Admin");
            admin.setPrenom("METAPOWER");
            admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setEnabled(true);
            admin.getAppRoles().add(roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow());
            userRepository.save(admin);
            log.info("[DataInitializer] Compte admin créé — username=admin, password={}", adminDefaultPassword);
        }
    }
}
