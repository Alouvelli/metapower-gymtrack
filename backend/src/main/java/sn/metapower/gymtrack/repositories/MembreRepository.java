package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.StatutMembre;

import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {
    Optional<Membre> findByQrCodeToken(String qrCodeToken);
    Optional<Membre> findByNumeroCarte(String numeroCarte);
    boolean existsByEmail(String email);
    boolean existsByTelephone(String telephone);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByTelephoneAndIdNot(String telephone, Long id);
    long countByNumeroCarteStartingWith(String prefix);

    /** Plus grand numéro de la série « MBR-NNNN » (0 si aucun). Ignore les anciens « MBR-2026-NNNN ». */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_carte FROM '^MBR-([0-9]+)$') AS INTEGER)), 0) FROM membre",
           nativeQuery = true)
    int findMaxNumeroCarteSerie();
    long countByStatut(StatutMembre statut);
}
