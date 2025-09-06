package com.fbadsautomation.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "campaigns")
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
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CampaignStatus {
        DRAFT, PENDING, ACTIVE, PAUSED, COMPLETED, FAILED;
    }
    
    public enum CampaignObjective {
        BRAND_AWARENESS, REACH, TRAFFIC, ENGAGEMENT, APP_INSTALLS, VIDEO_VIEWS, LEAD_GENERATION, CONVERSIONS, CATALOG_SALES, STORE_TRAFFIC;
    }
    
    public enum BudgetType {
        DAILY, LIFETIME;
    }
    
    // Manual getters to avoid reliance on Lombok
    public Long getId() { return id; }
    public String getName() { return name; }
    public CampaignStatus getStatus() { return status; }
    public CampaignObjective getObjective() { return objective; }
    public Double getBudget() { return budget; }
    public BudgetType getBudgetType() { return budgetType; }
    public Double getDailyBudget() { return dailyBudget; }
    public Double getTotalBudget() { return totalBudget; }
    public String getTargetAudience() { return targetAudience; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public User getUser() { return user; }
    public Set<Ad> getAds() { return ads; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Manual setters used by services
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStatus(CampaignStatus status) { this.status = status; }
    public void setObjective(CampaignObjective objective) { this.objective = objective; }
    public void setBudget(Double budget) { this.budget = budget; }
    public void setBudgetType(BudgetType budgetType) { this.budgetType = budgetType; }
    public void setDailyBudget(Double dailyBudget) { this.dailyBudget = dailyBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setUser(User user) { this.user = user; }
    public void setAds(Set<Ad> ads) { this.ads = ads; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Constructors
    public Campaign() {
        this.ads = new HashSet<>();
    }

    public Campaign(Long id, String name, CampaignStatus status, CampaignObjective objective, 
                   Double budget, BudgetType budgetType, Double dailyBudget, Double totalBudget,
                   String targetAudience, LocalDate startDate, LocalDate endDate, User user,
                   Set<Ad> ads, LocalDateTime createdDate, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.objective = objective;
        this.budget = budget;
        this.budgetType = budgetType;
        this.dailyBudget = dailyBudget;
        this.totalBudget = totalBudget;
        this.targetAudience = targetAudience;
        this.startDate = startDate;
        this.endDate = endDate;
        this.user = user;
        this.ads = ads != null ? ads : new HashSet<>();
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static CampaignBuilder builder() {
        return new CampaignBuilder();
    }

    public static class CampaignBuilder {
        private Long id;
        private String name;
        private CampaignStatus status;
        private CampaignObjective objective;
        private Double budget;
        private BudgetType budgetType;
        private Double dailyBudget;
        private Double totalBudget;
        private String targetAudience;
        private LocalDate startDate;
        private LocalDate endDate;
        private User user;
        private Set<Ad> ads = new HashSet<>();
        private LocalDateTime createdDate;
        private LocalDateTime updatedAt;

        public CampaignBuilder id(Long id) { this.id = id; return this; }
        public CampaignBuilder name(String name) { this.name = name; return this; }
        public CampaignBuilder status(CampaignStatus status) { this.status = status; return this; }
        public CampaignBuilder objective(CampaignObjective objective) { this.objective = objective; return this; }
        public CampaignBuilder budget(Double budget) { this.budget = budget; return this; }
        public CampaignBuilder budgetType(BudgetType budgetType) { this.budgetType = budgetType; return this; }
        public CampaignBuilder dailyBudget(Double dailyBudget) { this.dailyBudget = dailyBudget; return this; }
        public CampaignBuilder totalBudget(Double totalBudget) { this.totalBudget = totalBudget; return this; }
        public CampaignBuilder targetAudience(String targetAudience) { this.targetAudience = targetAudience; return this; }
        public CampaignBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public CampaignBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public CampaignBuilder user(User user) { this.user = user; return this; }
        public CampaignBuilder ads(Set<Ad> ads) { this.ads = ads; return this; }
        public CampaignBuilder createdDate(LocalDateTime createdDate) { this.createdDate = createdDate; return this; }
        public CampaignBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Campaign build() {
            return new Campaign(id, name, status, objective, budget, budgetType, dailyBudget, 
                              totalBudget, targetAudience, startDate, endDate, user, ads, 
                              createdDate, updatedAt);
        }
    }
}
