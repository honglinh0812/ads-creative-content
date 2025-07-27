package com.fbadsautomation.controller;

import com.fbadsautomation.dto.ProviderResponse;
import com.fbadsautomation.model.FacebookCTA;
import com.fbadsautomation.service.AIProviderService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    
    @GetMapping("/call-to-actions")
    public ResponseEntity<List<Map<String, String>>> getCallToActions(@RequestParam(defaultValue = "en") String language) {
        List<Map<String, String>> ctas = java.util.Arrays.stream(FacebookCTA.values())
            .map(cta -> Map.of(
                "value", cta.getValue(),
                "label", cta.getLabel(language)
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ctas);
    }
}
