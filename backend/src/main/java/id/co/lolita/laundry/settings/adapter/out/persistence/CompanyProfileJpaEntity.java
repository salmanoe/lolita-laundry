package id.co.lolita.laundry.settings.adapter.out.persistence;

import id.co.lolita.laundry.settings.domain.CompanyProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistence for the singleton company profile. The id is not generated — it is always
 * {@link CompanyProfile#SINGLETON_ID} so there can only ever be one row.
 */
@Entity
@Table(name = "company_profile")
@Getter
@Setter
@NoArgsConstructor
class CompanyProfileJpaEntity {

    @Id
    private Long id;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 30)
    private String phone;

    static CompanyProfileJpaEntity fromDomain(CompanyProfile p) {
        var e = new CompanyProfileJpaEntity();
        e.id = p.getId() == null ? CompanyProfile.SINGLETON_ID : p.getId();
        e.companyName = p.getCompanyName();
        e.address = p.getAddress();
        e.phone = p.getPhone();
        return e;
    }

    CompanyProfile toDomain() {
        return new CompanyProfile(id, companyName, address, phone);
    }
}
