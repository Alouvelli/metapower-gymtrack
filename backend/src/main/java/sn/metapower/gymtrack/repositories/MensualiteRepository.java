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
    /** Paiements de la période hors annulés (rapports PDF / Excel). */
    List<Mensualite> findByDatePaiementBetweenAndStatutNot(LocalDate from, LocalDate to, StatutPaiement statut);

    @Query("SELECT COALESCE(SUM(m.montant), 0) FROM Mensualite m " +
           "WHERE m.statut = :statut AND m.datePaiement BETWEEN :from AND :to")
    long sumMontantByStatutAndPeriod(@Param("statut") StatutPaiement statut,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /** Nombre de membres distincts dont un paiement couvre au moins un jour de [debut, fin]. */
    @Query("SELECT COUNT(DISTINCT m.membre.id) FROM Mensualite m " +
           "WHERE m.statut = :statut AND m.dateDebut <= :fin AND m.dateFin >= :debut")
    long countMembresCouverts(@Param("statut") StatutPaiement statut,
                              @Param("debut") LocalDate debut,
                              @Param("fin") LocalDate fin);

    /** Vrai si un paiement du membre couvre le jour donné. */
    @Query("SELECT COUNT(m) > 0 FROM Mensualite m " +
           "WHERE m.membre = :membre AND m.statut = :statut " +
           "AND m.dateDebut <= :jour AND m.dateFin >= :jour")
    boolean existsCouvrant(@Param("membre") Membre membre,
                           @Param("statut") StatutPaiement statut,
                           @Param("jour") LocalDate jour);

    /** Paiements du membre qui chevauchent la période [debut, fin]. */
    @Query("SELECT m FROM Mensualite m " +
           "WHERE m.membre = :membre AND m.statut = :statut " +
           "AND m.dateDebut <= :fin AND m.dateFin >= :debut")
    List<Mensualite> findChevauchant(@Param("membre") Membre membre,
                                     @Param("statut") StatutPaiement statut,
                                     @Param("debut") LocalDate debut,
                                     @Param("fin") LocalDate fin);

    /** Dernier jour couvert par les paiements du membre (vide s'il n'a jamais payé). */
    @Query("SELECT MAX(m.dateFin) FROM Mensualite m WHERE m.membre = :membre AND m.statut = :statut")
    Optional<LocalDate> findDerniereFin(@Param("membre") Membre membre,
                                        @Param("statut") StatutPaiement statut);

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
           "AND m.statut <> sn.metapower.gymtrack.entities.StatutPaiement.ANNULE " +
           "AND m.datePaiement BETWEEN :from AND :to")
    long countByUserAndPeriod(@Param("userId") Long userId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
