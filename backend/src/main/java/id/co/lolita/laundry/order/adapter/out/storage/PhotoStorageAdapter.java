package id.co.lolita.laundry.order.adapter.out.storage;

import id.co.lolita.laundry.order.domain.port.out.PhotoStoragePort;
import id.co.lolita.laundry.storage.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * Bridges the order module's {@link PhotoStoragePort} to the shared {@code storage}
 * module's {@link StoragePort} (MinIO in dev, Cloudflare R2 in prod).
 */
@Component
@RequiredArgsConstructor
class PhotoStorageAdapter implements PhotoStoragePort {

    private final StoragePort storage;

    @Override
    public String store(String key, byte[] content, String contentType) {
        return storage.store(key, new ByteArrayInputStream(content), contentType, content.length);
    }

    @Override
    public String presignedUrl(String key, int expirySeconds) {
        return storage.generatePresignedUrl(key, expirySeconds);
    }
}