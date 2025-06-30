package com.fbadsautomation.controller;

import com.fbadsautomation.model.LeadFormAd;
import com.fbadsautomation.service.LeadFormAdService;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/lead-form-ads")
public class LeadFormAdController {
    
    @Autowired
    private LeadFormAdService leadFormAdService;
    
    /**
     * Lấy tất cả LeadFormAd theo campaignId
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<LeadFormAd>> getAllByCampaignId(@PathVariable Long campaignId) {
        List<LeadFormAd> leadFormAds = leadFormAdService.findByCampaignId(campaignId);
        return ResponseEntity.ok(leadFormAds);
    }
    
    /**
     * Lấy LeadFormAd theo adId
     */
    @GetMapping("/ad/{adId}")
    public ResponseEntity<LeadFormAd> getByAdId(@PathVariable Long adId) {
        return leadFormAdService.findByAdId(adId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ApiException("LeadFormAd not found with adId: " + adId));
    }
    
    /**
     * Tạo mới LeadFormAd
     */
    @PostMapping
    public ResponseEntity<LeadFormAd> create(@RequestBody LeadFormAd leadFormAd, Authentication authentication) {
        String username = authentication.getName();
        LeadFormAd createdLeadFormAd = leadFormAdService.create(leadFormAd, username);
        return ResponseEntity.ok(createdLeadFormAd);
    }
    
    /**
     * Cập nhật LeadFormAd
     */
    @PutMapping("/{id}")
    public ResponseEntity<LeadFormAd> update(@PathVariable Long id, @RequestBody LeadFormAd leadFormAdDetails, Authentication authentication) {
        String username = authentication.getName();
        LeadFormAd updatedLeadFormAd = leadFormAdService.update(id, leadFormAdDetails, username);
        return ResponseEntity.ok(updatedLeadFormAd);
    }
    
    /**
     * Xóa LeadFormAd
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        leadFormAdService.delete(id);
        return ResponseEntity.ok().build();
    }
}
