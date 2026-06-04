package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.ClientPriceList;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceListResponse(Long itemId, BigDecimal pricePerUnit, LocalDate effectiveDate) {
    public static PriceListResponse from(ClientPriceList entry) {
        return new PriceListResponse(entry.itemId(), entry.pricePerUnit(), entry.effectiveDate());
    }
}
