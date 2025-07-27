package com.fbadsautomation.controller;

import com.fbadsautomation.service.ScrapeCreatorsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/scrape-creators")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")

public class ScrapeCreatorsController {

    private final ScrapeCreatorsService scrapeCreatorsService;

    /**
     * Scrape single ad from ScrapeCreators API
     */
    @GetMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapeAd(
            @RequestParam String id,
            @RequestParam(defaultValue = "false") boolean getTranscript) {
        
        log.info("Scraping ad with ID: {}, getTranscript: {}", id, getTranscript);
        
        try {
            Map<String, Object> result = scrapeCreatorsService.scrapeCreators(id, getTranscript);
            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error scraping ad with ID {}: {}", id, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi scrape ad: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Scrape multiple ads from ScrapeCreators API
     */
    @PostMapping("/scrape-batch")
    public ResponseEntity<List<Map<String, Object>>> scrapeAdsBatch(
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<String> adIds = (List<String>) request.get("adIds");
        Boolean getTranscript = (Boolean) request.getOrDefault("getTranscript", false);
        
        log.info("Scraping batch ads: {}, getTranscript: {}", adIds, getTranscript);
        
        try {
            List<Map<String, Object>> results = scrapeCreatorsService.scrapeCreatorsBatch(adIds, getTranscript);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("Error scraping batch ads: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi khi scrape batch ads: " + e.getMessage());
            return ResponseEntity.internalServerError().body(List.of(error));
        }
    }
} 