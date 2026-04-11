package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.MembreCreateRequest;
import sn.metapower.gymtrack.dto.MembreResponse;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.services.MembreService;

import java.util.List;

@RestController
@RequestMapping("/api/membres")
public class MembreController {

    private final MembreService membreService;
    private final MembreRepository membreRepository;

    public MembreController(MembreService membreService, MembreRepository membreRepository) {
        this.membreService = membreService;
        this.membreRepository = membreRepository;
    }

    @GetMapping
    public List<MembreResponse> findAll() {
        return membreRepository.findAll().stream().map(MembreResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MembreResponse findById(@PathVariable Long id) {
        return MembreResponse.from(membreService.findById(id));
    }

    @PostMapping
    public ResponseEntity<MembreResponse> create(@Valid @RequestBody MembreCreateRequest request) {
        Membre created = membreService.create(request);
        return ResponseEntity.ok(MembreResponse.from(created));
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrCode(@PathVariable Long id) {
        return membreService.generateQrCodePng(id);
    }
}
