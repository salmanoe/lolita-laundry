package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.Client;
import id.co.lolita.laundry.client.domain.port.out.ClientRepository;
import id.co.lolita.laundry.shared.Page;
import id.co.lolita.laundry.shared.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class ClientJpaAdapter implements ClientRepository {

    private final ClientJpaRepository jpaRepository;

    @Override
    public Page<Client> findAll(PageQuery query) {
        var springPage = jpaRepository.findAll(PageMapper.toPageable(query));
        return new Page<>(
                springPage.getContent().stream().map(ClientJpaEntity::toDomain).toList(),
                springPage.getNumber(), springPage.getSize(),
                springPage.getTotalElements(), springPage.getTotalPages()
        );
    }

    @Override
    public List<Client> findAllActive() {
        return jpaRepository.findByActiveTrue().stream().map(ClientJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpaRepository.findById(id).map(ClientJpaEntity::toDomain);
    }

    @Override
    public Optional<Client> findByOrderToken(UUID token) {
        return jpaRepository.findByOrderToken(token).map(ClientJpaEntity::toDomain);
    }

    @Override
    public boolean existsByClientCode(String clientCode) {
        return jpaRepository.existsByClientCode(clientCode);
    }

    @Override
    public Client save(Client client) {
        return jpaRepository.save(ClientJpaEntity.fromDomain(client)).toDomain();
    }
}
