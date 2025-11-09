package com.fbadsautomation.model;

import com.fbadsautomation.model.FacebookCTA;
import java.time.LocalDateTime;
import javax.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ad_contents")
public class AdContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType contentType;
    
    @Column(name = "primary_text", length = 10000) // Increased to accommodate longer AI-generated content
    private String primaryText;
    
    @Column(name = "headline", length = 500) // Increased from 255 to accommodate AI-generated content
    private String headline;
    
    @Column(name = "description", length = 1000) // Increased from 500 to accommodate AI-generated content
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "call_to_action", length = 50)
    private FacebookCTA callToAction;
   
    public FacebookCTA getCta() {
        return callToAction;
    }

    public void setCta(FacebookCTA callToAction) {
        this.callToAction = callToAction;
    }

    @Column(name = "image_url", length = 1000) // Increased to accommodate longer file paths
    private String imageUrl;
    
    @Column(name = "is_selected")
    private Boolean isSelected;
    
    @Column(name = "preview_order")
    private Integer previewOrder;
    
    @Column(name = "ai_provider")
    @Enumerated(EnumType.STRING)
    private AIProvider aiProvider;

    @Column(name = "quality_score")
    private Integer qualityScore; // 0-100 quality score

    @Column(name = "validation_warnings", length = 2000)
    private String validationWarnings; // JSON array or comma-separated warnings

    @Column(name = "has_warnings")
    private Boolean hasWarnings; // Flag for quick filtering

    @Column(name = "needs_review")
    private Boolean needsReview = false; // Flag for content truncated to meet Facebook limits

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Explicit getters/setters to avoid reliance on Lombok during processing issues
    public void setAd(Ad ad) { this.ad = ad; }
    public Ad getAd() { return ad; }
    public void setUser(User user) { this.user = user; }
    public User getUser() { return user; }
    public void setPreviewOrder(Integer previewOrder) { this.previewOrder = previewOrder; }
    public Integer getPreviewOrder() { return previewOrder; }
    public void setIsSelected(Boolean isSelected) { this.isSelected = isSelected; }
    public Boolean getIsSelected() { return isSelected; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getHeadline() { return headline; }
    public void setPrimaryText(String primaryText) { this.primaryText = primaryText; }
    public String getPrimaryText() { return primaryText; }
    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }
    public void setCallToAction(FacebookCTA callToAction) { this.callToAction = callToAction; }
    public FacebookCTA getCallToAction() { return callToAction; }
    public void setAiProvider(AIProvider aiProvider) { this.aiProvider = aiProvider; }
    public AIProvider getAiProvider() { return aiProvider; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public String getValidationWarnings() { return validationWarnings; }
    public void setValidationWarnings(String validationWarnings) { this.validationWarnings = validationWarnings; }
    public Boolean getHasWarnings() { return hasWarnings; }
    public void setHasWarnings(Boolean hasWarnings) { this.hasWarnings = hasWarnings; }
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Constructors
    public AdContent() {
    }

    public AdContent(Long id, ContentType contentType, String primaryText, String headline,
                    String description, FacebookCTA callToAction, String imageUrl, Boolean isSelected,
                    Integer previewOrder, AIProvider aiProvider, Integer qualityScore,
                    String validationWarnings, Boolean hasWarnings, Ad ad, User user,
                    LocalDateTime createdDate, LocalDateTime updatedAt) {
        this.id = id;
        this.contentType = contentType;
        this.primaryText = primaryText;
        this.headline = headline;
        this.description = description;
        this.callToAction = callToAction;
        this.imageUrl = imageUrl;
        this.isSelected = isSelected;
        this.previewOrder = previewOrder;
        this.aiProvider = aiProvider;
        this.qualityScore = qualityScore;
        this.validationWarnings = validationWarnings;
        this.hasWarnings = hasWarnings;
        this.ad = ad;
        this.user = user;
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static AdContentBuilder builder() {
        return new AdContentBuilder();
    }

    public static class AdContentBuilder {
        private Long id;
        private ContentType contentType;
        private String primaryText;
        private String headline;
        private String description;
        private FacebookCTA callToAction;
        private String imageUrl;
        private Boolean isSelected;
        private Integer previewOrder;
        private AIProvider aiProvider;
        private Integer qualityScore;
        private String validationWarnings;
        private Boolean hasWarnings;
        private Ad ad;
        private User user;
        private LocalDateTime createdDate;
        private LocalDateTime updatedAt;

        public AdContentBuilder id(Long id) { this.id = id; return this; }
        public AdContentBuilder contentType(ContentType contentType) { this.contentType = contentType; return this; }
        public AdContentBuilder primaryText(String primaryText) { this.primaryText = primaryText; return this; }
        public AdContentBuilder headline(String headline) { this.headline = headline; return this; }
        public AdContentBuilder description(String description) { this.description = description; return this; }
        public AdContentBuilder callToAction(FacebookCTA callToAction) { this.callToAction = callToAction; return this; }
        public AdContentBuilder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public AdContentBuilder isSelected(Boolean isSelected) { this.isSelected = isSelected; return this; }
        public AdContentBuilder previewOrder(Integer previewOrder) { this.previewOrder = previewOrder; return this; }
        public AdContentBuilder aiProvider(AIProvider aiProvider) { this.aiProvider = aiProvider; return this; }
        public AdContentBuilder qualityScore(Integer qualityScore) { this.qualityScore = qualityScore; return this; }
        public AdContentBuilder validationWarnings(String validationWarnings) { this.validationWarnings = validationWarnings; return this; }
        public AdContentBuilder hasWarnings(Boolean hasWarnings) { this.hasWarnings = hasWarnings; return this; }
        public AdContentBuilder ad(Ad ad) { this.ad = ad; return this; }
        public AdContentBuilder user(User user) { this.user = user; return this; }
        public AdContentBuilder createdDate(LocalDateTime createdDate) { this.createdDate = createdDate; return this; }
        public AdContentBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AdContent build() {
            return new AdContent(id, contentType, primaryText, headline, description, callToAction,
                               imageUrl, isSelected, previewOrder, aiProvider, qualityScore,
                               validationWarnings, hasWarnings, ad, user, createdDate, updatedAt);
        }
    }

    public enum ContentType {
        TEXT, IMAGE, COMBINED, PAGE_POST;
    }
    
    // Added HUGGINGFACE based on provider implementation
    public enum AIProvider {
        OPENAI, GEMINI, HUGGINGFACE, ANTHROPIC, FAL_AI, STABLE_DIFFUSION, MOCK, FALLBACK;
    }
}
