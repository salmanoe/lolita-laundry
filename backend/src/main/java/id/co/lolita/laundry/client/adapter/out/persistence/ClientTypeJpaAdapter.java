package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientType;
import id.co.lolita.laundry.client.domain.port.out.ClientTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ClientTypeJpaAdapter implements ClientTypeRepository {

    private final ClientTypeJpaRepository jpaRepository;

    @Override
    public List<ClientType> findAll() {
        return jpaRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(ClientTypeJpaEntity::toDomain).toList();
    }

    @Override
    public List<ClientType> findAllActive() {
        return jpaRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(ClientTypeJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ClientType> findById(Long id) {
        return jpaRepository.findById(id).map(ClientTypeJpaEntity::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public ClientType save(ClientType clientType) {
        return jpaRepository.save(ClientTypeJpaEntity.fromDomain(clientType)).toDomain();
    }
}