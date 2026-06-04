package id.co.lolita.laundry.storage.adapter.out.s3;

import id.co.lolita.laundry.storage.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@RequiredArgsConstructor
class S3StorageAdapter implements StoragePort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public String store(String key, InputStream content, String contentType, long sizeBytes) {
        var request = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build();

        s3.putObject(request, RequestBody.fromInputStream(content, sizeBytes));
        return key;
    }

    @Override
    public String generatePresignedUrl(String key, int expirySeconds) {
        var getRequest = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirySeconds))
                .getObjectRequest(getRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String key) {
        var request = DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .build();

        s3.deleteObject(request);
    }
}
