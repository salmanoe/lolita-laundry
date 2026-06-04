package id.co.lolita.laundry.client.adapter.out.persistence;

import id.co.lolita.laundry.client.domain.ClientPriceList;
import id.co.lolita.laundry.client.domain.port.out.ClientPriceListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ClientPriceListJpaAdapter implements ClientPriceListRepository {

    private final ClientPriceListJpaRepository jpaRepository;

    @Override
    public List<ClientPriceList> findCurrentPrices(Long clientId) {
        return jpaRepository.findCurrentPrices(clientId).stream()
                .map(ClientPriceListJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<ClientPriceList> findEffectivePrice(Long clientId, Long itemId, LocalDate asOf) {
        return jpaRepository.findEffectivePrice(clientId, itemId, asOf)
                .map(ClientPriceListJpaEntity::toDomain);
    }

    @Override
    public ClientPriceList save(ClientPriceList entry) {
        return jpaRepository.save(ClientPriceListJpaEntity.fromDomain(entry)).toDomain();
    }
}
