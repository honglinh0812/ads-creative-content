package com.fbadsautomation.controller;

import com.fbadsautomation.service.ScrapeCreatorsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scrape-creators")
@CrossOrigin(origins = "*")
@Tag(name = "Scrape Creators", description = "API endpoints for scraping ad content from external sources")
public class ScrapeCreatorsController {

    private static final Logger log = LoggerFactory.getLogger(ScrapeCreatorsController.class);
    private final ScrapeCreatorsService scrapeCreatorsService;
    
    @Autowired
    public ScrapeCreatorsController(ScrapeCreatorsService scrapeCreatorsService) {
        this.scrapeCreatorsService = scrapeCreatorsService;
    }

    @Operation(summary = "Scrape single ad", description = "Scrape ad content from ScrapeCreators API by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ad scraped successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or scraping error"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapeAd(
            @Parameter(description = "Ad ID to scrape") @RequestParam String id,
            @Parameter(description = "Whether to get transcript") @RequestParam(defaultValue = "false") boolean getTranscript) {
        
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

    @Operation(summary = "Scrape multiple ads", description = "Scrape multiple ad contents from ScrapeCreators API in batch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ads scraped successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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