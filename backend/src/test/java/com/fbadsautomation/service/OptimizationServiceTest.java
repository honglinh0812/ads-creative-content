package com.fbadsautomation.service;

import com.fbadsautomation.dto.AnalyticsResponse;
import com.fbadsautomation.dto.OptimizationResponse;
import com.fbadsautomation.dto.OptimizationResponse.*;
import com.fbadsautomation.model.User;
import com.fbadsautomation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OptimizationServiceTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private OptimizationRulesEngine rulesEngine;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OptimizationService optimizationService;

    private User testUser;
    private AnalyticsResponse mockAnalytics;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        // Create mock analytics data
        mockAnalytics = new AnalyticsResponse();
        AnalyticsResponse.KPIMetrics kpiMetrics = new AnalyticsResponse.KPIMetrics(
            5L, 15L, 3L, 10L, 2500.0, 1800.0, 3.2, 0.95, 75000L, 2400L, 9.1, 18.7, 30, 750.0
        );
        mockAnalytics.setKpiMetrics(kpiMetrics);
        mockAnalytics.setCampaignAnalytics(createMockCampaignAnalytics());
        mockAnalytics.setAiProviderAnalytics(createMockAIProviderAnalytics());
        mockAnalytics.setContentAnalytics(createMockContentAnalytics());
    }

    @Test
    void testGenerateRecommendations_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(analyticsService.getAnalytics(1L, "30d")).thenReturn(mockAnalytics);
        
        List<Recommendation> budgetRecommendations = createMockBudgetRecommendations();
        List<Recommendation> aiProviderRecommendations = createMockAIProviderRecommendations();
        List<Recommendation> objectiveRecommendations = createMockObjectiveRecommendations();
        List<Recommendation> contentRecommendations = createMockContentRecommendations();
        
        when(rulesEngine.generateBudgetRecommendations(mockAnalytics)).thenReturn(budgetRecommendations);
        when(rulesEngine.generateAIProviderRecommendations(mockAnalytics)).thenReturn(aiProviderRecommendations);
        when(rulesEngine.generateCampaignObjectiveRecommendations(mockAnalytics)).thenReturn(objectiveRecommendations);
        when(rulesEngine.generateContentTypeRecommendations(mockAnalytics)).thenReturn(contentRecommendations);

        // Act
        OptimizationResponse result = optimizationService.generateRecommendations(1L, "30d");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getRecommendations());
        assertNotNull(result.getSummary());
        assertNotNull(result.getGeneratedAt());
        
        assertEquals(4, result.getRecommendations().size()); // 1 from each category
        
        // Verify recommendations are sorted by priority
        List<Recommendation> recommendations = result.getRecommendations();
        for (int i = 0; i < recommendations.size() - 1; i++) {
            Priority current = recommendations.get(i).getPriority();
            Priority next = recommendations.get(i + 1).getPriority();
            assertTrue(getPriorityValue(current) >= getPriorityValue(next));
        }
        
        // Verify summary
        OptimizationSummary summary = result.getSummary();
        assertEquals(4, summary.getTotalRecommendations());
        assertTrue(summary.getAverageConfidence() > 0);
        
        // Verify service calls
        verify(userRepository).findById(1L);
        verify(analyticsService).getAnalytics(1L, "30d");
        verify(rulesEngine).generateBudgetRecommendations(mockAnalytics);
        verify(rulesEngine).generateAIProviderRecommendations(mockAnalytics);
        verify(rulesEngine).generateCampaignObjectiveRecommendations(mockAnalytics);
        verify(rulesEngine).generateContentTypeRecommendations(mockAnalytics);
    }

    @Test
    void testGenerateRecommendations_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            optimizationService.generateRecommendations(1L, "30d");
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findById(1L);
        verifyNoInteractions(analyticsService, rulesEngine);
    }

    @Test
    void testGenerateRecommendations_AnalyticsServiceException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(analyticsService.getAnalytics(1L, "30d")).thenThrow(new RuntimeException("Analytics service error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            optimizationService.generateRecommendations(1L, "30d");
        });
        
        assertTrue(exception.getMessage().contains("Failed to generate optimization recommendations"));
        verify(userRepository).findById(1L);
        verify(analyticsService).getAnalytics(1L, "30d");
    }

    @Test
    void testGetRecommendationsByType_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(analyticsService.getAnalytics(1L, "30d")).thenReturn(mockAnalytics);
        
        List<Recommendation> budgetRecommendations = createMockBudgetRecommendations();
        when(rulesEngine.generateBudgetRecommendations(mockAnalytics)).thenReturn(budgetRecommendations);
        when(rulesEngine.generateAIProviderRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());
        when(rulesEngine.generateCampaignObjectiveRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());
        when(rulesEngine.generateContentTypeRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());

        // Act
        List<Recommendation> result = optimizationService.getRecommendationsByType(1L, RecommendationType.BUDGET_REALLOCATION, "30d");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(RecommendationType.BUDGET_REALLOCATION, result.get(0).getType());
    }

    @Test
    void testGetHighPriorityRecommendations_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(analyticsService.getAnalytics(1L, "30d")).thenReturn(mockAnalytics);
        
        List<Recommendation> budgetRecommendations = createMockBudgetRecommendations();
        budgetRecommendations.get(0).setPriority(Priority.HIGH);
        
        when(rulesEngine.generateBudgetRecommendations(mockAnalytics)).thenReturn(budgetRecommendations);
        when(rulesEngine.generateAIProviderRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());
        when(rulesEngine.generateCampaignObjectiveRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());
        when(rulesEngine.generateContentTypeRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());

        // Act
        List<Recommendation> result = optimizationService.getHighPriorityRecommendations(1L, "30d");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Priority.HIGH, result.get(0).getPriority());
    }

    @Test
    void testOptimizationSummaryGeneration() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(analyticsService.getAnalytics(1L, "30d")).thenReturn(mockAnalytics);
        
        List<Recommendation> allRecommendations = new ArrayList<>();
        allRecommendations.addAll(createMockBudgetRecommendations());
        allRecommendations.addAll(createMockAIProviderRecommendations());
        
        when(rulesEngine.generateBudgetRecommendations(mockAnalytics)).thenReturn(createMockBudgetRecommendations());
        when(rulesEngine.generateAIProviderRecommendations(mockAnalytics)).thenReturn(createMockAIProviderRecommendations());
        when(rulesEngine.generateCampaignObjectiveRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());
        when(rulesEngine.generateContentTypeRecommendations(mockAnalytics)).thenReturn(new ArrayList<>());

        // Act
        OptimizationResponse result = optimizationService.generateRecommendations(1L, "30d");

        // Assert
        OptimizationSummary summary = result.getSummary();
        assertNotNull(summary);
        assertEquals(2, summary.getTotalRecommendations());
        assertTrue(summary.getImplementableRecommendations() >= 0);
        assertTrue(summary.getAverageConfidence() >= 0 && summary.getAverageConfidence() <= 1);
        assertNotNull(summary.getRecommendationsByType());
    }

    // Helper methods
    private List<AnalyticsResponse.CampaignAnalytics> createMockCampaignAnalytics() {
        List<AnalyticsResponse.CampaignAnalytics> campaigns = new ArrayList<>();
        
        AnalyticsResponse.CampaignAnalytics campaign1 = new AnalyticsResponse.CampaignAnalytics();
        campaign1.setCampaignId(1L);
        campaign1.setCampaignName("Test Campaign 1");
        campaign1.setStatus("ACTIVE");
        campaign1.setObjective("CONVERSIONS");
        campaign1.setBudget(1000.0);
        campaign1.setSpent(750.0);
        campaign1.setRoi(25.5);
        campaign1.setBudgetUtilization(75.0);
        campaigns.add(campaign1);
        
        return campaigns;
    }

    private AnalyticsResponse.AIProviderAnalytics createMockAIProviderAnalytics() {
        AnalyticsResponse.AIProviderAnalytics.ProviderMetrics openAIMetrics = 
            new AnalyticsResponse.AIProviderAnalytics.ProviderMetrics(
                "OPENAI", 25, 85.0, 1500.0, 0.75, 75.0
            );
        
        Map<String, AnalyticsResponse.AIProviderAnalytics.ProviderMetrics> providerMetrics = new HashMap<>();
        providerMetrics.put("OPENAI", openAIMetrics);
        
        return new AnalyticsResponse.AIProviderAnalytics(
            providerMetrics, "OPENAI", "OPENAI", 15.75, 500.0, 25
        );
    }

    private AnalyticsResponse.ContentAnalytics createMockContentAnalytics() {
        AnalyticsResponse.ContentAnalytics contentAnalytics = new AnalyticsResponse.ContentAnalytics();
        contentAnalytics.setTotalContentGenerated(100);
        contentAnalytics.setSelectedContent(65);
        contentAnalytics.setSelectionRate(65.0);
        
        Map<String, Integer> contentTypeDistribution = new HashMap<>();
        contentTypeDistribution.put("headline", 40);
        contentTypeDistribution.put("primary_text", 35);
        contentTypeDistribution.put("description", 25);
        contentAnalytics.setContentTypeDistribution(contentTypeDistribution);
        
        return contentAnalytics;
    }

    private List<Recommendation> createMockBudgetRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        
        Recommendation recommendation = new Recommendation();
        recommendation.setId(UUID.randomUUID().toString());
        recommendation.setType(RecommendationType.BUDGET_REALLOCATION);
        recommendation.setPriority(Priority.HIGH);
        recommendation.setTitle("Increase Budget for High-Performing Campaign");
        recommendation.setDescription("Test budget recommendation");
        recommendation.setCategory("Budget Optimization");
        
        Impact impact = new Impact("ROI", 15.0, "increase", "1-2 weeks", 0.85, new HashMap<>());
        recommendation.setExpectedImpact(impact);
        
        List<Action> actions = Arrays.asList(
            new Action("increase_budget", "Increase budget", new HashMap<>(), true, "Confirm increase?")
        );
        recommendation.setActions(actions);
        
        recommendations.add(recommendation);
        return recommendations;
    }

    private List<Recommendation> createMockAIProviderRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        
        Recommendation recommendation = new Recommendation();
        recommendation.setId(UUID.randomUUID().toString());
        recommendation.setType(RecommendationType.AI_PROVIDER_SWITCH);
        recommendation.setPriority(Priority.MEDIUM);
        recommendation.setTitle("Switch to Better AI Provider");
        recommendation.setDescription("Test AI provider recommendation");
        recommendation.setCategory("AI Optimization");
        
        Impact impact = new Impact("Selection Rate", 20.0, "increase", "1-2 weeks", 0.80, new HashMap<>());
        recommendation.setExpectedImpact(impact);
        
        List<Action> actions = Arrays.asList(
            new Action("switch_provider", "Switch provider", new HashMap<>(), false, null)
        );
        recommendation.setActions(actions);
        
        recommendations.add(recommendation);
        return recommendations;
    }

    private List<Recommendation> createMockObjectiveRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        
        Recommendation recommendation = new Recommendation();
        recommendation.setId(UUID.randomUUID().toString());
        recommendation.setType(RecommendationType.CAMPAIGN_OBJECTIVE_OPTIMIZATION);
        recommendation.setPriority(Priority.LOW);
        recommendation.setTitle("Optimize Campaign Objective");
        recommendation.setDescription("Test objective recommendation");
        recommendation.setCategory("Campaign Strategy");
        
        Impact impact = new Impact("Conversion Rate", 10.0, "increase", "2-3 weeks", 0.70, new HashMap<>());
        recommendation.setExpectedImpact(impact);
        
        recommendations.add(recommendation);
        return recommendations;
    }

    private List<Recommendation> createMockContentRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        
        Recommendation recommendation = new Recommendation();
        recommendation.setId(UUID.randomUUID().toString());
        recommendation.setType(RecommendationType.CONTENT_TYPE_OPTIMIZATION);
        recommendation.setPriority(Priority.LOW);
        recommendation.setTitle("Diversify Content Types");
        recommendation.setDescription("Test content recommendation");
        recommendation.setCategory("Content Optimization");
        
        Impact impact = new Impact("Selection Rate", 8.0, "increase", "2-4 weeks", 0.65, new HashMap<>());
        recommendation.setExpectedImpact(impact);
        
        recommendations.add(recommendation);
        return recommendations;
    }

    private int getPriorityValue(Priority priority) {
        switch (priority) {
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }
}
