package com.fbadsautomation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampaignDTO {
    private Long id;
    private String name;
    private String status;
    private String objective;
    private String budgetType;
    private Double dailyBudget;
    private Double totalBudget;
    private String targetAudience;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalAds;           // Total number of ads in this campaign
    private LocalDateTime createdDate;  // Campaign creation date

    // Constructors
    public CampaignDTO() {
    }

    public CampaignDTO(Long id, String name, String status, String objective, String budgetType,
                       Double dailyBudget, Double totalBudget, String targetAudience,
                       LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.objective = objective;
        this.budgetType = budgetType;
        this.dailyBudget = dailyBudget;
        this.totalBudget = totalBudget;
        this.targetAudience = targetAudience;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public CampaignDTO(Long id, String name, String status, String objective, String budgetType,
                       Double dailyBudget, Double totalBudget, String targetAudience,
                       LocalDate startDate, LocalDate endDate, Integer totalAds, LocalDateTime createdDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.objective = objective;
        this.budgetType = budgetType;
        this.dailyBudget = dailyBudget;
        this.totalBudget = totalBudget;
        this.targetAudience = targetAudience;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAds = totalAds;
        this.createdDate = createdDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    
    public String getBudgetType() { return budgetType; }
    public void setBudgetType(String budgetType) { this.budgetType = budgetType; }
    
    public Double getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(Double dailyBudget) { this.dailyBudget = dailyBudget; }
    
    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget; }
    
    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getTotalAds() { return totalAds; }
    public void setTotalAds(Integer totalAds) { this.totalAds = totalAds; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
