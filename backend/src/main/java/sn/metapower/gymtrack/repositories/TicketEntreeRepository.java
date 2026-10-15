package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.entities.TicketEntree;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketEntreeRepository extends JpaRepository<TicketEntree, Long> {
    Optional<TicketEntree> findByQrCodeToken(String qrCodeToken);
    long countBySiteAndDatePaiementBetween(Site site, Instant from, Instant to);
    long countByDatePaiementBetween(Instant from, Instant to);
    List<TicketEntree> findBySiteAndDatePaiementBetween(Site site, Instant from, Instant to);
    List<TicketEntree> findByDatePaiementBetween(Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(t.prix), 0) FROM TicketEntree t " +
           "WHERE t.datePaiement BETWEEN :from AND :to " +
           "AND (:siteId IS NULL OR t.site.id = :siteId)")
    long sumCaByPeriodAndSite(@Param("from") Instant from,
                              @Param("to") Instant to,
                              @Param("siteId") Long siteId);

    // ── Requêtes filtrées par gérant (vendeur) ─────────────────────────────────

    List<TicketEntree> findByVendeurId(Long userId);

    @Query("SELECT COALESCE(SUM(t.prix), 0) FROM TicketEntree t " +
           "WHERE t.vendeur.id = :userId " +
           "AND t.datePaiement BETWEEN :from AND :to")
    long sumCaByUserAndPeriod(@Param("userId") Long userId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);

    @Query("SELECT COUNT(t) FROM TicketEntree t " +
           "WHERE t.vendeur.id = :userId " +
           "AND t.datePaiement BETWEEN :from AND :to")
    long countByUserAndPeriod(@Param("userId") Long userId,
                              @Param("from") Instant from,
                              @Param("to") Instant to);
}
