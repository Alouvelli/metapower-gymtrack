package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mensualite", indexes = {
        @Index(name = "idx_mensualite_membre_mois", columnList = "membre_id,mois_concerne")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mensualite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    /** Premier jour du mois concerné. */
    @Column(name = "mois_concerne", nullable = false)
    private LocalDate moisConcerne;

    /** Nombre de mois couverts (durée du plan au moment du paiement). */
    @Column(name = "nb_mois", nullable = false)
    private Integer nbMois = 1;

    /** Premier jour du dernier mois couvert (= moisConcerne si nbMois = 1). */
    @Column(name = "mois_fin", nullable = false)
    private LocalDate moisFin;

    /** Premier jour couvert (date du paiement, ou lendemain de la fin de l'abonnement en cours). */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /** Dernier jour couvert, inclus : payé le 29/09 pour 1 mois → 28/10. */
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private Integer montant;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    /** Référence externe Wave / Orange Money / chèque… */
    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enregistre_par_id")
    private AppUser enregistrePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
