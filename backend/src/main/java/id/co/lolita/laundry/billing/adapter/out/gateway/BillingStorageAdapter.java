package id.co.lolita.laundry.billing.adapter.out.gateway;

import id.co.lolita.laundry.billing.domain.port.out.BillingStoragePort;
import id.co.lolita.laundry.storage.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * Bridges billing's {@link BillingStoragePort} to the shared {@code storage} module's
 * {@link StoragePort} (MinIO in dev, Cloudflare R2 in prod). PDFs are stored as
 * {@code application/pdf}.
 */
@Component
@RequiredArgsConstructor
class BillingStorageAdapter implements BillingStoragePort {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final StoragePort storage;

    @Override
    public String store(String key, byte[] pdf) {
        return storage.store(key, new ByteArrayInputStream(pdf), PDF_CONTENT_TYPE, pdf.length);
    }

    @Override
    public String presignedUrl(String key, int expirySeconds) {
        return storage.generatePresignedUrl(key, expirySeconds);
    }

    @Override
    public String presignedUrl(String key, int expirySeconds, String downloadFilename) {
        return storage.generatePresignedUrl(key, expirySeconds, downloadFilename);
    }
}