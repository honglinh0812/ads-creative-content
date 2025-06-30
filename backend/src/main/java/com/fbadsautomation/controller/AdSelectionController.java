package com.fbadsautomation.controller;

import com.fbadsautomation.model.AdContent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ads")
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi nguồn trong môi trường dev
public class AdSelectionController {

    @PostMapping("/save-selected")
    public ResponseEntity<String> saveSelectedAd(@RequestBody AdContent adContent) {
        // Trong demo này, chúng ta chỉ giả lập việc lưu quảng cáo đã chọn
        // Trong thực tế, bạn sẽ lưu vào database và có thể tích hợp với Facebook API
        
        return ResponseEntity.ok("Quảng cáo đã được lưu thành công");
    }
}
