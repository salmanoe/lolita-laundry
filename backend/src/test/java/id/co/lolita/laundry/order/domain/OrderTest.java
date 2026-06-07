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
        order.edit(LocalDate.now().plusDays(1), "updated", null);
        assertThat(order.getNotes()).isEqualTo("updated");
    }

    @Test
    void edit_rejectedOnceDone() {
        var order = newOrder(BigDecimal.ONE, line(10L, "1", "10"));
        order.advanceStatus(OrderStatus.PROCESSING);
        order.advanceStatus(OrderStatus.DONE);
        assertThatThrownBy(() -> order.edit(null, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
