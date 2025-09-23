package com.screenleads.backend.app.domain.model;


import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


@Entity
@Table(name = "app_user",
indexes = {
@Index(name = "ix_user_username", columnList = "username", unique = true),
@Index(name = "ix_user_email", columnList = "email", unique = true)
}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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


@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(name = "user_role",
joinColumns = @JoinColumn(name = "user_id"),
inverseJoinColumns = @JoinColumn(name = "role_id"))
private Set<Role> roles = new HashSet<>();


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "company_id",
foreignKey = @ForeignKey(name = "fk_user_company"))
private Company company;


@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "profile_image_id",
foreignKey = @ForeignKey(name = "fk_user_profile_image"))
private Media profileImage;


// === UserDetails ===
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream().map(r -> new SimpleGrantedAuthority(r.getRole())).toList();
}
@Override public boolean isAccountNonExpired() { return true; }
@Override public boolean isAccountNonLocked() { return true; }
@Override public boolean isCredentialsNonExpired() { return true; }
@Override public boolean isEnabled() { return true; }
}