package sn.metapower.gymtrack.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 120)
    private String email;

    @Column(length = 80)
    private String nom;

    @Column(length = 80)
    private String prenom;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Collection<AppRole> appRoles = new ArrayList<>();

    /**
     * Sites auxquels cet utilisateur est rattaché (réceptionniste/caissier d'une salle).
     * Un ADMIN peut n'avoir aucun site (accès global).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "staff_site",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "site_id"))
    private Set<Site> sites = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "reset_password_token", unique = true)
    @JsonIgnore
    private String resetPasswordToken;

    @JsonIgnore
    @Column(name = "reset_password_expires_at")
    private Date resetPasswordExpiresAt;
}
