package com.fbadsautomation.service;

import com.fbadsautomation.model.LeadFormAd;
import com.fbadsautomation.model.LeadFormField;
import com.fbadsautomation.model.Ad;
import com.fbadsautomation.repository.LeadFormAdRepository;
import com.fbadsautomation.repository.LeadFormFieldRepository;
import com.fbadsautomation.repository.AdRepository;
import com.fbadsautomation.exception.ApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class LeadFormAdService {
    
    @Autowired
    private LeadFormAdRepository leadFormAdRepository;
    
    @Autowired
    private LeadFormFieldRepository leadFormFieldRepository;
    
    @Autowired
    private AdRepository adRepository;
    
    /**
     * Tìm tất cả LeadFormAd theo campaignId
     */
    public List<LeadFormAd> findByCampaignId(Long campaignId) {
        return leadFormAdRepository.findByAdCampaignId(campaignId);
    }
    
    /**
     * Tìm LeadFormAd theo adId
     */
    public Optional<LeadFormAd> findByAdId(Long adId) {
        return leadFormAdRepository.findByAdId(adId);
    }
    
    /**
     * Tạo mới LeadFormAd
     */
    @Transactional
    public LeadFormAd create(LeadFormAd leadFormAd, String username) {
        // Kiểm tra Ad có tồn tại không
        Ad ad = adRepository.findById(leadFormAd.getAd().getId())
            .orElseThrow(() -> new ApiException("Ad not found with id: " + leadFormAd.getAd().getId()));
        
        // Thiết lập thông tin audit
        LocalDateTime now = LocalDateTime.now();
        leadFormAd.setCreatedBy(username);
        leadFormAd.setUpdatedBy(username);
        leadFormAd.setCreatedDate(now);
        leadFormAd.setUpdatedDate(now);
        
        // Lưu LeadFormAd trước
        LeadFormAd savedLeadFormAd = leadFormAdRepository.save(leadFormAd);
        
        // Lưu các trường form
        if (leadFormAd.getFields() != null && !leadFormAd.getFields().isEmpty()) {
            for (LeadFormField field : leadFormAd.getFields()) {
                field.setLeadFormAd(savedLeadFormAd);
                field.setCreatedBy(username);
                field.setUpdatedBy(username);
                field.setCreatedDate(now);
                field.setUpdatedDate(now);
            }
            leadFormFieldRepository.saveAll(leadFormAd.getFields());
        }
        
        return savedLeadFormAd;
    }
    
    /**
     * Cập nhật LeadFormAd
     */
    @Transactional
    public LeadFormAd update(Long id, LeadFormAd leadFormAdDetails, String username) {
        LeadFormAd leadFormAd = leadFormAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("LeadFormAd not found with id: " + id));
        
        // Cập nhật thông tin
        leadFormAd.setFormName(leadFormAdDetails.getFormName());
        leadFormAd.setPrivacyPolicyUrl(leadFormAdDetails.getPrivacyPolicyUrl());
        leadFormAd.setThanksMessage(leadFormAdDetails.getThanksMessage());
        leadFormAd.setFbFormId(leadFormAdDetails.getFbFormId());
        
        // Cập nhật thông tin audit
        leadFormAd.setUpdatedBy(username);
        leadFormAd.setUpdatedDate(LocalDateTime.now());
        
        // Cập nhật các trường form nếu có
        if (leadFormAdDetails.getFields() != null) {
            // Xóa các trường cũ
            leadFormFieldRepository.deleteAll(leadFormAd.getFields());
            leadFormAd.getFields().clear();
            
            // Thêm các trường mới
            LocalDateTime now = LocalDateTime.now();
            for (LeadFormField field : leadFormAdDetails.getFields()) {
                field.setLeadFormAd(leadFormAd);
                field.setCreatedBy(username);
                field.setUpdatedBy(username);
                field.setCreatedDate(now);
                field.setUpdatedDate(now);
                leadFormAd.getFields().add(field);
            }
        }
        
        return leadFormAdRepository.save(leadFormAd);
    }
    
    /**
     * Xóa LeadFormAd
     */
    @Transactional
    public void delete(Long id) {
        LeadFormAd leadFormAd = leadFormAdRepository.findById(id)
            .orElseThrow(() -> new ApiException("LeadFormAd not found with id: " + id));
        
        // Xóa các trường form trước
        leadFormFieldRepository.deleteAll(leadFormAd.getFields());
        
        // Xóa LeadFormAd
        leadFormAdRepository.delete(leadFormAd);
    }
}
