package com.fbadsautomation.controller;

import com.fbadsautomation.model.PagePostAd;
import com.fbadsautomation.service.PagePostAdService;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/page-post-ads")
public class PagePostAdController {
    
    @Autowired
    private PagePostAdService pagePostAdService;
    
    /**
     * Lấy tất cả PagePostAd theo campaignId
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<PagePostAd>> getAllByCampaignId(@PathVariable Long campaignId) {
        List<PagePostAd> pagePostAds = pagePostAdService.findByCampaignId(campaignId);
        return ResponseEntity.ok(pagePostAds);
    }
    
    /**
     * Lấy PagePostAd theo adId
     */
    @GetMapping("/ad/{adId}")
    public ResponseEntity<PagePostAd> getByAdId(@PathVariable Long adId) {
        return pagePostAdService.findByAdId(adId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ApiException("PagePostAd not found with adId: " + adId));
    }
    
    /**
     * Tạo mới PagePostAd
     */
    @PostMapping
    public ResponseEntity<PagePostAd> create(@RequestBody PagePostAd pagePostAd, Authentication authentication) {
        String username = authentication.getName();
        PagePostAd createdPagePostAd = pagePostAdService.create(pagePostAd, username);
        return ResponseEntity.ok(createdPagePostAd);
    }
    
    /**
     * Cập nhật PagePostAd
     */
    @PutMapping("/{id}")
    public ResponseEntity<PagePostAd> update(@PathVariable Long id, @RequestBody PagePostAd pagePostAdDetails, Authentication authentication) {
        String username = authentication.getName();
        PagePostAd updatedPagePostAd = pagePostAdService.update(id, pagePostAdDetails, username);
        return ResponseEntity.ok(updatedPagePostAd);
    }
    
    /**
     * Xóa PagePostAd
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        pagePostAdService.delete(id);
        return ResponseEntity.ok().build();
    }
}
