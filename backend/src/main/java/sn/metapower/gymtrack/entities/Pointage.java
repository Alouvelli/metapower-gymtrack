package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "pointage", indexes = {
        @Index(name = "idx_pointage_membre_heure", columnList = "membre_id,heure_entree"),
        @Index(name = "idx_pointage_site_heure", columnList = "site_id,heure_entree")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Renseigné si le pointage est issu d'un membre. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id")
    private Membre membre;

    /** Renseigné si le pointage est issu d'un ticket d'entrée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private TicketEntree ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "heure_entree", nullable = false)
    private Instant heureEntree = Instant.now();

    @Column(name = "heure_sortie")
    private Instant heureSortie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPointage statut;

    @Column(name = "motif_refus", length = 60)
    private String motifRefus;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_source", nullable = false, length = 20)
    private ScanSource scanSource = ScanSource.TABLETTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_acces", nullable = false, length = 10)
    private TypeAcces typeAcces;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private AppUser validePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
