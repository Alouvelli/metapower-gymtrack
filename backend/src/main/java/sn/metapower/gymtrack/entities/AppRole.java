package sn.metapower.gymtrack.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ROLE_ADMIN, ROLE_RECEPTIONNISTE, ROLE_CAISSIER */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    public AppRole(String roleName) {
        this.roleName = roleName;
    }
}
