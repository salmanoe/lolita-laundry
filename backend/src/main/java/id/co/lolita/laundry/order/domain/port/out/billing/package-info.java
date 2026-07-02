/**
 * Order-owned SPI implemented by another module. Exposed as a Spring Modulith named interface
 * so {@code billing} may provide {@link id.co.lolita.laundry.order.domain.port.out.billing.BillingStatusPort}
 * without the order module depending on billing (which would form a cycle — billing already
 * depends on order). The rest of {@code port.out} stays internal to the order module.
 */
@NamedInterface("billing-spi")
package id.co.lolita.laundry.order.domain.port.out.billing;

import org.springframework.modulith.NamedInterface;
