package id.co.lolita.laundry.order.domain.port.in;

import java.util.List;

/**
 * Builds the data a DAILY_STAFF operator needs to fill the in-house order form for a given client
 * (selected from the hotel dropdown).
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
         * itself is intentionally NOT exposed on the form payload — staff submit quantities, and
         * prices are resolved server-side at order creation. {@code departmentId} is the item's
         * department for PER_DEPARTMENT clients (null for COMBINED) — the form groups items by
         * department.
         */
        public record ItemLine(Long itemId, String name, Long unitId, String unitName,
                               Long departmentId) {
        }
    }

    OrderFormView getOrderForm(Long clientId);
}