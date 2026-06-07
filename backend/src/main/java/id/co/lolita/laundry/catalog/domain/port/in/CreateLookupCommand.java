package id.co.lolita.laundry.catalog.domain.port.in;

/**
 * Command to create a reference-data entry (item unit). New entries start active.
 */
public record CreateLookupCommand(String code, String displayName, int sortOrder) {
}
