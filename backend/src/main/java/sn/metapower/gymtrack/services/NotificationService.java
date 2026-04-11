package sn.metapower.gymtrack.services;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import sn.metapower.gymtrack.entities.Membre;
import sn.metapower.gymtrack.entities.StatutMembre;
import sn.metapower.gymtrack.entities.StatutPaiement;
import sn.metapower.gymtrack.repositories.MembreRepository;
import sn.metapower.gymtrack.repositories.MensualiteRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Notifications email aux membres :
 *  - Rappel de mensualité 5 jours avant la fin du mois.
 *  - Lancement automatique tous les jours à 08:00.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MembreRepository membreRepository;
    private final MensualiteRepository mensualiteRepository;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public NotificationService(JavaMailSender mailSender,
                               TemplateEngine templateEngine,
                               MembreRepository membreRepository,
                               MensualiteRepository mensualiteRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.membreRepository = membreRepository;
        this.mensualiteRepository = mensualiteRepository;
    }

    /** Tous les jours à 08:00. */
    @Scheduled(cron = "0 0 8 * * *")
    public void envoyerRappelsExpiration() {
        LocalDate today = LocalDate.now();
        LocalDate finMois = today.withDayOfMonth(today.lengthOfMonth());

        // Déclencher uniquement quand on est à 5 jours ou moins de la fin du mois
        if (today.until(finMois).getDays() > 5) {
            return;
        }

        log.info("[NotificationService] Lancement des rappels d'expiration ({} → {})", today, finMois);

        LocalDate moisCourant = today.withDayOfMonth(1);
        int envoyes = 0;

        for (Membre m : membreRepository.findAll()) {
            if (m.getStatut() != StatutMembre.ACTIF) continue;
            if (m.getEmail() == null || m.getEmail().isBlank()) continue;

            boolean dejaPaye = mensualiteRepository
                    .findFirstByMembreAndMoisConcerne(m, moisCourant)
                    .map(mens -> mens.getStatut() == StatutPaiement.PAYE)
                    .orElse(false);

            if (!dejaPaye) {
                envoyerRappel(m, finMois);
                envoyes++;
            }
        }

        log.info("[NotificationService] {} rappel(s) envoyé(s)", envoyes);
    }

    public void envoyerRappel(Membre membre, LocalDate dateLimite) {
        try {
            Context ctx = new Context();
            ctx.setVariable("prenom", membre.getPrenom());
            ctx.setVariable("numeroCarte", membre.getNumeroCarte());
            ctx.setVariable("planNom", membre.getPlan() != null ? membre.getPlan().getNom() : "—");
            ctx.setVariable("montant", membre.getPlan() != null ? membre.getPlan().getPrix() : 0);
            ctx.setVariable("dateLimite", FMT.format(dateLimite));

            String html = templateEngine.process("mail-rappel-expiration", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(membre.getEmail());
            helper.setSubject("METAPOWER GYM — Rappel mensualité");
            helper.setText(html, true);

            mailSender.send(message);
            log.info("[NotificationService] Rappel envoyé à {} ({})", membre.getEmail(), membre.getNumeroCarte());
        } catch (Exception e) {
            log.error("[NotificationService] Échec envoi rappel à {}: {}", membre.getEmail(), e.getMessage());
        }
    }
}
