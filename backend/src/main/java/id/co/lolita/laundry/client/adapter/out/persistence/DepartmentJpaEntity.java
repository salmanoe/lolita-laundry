package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
class DepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    static DepartmentJpaEntity fromDomain(Department dept) {
        var e = new DepartmentJpaEntity();
        e.id = dept.getId();
        e.clientId = dept.getClientId();
        e.name = dept.getName();
        e.active = dept.isActive();
        return e;
    }

    Department toDomain() {
        return new Department(id, clientId, name, active);
    }
}
