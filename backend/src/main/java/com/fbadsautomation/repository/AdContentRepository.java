package com.fbadsautomation.repository;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.AdContent;
import com.fbadsautomation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdContentRepository extends JpaRepository<AdContent, Long> {
    List<AdContent> findByAd(Ad ad);
    List<AdContent> findByAdOrderByCreatedAtDesc(Ad ad);
    List<AdContent> findByAdOrderByPreviewOrder(Ad ad);
    Optional<AdContent> findByIdAndAd(Long id, Ad ad);
    List<AdContent> findByAdAndIsSelected(Ad ad, Boolean isSelected);
    Optional<AdContent> findByIdAndAdId(Long id, Long adId);
    List<AdContent> findByAdId(Long adId);
    
    // Thêm các phương thức truy vấn theo user để đảm bảo cô lập dữ liệu
    List<AdContent> findByUser(User user);
    List<AdContent> findByUserOrderByCreatedAtDesc(User user);
    Optional<AdContent> findByIdAndUser(Long id, User user);
    List<AdContent> findByAdAndUser(Ad ad, User user);
    List<AdContent> findByAdAndUserOrderByPreviewOrder(Ad ad, User user);
}

