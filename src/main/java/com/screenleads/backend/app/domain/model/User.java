import jakarta.persistence.*;
import java.util.Set;


@Entity
@Entity
@Builder(toBuilder = true)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "username" }),@UniqueConstraint(columnNames = { "email" }) })
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String lastName;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToOne
    @JoinColumn(name = "profile_image_id")
    private Media profileImage;

    // Getters, setters y constructores
}
