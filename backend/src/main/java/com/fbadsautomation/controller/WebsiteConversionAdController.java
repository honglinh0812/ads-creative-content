package com.fbadsautomation.controller;

import com.fbadsautomation.model.WebsiteConversionAd;
import com.fbadsautomation.service.WebsiteConversionAdService;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/website-conversion-ads")
public class WebsiteConversionAdController {
    
    @Autowired
    private WebsiteConversionAdService websiteConversionAdService;
    
    /**
     * Lấy tất cả WebsiteConversionAd theo campaignId
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<WebsiteConversionAd>> getAllByCampaignId(@PathVariable Long campaignId) {
        List<WebsiteConversionAd> websiteConversionAds = websiteConversionAdService.findByCampaignId(campaignId);
        return ResponseEntity.ok(websiteConversionAds);
    }
    
    /**
     * Lấy WebsiteConversionAd theo adId
     */
    @GetMapping("/ad/{adId}")
    public ResponseEntity<WebsiteConversionAd> getByAdId(@PathVariable Long adId) {
        return websiteConversionAdService.findByAdId(adId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ApiException("WebsiteConversionAd not found with adId: " + adId));
    }
    
    /**
     * Tạo mới WebsiteConversionAd
     */
    @PostMapping
    public ResponseEntity<WebsiteConversionAd> create(@RequestBody WebsiteConversionAd websiteConversionAd, Authentication authentication) {
        String username = authentication.getName();
        WebsiteConversionAd createdWebsiteConversionAd = websiteConversionAdService.create(websiteConversionAd, username);
        return ResponseEntity.ok(createdWebsiteConversionAd);
    }
    
    /**
     * Cập nhật WebsiteConversionAd
     */
    @PutMapping("/{id}")
    public ResponseEntity<WebsiteConversionAd> update(@PathVariable Long id, @RequestBody WebsiteConversionAd websiteConversionAdDetails, Authentication authentication) {
        String username = authentication.getName();
        WebsiteConversionAd updatedWebsiteConversionAd = websiteConversionAdService.update(id, websiteConversionAdDetails, username);
        return ResponseEntity.ok(updatedWebsiteConversionAd);
    }
    
    /**
     * Xóa WebsiteConversionAd
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        websiteConversionAdService.delete(id);
        return ResponseEntity.ok().build();
    }
}
