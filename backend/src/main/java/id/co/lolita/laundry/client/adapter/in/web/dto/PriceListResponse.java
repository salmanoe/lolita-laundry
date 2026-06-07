package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.port.in.ManagePriceListUseCase.CurrentPrice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceListResponse(Long itemId, BigDecimal pricePerUnit, LocalDate effectiveDate, Long departmentId) {
    public static PriceListResponse from(CurrentPrice entry) {
        return new PriceListResponse(entry.itemId(), entry.pricePerUnit(), entry.effectiveDate(), entry.departmentId());
    }
}
