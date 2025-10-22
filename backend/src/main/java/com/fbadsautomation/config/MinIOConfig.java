package com.fbadsautomation.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinIOConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name:fbads-content}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            log.info("MinIO client configured successfully with endpoint: {}", endpoint);

            // Health check: Verify connection and bucket
            try {
                boolean bucketExists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
                );

                if (!bucketExists) {
                    log.warn("Bucket '{}' does not exist, creating it...", bucketName);
                    client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                    );
                    log.info("Bucket '{}' created successfully", bucketName);
                } else {
                    log.info("MinIO health check passed - bucket '{}' exists and is accessible", bucketName);
                }
            } catch (Exception healthCheckEx) {
                log.error("MinIO health check failed - cannot access bucket '{}': {}",
                         bucketName, healthCheckEx.getMessage());
                throw new RuntimeException("MinIO health check failed", healthCheckEx);
            }

            return client;
        } catch (Exception e) {
            log.error("Failed to configure MinIO client", e);
            throw new RuntimeException("Failed to configure MinIO client", e);
        }
    }
}