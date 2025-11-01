package com.fbadsautomation.controller;

import com.fbadsautomation.service.MinIOStorageService;
import io.minio.StatObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/images")
@CrossOrigin(origins = "*")
@Tag(name = "Images", description = "Image serving endpoints")
public class ImageController {

    @Autowired
    private MinIOStorageService minioStorageService;

    @Value("${app.image.storage.location:uploads/images}")
    private String imageStorageLocation;

    @Operation(summary = "Serve image file", description = "Serves an image file by filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image file served successfully"),
            @ApiResponse(responseCode = "404", description = "Image file not found")
    })
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "Image filename to serve") @PathVariable String filename) {
        log.info("🖼️  [IMAGE REQUEST] Serving image: {}", filename);

        try {
            // Try MinIO first
            if (minioStorageService.fileExists(filename)) {
                log.debug("✅ [IMAGE FOUND IN MINIO] File exists: {}", filename);

                // Get file info and stream from MinIO
                StatObjectResponse fileInfo = minioStorageService.getFileInfo(filename);
                InputStream inputStream = minioStorageService.downloadFile(filename);

                Resource resource = new InputStreamResource(inputStream);

                // Determine content type
                String contentType = fileInfo.contentType();
                if (contentType == null || contentType.isEmpty()) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                log.info("✅ [SERVED FROM MINIO] {} (type: {})", filename, contentType);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }

            // Fallback to local filesystem
            log.debug("File not in MinIO, checking local filesystem: {}", filename);

            Path localPath = Paths.get(imageStorageLocation).resolve(filename);
            if (Files.exists(localPath)) {
                log.info("✅ [IMAGE FOUND IN LOCAL] Serving from: {}", localPath);

                Resource resource = new FileSystemResource(localPath);
                String contentType = Files.probeContentType(localPath);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }

                log.info("✅ [SERVED FROM LOCAL] {} (type: {})", filename, contentType);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }

            // Both MinIO and local failed
            log.warn("⚠️  [IMAGE NOT FOUND] File does not exist in MinIO or local: {}", filename);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ [IMAGE ERROR] Failed to serve image {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
