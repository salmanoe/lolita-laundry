package id.co.lolita.laundry.client.domain.port.in;

import id.co.lolita.laundry.client.domain.ClientPriceList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ManagePriceListUseCase {

    record SetPriceCommand(Long clientId, Long itemId, BigDecimal pricePerUnit, LocalDate effectiveDate) {
    }

    List<ClientPriceList> getCurrentPrices(Long clientId);

    ClientPriceList setPrice(SetPriceCommand command);
}
