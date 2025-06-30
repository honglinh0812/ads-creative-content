package com.fbadsautomation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ad_contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType contentType;
    
    @Column(name = "primary_text", length = 2000) // Increased from 1000 to accommodate AI-generated content
    private String primaryText;
    
    @Column(name = "headline", length = 500) // Increased from 255 to accommodate AI-generated content
    private String headline;
    
    @Column(name = "description", length = 1000) // Increased from 500 to accommodate AI-generated content
    private String description;
    
    @Column(name = "call_to_action", length = 500) // Increased to accommodate longer CTAs
    private String callToAction;
   
    public String getCta() {
    	return callToAction;
    }

    public void setCta(String callToAction) {
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
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ContentType {
        TEXT, IMAGE, COMBINED, PAGE_POST
    }
    
    // Added HUGGINGFACE based on provider implementation
    public enum AIProvider {
        OPENAI, GEMINI, HUGGINGFACE 
    }
}

