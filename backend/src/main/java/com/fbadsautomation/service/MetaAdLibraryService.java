package com.fbadsautomation.service;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service

public class MetaAdLibraryService {

    private final ScrapeCreatorsService scrapeCreatorsService;
    
    @Autowired
    public MetaAdLibraryService(ScrapeCreatorsService scrapeCreatorsService) {
        this.scrapeCreatorsService = scrapeCreatorsService;
    }

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
}
