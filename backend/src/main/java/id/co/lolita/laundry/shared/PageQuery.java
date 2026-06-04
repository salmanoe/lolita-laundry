package id.co.lolita.laundry.shared;

/**
 * Framework-free pagination request passed across module ports, so the domain layer
 * never imports Spring Data's {@code Pageable}. Persistence adapters translate this
 * into a Spring {@code PageRequest}.
 *
 * <p>{@code page} is zero-based. {@code sortBy} is an entity property name (or null for
 * unsorted). Values are normalised in the compact constructor.
 */
public record PageQuery(int page, int size, String sortBy, Direction direction) {

    public enum Direction {ASC, DESC}

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) page = 0;
        if (size < 1) size = DEFAULT_SIZE;
        if (size > MAX_SIZE) size = MAX_SIZE;
        if (direction == null) direction = Direction.ASC;
    }

    /**
     * Builds a query from nullable request parameters, applying defaults.
     */
    public static PageQuery of(Integer page, Integer size, String sortBy, String direction) {
        Direction dir = "desc".equalsIgnoreCase(direction) ? Direction.DESC : Direction.ASC;
        return new PageQuery(
                page == null ? 0 : page,
                size == null ? DEFAULT_SIZE : size,
                sortBy == null || sortBy.isBlank() ? null : sortBy,
                dir
        );
    }
}
