package id.co.lolita.laundry.order.adapter.out.gateway;

import id.co.lolita.laundry.catalog.domain.port.in.CatalogQuery;
import id.co.lolita.laundry.order.domain.port.out.CatalogGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CatalogGatewayAdapter implements CatalogGateway {

    private final CatalogQuery catalog;

    @Override
    public List<CatalogItem> activeItems() {
        return catalog.activeItems().stream().map(CatalogGatewayAdapter::toItem).toList();
    }

    @Override
    public Optional<CatalogItem> findActiveById(Long itemId) {
        return catalog.findActiveById(itemId).map(CatalogGatewayAdapter::toItem);
    }

    private static CatalogItem toItem(CatalogQuery.CatalogItemSnapshot s) {
        return new CatalogItem(s.id(), s.name(), s.unitId(), s.unitName(), s.categoryId(), s.categoryName());
    }
}