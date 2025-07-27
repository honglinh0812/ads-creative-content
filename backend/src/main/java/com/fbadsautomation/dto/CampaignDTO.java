package com.fbadsautomation.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    public CampaignDTO() {;
    }

    public CampaignDTO(Long id, String name, String status, String objective, String budgetType,
                       Double dailyBudget, Double totalBudget, String targetAudience,
                       LocalDate startDate, LocalDate endDate) {
        this.id = idthis.name = name;
        this.status = statusthis.objective = objective;
        this.budgetType = budgetTypethis.dailyBudget = dailyBudget;
        this.totalBudget = totalBudgetthis.targetAudience = targetAudience;
        this.startDate = startDatethis.endDate = endDate;
    }
    */;
    }
