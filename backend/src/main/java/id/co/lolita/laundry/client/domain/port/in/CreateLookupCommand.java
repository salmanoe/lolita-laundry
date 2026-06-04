package id.co.lolita.laundry.client.domain.port.in;

/**
 * Command to create a reference-data entry (client type). New entries start active.
 */
public record CreateLookupCommand(String code, String displayName, int sortOrder) {
}