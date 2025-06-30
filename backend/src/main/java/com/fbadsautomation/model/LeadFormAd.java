package com.fbadsautomation.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "lead_form_ads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadFormAd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "ad_id")
    private Ad ad;
    
    private String formName;
    
    private String privacyPolicyUrl;
    
    private String thanksMessage;
    
    private String fbFormId;
    
    // Audit fields
    private String createdBy;
    private String updatedBy;
    
    @Column(name = "created_date")
    private java.time.LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private java.time.LocalDateTime updatedDate;
    
    // Relationship with form fields
    @OneToMany(mappedBy = "leadFormAd", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<LeadFormField> fields = new java.util.ArrayList<>();
}
