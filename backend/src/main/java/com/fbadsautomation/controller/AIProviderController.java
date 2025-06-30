package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.service.AIProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-providers")
@CrossOrigin(origins = "*")
public class AIProviderController {

    @Autowired
    private AIProviderService aiProviderService;

    @GetMapping("/text")
    public ResponseEntity<List<ProviderResponse>> getTextProviders() {
        List<ProviderResponse> providers = aiProviderService.getTextProviders();
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/image")
    public ResponseEntity<List<ProviderResponse>> getImageProviders() {
        List<ProviderResponse> providers = aiProviderService.getImageProviders();
        return ResponseEntity.ok(providers);
    }
}

