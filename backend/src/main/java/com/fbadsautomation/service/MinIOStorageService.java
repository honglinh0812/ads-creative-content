package com.fbadsautomation.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public void initializeBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Created bucket: {}", bucketName);
            } else {
                log.info("Bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            initializeBucket();

            String originalFilename = file.getOriginalFilename();
            String filename = generateSecureFilename(originalFilename);
            String contentType = file.getContentType();

            log.info("⬆️  [MINIO UPLOAD] Starting upload: {} → {} (bucket: {}, size: {} bytes)",
                    originalFilename, filename, bucketName, file.getSize());

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }

            log.info("✅ [MINIO UPLOAD] File uploaded successfully: {} ({})", filename, contentType);
            return filename;

        } catch (Exception e) {
            log.error("❌ [MINIO UPLOAD] Error uploading file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public String uploadFileWithDuplicateCheck(MultipartFile file) {
        try {
            initializeBucket();

            String fileHash = getFileHash(file.getInputStream());
            String existingFile = findFileByHash(fileHash);

            if (existingFile != null) {
                log.info("File with same hash already exists: {}", existingFile);
                return existingFile;
            }

            return uploadFile(file);

        } catch (Exception e) {
            log.error("Error uploading file with duplicate check: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public InputStream downloadFile(String filename) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file: {}", filename, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    public void deleteFile(String filename) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
            log.info("File deleted successfully: {}", filename);
        } catch (Exception e) {
            log.error("Error deleting file: {}", filename, e);
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    public boolean fileExists(String filename) {
        try {
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFileUrl(String filename) {
        // Return API gateway path for frontend access
        // Backend ImageController serves images from MinIO at /api/images/
        return "/api/images/" + filename;
    }

    public StatObjectResponse getFileInfo(String filename) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error getting file info: {}", filename, e);
            throw new RuntimeException("Failed to get file info from MinIO", e);
        }
    }

    /**
     * Get a publicly accessible presigned URL for a file
     * This URL is valid for 7 days and can be used for external access (e.g., Facebook Ads import)
     *
     * @param filename The filename to get the public URL for
     * @return Presigned URL valid for 7 days
     * @throws Exception if URL generation fails
     */
    public String getPublicUrl(String filename) throws Exception {
        try {
            // Generate presigned URL valid for 7 days (Facebook's import window)
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );

            log.debug("Generated presigned URL for file: {} → {}", filename, presignedUrl);
            return presignedUrl;

        } catch (Exception e) {
            log.error("Error generating public URL for file: {}", filename, e);
            throw new Exception("Failed to generate public URL for file: " + filename, e);
        }
    }

    private String generateSecureFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension.toLowerCase();
    }

    private String getFileHash(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];

        try (DigestInputStream dis = new DigestInputStream(inputStream, digest)) {
            while (dis.read(buffer) != -1) {
                // Reading the file to compute hash
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String findFileByHash(String hash) {
        // For now, we'll implement a simple approach
        // In a production system, you might want to store file hashes in database
        // or use MinIO's metadata features to store and search by hash
        return null;
    }
}