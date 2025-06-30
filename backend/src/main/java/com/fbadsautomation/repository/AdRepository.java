package com.fbadsautomation.repository;

import com.fbadsautomation.model.Ad;
import com.fbadsautomation.model.Campaign;
import com.fbadsautomation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long> {
    List<Ad> findByCampaign(Campaign campaign);
    List<Ad> findByCampaignOrderByCreatedDateDesc(Campaign campaign);
    Optional<Ad> findByIdAndCampaign(Long id, Campaign campaign);
    Optional<Ad> findByIdAndCampaignId(Long id, Long campaignId);
    List<Ad> findByCampaignId(Long campaignId);
    
    // Thêm các phương thức truy vấn theo user để đảm bảo cô lập dữ liệu
    List<Ad> findByUser(User user);
    List<Ad> findByUserOrderByCreatedDateDesc(User user);
    Optional<Ad> findByIdAndUser(Long id, User user);
    List<Ad> findByCampaignAndUser(Campaign campaign, User user);
    
    List<Ad> findTop10ByOrderByCreatedDateDesc();
    List<Ad> findTop10ByUserOrderByCreatedDateDesc(User user);
    
    @Query("SELECT COUNT(a) FROM Ad a WHERE a.campaign.user = :user")
    long countByCampaignUser(@Param("user") User user);
    
    @Query("SELECT COUNT(a) FROM Ad a WHERE a.campaign.user = :user AND a.status = :status")
    long countByCampaignUserAndStatus(@Param("user") User user, @Param("status") String status);
    
    // Thêm phương thức đếm theo user trực tiếp
    long countByUser(User user);
    long countByUserAndStatus(User user, String status);
}

