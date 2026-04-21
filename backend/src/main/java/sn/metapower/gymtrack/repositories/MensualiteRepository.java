package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.StatutPaiement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MensualiteRepository extends JpaRepository<Mensualite, Long> {
    Optional<Mensualite> findFirstByMembreAndMoisConcerne(Membre membre, LocalDate moisConcerne);
    List<Mensualite> findByMembre(Membre membre);
    List<Mensualite> findByDatePaiementBetween(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mensualite m " +
           "WHERE m.statut = :statut AND m.datePaiement BETWEEN :from AND :to")
    long sumMontantByStatutAndPeriod(@Param("statut") StatutPaiement statut,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /** Nombre de membres distincts ayant payé leur mensualité pour un mois donné. */
    @Query("SELECT COUNT(DISTINCT m.membre.id) FROM Mensualite m " +
           "WHERE m.statut = :statut AND m.moisConcerne = :moisConcerne")
    long countMembresPayesPourMois(@Param("statut") StatutPaiement statut,
                                   @Param("moisConcerne") LocalDate moisConcerne);
}
