package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.PlanAbonnement;

import java.util.List;

public interface PlanAbonnementRepository extends JpaRepository<PlanAbonnement, Long> {
    List<PlanAbonnement> findByActifTrue();
}
