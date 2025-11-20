package com.fbadsautomation.service;

import com.fbadsautomation.dto.ReferenceAdData;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaAdLibraryService {

    private final ScrapeCreatorsService scrapeCreatorsService;
    private final RestTemplate restTemplate;

    @Value("${meta.api.base-url:https://graph.facebook.com/v24.0}")
    private String metaApiBaseUrl;

    /**
     * Hàm này gọi trực tiếp ScrapeCreators API thông qua ScrapeCreatorsService
     * để lấy dữ liệu quảng cáo Facebook.
     * Kết quả trả về là Map chứa thông tin quảng cáo (body, images, ...)
     */
    public List<Map<String, Object>> extractAdsByAdIds(List<String> adLinks) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> adIds = new ArrayList<>(); // Extract ad IDs từ URLs
        for (String adLink : adLinks) {
            String adId = extractAdIdFromUrl(adLink);
            if (adId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Không tìm được ad_id từ link: " + adLink);
                results.add(error);
            } else {
                adIds.add(adId);
            }
        }
            
        // Gọi ScrapeCreators API cho tất cả ad IDs
        if (!adIds.isEmpty()) {
            List<Map<String, Object>> apiResults = scrapeCreatorsService.scrapeCreatorsBatch(adIds, false);
            results.addAll(apiResults);
        }
        
        return results;
    }

    /**
     * Hàm này parse kết quả từ ScrapeCreators API, chỉ lấy text (body) và images.
     * Trả về List object chuẩn cho frontend/backend sử dụng.
     */
    public List<Map<String, Object>> extractAdTextAndImages(List<String> adLinks) {
        List<Map<String, Object>> rawResults = extractAdsByAdIds(adLinks);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> ad : rawResults) {
            Map<String, Object> item = new HashMap<>(); // Lấy text quảng cáo
            String text = null;
            if (ad.containsKey("body")) {
                text = String.valueOf(ad.get("body"));
            } else if (ad.containsKey("snapshot")) {
                // Một số response có thể nằm trong snapshot.body
                Object snapshot = ad.get("snapshot");
                if (snapshot instanceof Map) {
                    Object body = ((Map<?, ?>) snapshot).get("body");
                    if (body != null) text = String.valueOf(body);
                }
            }
            item.put("text", text);
            // Lấy danh sách image URLs
            List<String> images = new ArrayList<>();
            if (ad.containsKey("images")) {
                Object imgs = ad.get("images");
                if (imgs instanceof List) {
                    for (Object img : (List<?>) imgs) {
                        images.add(String.valueOf(img));
                    }
                }
            } else if (ad.containsKey("snapshot")) {
                Object snapshot = ad.get("snapshot");
                if (snapshot instanceof Map) {
                    Object imgs = ((Map<?, ?>) snapshot).get("images");
                    if (imgs instanceof List) {
                        for (Object img : (List<?>) imgs) {
                            images.add(String.valueOf(img));
                        }
                    }
                }
            }
            item.put("images", images);
            result.add(item);
        }
        return result;
    }
    
    /**
     * Extract ad_id từ URL Facebook Ad Library
     */
    public String extractAdIdFromUrl(String url) {
        try {
            // Ví dụ: https://www.facebook.com/ads/library/?id= 702369045530963
            int idx = url.indexOf("id=");
            if (idx == -1) return null;
            String idPart = url.substring(idx + 3);
            int ampIdx = idPart.indexOf('&');
            if (ampIdx != -1) {
                idPart = idPart.substring(0, ampIdx);
            }
            return idPart;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isValidAdLibraryUrl(String url) {
        return url != null && url.contains("facebook.com/ads/library") && url.contains("id=");
    }

    /**
     * Gọi trực tiếp Meta Ad Library API khi có access token hợp lệ (fallback).
     */
    public Map<String, Object> extractOfficialAdContent(String adId, String accessToken) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(accessToken)) {
            result.put("success", false);
            result.put("message", "Access token is required for official Meta API.");
            return result;
        }
        try {
            String apiUrl = String.format("%s/%s?fields=id,ad_creative,ad_snapshot_url,advertiser_name&access_token=%s",
                    metaApiBaseUrl, adId, accessToken);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> adData = response.getBody();
                ReferenceAdData reference = buildReferenceAdData(adData);
                result.put("success", true);
                result.put("content", adData);
                result.put("referenceAdData", reference);
                return result;
            } else {
                log.error("Meta API returned error status {}", response.getStatusCode());
                result.put("success", false);
                result.put("message", "Meta API returned status: " + response.getStatusCode());
                return result;
            }
        } catch (Exception ex) {
            log.error("Error calling Meta API: {}", ex.getMessage(), ex);
            result.put("success", false);
            result.put("message", "Error calling Meta API: " + ex.getMessage());
            return result;
        }
    }

    public ReferenceAdData buildReferenceAdData(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> creative = (Map<String, Object>) metadata.get("ad_creative");
            if (creative == null) {
                return null;
            }
            String body = asString(creative.get("body"));
            String title = asString(creative.get("title"));
            String description = asString(creative.get("description"));
            String cta = asString(creative.get("call_to_action_type"));
            String image = asString(creative.get("image_url"));
            return ReferenceAdData.builder()
                    .primaryText(body)
                    .headline(title)
                    .description(description)
                    .callToAction(cta)
                    .mediaUrl(image)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse reference ad data: {}", e.getMessage());
            return null;
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    public String summarizeReferenceAd(ReferenceAdData data, Map<String, Object> metadata) {
        if (data == null && (metadata == null || metadata.isEmpty())) {
            return "";
        }

        if (data == null) {
            return metadata.toString();
        }

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(data.getHeadline())) {
            builder.append("Headline: ").append(data.getHeadline()).append('\n');
        }
        if (StringUtils.hasText(data.getPrimaryText())) {
            builder.append("Primary Text: ").append(data.getPrimaryText()).append('\n');
        }
        if (StringUtils.hasText(data.getDescription())) {
            builder.append("Description: ").append(data.getDescription()).append('\n');
        }
        if (StringUtils.hasText(data.getCallToAction())) {
            builder.append("CTA: ").append(data.getCallToAction()).append('\n');
        }
        if (StringUtils.hasText(data.getMediaUrl())) {
            builder.append("Media URL: ").append(data.getMediaUrl()).append('\n');
        }
        return builder.toString();
    }
}
