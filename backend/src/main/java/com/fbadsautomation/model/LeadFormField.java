package com.fbadsautomation.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "lead_form_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadFormField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "lead_form_ad_id")
    private LeadFormAd leadFormAd;
    
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;
    
    private String fieldName;
    
    private boolean isRequired;
    
    // Audit fields
    private String createdBy;
    private String updatedBy;
    
    @Column(name = "created_date")
    private java.time.LocalDateTime createdDate;
    
    @Column(name = "updated_date")
    private java.time.LocalDateTime updatedDate;
    
    /**
     * Enum representing different types of fields in a lead form
     */
    public enum FieldType {
        NAME,
        EMAIL,
        PHONE,
        FACEBOOK,
        LINKEDIN
    }
}
