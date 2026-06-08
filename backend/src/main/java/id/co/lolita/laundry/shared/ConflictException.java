package id.co.lolita.laundry.shared;

/**
 * Thrown when a request conflicts with the current state of a resource — typically a
 * concurrency collision that a unique constraint serializes (e.g. two drivers confirming
 * the same delivery). Carries a user-facing message.
 * Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
