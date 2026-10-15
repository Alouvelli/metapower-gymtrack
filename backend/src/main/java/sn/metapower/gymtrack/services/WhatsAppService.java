package sn.metapower.gymtrack.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.metapower.gymtrack.entities.TicketEntree;
import sn.metapower.gymtrack.exceptions.BusinessException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Envoi du ticket par WhatsApp via un lien « click-to-chat » (wa.me) :
 * la caisse ouvre le lien, le message est pré-rempli, il suffit d'appuyer sur Envoyer.
 * Aucun compte WhatsApp Business / API requis.
 */
@Service
public class WhatsAppService {

    private static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(DAKAR);

    @Value("${app.base-url:http://localhost:4200}")
    private String appBaseUrl;

    /**
     * Normalise un numéro au format international sans « + » (attendu par wa.me).
     * « 77 123 45 67 », « +221 77 123 45 67 », « 00221771234567 » → « 221771234567 ».
     */
    public String normaliserNumero(String saisie) {
        if (saisie == null || saisie.isBlank()) {
            throw new BusinessException("Le numéro WhatsApp du client est obligatoire");
        }
        String chiffres = saisie.replaceAll("[^0-9]", "");
        if (chiffres.startsWith("00")) {
            chiffres = chiffres.substring(2);
        }
        // Numéro mobile sénégalais saisi sans indicatif (9 chiffres commençant par 7)
        if (chiffres.length() == 9 && chiffres.startsWith("7")) {
            chiffres = "221" + chiffres;
        }
        boolean senegalValide = chiffres.startsWith("221") && chiffres.length() == 12 && chiffres.charAt(3) == '7';
        boolean etrangerValide = !chiffres.startsWith("221") && chiffres.length() >= 10 && chiffres.length() <= 15;
        if (!senegalValide && !etrangerValide) {
            throw new BusinessException("Numéro WhatsApp invalide : " + saisie);
        }
        return chiffres;
    }

    /** Page publique du ticket (QR plein écran) que le client présente à l'entrée. */
    public String lienTicket(TicketEntree ticket) {
        return appBaseUrl.replaceAll("/+$", "") + "/t/" + ticket.getQrCodeToken();
    }

    /** Lien wa.me avec le message pré-rempli contenant le lien du ticket. */
    public String lienWhatsApp(TicketEntree ticket) {
        String bonjour = (ticket.getNomClient() != null && !ticket.getNomClient().isBlank())
                ? "Bonjour " + ticket.getNomClient().trim() + ","
                : "Bonjour,";
        String message = "METAPOWER GYM - " + ticket.getSite().getNom() + "\n"
                + bonjour + " voici votre ticket " + ticket.getNumero()
                + " (" + ticket.getTarif().getLibelle() + ", " + ticket.getPrix() + " FCFA).\n"
                + "Valable jusqu'au " + DATE_FMT.format(ticket.getDateValidite()) + ".\n"
                + "Présentez ce QR code à l'entrée : " + lienTicket(ticket);
        return "https://wa.me/" + ticket.getTelephoneClient()
                + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
