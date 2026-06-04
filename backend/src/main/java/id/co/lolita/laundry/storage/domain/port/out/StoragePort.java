package id.co.lolita.laundry.storage.domain.port.out;

import java.io.InputStream;

/**
 * Outbound port for object storage (delivery photos, generated PDFs).
 * Implemented by {@code S3StorageAdapter} against MinIO (dev) or Cloudflare R2 (prod).
 */
public interface StoragePort {

    /**
     * Stores an object and returns its storage key.
     *
     * @param key         object key (path within the bucket), e.g. "photos/PBS-20260601-001.jpg"
     * @param content     object data as a stream
     * @param contentType MIME type, e.g. "image/jpeg" or "application/pdf"
     * @param sizeBytes   stream length in bytes (required by the S3 SDK)
     * @return the stored object key (same as {@code key})
     */
    String store(String key, InputStream content, String contentType, long sizeBytes);

    /**
     * Generates a pre-signed URL valid for the given duration so the frontend
     * can serve delivery photos directly from R2 without proxying through the API.
     *
     * @param key           object key
     * @param expirySeconds how long the URL remains valid
     * @return pre-signed URL string
     */
    String generatePresignedUrl(String key, int expirySeconds);

    /**
     * Deletes an object. Used when an order is canceled before delivery.
     *
     * @param key object key to delete
     */
    void delete(String key);
}
