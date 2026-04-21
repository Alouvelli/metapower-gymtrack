package sn.metapower.gymtrack.services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import sn.metapower.gymtrack.entities.Mensualite;
import sn.metapower.gymtrack.entities.Site;
import sn.metapower.gymtrack.entities.TicketEntree;
import sn.metapower.gymtrack.exceptions.RessourceNotFoundException;
import sn.metapower.gymtrack.repositories.MensualiteRepository;
import sn.metapower.gymtrack.repositories.SiteRepository;
import sn.metapower.gymtrack.repositories.TicketEntreeRepository;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RapportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final MensualiteRepository mensualiteRepository;
    private final TicketEntreeRepository ticketRepository;
    private final SiteRepository siteRepository;

    public RapportService(MensualiteRepository mensualiteRepository,
                          TicketEntreeRepository ticketRepository,
                          SiteRepository siteRepository) {
        this.mensualiteRepository = mensualiteRepository;
        this.ticketRepository = ticketRepository;
        this.siteRepository = siteRepository;
    }

    // ─── Mensualités du mois (Excel) ──────────────────────────
    public byte[] mensualitesExcel(LocalDate mois) {
        LocalDate from = mois.withDayOfMonth(1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        List<Mensualite> data = mensualiteRepository.findByDatePaiementBetween(from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Mensualités " + from);
            CellStyle headerStyle = headerStyle(wb);

            String[] headers = {"N° Carte", "Nom", "Prénom", "Téléphone", "Mois",
                    "Montant", "Date paiement", "Mode", "Référence"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            long total = 0;
            for (Mensualite m : data) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(m.getMembre().getNumeroCarte());
                r.createCell(1).setCellValue(m.getMembre().getNom());
                r.createCell(2).setCellValue(m.getMembre().getPrenom());
                r.createCell(3).setCellValue(m.getMembre().getTelephone());
                r.createCell(4).setCellValue(DATE_FMT.format(m.getMoisConcerne()));
                r.createCell(5).setCellValue(m.getMontant());
                r.createCell(6).setCellValue(m.getDatePaiement() != null ? DATE_FMT.format(m.getDatePaiement()) : "");
                r.createCell(7).setCellValue(m.getModePaiement() != null ? m.getModePaiement().name() : "");
                r.createCell(8).setCellValue(m.getReference() != null ? m.getReference() : "");
                total += m.getMontant();
            }

            Row totalRow = sheet.createRow(rowIdx + 1);
            Cell tl = totalRow.createCell(4);
            tl.setCellValue("TOTAL");
            tl.setCellStyle(headerStyle);
            Cell tv = totalRow.createCell(5);
            tv.setCellValue(total);
            tv.setCellStyle(headerStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération Excel mensualités", e);
        }
    }

    // ─── Tickets vendus (Excel, filtré par site optionnel) ────
    public byte[] ticketsExcel(LocalDate mois, Long siteId) {
        Site site = null;
        if (siteId != null) {
            site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new RessourceNotFoundException("Site introuvable : " + siteId));
        }
        ZoneId z = ZoneId.systemDefault();
        Instant from = mois.withDayOfMonth(1).atStartOfDay(z).toInstant();
        Instant to = mois.withDayOfMonth(1).plusMonths(1).atStartOfDay(z).toInstant();

        List<TicketEntree> data = (site != null)
                ? ticketRepository.findBySiteAndDatePaiementBetween(site, from, to)
                : ticketRepository.findByDatePaiementBetween(from, to);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Tickets");
            CellStyle headerStyle = headerStyle(wb);

            String[] headers = {"Numéro", "Site", "Date vente", "Tarif", "Prix",
                    "Mode", "Client", "Téléphone", "Statut"};
            Row hr = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hr.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            long total = 0;
            for (TicketEntree t : data) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(t.getNumero());
                r.createCell(1).setCellValue(t.getSite().getNom());
                r.createCell(2).setCellValue(DATETIME_FMT.format(t.getDatePaiement()));
                r.createCell(3).setCellValue(t.getTarif() != null ? t.getTarif().getLibelle() : "");
                r.createCell(4).setCellValue(t.getPrix());
                r.createCell(5).setCellValue(t.getModePaiement().name());
                r.createCell(6).setCellValue(t.getNomClient() != null ? t.getNomClient() : "");
                r.createCell(7).setCellValue(t.getTelephoneClient() != null ? t.getTelephoneClient() : "");
                r.createCell(8).setCellValue(t.getStatut().name());
                total += t.getPrix();
            }

            Row totalRow = sheet.createRow(rowIdx + 1);
            Cell tl = totalRow.createCell(3);
            tl.setCellValue("TOTAL CA TICKETS");
            tl.setCellStyle(headerStyle);
            Cell tv = totalRow.createCell(4);
            tv.setCellValue(total);
            tv.setCellStyle(headerStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération Excel tickets", e);
        }
    }

    // ─── Mensualités du mois (PDF) ────────────────────────────
    public byte[] mensualitesPdf(LocalDate mois) {
        LocalDate from = mois.withDayOfMonth(1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        List<Mensualite> data = mensualiteRepository.findByDatePaiementBetween(from, to);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(31, 56, 100));
            Paragraph title = new Paragraph("METAPOWER GymTrack — Rapport mensualités " + from, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 2, 2, 2, 2});
            table.setWidthPercentage(100);
            String[] headers = {"N° Carte", "Membre", "Téléphone", "Mois", "Montant", "Mode", "Date"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
                cell.setBackgroundColor(new Color(31, 56, 100));
                cell.setPadding(6);
                table.addCell(cell);
            }
            long total = 0;
            for (Mensualite m : data) {
                table.addCell(m.getMembre().getNumeroCarte());
                table.addCell(m.getMembre().getNom() + " " + m.getMembre().getPrenom());
                table.addCell(m.getMembre().getTelephone());
                table.addCell(DATE_FMT.format(m.getMoisConcerne()));
                table.addCell(String.format("%,d FCFA", m.getMontant()).replace(',', ' '));
                table.addCell(m.getModePaiement() != null ? m.getModePaiement().name() : "—");
                table.addCell(m.getDatePaiement() != null ? DATE_FMT.format(m.getDatePaiement()) : "—");
                total += m.getMontant();
            }
            doc.add(table);

            doc.add(new Paragraph(" "));
            Paragraph totalP = new Paragraph(
                    "TOTAL : " + String.format("%,d FCFA", total).replace(',', ' '),
                    new Font(Font.HELVETICA, 12, Font.BOLD));
            totalP.setAlignment(Element.ALIGN_RIGHT);
            doc.add(totalP);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération PDF mensualités", e);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
