package com.fbadsautomation.service;

import java.net.URLEncoder;
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

    /**
     * Search for ads by company name (Direct approach)
     *
     * @param companyName Brand/company name to search
     * @param country ISO country code (US, UK, VN, etc.)
     * @param limit Maximum number of ads to return
     * @return Map containing ads and pagination info
     */
    public Map<String, Object> searchAdsByCompanyName(String companyName, String country, int limit) {
        log.info("Searching ads for company: {}, country: {}, limit: {}", companyName, country, limit);

        // API key validation
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "API_KEY_NOT_CONFIGURED");
            error.put("message", "ScrapeCreators API key not configured");
            return error;
        }

        try {
            // Build URL
            String url = String.format(
                "%s/facebook/adLibrary/company/ads?companyName=%s&country=%s&trim=true",
                baseUrl,
                URLEncoder.encode(companyName, "UTF-8"),
                country
            );

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Accept", "application/json");

            // Make request
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // Extract ads array and limit results
                if (body.containsKey("ads")) {
                    List<Object> ads = (List<Object>) body.get("ads");
                    if (ads.size() > limit) {
                        body.put("ads", ads.subList(0, limit));
                    }
                }

                log.info("Successfully fetched ads for company: {}", companyName);
                return body;
            } else {
                log.error("API returned error status: {}", response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "API_ERROR_STATUS");
                error.put("statusCode", response.getStatusCode().value());
                return error;
            }

        } catch (Exception e) {
            log.error("Error searching company ads: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NETWORK_ERROR");
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * Two-step search: Find companies then get their ads
     * More accurate for exact brand matching
     *
     * @param brandName Brand name to search
     * @param country Country code
     * @param limit Max ads to return
     * @return Map containing ads data
     */
    public Map<String, Object> searchAdsByBrandTwoStep(String brandName, String country, int limit) {
        log.info("Two-step search for brand: {}", brandName);

        // API key validation
        if (apiKey == null || apiKey.isEmpty() || "your-api-key-here".equals(apiKey)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "API_KEY_NOT_CONFIGURED");
            error.put("message", "ScrapeCreators API key not configured");
            return error;
        }

        try {
            // Step 1: Search for company/page
            String searchUrl = String.format(
                "%s/facebook/adLibrary/search/companies?query=%s",
                baseUrl,
                URLEncoder.encode(brandName, "UTF-8")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, Map.class);

            if (!searchResponse.getStatusCode().is2xxSuccessful() || searchResponse.getBody() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "COMPANY_SEARCH_FAILED");
                return error;
            }

            Map<String, Object> searchBody = searchResponse.getBody();

            // Extract first matching page ID
            List<Map<String, Object>> companies = (List<Map<String, Object>>) searchBody.get("companies");

            if (companies == null || companies.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "NO_COMPANY_FOUND");
                error.put("message", "No company found with name: " + brandName);
                return error;
            }

            String pageId = String.valueOf(companies.get(0).get("pageId"));

            // Step 2: Get ads for this page
            String adsUrl = String.format(
                "%s/facebook/adLibrary/company/ads?pageId=%s&country=%s&trim=true",
                baseUrl, pageId, country
            );

            ResponseEntity<Map> adsResponse = restTemplate.exchange(adsUrl, HttpMethod.GET, entity, Map.class);

            if (adsResponse.getStatusCode().is2xxSuccessful() && adsResponse.getBody() != null) {
                Map<String, Object> adsBody = adsResponse.getBody();

                // Limit results
                if (adsBody.containsKey("ads")) {
                    List<Object> ads = (List<Object>) adsBody.get("ads");
                    if (ads.size() > limit) {
                        adsBody.put("ads", ads.subList(0, limit));
                    }
                }

                return adsBody;
            }

            Map<String, Object> error = new HashMap<>();
            error.put("error", "ADS_FETCH_FAILED");
            return error;

        } catch (Exception e) {
            log.error("Error in two-step search: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SEARCH_ERROR");
            error.put("message", e.getMessage());
            return error;
        }
    }
}