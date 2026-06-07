package id.co.lolita.laundry.client.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SetPriceRequest(
        @NotNull Long itemId,
        // >= 0 to match the DB CHECK (price_per_unit >= 0); 0 allows a complimentary item
        @NotNull @PositiveOrZero BigDecimal pricePerUnit,
        LocalDate effectiveDate,  // nullable — defaults to today in the service
        // required for PER_DEPARTMENT clients (the item→department assignment), null otherwise
        Long departmentId
) {
}
