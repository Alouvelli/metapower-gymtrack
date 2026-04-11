package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.MensualiteCreateRequest;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.repositories.MensualiteRepository;
import sn.metapower.gymtrack.services.MensualiteService;

import java.util.List;

@RestController
@RequestMapping("/api/mensualites")
public class MensualiteController {

    private final MensualiteService mensualiteService;
    private final MensualiteRepository mensualiteRepository;

    public MensualiteController(MensualiteService mensualiteService,
                                MensualiteRepository mensualiteRepository) {
        this.mensualiteService = mensualiteService;
        this.mensualiteRepository = mensualiteRepository;
    }

    @GetMapping
    public List<Mensualite> findAll() {
        return mensualiteRepository.findAll();
    }

    @GetMapping("/membre/{membreId}")
    public List<Mensualite> findByMembre(@PathVariable Long membreId) {
        return mensualiteService.findByMembre(membreId);
    }

    @PostMapping
    public Mensualite enregistrer(@Valid @RequestBody MensualiteCreateRequest request) {
        return mensualiteService.enregistrerPaiement(request);
    }
}
