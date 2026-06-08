package id.co.lolita.laundry.settings.adapter.out.persistence;

import id.co.lolita.laundry.settings.domain.CompanyProfile;
import id.co.lolita.laundry.settings.domain.port.out.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class CompanyProfileJpaAdapter implements CompanyProfileRepository {

    private final CompanyProfileJpaRepository jpaRepository;

    @Override
    public Optional<CompanyProfile> find() {
        return jpaRepository.findById(CompanyProfile.SINGLETON_ID).map(CompanyProfileJpaEntity::toDomain);
    }

    @Override
    public CompanyProfile save(CompanyProfile profile) {
        return jpaRepository.save(CompanyProfileJpaEntity.fromDomain(profile)).toDomain();
    }
}
