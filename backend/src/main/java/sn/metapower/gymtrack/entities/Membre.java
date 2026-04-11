package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "membre", indexes = {
        @Index(name = "idx_membre_qr_token", columnList = "qrCodeToken", unique = true),
        @Index(name = "idx_membre_numero_carte", columnList = "numeroCarte", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format MBR-2026-0001. Calculé côté service. */
    @Column(name = "numero_carte", nullable = false, unique = true, length = 30)
    private String numeroCarte;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(nullable = false, length = 30)
    private String telephone;

    @Column(length = 120)
    private String email;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 255)
    private String adresse;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    /** UUID utilisé pour générer le QR code et identifier le membre lors du scan. */
    @Column(name = "qr_code_token", nullable = false, unique = true, length = 64)
    private String qrCodeToken = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutMembre statut = StatutMembre.ACTIF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PlanAbonnement plan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
