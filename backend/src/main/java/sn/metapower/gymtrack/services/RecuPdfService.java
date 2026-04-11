package sn.metapower.gymtrack.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import sn.metapower.gymtrack.entities.TicketEntree;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Génère le reçu PDF imprimable d'un ticket d'entrée. Format adapté à
 * une imprimante thermique 80 mm (largeur ~226 pt).
 */
@Service
public class RecuPdfService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final QrCodeService qrCodeService;

    public RecuPdfService(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    public byte[] genererRecu(TicketEntree ticket) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Format ticket thermique 80mm (226 pt de large)
            Rectangle pageSize = new Rectangle(226, 600);
            Document doc = new Document(pageSize, 10, 10, 10, 10);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
            Font smallFont = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);

            Paragraph title = new Paragraph("METAPOWER GYM", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph site = new Paragraph(ticket.getSite().getNom(), normalFont);
            site.setAlignment(Element.ALIGN_CENTER);
            doc.add(site);

            doc.add(new Paragraph(" ", smallFont));
            doc.add(line());

            doc.add(new Paragraph("TICKET D'ENTREE", new Font(Font.HELVETICA, 10, Font.BOLD)));
            doc.add(new Paragraph("N° " + ticket.getNumero(), normalFont));
            doc.add(new Paragraph("Date : " + FMT.format(ticket.getDatePaiement()), normalFont));
            doc.add(new Paragraph("Valide jusqu'à : " + FMT.format(ticket.getDateValidite()), normalFont));

            if (ticket.getNomClient() != null && !ticket.getNomClient().isBlank()) {
                doc.add(new Paragraph("Client : " + ticket.getNomClient(), normalFont));
            }

            doc.add(line());

            Paragraph prix = new Paragraph(
                    "MONTANT : " + formatPrix(ticket.getPrix()) + " FCFA",
                    new Font(Font.HELVETICA, 11, Font.BOLD));
            prix.setAlignment(Element.ALIGN_CENTER);
            doc.add(prix);

            Paragraph mode = new Paragraph(
                    "Paiement : " + ticket.getModePaiement().name().replace("_", " "),
                    normalFont);
            mode.setAlignment(Element.ALIGN_CENTER);
            doc.add(mode);

            doc.add(line());
            doc.add(new Paragraph(" ", smallFont));

            // QR code centré
            byte[] qrPng = qrCodeService.generatePng(ticket.getQrCodeToken(), 180);
            Image qr = Image.getInstance(qrPng);
            qr.scaleToFit(150, 150);
            qr.setAlignment(Image.ALIGN_CENTER);
            doc.add(qr);

            Paragraph instr = new Paragraph(
                    "Présentez ce QR code\nà l'accueil pour entrer", smallFont);
            instr.setAlignment(Element.ALIGN_CENTER);
            doc.add(instr);

            doc.add(new Paragraph(" ", smallFont));
            doc.add(line());
            Paragraph footer = new Paragraph("Merci de votre visite !", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur génération reçu PDF", e);
        }
    }

    private Paragraph line() {
        Font f = new Font(Font.HELVETICA, 7, Font.NORMAL, Color.GRAY);
        Paragraph p = new Paragraph("--------------------------------", f);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private String formatPrix(int prix) {
        return String.format("%,d", prix).replace(',', ' ');
    }
}
