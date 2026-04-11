package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.AppUser;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    AppUser findByUsername(String username);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByResetPasswordToken(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
