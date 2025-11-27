package com.fbadsautomation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.validation.constraints.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class CampaignCreateRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(min = 3, max = 255, message = "Campaign name must be between 3 and 255 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-_.,()]+$", message = "Campaign name contains invalid characters")
    private String name;

    @NotBlank(message = "Campaign objective is required")
    @Pattern(
        regexp = "^(BRAND_AWARENESS|REACH|TRAFFIC|ENGAGEMENT|APP_INSTALLS|VIDEO_VIEWS|LEAD_GENERATION|CONVERSIONS|CATALOG_SALES|STORE_TRAFFIC)$",
        message = "Invalid campaign objective"
    )
    private String objective;

    @NotBlank(message = "Budget type is required")
    @Pattern(regexp = "^(DAILY|LIFETIME)$", message = "Budget type must be DAILY or LIFETIME")
    private String budgetType;

    @DecimalMin(value = "1.0", message = "Daily budget must be at least $1.00")
    @DecimalMax(value = "10000.0", message = "Daily budget cannot exceed $10,000.00")
    private Double dailyBudget;

    @DecimalMin(value = "1.0", message = "Total budget must be at least $1.00")
    @DecimalMax(value = "100000.0", message = "Total budget cannot exceed $100,000.00")
    private Double totalBudget;

    @Size(max = 1000, message = "Target audience description cannot exceed 1000 characters")
    private String targetAudience;

    @DecimalMin(value = "0.0", inclusive = false, message = "Bid cap must be greater than 0")
    private Double bidCap;

    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    private LocalDate endDate;

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true; // Let other validations handle null values
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Either daily budget or total budget must be specified")
    public boolean isBudgetValid() {
        return dailyBudget != null || totalBudget != null;
    }

    // Constructors
    public CampaignCreateRequest() {;
    }

    public CampaignCreateRequest(String name, String objective, String budgetType, 
                               Double dailyBudget, Double totalBudget, String targetAudience,
                               Double bidCap,
                               LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.objective = objective;
        this.budgetType = budgetType;
        this.dailyBudget = dailyBudget;
        this.totalBudget = totalBudget;
        this.targetAudience = targetAudience;
        this.bidCap = bidCap;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name;
    }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective;
    }

    public String getBudgetType() { return budgetType; }
    public void setBudgetType(String budgetType) { this.budgetType = budgetType;
    }

    public Double getDailyBudget() { return dailyBudget; }
    public void setDailyBudget(Double dailyBudget) { this.dailyBudget = dailyBudget;
    }

    public Double getTotalBudget() { return totalBudget; }
    public void setTotalBudget(Double totalBudget) { this.totalBudget = totalBudget;
    }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience;
    }
    public Double getBidCap() { return bidCap; }
    public void setBidCap(Double bidCap) { this.bidCap = bidCap;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate;
    }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; };
    }
