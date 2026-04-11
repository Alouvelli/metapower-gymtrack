package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.Pointage;
import sn.metapower.gymtrack.entities.Site;

import java.time.Instant;
import java.util.List;

public interface PointageRepository extends JpaRepository<Pointage, Long> {
    List<Pointage> findBySiteAndHeureEntreeBetween(Site site, Instant from, Instant to);
    long countBySiteAndHeureEntreeBetween(Site site, Instant from, Instant to);
    long countByHeureEntreeBetween(Instant from, Instant to);
}
