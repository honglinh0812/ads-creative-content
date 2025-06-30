package com.fbadsautomation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampaignCreateRequest {
    private String name;
    private String objective;
    private String budgetType;
    private Double dailyBudget;
    private Double totalBudget;
    private String targetAudience;
    private LocalDate startDate;
    private LocalDate endDate;

    // Constructors
    public CampaignCreateRequest() {}

    public CampaignCreateRequest(String name, String objective, String budgetType, 
                               Double dailyBudget, Double totalBudget, String targetAudience,
                               LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.objective = objective;
        this.budgetType = budgetType;
        this.dailyBudget = dailyBudget;
        this.totalBudget = totalBudget;
        this.targetAudience = targetAudience;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
}

