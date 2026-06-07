package id.co.lolita.laundry.client.domain;

/**
 * Maps a catalogue item to one of a client's departments. Only meaningful for
 * {@code PER_DEPARTMENT} clients (e.g. PBS) — it is the per-item department assignment set in
 * the "Atur Harga" screen alongside the price. One department per item per client.
 *
 * <p>Unlike {@link ClientPriceList} (append-only price history) this assignment is not
 * time-versioned: it lives in its own table and is replaced in place when changed.
 */
public record ClientItemDepartment(Long id, Long clientId, Long itemId, Long departmentId) {
}