package sn.metapower.gymtrack.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.metapower.gymtrack.dto.DashboardStats;
import sn.metapower.gymtrack.dto.GerantStatsDTO;
import sn.metapower.gymtrack.entities.AppUser;
import sn.metapower.gymtrack.entities.GerantSite;
import sn.metapower.gymtrack.repositories.AppUserRepository;
import sn.metapower.gymtrack.repositories.GerantSiteRepository;
import sn.metapower.gymtrack.services.DashboardService;
import sn.metapower.gymtrack.services.GerantStatsService;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final GerantStatsService gerantStatsService;
    private final AppUserRepository userRepository;
    private final GerantSiteRepository gerantSiteRepository;

    public DashboardController(DashboardService dashboardService,
                               GerantStatsService gerantStatsService,
                               AppUserRepository userRepository,
                               GerantSiteRepository gerantSiteRepository) {
        this.dashboardService = dashboardService;
        this.gerantStatsService = gerantStatsService;
        this.userRepository = userRepository;
        this.gerantSiteRepository = gerantSiteRepository;
    }

    /**
     * Stats du tableau de bord.
     * - Admin : filtre optionnel par siteId
     * - Gérant : siteId et userId forcés automatiquement sur son affectation
     */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public DashboardStats stats(@RequestParam(required = false) Long siteId,
                                Authentication authentication) {
        boolean isGerant = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GERANT"));

        if (isGerant) {
            AppUser user = userRepository.findByUsername(authentication.getName());
            List<GerantSite> assignments = gerantSiteRepository.findByUserId(user.getId());
            if (!assignments.isEmpty()) {
                GerantSite gs = assignments.get(0);
                return dashboardService.getStats(gs.getSite().getId(), user.getId());
            }
            // Gérant sans affectation : stats vides sur son userId
            return dashboardService.getStats(null, user.getId());
        }

        return dashboardService.getStats(siteId);
    }

    /**
     * Rapport des stats par gérant pour un mois donné (admin uniquement).
     * Param : mois au format YYYY-MM (ex. 2026-05)
     */
    @GetMapping("/gerants/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<GerantStatsDTO> gerantsStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mois) {
        return gerantStatsService.computeStats(mois);
    }
}
