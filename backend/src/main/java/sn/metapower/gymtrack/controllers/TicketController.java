package sn.metapower.gymtrack.controllers;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sn.metapower.gymtrack.dto.TicketVenteRequest;
import sn.metapower.gymtrack.dto.TicketVenteResponse;
import sn.metapower.gymtrack.entities.TicketEntree;
import sn.metapower.gymtrack.repositories.TicketEntreeRepository;
import sn.metapower.gymtrack.services.QrCodeService;
import sn.metapower.gymtrack.services.RecuPdfService;
import sn.metapower.gymtrack.services.TicketService;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketEntreeRepository ticketRepository;
    private final RecuPdfService recuPdfService;
    private final QrCodeService qrCodeService;

    public TicketController(TicketService ticketService,
                            TicketEntreeRepository ticketRepository,
                            RecuPdfService recuPdfService,
                            QrCodeService qrCodeService) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.recuPdfService = recuPdfService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    public List<TicketEntree> findAll() {
        return ticketRepository.findAll();
    }

    @GetMapping("/{id}")
    public TicketEntree findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PostMapping
    public TicketVenteResponse vendre(@Valid @RequestBody TicketVenteRequest request) {
        TicketEntree ticket = ticketService.vendre(request);
        String qrBase64 = qrCodeService.generatePngBase64(ticket.getQrCodeToken());
        return TicketVenteResponse.from(ticket, qrBase64);
    }

    @GetMapping(value = "/{id}/recu", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> recu(@PathVariable Long id) {
        TicketEntree ticket = ticketService.findById(id);
        byte[] pdf = recuPdfService.genererRecu(ticket);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"recu-" + ticket.getNumero() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qrCode(@PathVariable Long id) {
        TicketEntree ticket = ticketService.findById(id);
        return qrCodeService.generatePng(ticket.getQrCodeToken());
    }
}
