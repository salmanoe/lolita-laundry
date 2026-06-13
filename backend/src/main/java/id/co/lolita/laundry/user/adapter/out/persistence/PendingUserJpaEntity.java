package id.co.lolita.laundry.user.adapter.out.persistence;

import id.co.lolita.laundry.user.domain.PendingUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "pending_users")
@Getter
@Setter
@NoArgsConstructor
class PendingUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth0_sub", nullable = false, unique = true, length = 128)
    private String auth0Sub;

    @Column(length = 160)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    static PendingUserJpaEntity fromDomain(PendingUser pending) {
        var entity = new PendingUserJpaEntity();
        entity.id = pending.id();
        entity.auth0Sub = pending.auth0Sub();
        entity.email = pending.email();
        entity.fullName = pending.fullName();
        entity.requestedAt = pending.requestedAt();
        return entity;
    }

    PendingUser toDomain() {
        return new PendingUser(id, auth0Sub, email, fullName, requestedAt);
    }
}
