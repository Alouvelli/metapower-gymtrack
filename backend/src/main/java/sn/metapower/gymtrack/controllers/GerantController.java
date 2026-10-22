package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.GerantCreateDTO;
import sn.metapower.gymtrack.dto.GerantSiteDTO;
import sn.metapower.gymtrack.services.GerantService;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gerants")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class GerantController {

    private final GerantService gerantService;

    public GerantController(GerantService gerantService) {
        this.gerantService = gerantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GerantSiteDTO create(@Valid @RequestBody GerantCreateDTO dto) {
        return gerantService.createGerantAccount(dto);
    }

    @GetMapping
    public List<GerantSiteDTO> list() {
        return gerantService.listGerants();
    }

    @GetMapping("/{id}")
    public GerantSiteDTO get(@PathVariable Long id) {
        return gerantService.getGerant(id);
    }

    @GetMapping("/site/{siteId}")
    public List<GerantSiteDTO> listBySite(@PathVariable Long siteId) {
        return gerantService.listGerantsBySite(siteId);
    }

    @PutMapping("/{id}/horaire")
    public GerantSiteDTO updateSchedule(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        LocalTime debut = LocalTime.parse(body.get("heureDebut"));
        LocalTime fin   = LocalTime.parse(body.get("heureFin"));
        return gerantService.updateSchedule(id, debut, fin);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        gerantService.deactivate(id);
    }
}
