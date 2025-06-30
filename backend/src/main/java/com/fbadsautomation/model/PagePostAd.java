package com.fbadsautomation.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "page_post_ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagePostAd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "ad_id")
    private Ad ad;
    
    private String pageId;
    
    private String postType;
    
    // Additional fields specific to Page Post Ads
    private String postMessage;
    private String linkUrl;
    
    // Audit fields
    private String createdBy;
    private String updatedBy;
    
    @Column(name = "created_date")
    private java.time.LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private java.time.LocalDateTime updatedDate;
}
