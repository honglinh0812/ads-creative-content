package com.fbadsautomation.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ads")
@ToString(exclude = {"campaign", "user"})// Includes getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(name = "call_to_action", length = 500) // Increased from 255 to accommodate AI-generated content
    private String callToAction; // Call to action

    @Column(name = "image_url", length = 1000) // Increased to accommodate longer file paths
    private String imageUrl;
    
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
    
    @Column(name = "updated_date")
    private java.time.LocalDateTime updatedDate;
    
    public enum AdType {
        PAGE_POST_AD, WEBSITE_CONVERSION_AD, LEAD_FORM_AD
    }
}

