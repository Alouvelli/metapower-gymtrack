package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_abonnement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PlanAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(name = "duree_en_mois", nullable = false)
    private Integer dureeEnMois;

    /** Prix en FCFA, sans décimales. */
    @Column(nullable = false)
    private Integer prix;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean actif = true;
}
