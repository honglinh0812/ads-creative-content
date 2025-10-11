package com.fbadsautomation.controller;

import com.fbadsautomation.service.MinIOStorageService;
import io.minio.StatObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
@Tag(name = "Images", description = "Image serving endpoints")
public class ImageController {

    @Autowired
    private MinIOStorageService minioStorageService;
    
    @Operation(summary = "Serve image file", description = "Serves an image file by filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image file served successfully"),
            @ApiResponse(responseCode = "404", description = "Image file not found")
    })
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "Image filename to serve") @PathVariable String filename) {
        try {
            // Check if file exists in MinIO
            if (!minioStorageService.fileExists(filename)) {
                return ResponseEntity.notFound().build();
            }

            // Get file info and stream from MinIO
            StatObjectResponse fileInfo = minioStorageService.getFileInfo(filename);
            InputStream inputStream = minioStorageService.downloadFile(filename);

            Resource resource = new InputStreamResource(inputStream);

            // Determine content type
            String contentType = fileInfo.contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error serving file: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }
}
