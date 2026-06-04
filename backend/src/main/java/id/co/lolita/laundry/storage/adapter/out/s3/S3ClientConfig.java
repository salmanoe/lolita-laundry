package id.co.lolita.laundry.storage.adapter.out.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
class S3ClientConfig {

    private final StorageProperties props;

    @Bean
    AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())
        );
    }

    @Bean
    S3Client s3Client(AwsCredentialsProvider credentials) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .credentialsProvider(credentials)
                .region(Region.of(props.getRegion()))
                .forcePathStyle(props.isPathStyleAccess())  // required for MinIO + Cloudflare R2
                .build();
    }

    @Bean
    S3Presigner s3Presigner(AwsCredentialsProvider credentials) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .credentialsProvider(credentials)
                .region(Region.of(props.getRegion()))
                // Presigner has no forcePathStyle() shortcut — set it via S3Configuration (MinIO + R2 need it)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.isPathStyleAccess())
                        .build())
                .build();
    }
}
