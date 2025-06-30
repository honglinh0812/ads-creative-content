package com.fbadsautomation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;
    
    @Column(name = "objective")
    @Enumerated(EnumType.STRING)
    private CampaignObjective objective;
    
    @Column(name = "budget")
    private Double budget;
    
    @Column(name = "budget_type")
    @Enumerated(EnumType.STRING)
    private BudgetType budgetType;
    
    @Column(name = "daily_budget")
    private Double dailyBudget;
    
    @Column(name = "total_budget")
    private Double totalBudget;
    
    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Ad> ads = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CampaignStatus {
        DRAFT, PENDING, ACTIVE, PAUSED, COMPLETED, FAILED
    }
    
    public enum CampaignObjective {
        BRAND_AWARENESS, REACH, TRAFFIC, ENGAGEMENT, APP_INSTALLS, VIDEO_VIEWS, LEAD_GENERATION, CONVERSIONS, CATALOG_SALES, STORE_TRAFFIC
    }
    
    public enum BudgetType {
        DAILY, LIFETIME
    }
}

