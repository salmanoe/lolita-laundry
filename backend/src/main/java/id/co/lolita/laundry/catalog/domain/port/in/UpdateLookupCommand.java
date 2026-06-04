package id.co.lolita.laundry.catalog.domain.port.in;

/**
 * Command to update a reference-data entry. {@code code} is immutable, so it is not included.
 */
public record UpdateLookupCommand(Long id, String displayName, int sortOrder, boolean active) {
}
