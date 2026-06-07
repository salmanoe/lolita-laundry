package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientItemDepartment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_item_departments")
@Getter
@Setter
@NoArgsConstructor
class ClientItemDepartmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    static ClientItemDepartmentJpaEntity fromDomain(ClientItemDepartment m) {
        var e = new ClientItemDepartmentJpaEntity();
        e.id = m.id();
        e.clientId = m.clientId();
        e.itemId = m.itemId();
        e.departmentId = m.departmentId();
        return e;
    }

    ClientItemDepartment toDomain() {
        return new ClientItemDepartment(id, clientId, itemId, departmentId);
    }
}