package id.co.lolita.laundry.order.domain.port.out;

/**
 * Order module's view of object storage for delivery photos. The adapter delegates to the
 * {@code storage} module's {@code StoragePort}.
 */
public interface PhotoStoragePort {

    /**
     * Stores the photo bytes under {@code key} and returns the stored key.
     */
    String store(String key, byte[] content, String contentType);

    /**
     * Pre-signed URL so the frontend can fetch the photo directly.
     */
    String presignedUrl(String key, int expirySeconds);
}