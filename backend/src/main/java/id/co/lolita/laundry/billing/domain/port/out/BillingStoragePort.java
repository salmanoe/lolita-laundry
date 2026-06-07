package id.co.lolita.laundry.billing.domain.port.out;

/**
 * Billing's view of object storage for generated PDFs. The adapter delegates to the
 * {@code storage} module's {@code StoragePort} (MinIO in dev, Cloudflare R2 in prod).
 */
public interface BillingStoragePort {

    /** Stores the PDF bytes under {@code key} and returns the stored key. */
    String store(String key, byte[] pdf);

    /** Pre-signed URL so the frontend can fetch the PDF directly. */
    String presignedUrl(String key, int expirySeconds);
}