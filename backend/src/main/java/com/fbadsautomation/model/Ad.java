package com.fbadsautomation.model;

import com.fbadsautomation.model.FacebookCTA;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "ads")
@ToString(exclude = {"campaign", "user"}) // Includes getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class Ad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type")
    private AdType adType;
    
    // Fields apparently used by AdService/AdContentService in original code
    @Column(length = 500) // Increased from 255 to accommodate AI-generated content
    private String headline;
    
    @Column(name = "primary_text", length = 2000) // Increased from 1000 to accommodate AI-generated content
    private String primaryText;
    
    @Column(length = 1000) // Increased from 500 to accommodate AI-generated content
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_to_action", length = 50)
    private FacebookCTA callToAction;

    @Column(name = "image_url", length = 1000) // Increased to accommodate longer file paths
    private String imageUrl;

    @Column(name = "video_url", length = 1000) // Added to support video content
    private String videoUrl;
    
    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "media_file_path", length = 1000) // Increased to accommodate longer file paths
    private String mediaFilePath;
    
    @Column(length = 500) // Increased to accommodate longer ad names
    private String name;
    
    @Column(name = "selected_content_id")
    private Long selectedContentId;
    
    private String status;
    
    // Audit fields
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @Column(name = "created_date")
    private java.time.LocalDateTime createdDate;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    // Getter & Setter cho tất cả các trường
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public AdType getAdType() { return adType; }
    public void setAdType(AdType adType) { this.adType = adType; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getPrimaryText() { return primaryText; }
    public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FacebookCTA getCallToAction() { return callToAction; }
    public void setCallToAction(FacebookCTA callToAction) { this.callToAction = callToAction; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getMediaFilePath() { return mediaFilePath; }
    public void setMediaFilePath(String mediaFilePath) { this.mediaFilePath = mediaFilePath; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getSelectedContentId() { return selectedContentId; }
    public void setSelectedContentId(Long selectedContentId) { this.selectedContentId = selectedContentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public java.time.LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(java.time.LocalDateTime createdDate) { this.createdDate = createdDate; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
