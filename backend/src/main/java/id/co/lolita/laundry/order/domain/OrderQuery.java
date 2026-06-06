package id.co.lolita.laundry.order.domain;

import id.co.lolita.laundry.shared.PageQuery;

import java.time.LocalDate;

/**
 * Filter + pagination criteria for listing orders. Any filter may be null (no constraint).
 */
public record OrderQuery(Long clientId, OrderStatus status, LocalDate from, LocalDate to, PageQuery page) {
}
