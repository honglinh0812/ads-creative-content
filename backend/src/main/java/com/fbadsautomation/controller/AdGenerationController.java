package com.fbadsautomation.controller;

import com.fbadsautomation.model.AdRequest;
import com.fbadsautomation.model.AdResponse;
import com.fbadsautomation.service.AdGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ads")
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi nguồn trong môi trường dev
public class AdGenerationController {

    private final AdGenerationService adGenerationService;

    public AdGenerationController(AdGenerationService adGenerationService) {
        this.adGenerationService = adGenerationService;
    }
    /*
    @PostMapping("/generate")
    public ResponseEntity<AdResponse> generateAd(@RequestBody AdRequest request) {
        AdResponse response = adGenerationService.generateAd(request);
        return ResponseEntity.ok(response);
    }
    */
}
