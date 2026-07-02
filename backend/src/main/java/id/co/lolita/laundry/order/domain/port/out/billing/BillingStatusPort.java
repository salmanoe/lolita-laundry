package id.co.lolita.laundry.order.domain.port.out.billing;

/**
 * Outbound SPI the order module uses to ask whether an order has already landed on a frozen
 * (ISSUED/PAID) monthly billing.
 *
 * <p>Dependency-inverted on purpose: {@code billing} already depends on {@code order}, so the
 * order module cannot depend back on billing without a Modulith cycle. Instead the order module
 * <em>owns</em> this contract (exposed as the {@code billing-spi} named interface) and billing
 * <em>implements</em> it — the only import edge remains billing → order, so no cycle forms.
 *
 * <p>Used to reject a SUPER_ADMIN order-date change once the order sits on an already-issued
 * invoice: an issued document has been sent to the client and must not be retroactively moved
 * to a different billing period.
 */
public interface BillingStatusPort {

    /**
     * @return {@code true} if the order currently has a line on any ISSUED or PAID monthly billing.
     */
    boolean isOrderOnIssuedBilling(Long orderId);
}
