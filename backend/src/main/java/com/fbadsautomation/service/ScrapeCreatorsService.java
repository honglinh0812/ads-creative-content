package com.fbadsautomation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor

public class ScrapeCreatorsService {

    private final RestTemplate restTemplate;
    
    @Value("${scrape.creators.api.key:your-api-key-here}")
    private String apiKey;
    
    @Value("${scrape.creators.api.base.url:https://api.scrapecreators.com/v1}")
    private String baseUrl;

    /**
     * Gọi ScrapeCreators API để lấy thông tin quảng cáo Facebook
     * @param adId Facebook Ad ID
     * @param getTranscript Có lấy transcript hay không
     * @return Kết quả từ API
     */
    public Map<String, Object> scrapeCreators(String adId, boolean getTranscript) {
        log.info("Calling ScrapeCreators API for ad ID: {}", adId);
        
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SCRAPE_CREATORS_API_KEY chưa được cấu hình");
            return error;
        }
        
        try {
            String url = baseUrl + "/facebook/adLibrary/ad?id=" + adId;
            if (getTranscript) {
                url += "&get_transcript=true";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("ScrapeCreators API call successful for ad ID: {}", adId);
                return response.getBody();
            } else {
                log.error("ScrapeCreators API returned error status: {}", response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "API returned status: " + response.getStatusCode());
                return error;
            }
        } catch (Exception e) {
            log.error("Error calling ScrapeCreators API for ad ID {}: {}", adId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Lỗi gọi ScrapeCreators API: " + e.getMessage());
            return error;
        }
    }

    /**
     * Gọi ScrapeCreators API cho nhiều ad IDs
     * @param adIds Danh sách Facebook Ad IDs
     * @param getTranscript Có lấy transcript hay không
     * @return Danh sách kết quả
     */
    public List<Map<String, Object>> scrapeCreatorsBatch(List<String> adIds, boolean getTranscript) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (String adId : adIds) {
            Map<String, Object> result = scrapeCreators(adId, getTranscript);
            results.add(result);
            
            // Thêm delay nhỏ giữa các requests để tránh rate limiting
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return results;
    }
} 