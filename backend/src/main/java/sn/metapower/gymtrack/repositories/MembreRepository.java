package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.StatutMembre;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByQrCodeToken(String qrCodeToken);
    Optional<Membre> findByNumeroCarte(String numeroCarte);
    boolean existsByEmail(String email);
    boolean existsByTelephone(String telephone);
    long countByNumeroCarteStartingWith(String prefix);
    long countByStatut(StatutMembre statut);
}
