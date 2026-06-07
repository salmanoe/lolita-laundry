package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientItemDepartment;
import id.co.lolita.laundry.client.domain.port.out.ClientItemDepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ClientItemDepartmentJpaAdapter implements ClientItemDepartmentRepository {

    private final ClientItemDepartmentJpaRepository jpaRepository;

    @Override
    public List<ClientItemDepartment> findByClient(Long clientId) {
        return jpaRepository.findByClientId(clientId).stream()
                .map(ClientItemDepartmentJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ClientItemDepartment> find(Long clientId, Long itemId) {
        return jpaRepository.findByClientIdAndItemId(clientId, itemId)
                .map(ClientItemDepartmentJpaEntity::toDomain);
    }

    @Override
    public ClientItemDepartment upsert(Long clientId, Long itemId, Long departmentId) {
        var entity = jpaRepository.findByClientIdAndItemId(clientId, itemId)
                .orElseGet(ClientItemDepartmentJpaEntity::new);
        entity.setClientId(clientId);
        entity.setItemId(itemId);
        entity.setDepartmentId(departmentId);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public void delete(Long clientId, Long itemId) {
        jpaRepository.findByClientIdAndItemId(clientId, itemId).ifPresent(jpaRepository::delete);
    }
}