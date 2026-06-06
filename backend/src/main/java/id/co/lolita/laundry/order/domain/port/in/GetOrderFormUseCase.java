package id.co.lolita.laundry.order.domain.port.in;

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

        /**
         * Only items the client has a current price for appear on the form. The price value
         * itself is intentionally NOT exposed — hotel staff submit quantities, not prices.
         */
        public record ItemLine(Long itemId, String name, Long unitId, String unitName,
                               Long categoryId, String categoryName) {
        }
    }

    OrderFormView getPublicOrderForm(UUID token);
}