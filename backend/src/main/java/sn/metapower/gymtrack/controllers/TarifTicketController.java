package sn.metapower.gymtrack.controllers;

import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.entities.TarifTicket;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.TarifTicketRepository;

import java.util.List;

@RestController
@RequestMapping("/api/tarifs-tickets")
public class TarifTicketController {

    private final TarifTicketRepository repository;

    public TarifTicketController(TarifTicketRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TarifTicket> findAll() {
        return repository.findAll();
    }

    @GetMapping("/actifs")
    public List<TarifTicket> findActifs() {
        return repository.findByActifTrue();
    }

    @PostMapping
    public TarifTicket create(@RequestBody TarifTicket tarif) {
        tarif.setId(null);
        return repository.save(tarif);
    }

    @PutMapping("/{id}")
    public TarifTicket update(@PathVariable Long id, @RequestBody TarifTicket tarif) {
        TarifTicket existing = repository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("Tarif introuvable : " + id));
        existing.setLibelle(tarif.getLibelle());
        existing.setPrix(tarif.getPrix());
        existing.setDureeValiditeHeures(tarif.getDureeValiditeHeures());
        existing.setActif(tarif.isActif());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
