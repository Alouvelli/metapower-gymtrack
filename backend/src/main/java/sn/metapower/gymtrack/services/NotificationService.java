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

    /** Nombre de jours avant la fin de l'abonnement où le rappel est envoyé. */
    private static final int JOURS_AVANT_FIN = 3;

    /**
     * Tous les jours à 08:00 : rappel aux membres dont l'abonnement se termine dans
     * {@value #JOURS_AVANT_FIN} jours (période « de date à date » propre à chaque membre).
     * Un membre qui a déjà renouvelé a une fin plus lointaine et n'est pas relancé.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void envoyerRappelsExpiration() {
        LocalDate echeance = LocalDate.now().plusDays(JOURS_AVANT_FIN);
        int envoyes = 0;

        for (Membre m : membreRepository.findAll()) {
            if (m.getStatut() != StatutMembre.ACTIF) continue;
            if (m.getEmail() == null || m.getEmail().isBlank()) continue;

            boolean finDansTroisJours = mensualiteRepository
                    .findDerniereFin(m, StatutPaiement.PAYE)
                    .map(echeance::equals)
                    .orElse(false);

            if (finDansTroisJours) {
                envoyerRappel(m, echeance);
                envoyes++;
            }
        }

        log.info("[NotificationService] {} rappel(s) envoyé(s) (fin d'abonnement le {})", envoyes, echeance);
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
