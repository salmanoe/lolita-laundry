package id.co.lolita.laundry.user.adapter.out.persistence;

import id.co.lolita.laundry.user.domain.Role;
import id.co.lolita.laundry.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth0_sub", nullable = false, unique = true, length = 128)
    private String auth0Sub;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static UserJpaEntity fromDomain(User user) {
        var entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.auth0Sub = user.getAuth0Sub();
        entity.fullName = user.getFullName();
        entity.role = user.getRole();
        entity.active = user.isActive();
        entity.createdAt = user.getCreatedAt();
        return entity;
    }

    User toDomain() {
        return new User(id, auth0Sub, fullName, role, active, createdAt);
    }
}
