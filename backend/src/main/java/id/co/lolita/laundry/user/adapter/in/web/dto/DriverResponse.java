package id.co.lolita.laundry.user.adapter.in.web.dto;

import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery.DriverSummary;

/**
 * A driver option for the staff "assign to driver" picker.
 */
public record DriverResponse(Long id, String fullName) {
    public static DriverResponse from(DriverSummary d) {
        return new DriverResponse(d.id(), d.fullName());
    }
}