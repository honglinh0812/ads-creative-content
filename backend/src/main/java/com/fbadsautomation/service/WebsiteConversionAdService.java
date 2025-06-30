package com.fbadsautomation.service;

import com.fbadsautomation.model.WebsiteConversionAd;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.repository.WebsiteConversionAdRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class WebsiteConversionAdService {
    
    @Autowired
    private WebsiteConversionAdRepository websiteConversionAdRepository;
    
    @Autowired
    private AdRepository adRepository;
    
    /**
     * Tìm tất cả WebsiteConversionAd theo campaignId
     */
    public List<WebsiteConversionAd> findByCampaignId(Long campaignId) {
        return websiteConversionAdRepository.findByAdCampaignId(campaignId);
    }
    
    /**
     * Tìm WebsiteConversionAd theo adId
     */
    public Optional<WebsiteConversionAd> findByAdId(Long adId) {
        return websiteConversionAdRepository.findByAdId(adId);
    }
    
    /**
     * Tạo mới WebsiteConversionAd
     */
    @Transactional
    public WebsiteConversionAd create(WebsiteConversionAd websiteConversionAd, String username) {
        // Kiểm tra Ad có tồn tại không
        Ad ad = adRepository.findById(websiteConversionAd.getAd().getId())
            .orElseThrow(() -> new ApiException("Ad not found with id: " + websiteConversionAd.getAd().getId()));
        
        // Thiết lập thông tin audit
        LocalDateTime now = LocalDateTime.now();
        websiteConversionAd.setCreatedBy(username);
        websiteConversionAd.setUpdatedBy(username);
        websiteConversionAd.setCreatedDate(now);
        websiteConversionAd.setUpdatedDate(now);
        
        return websiteConversionAdRepository.save(websiteConversionAd);
    }
    
    /**
     * Cập nhật WebsiteConversionAd
     */
    @Transactional
    public WebsiteConversionAd update(Long id, WebsiteConversionAd websiteConversionAdDetails, String username) {
        WebsiteConversionAd websiteConversionAd = websiteConversionAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("WebsiteConversionAd not found with id: " + id));
        
        // Cập nhật thông tin
        websiteConversionAd.setWebsiteUrl(websiteConversionAdDetails.getWebsiteUrl());
        websiteConversionAd.setPixelId(websiteConversionAdDetails.getPixelId());
        websiteConversionAd.setConversionEvents(websiteConversionAdDetails.getConversionEvents());
        
        // Cập nhật thông tin audit
        websiteConversionAd.setUpdatedBy(username);
        websiteConversionAd.setUpdatedDate(LocalDateTime.now());
        
        return websiteConversionAdRepository.save(websiteConversionAd);
    }
    
    /**
     * Xóa WebsiteConversionAd
     */
    @Transactional
    public void delete(Long id) {
        WebsiteConversionAd websiteConversionAd = websiteConversionAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("WebsiteConversionAd not found with id: " + id));
        
        websiteConversionAdRepository.delete(websiteConversionAd);
    }
}
