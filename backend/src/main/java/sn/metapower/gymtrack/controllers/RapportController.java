package sn.metapower.gymtrack.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.metapower.gymtrack.services.RapportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports")
public class RapportController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @GetMapping(value = "/mensualites/excel", produces = XLSX)
    public ResponseEntity<byte[]> mensualitesExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mois) {
        byte[] data = rapportService.mensualitesExcel(mois);
        return download(data, "mensualites-" + mois + ".xlsx", XLSX);
    }

    @GetMapping(value = "/mensualites/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> mensualitesPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mois) {
        byte[] data = rapportService.mensualitesPdf(mois);
        return download(data, "mensualites-" + mois + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping(value = "/tickets/excel", produces = XLSX)
    public ResponseEntity<byte[]> ticketsExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mois,
            @RequestParam(required = false) Long siteId) {
        byte[] data = rapportService.ticketsExcel(mois, siteId);
        String name = "tickets-" + mois + (siteId != null ? "-site" + siteId : "") + ".xlsx";
        return download(data, name, XLSX);
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }
}
