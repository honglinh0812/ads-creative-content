package com.fbadsautomation.service;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MetaAdLibraryApiService {

    private static final Logger log = LoggerFactory.getLogger(MetaAdLibraryApiService.class);
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public MetaAdLibraryApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    private static final String META_API_BASE_URL = "https://graph.facebook.com/v18.0";
    /**
     * Extract ad content using Meta Ad Library API
     * @param adId Meta Ad ID from the library URL
     * @param accessToken User's Facebook access token
     * @return Extracted ad content
     */
    public Map<String, Object> extractAdContent(String adId, String accessToken) {
        log.info("Extracting ad content using Meta API for ad ID: {}", adId);
        
        Map<String, Object> result = new HashMap<>();
        try {
            // Call Meta Ad Library API
            String apiUrl = String.format("%s/%s?fields=id,ad_creative,ad_snapshot_url&access_token=%s", 
                META_API_BASE_URL, adId, accessToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> adData = response.getBody();
                return processAdData(adData);
            } else {
                log.error("Meta API returned error: {}", response.getStatusCode());
                result.put("success", false);
                result.put("message", "Không thể truy cập Meta Ad Library API");
                return result;
            }
        } catch (Exception e) {
            log.error("Error calling Meta Ad Library API: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Lỗi khi gọi Meta API: " + e.getMessage());
            return result;
        }
    }

    /**
     * Process ad data from Meta API response
     */
    private Map<String, Object> processAdData(Map<String, Object> adData) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> content = new HashMap<>();
        
        try {
            // Extract ad creative data
            if (adData.containsKey("ad_creative")) {
                Map<String, Object> creative = (Map<String, Object>) adData.get("ad_creative");
                if (creative.containsKey("body")) {
                    content.put("text", (String) creative.get("body"));
                }
                
                if (creative.containsKey("title")) {
                    content.put("title", (String) creative.get("title"));
                }
                
                if (creative.containsKey("image_url")) {
                    content.put("images", (String) creative.get("image_url"));
                }
                
                if (creative.containsKey("video_id")) {
                    content.put("video_id", (String) creative.get("video_id"));
                }
            }
            
            // Extract advertiser info
            if (adData.containsKey("advertiser_name")) {
                content.put("advertiser", (String) adData.get("advertiser_name"));
            }
            
            result.put("success", true);
            result.put("content", content);
            result.put("message", "Đã trích xuất nội dung thành công từ Meta API");
            
        } catch (Exception e) {
            log.error("Error processing ad data: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Lỗi khi xử lý dữ liệu từ Meta API");
        }
        
        return result;
    }

    /**
     * Extract ad ID from Facebook Ads Library URL
     */
    public String extractAdIdFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        // Extract ad ID from URL like: https://www.facebook.com/ads/library/?id = 765596452805624
        String[] parts = url.split("id=");
        if (parts.length > 1) {
            return parts[1].split("&")[0]; // Remove any additional parameters
        }
        
        return null;
    }

    /**
     * Validate if URL is a valid Facebook Ads Library URL
     */
    public boolean isValidAdLibraryUrl(String url) {
        return url != null && url.contains("facebook.com/ads/library") && 
               url.contains("id=");
    }
}
