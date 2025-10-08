// src/main/java/com/screenleads/backend/app/domain/model/User.java
package com.screenleads.backend.app.domain.model;

import java.util.Collection;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "ix_user_username", columnList = "username", unique = true),
        @Index(name = "ix_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@org.hibernate.annotations.Filter(name = "companyFilter", condition = "company_id = :companyId")
public class User extends Auditable implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Email
    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(name = "last_name", length = 100)
    private String lastName;

    /** 🔁 Cambiamos de ManyToMany a ManyToOne */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(name = "fk_user_company"))
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id", foreignKey = @ForeignKey(name = "fk_user_profile_image"))
    private Media profileImage;

    // === UserDetails ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Un único rol → una única authority
        String authority = (role != null && role.getRole() != null) ? role.getRole() : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
