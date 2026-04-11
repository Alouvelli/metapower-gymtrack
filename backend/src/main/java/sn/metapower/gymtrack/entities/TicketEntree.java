package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Ticket d'entrée à usage unique vendu à un non-membre pour accéder à la salle.
 * Le QR code imprimé sur le reçu est scanné au tourniquet exactement comme un membre.
 */
@Entity
@Table(name = "ticket_entree", indexes = {
        @Index(name = "idx_ticket_qr_token", columnList = "qrCodeToken", unique = true),
        @Index(name = "idx_ticket_numero", columnList = "numero", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TicketEntree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format TKT-YYYYMMDD-XXXX. */
    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    /** UUID utilisé pour générer le QR code. */
    @Column(name = "qr_code_token", nullable = false, unique = true, length = 64)
    private String qrCodeToken = UUID.randomUUID().toString();

    /** Site sur lequel le ticket a été vendu (un ticket reste valable seulement sur ce site). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarif_id")
    private TarifTicket tarif;

    @Column(nullable = false)
    private Integer prix;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false, length = 20)
    private ModePaiement modePaiement;

    @Column(name = "date_paiement", nullable = false)
    private Instant datePaiement = Instant.now();

    /** Limite de validité (date+heure). */
    @Column(name = "date_validite", nullable = false)
    private Instant dateValidite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTicket statut = StatutTicket.VALIDE;

    /** Optionnel : nom du client (souhait du caissier). */
    @Column(name = "nom_client", length = 120)
    private String nomClient;

    @Column(name = "telephone_client", length = 30)
    private String telephoneClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id")
    private AppUser vendeur;

    @Column(name = "date_utilisation")
    private Instant dateUtilisation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
