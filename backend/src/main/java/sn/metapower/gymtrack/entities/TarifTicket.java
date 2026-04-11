package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tarif_ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TarifTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String libelle;

    /** Prix en FCFA. */
    @Column(nullable = false)
    private Integer prix;

    /** Durée de validité du ticket en heures (24 = la journée). */
    @Column(name = "duree_validite_heures", nullable = false)
    private Integer dureeValiditeHeures = 24;

    @Column(nullable = false)
    private boolean actif = true;
}
