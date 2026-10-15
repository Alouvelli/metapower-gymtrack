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

    /** Nombre de membres distincts dont un paiement couvre le mois donné (y compris multi-mois). */
    @Query("SELECT COUNT(DISTINCT m.membre.id) FROM Mensualite m " +
           "WHERE m.statut = :statut AND m.moisConcerne <= :moisConcerne AND m.moisFin >= :moisConcerne")
    long countMembresPayesPourMois(@Param("statut") StatutPaiement statut,
                                   @Param("moisConcerne") LocalDate moisConcerne);

    /** Vrai si un paiement du membre couvre le mois donné. */
    @Query("SELECT COUNT(m) > 0 FROM Mensualite m " +
           "WHERE m.membre = :membre AND m.statut = :statut " +
           "AND m.moisConcerne <= :mois AND m.moisFin >= :mois")
    boolean existsCouvrant(@Param("membre") Membre membre,
                           @Param("statut") StatutPaiement statut,
                           @Param("mois") LocalDate mois);

    /** Paiements du membre qui chevauchent la période [debut, fin]. */
    @Query("SELECT m FROM Mensualite m " +
           "WHERE m.membre = :membre AND m.statut = :statut " +
           "AND m.moisConcerne <= :fin AND m.moisFin >= :debut")
    List<Mensualite> findChevauchant(@Param("membre") Membre membre,
                                     @Param("statut") StatutPaiement statut,
                                     @Param("debut") LocalDate debut,
                                     @Param("fin") LocalDate fin);

    // ── Requêtes filtrées par gérant (enregistrePar) ──────────────────────────

    List<Mensualite> findByEnregistreParId(Long userId);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mensualite m " +
           "WHERE m.enregistrePar.id = :userId " +
           "AND m.statut = :statut " +
           "AND m.datePaiement BETWEEN :from AND :to")
    long sumMontantByUserAndPeriod(@Param("userId") Long userId,
                                   @Param("statut") StatutPaiement statut,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    @Query("SELECT COUNT(m) FROM Mensualite m " +
           "WHERE m.enregistrePar.id = :userId " +
           "AND m.datePaiement BETWEEN :from AND :to")
    long countByUserAndPeriod(@Param("userId") Long userId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
