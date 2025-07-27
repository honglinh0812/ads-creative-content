package com.fbadsautomation.model;

import com.fbadsautomation.model.FacebookCTA;
import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ad_contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j

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
    
    public enum ContentType {
        TEXT, IMAGE, COMBINED, PAGE_POST;
    }
    
    // Added HUGGINGFACE based on provider implementation
    public enum AIProvider {
        OPENAI, GEMINI, HUGGINGFACE, ANTHROPIC, FAL_AI, STABLE_DIFFUSION, MOCK;
    }
}
