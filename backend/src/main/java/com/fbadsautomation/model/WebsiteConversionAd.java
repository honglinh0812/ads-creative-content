package com.fbadsautomation.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "website_conversion_ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteConversionAd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "ad_id")
    private Ad ad;
    
    private String websiteUrl;
    
    private String pixelId;
    
    @ElementCollection
    @CollectionTable(
        name = "website_conversion_events",
        joinColumns = @JoinColumn(name = "website_conversion_ad_id")
    )
    @Enumerated(EnumType.STRING)
    private java.util.Set<ConversionEventType> conversionEvents = new java.util.HashSet<>();
    
    // Audit fields
    private String createdBy;
    private String updatedBy;
    
    @Column(name = "created_date")
    private java.time.LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private java.time.LocalDateTime updatedDate;
    
    /**
     * Enum representing different types of conversion events
     */
    public enum ConversionEventType {
        REGISTRATION,
        PAGE_VIEW
    }
}
