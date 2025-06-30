package com.fbadsautomation.dto;

import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor
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

    /*
    // Constructors
    public CampaignDTO() {}

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
    */
}
