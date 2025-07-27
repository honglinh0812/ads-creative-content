package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${app.image.storage.location}")
    private String uploadDir;

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

    /**
     * Generate secure filename
     */
    private String generateSecureFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + extension.toLowerCase();
    }

    @PostMapping("/media")
    public ResponseEntity<Map<String, Object>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Kiểm tra người dùng đã đăng nhập
            User currentUser = authService.getCurrentUser(authentication);
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.status(401).body(response);
            }

            // Validate file security
            validateFile(file);

            // Tạo thư mục upload nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tính hash SHA-256 cho file upload mới
            String uploadedFileHash = getFileHash(file.getInputStream());

            // Scan thư mục, so sánh hash với các file đã có
            try (Stream<Path> paths = Files.list(uploadPath)) {
                for (Path existingFile : (Iterable<Path>) paths::iterator) {
                    if (Files.isRegularFile(existingFile)) {
                        try (InputStream is = Files.newInputStream(existingFile)) {
                            String existingHash = getFileHash(is);
                            if (uploadedFileHash.equals(existingHash)) {
                                // Nếu trùng hash, trả về URL file cũ
                                String fileUrl = "/api/images/" + existingFile.getFileName().toString();
                                response.put("success", true);
                                response.put("message", "File đã tồn tại (trùng nội dung), không cần upload lại");
                                response.put("fileUrl", fileUrl);
                                response.put("fileName", existingFile.getFileName().toString());
                                response.put("originalName", file.getOriginalFilename());
                                response.put("contentType", file.getContentType());
                                response.put("size", Files.size(existingFile));
                                return ResponseEntity.ok(response);
                            }
                        }
                    }
                }
            }

            // Generate secure filename to prevent conflicts and security issues
            String originalFilename = file.getOriginalFilename();
            String targetFilename = generateSecureFilename(originalFilename);
            Path filePath = uploadPath.resolve(targetFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về URL của file
            String fileUrl = "/api/images/" + targetFilename;
            response.put("success", true);
            response.put("message", "Upload file thành công");
            response.put("fileUrl", fileUrl);
            response.put("fileName", targetFilename);
            response.put("originalName", originalFilename);
            response.put("contentType", file.getContentType());
            response.put("size", file.getSize());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error saving file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Hàm tính SHA-256 cho file
    private String getFileHash(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream dis = new DigestInputStream(inputStream, digest)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // đọc hết file
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
