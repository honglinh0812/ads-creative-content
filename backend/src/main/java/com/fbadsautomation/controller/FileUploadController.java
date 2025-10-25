package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import com.fbadsautomation.service.MinIOStorageService;
import io.minio.StatObjectResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")

public class FileUploadController {

    @Autowired
    private AuthService authService;

    @Autowired
    private MinIOStorageService minioStorageService;

    // Security constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov", "avi");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp",
        "video/mp4", "video/quicktime", "video/x-msvideo"
    );

    /**
     * Validate uploaded file for security
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Check file extension
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file content type");
        }

        // Check for path traversal attempts
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename - path traversal detected");
        }
    }


    @PostMapping("/media")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Check if user is authenticated
            User currentUser = authService.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            // Validate file security
            validateFile(file);

            log.info("📤 [UPLOAD] User {} uploading file: {} (size: {} bytes, type: {})",
                    currentUser.getId(), file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Upload file to MinIO with duplicate check
            String filename = minioStorageService.uploadFileWithDuplicateCheck(file);

            // Verify upload succeeded by checking file existence
            if (!minioStorageService.fileExists(filename)) {
                log.error("❌ [UPLOAD FAILED] File uploaded but not found in MinIO: {}", filename);
                throw new RuntimeException("Upload verification failed - file not found in storage");
            }

            // Get file info from MinIO
            StatObjectResponse fileInfo = minioStorageService.getFileInfo(filename);

            // Return file URL
            String fileUrl = "/api/images/" + filename;

            log.info("✅ [UPLOAD SUCCESS] File uploaded and verified: {} → {}", file.getOriginalFilename(), fileUrl);

            response.put("success", true);
            response.put("message", "Upload file thành công");
            response.put("fileUrl", fileUrl);
            response.put("fileName", filename);
            response.put("originalName", file.getOriginalFilename());
            response.put("contentType", file.getContentType());
            response.put("size", fileInfo.size());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
