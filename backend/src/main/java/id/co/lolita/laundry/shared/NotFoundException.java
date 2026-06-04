package id.co.lolita.laundry.shared;

/**
 * Thrown when a requested resource does not exist.
 * Mapped to HTTP 404 Not Found by {@code GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
