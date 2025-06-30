package com.fbadsautomation.service;

import com.fbadsautomation.model.PagePostAd;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.repository.PagePostAdRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class PagePostAdService {
    
    @Autowired
    private PagePostAdRepository pagePostAdRepository;
    
    @Autowired
    private AdRepository adRepository;
    
    /**
     * Tìm tất cả PagePostAd theo campaignId
     */
    public List<PagePostAd> findByCampaignId(Long campaignId) {
        return pagePostAdRepository.findByAdCampaignId(campaignId);
    }
    
    /**
     * Tìm PagePostAd theo adId
     */
    public Optional<PagePostAd> findByAdId(Long adId) {
        return pagePostAdRepository.findByAdId(adId);
    }
    
    /**
     * Tạo mới PagePostAd
     */
    @Transactional
    public PagePostAd create(PagePostAd pagePostAd, String username) {
        // Kiểm tra Ad có tồn tại không
        Ad ad = adRepository.findById(pagePostAd.getAd().getId())
            .orElseThrow(() -> new ApiException("Ad not found with id: " + pagePostAd.getAd().getId()));
        
        // Thiết lập thông tin audit
        LocalDateTime now = LocalDateTime.now();
        pagePostAd.setCreatedBy(username);
        pagePostAd.setUpdatedBy(username);
        pagePostAd.setCreatedDate(now);
        pagePostAd.setUpdatedDate(now);
        
        return pagePostAdRepository.save(pagePostAd);
    }
    
    /**
     * Cập nhật PagePostAd
     */
    @Transactional
    public PagePostAd update(Long id, PagePostAd pagePostAdDetails, String username) {
        PagePostAd pagePostAd = pagePostAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("PagePostAd not found with id: " + id));
        
        // Cập nhật thông tin
        pagePostAd.setPageId(pagePostAdDetails.getPageId());
        pagePostAd.setPostType(pagePostAdDetails.getPostType());
        pagePostAd.setPostMessage(pagePostAdDetails.getPostMessage());
        pagePostAd.setLinkUrl(pagePostAdDetails.getLinkUrl());
        
        // Cập nhật thông tin audit
        pagePostAd.setUpdatedBy(username);
        pagePostAd.setUpdatedDate(LocalDateTime.now());
        
        return pagePostAdRepository.save(pagePostAd);
    }
    
    /**
     * Xóa PagePostAd
     */
    @Transactional
    public void delete(Long id) {
        PagePostAd pagePostAd = pagePostAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("PagePostAd not found with id: " + id));
        
        pagePostAdRepository.delete(pagePostAd);
    }
}
