package id.co.lolita.laundry.client.adapter.in.web.dto;

import id.co.lolita.laundry.client.domain.Department;

public record DepartmentResponse(Long id, Long clientId, String name, boolean active) {
    public static DepartmentResponse from(Department dept) {
        return new DepartmentResponse(dept.getId(), dept.getClientId(), dept.getName(), dept.isActive());
    }
}
