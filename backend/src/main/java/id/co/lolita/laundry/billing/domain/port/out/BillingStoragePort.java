package id.co.lolita.laundry.billing.domain.port.out;

/**
 * Billing's view of object storage for generated PDFs. The adapter delegates to the
 * {@code storage} module's {@code StoragePort} (MinIO in dev, Cloudflare R2 in prod).
 */
public interface BillingStoragePort {

    /** Stores the PDF bytes under {@code key} and returns the stored key. */
    String store(String key, byte[] pdf);

    /** Pre-signed URL so the frontend can fetch the PDF directly (inline view). */
    String presignedUrl(String key, int expirySeconds);

    /**
     * Pre-signed URL that forces a browser download named {@code downloadFilename} (the "Unduh"
     * action) instead of an inline render — reliable on mobile browsers.
     */
    String presignedUrl(String key, int expirySeconds, String downloadFilename);
}