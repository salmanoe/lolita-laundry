package id.co.lolita.laundry.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain rules of the Order aggregate: subtotal computation with the pricing
 * multiplier and the one-way status lifecycle. No Spring, no mocks.
 */
class OrderTest {

    private static Order newOrder(BigDecimal multiplier, Order.NewLine... lines) {
        return Order.create("AYI-20260101-001", 1L, LocalDate.now(), null, multiplier,
                "Staff", null, null, List.of(lines), Instant.now());
    }

    private static Order.NewLine line(Long itemId, String qty, String price) {
        return new Order.NewLine(itemId, new BigDecimal(qty), new BigDecimal(price), null);
    }

    @Test
    void subtotal_isQuantityTimesPriceTimesMultiplier() {
        var order = newOrder(new BigDecimal("2.0"), line(10L, "3", "5000"));

        assertThat(order.getLineItems()).hasSize(1);
        assertThat(order.getLineItems().getFirst().subtotal()).isEqualByComparingTo("30000.00");
        assertThat(order.total()).isEqualByComparingTo("30000.00");
    }

    @Test
    void reguler_multiplierOne_leavesSubtotalUnscaled() {
        var order = newOrder(BigDecimal.ONE,
                line(10L, "2", "4500"),
                line(20L, "1", "1000"));

        assertThat(order.total()).isEqualByComparingTo("10000.00");
    }

    @Test
    void create_startsAtReceived() {
        assertThat(newOrder(BigDecimal.ONE, line(10L, "1", "10"))
                .getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void create_rejectsEmptyLines() {
        assertThatThrownBy(() -> Order.create("X", 1L, LocalDate.now(), null, BigDecimal.ONE,
                "Staff", null, null, List.of(), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advanceStatus_allowsSingleForwardStep() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.advanceStatus(OrderStatus.DONE);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DONE);
    }

    @Test
    void advanceStatus_rejectsSkippingAStep() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        assertThatThrownBy(() -> order.advanceStatus(OrderStatus.DONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void advanceStatus_rejectsGoingBackwards() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> order.advanceStatus(OrderStatus.RECEIVED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_allowedWhileProcessing() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.edit(null, null, LocalDate.now().plusDays(1), "updated", null);
        assertThat(order.getNotes()).isEqualTo("updated");
    }

    @Test
    void edit_changesOrderDateWhenProvided() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        var newDate = order.getOrderDate().minusDays(3);
        order.edit(newDate, null, null, null, null);
        assertThat(order.getOrderDate()).isEqualTo(newDate);
    }

    @Test
    void edit_keepsOrderDateWhenNull() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        var original = order.getOrderDate();
        order.edit(null, null, LocalDate.now().plusDays(1), "updated", null);
        assertThat(order.getOrderDate()).isEqualTo(original);
    }

    @Test
    void edit_treatmentCorrection_reprisesExistingLinesWithoutNewLineList() {
        var order = newOrder(BigDecimal.ONE, line(10L, "3", "5000"));
        assertThat(order.total()).isEqualByComparingTo("15000.00");

        // Flip Reguler → Treatment (×2) with no new item list — existing lines re-price in place.
        order.edit(null, new BigDecimal("2.0"), null, null, null);

        assertThat(order.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(order.getLineItems().getFirst().priceAtOrder()).isEqualByComparingTo("5000");
        assertThat(order.total()).isEqualByComparingTo("30000.00");
    }

    @Test
    void edit_keepsMultiplierWhenNull() {
        var order = newOrder(new BigDecimal("2.0"), line(10L, "3", "5000"));
        order.edit(null, null, null, "note", null);
        assertThat(order.getPricingMultiplier()).isEqualByComparingTo("2.0");
        assertThat(order.total()).isEqualByComparingTo("30000.00");
    }

    @Test
    void edit_rejectedOnceDone() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.advanceStatus(OrderStatus.DONE);
        assertThatThrownBy(() -> order.edit(null, null, null, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reactivate_restoresGivenStatus() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        order.reactivate(OrderStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void reactivate_rejectedWhenNotCancelled() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        assertThatThrownBy(() -> order.reactivate(OrderStatus.RECEIVED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reactivate_rejectsDeliveredOrCancelledTarget() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.cancel();
        assertThatThrownBy(() -> order.reactivate(OrderStatus.DELIVERED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.reactivate(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order.reactivate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void correctItems_replacesAndRepricesOnLockedOrder() {
        var order = newOrder(new BigDecimal("2.0"), line(10L, "3", "5000"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.advanceStatus(OrderStatus.DONE);
        // DONE is past the normal edit window, but a correction is allowed.
        order.correctItems(List.of(new Order.NewLine(20L, new BigDecimal("2"), new BigDecimal("4000"), null)));

        assertThat(order.getLineItems()).hasSize(1);
        assertThat(order.getLineItems().getFirst().itemId()).isEqualTo(20L);
        // Multiplier (×2) is preserved: 2 × 4000 × 2.
        assertThat(order.total()).isEqualByComparingTo("16000.00");
    }

    @Test
    void correctItems_rejectedOnCancelledOrder() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.cancel();
        assertThatThrownBy(() -> order.correctItems(
                List.of(new Order.NewLine(20L, new BigDecimal("1"), new BigDecimal("10"), null))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void correctItems_rejectsEmptyLines() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        assertThatThrownBy(() -> order.correctItems(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
