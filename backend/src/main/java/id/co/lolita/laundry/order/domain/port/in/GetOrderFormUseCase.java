package id.co.lolita.laundry.order.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Builds the data a hotel staffer needs to fill the public order form for a given token.
 */
public interface GetOrderFormUseCase {

    record OrderFormView(
            Long clientId,
            String clientName,
            String clientCode,
            boolean perDepartment,
            boolean treatmentAvailable,
            List<DepartmentLine> departments,
            List<ItemLine> items
    ) {
        public record DepartmentLine(Long id, String name) {
        }

        /** {@code price} is null when the client has no price set for the item yet. */
        public record ItemLine(Long itemId, String name, Long unitId, Long categoryId, BigDecimal price) {
        }
    }

    OrderFormView getPublicOrderForm(UUID token);
}