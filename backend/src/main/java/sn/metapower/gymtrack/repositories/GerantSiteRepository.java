package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.GerantSite;

import java.util.List;
import java.util.Optional;

public interface GerantSiteRepository extends JpaRepository<GerantSite, Long> {

    List<GerantSite> findByUserId(Long userId);

    List<GerantSite> findBySiteId(Long siteId);

    Optional<GerantSite> findByUserIdAndSiteId(Long userId, Long siteId);

    List<GerantSite> findByActifTrue();

    List<GerantSite> findBySiteIdAndActifTrue(Long siteId);
}
