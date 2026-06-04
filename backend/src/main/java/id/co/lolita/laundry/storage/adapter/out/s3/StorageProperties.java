package id.co.lolita.laundry.storage.adapter.out.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.storage.*} properties from application-dev.yaml / application-prod.yaml.
 * MinIO (dev) and Cloudflare R2 (prod) both use S3-compatible APIs with path-style access.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * S3-compatible endpoint URL (e.g. <a href="http://localhost:9000">localhost:9000</a> or <a href="https://ACCOUNT.r2.cloudflarestorage.com">ACCOUNT.r2.cloudflarestorage.com</a>)
     */
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "us-east-1";
    /**
     * Must be true for MinIO and Cloudflare R2 (both require path-style URLs).
     */
    private boolean pathStyleAccess = true;
}
