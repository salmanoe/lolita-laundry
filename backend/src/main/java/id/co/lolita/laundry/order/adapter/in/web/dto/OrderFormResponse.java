package id.co.lolita.laundry.order.adapter.in.web.dto;

import id.co.lolita.laundry.order.domain.port.in.GetOrderFormUseCase.OrderFormView;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public order-form payload: which client the token belongs to, its departments (if it
 * bills per department), whether Treatment pricing applies, and the active items with the
 * client's current unit prices.
 */
public record OrderFormResponse(
        Long clientId,
        String clientName,
        String clientCode,
        boolean perDepartment,
        boolean treatmentAvailable,
        List<DepartmentDto> departments,
        List<ItemDto> items
) {
    public record DepartmentDto(Long id, String name) {
    }

    public record ItemDto(Long itemId, String name, Long unitId, Long categoryId, BigDecimal price) {
    }

    public static OrderFormResponse from(OrderFormView v) {
        return new OrderFormResponse(
                v.clientId(), v.clientName(), v.clientCode(), v.perDepartment(), v.treatmentAvailable(),
                v.departments().stream().map(d -> new DepartmentDto(d.id(), d.name())).toList(),
                v.items().stream()
                        .map(i -> new ItemDto(i.itemId(), i.name(), i.unitId(), i.categoryId(), i.price()))
                        .toList()
        );
    }
}
