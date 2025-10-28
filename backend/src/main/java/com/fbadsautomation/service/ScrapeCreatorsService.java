package com.fbadsautomation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ScrapeCreatorsService {

    private static final Logger log = LoggerFactory.getLogger(ScrapeCreatorsService.class);

    private final RestTemplate restTemplate;
    
    @Autowired
    public ScrapeCreatorsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
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

        // Kiểm tra API key với logging rõ ràng
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            log.error("❌ SCRAPE_CREATORS_API_KEY chưa được cấu hình hoặc không hợp lệ");
            log.error("Vui lòng thêm scrape.creators.api.key vào application.properties");
            Map<String, Object> error = new HashMap<>();
            error.put("error", "API_KEY_NOT_CONFIGURED");
            error.put("message", "ScrapeCreators API key chưa được cấu hình");
            error.put("userMessage", "Vui lòng liên hệ admin để cấu hình API key");
            return error;
        }

        try {
            String url = baseUrl + "/facebook/adLibrary/ad?id=" + adId;
            if (getTranscript) {
                url += "&get_transcript=true";
            }

            log.info("📡 Calling ScrapeCreators API: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Validate response có data không
                if (body.containsKey("error") || body.isEmpty()) {
                    log.warn("⚠️ ScrapeCreators API returned error or empty data for ad ID: {}", adId);
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "API_RETURNED_ERROR");
                    error.put("message", body.getOrDefault("error", "API không trả về dữ liệu"));
                    error.put("adId", adId);
                    return error;
                }

                log.info("✅ ScrapeCreators API call successful for ad ID: {}", adId);
                return body;
            } else {
                log.error("❌ ScrapeCreators API returned error status: {}", response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "API_ERROR_STATUS");
                error.put("message", "API returned status: " + response.getStatusCode());
                error.put("statusCode", response.getStatusCode().value());
                return error;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 4xx errors (authentication, not found, etc.)
            log.error("❌ ScrapeCreators API client error for ad ID {}: {} - {}",
                     adId, e.getStatusCode(), e.getResponseBodyAsString());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "API_CLIENT_ERROR");
            error.put("message", "Lỗi từ API: " + e.getMessage());
            error.put("statusCode", e.getStatusCode().value());
            error.put("details", e.getResponseBodyAsString());
            return error;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 5xx errors (server error)
            log.error("❌ ScrapeCreators API server error for ad ID {}: {}", adId, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "API_SERVER_ERROR");
            error.put("message", "API server đang gặp sự cố");
            error.put("statusCode", e.getStatusCode().value());
            return error;
        } catch (Exception e) {
            log.error("❌ Error calling ScrapeCreators API for ad ID {}: {}", adId, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NETWORK_ERROR");
            error.put("message", "Không thể kết nối tới API: " + e.getMessage());
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