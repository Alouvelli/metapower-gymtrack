package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.Site;

import java.util.List;

public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByActifTrue();
}
