package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.PointageMembreDto;
import sn.metapower.gymtrack.dto.ScanRequest;
import sn.metapower.gymtrack.dto.ScanResponse;
import sn.metapower.gymtrack.entities.Pointage;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.PointageRepository;
import sn.metapower.gymtrack.repositories.SiteRepository;
import sn.metapower.gymtrack.services.PointageService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/pointages")
public class PointageController {

    private final PointageService pointageService;
    private final PointageRepository pointageRepository;
    private final SiteRepository siteRepository;

    public PointageController(PointageService pointageService,
                              PointageRepository pointageRepository,
                              SiteRepository siteRepository) {
        this.pointageService = pointageService;
        this.pointageRepository = pointageRepository;
        this.siteRepository = siteRepository;
    }

    /**
     * Endpoint public pour les tablettes — protégé par DeviceAccessKeyFilter (X-Device-Key).
     */
    @PostMapping("/scan")
    public ScanResponse scan(@Valid @RequestBody ScanRequest request) {
        return pointageService.scan(request);
    }

    @GetMapping
    public List<Pointage> findAll() {
        return pointageRepository.findAll();
    }

    /**
     * Historique de pointages d'un membre (trié du plus récent au plus ancien).
     */
    @GetMapping("/membre/{membreId}")
    public List<PointageMembreDto> findByMembre(@PathVariable Long membreId) {
        return pointageRepository.findByMembreIdOrderByHeureEntreeDesc(membreId)
                .stream()
                .map(PointageMembreDto::from)
                .toList();
    }

    /**
     * Pointages d'un site sur la journée courante (pour vue temps-réel).
     */
    @GetMapping("/site/{siteId}/jour")
    public List<Pointage> findOfTheDay(@PathVariable Long siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + siteId));
        Instant from = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return pointageRepository.findBySiteAndHeureEntreeBetween(site, from, to);
    }
}
