package com.fbadsautomation.controller;

import com.fbadsautomation.model.User;
import com.fbadsautomation.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private AuthService authService;

    @Value("${app.image.storage.location}")
    private String uploadDir;

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
                response.put("message", "Người dùng chưa đăng nhập");
                return ResponseEntity.status(401).body(response);
            }

            // Kiểm tra file có hợp lệ không
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Kiểm tra loại file (chỉ cho phép ảnh và video)
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                response.put("success", false);
                response.put("message", "Chỉ cho phép upload file ảnh hoặc video");
                return ResponseEntity.badRequest().body(response);
            }

            // Tạo thư mục upload nếu chưa tồn tại
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file duy nhất
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Lưu file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về URL của file
            String fileUrl = "/api/images/" + uniqueFilename;
            
            response.put("success", true);
            response.put("message", "Upload file thành công");
            response.put("fileUrl", fileUrl);
            response.put("fileName", uniqueFilename);
            response.put("originalName", originalFilename);
            response.put("contentType", contentType);
            response.put("size", file.getSize());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Lỗi khi lưu file: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi không xác định: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

