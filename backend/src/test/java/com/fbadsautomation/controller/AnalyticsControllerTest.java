package com.fbadsautomation.controller;

import com.fbadsautomation.service.AnalyticsService;
import com.fbadsautomation.dto.AnalyticsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "USER")
    public void testGetAnalyticsDashboard() throws Exception {
        // Create mock analytics response
        AnalyticsResponse.KPIMetrics kpiMetrics = new AnalyticsResponse.KPIMetrics(
            10L, 25L, 8L, 20L, 5000.0, 3500.0, 2.5, 1.2, 150000L, 3750L, 8.5, 15.2, 50, 1250.0
        );
        kpiMetrics.setCampaignGrowth(12.5);
        kpiMetrics.setAdGrowth(18.3);
        kpiMetrics.setBudgetGrowth(15.2);
        kpiMetrics.setImpressionGrowth(23.5);

        AnalyticsResponse mockResponse = new AnalyticsResponse(
            kpiMetrics,
            List.of(), // Empty trends for simplicity
            List.of(), // Empty campaign analytics
            List.of(), // Empty ad analytics
            new AnalyticsResponse.AIProviderAnalytics(),
            new AnalyticsResponse.BudgetAnalytics(),
            new AnalyticsResponse.ContentAnalytics()
        );

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/dashboard")
                .param("timeRange", "30d")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kpiMetrics.totalCampaigns").value(10))
                .andExpect(jsonPath("$.data.kpiMetrics.totalAds").value(25))
                .andExpect(jsonPath("$.data.kpiMetrics.totalBudget").value(5000.0))
                .andExpect(jsonPath("$.data.kpiMetrics.campaignGrowth").value(12.5));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetKPIMetrics() throws Exception {
        AnalyticsResponse.KPIMetrics kpiMetrics = new AnalyticsResponse.KPIMetrics(
            5L, 15L, 4L, 12L, 2500.0, 1800.0, 3.2, 0.95, 75000L, 2400L, 9.1, 18.7, 30, 750.0
        );

        AnalyticsResponse mockResponse = new AnalyticsResponse();
        mockResponse.setKpiMetrics(kpiMetrics);

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/kpis")
                .param("timeRange", "7d")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCampaigns").value(5))
                .andExpect(jsonPath("$.data.totalAds").value(15))
                .andExpect(jsonPath("$.data.averageCTR").value(3.2))
                .andExpect(jsonPath("$.data.roi").value(18.7));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetPerformanceTrends() throws Exception {
        AnalyticsResponse.TimeSeriesData trendData = new AnalyticsResponse.TimeSeriesData(
            LocalDateTime.now().minusDays(1),
            "day",
            Map.of(
                "impressions", 10000.0,
                "clicks", 250.0,
                "ctr", 2.5,
                "spend", 125.0
            )
        );

        AnalyticsResponse mockResponse = new AnalyticsResponse();
        mockResponse.setPerformanceTrends(List.of(trendData));

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/trends")
                .param("timeRange", "30d")
                .param("metric", "impressions")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].period").value("day"))
                .andExpect(jsonPath("$.data[0].metrics.impressions").value(10000.0));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetCampaignAnalytics() throws Exception {
        AnalyticsResponse.CampaignAnalytics campaignAnalytics = new AnalyticsResponse.CampaignAnalytics();
        campaignAnalytics.setCampaignId(1L);
        campaignAnalytics.setCampaignName("Test Campaign");
        campaignAnalytics.setStatus("ACTIVE");
        campaignAnalytics.setObjective("CONVERSIONS");
        campaignAnalytics.setBudget(1000.0);
        campaignAnalytics.setSpent(750.0);
        campaignAnalytics.setImpressions(50000L);
        campaignAnalytics.setClicks(1250L);
        campaignAnalytics.setCtr(2.5);
        campaignAnalytics.setRoi(15.8);

        AnalyticsResponse mockResponse = new AnalyticsResponse();
        mockResponse.setCampaignAnalytics(List.of(campaignAnalytics));

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/campaigns")
                .param("timeRange", "30d")
                .param("status", "ACTIVE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].campaignName").value("Test Campaign"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].budget").value(1000.0))
                .andExpect(jsonPath("$.data[0].roi").value(15.8));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetAnalyticsSummary() throws Exception {
        AnalyticsResponse.KPIMetrics kpiMetrics = new AnalyticsResponse.KPIMetrics(
            8L, 20L, 6L, 16L, 4000.0, 2800.0, 2.8, 1.1, 120000L, 3360L, 7.9, 12.4, 40, 1000.0
        );
        kpiMetrics.setCampaignGrowth(10.2);
        kpiMetrics.setAdGrowth(15.7);

        AnalyticsResponse mockResponse = new AnalyticsResponse();
        mockResponse.setKpiMetrics(kpiMetrics);
        mockResponse.setGeneratedAt(LocalDateTime.now());

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/summary")
                .param("timeRange", "90d")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCampaigns").value(8))
                .andExpect(jsonPath("$.data.totalAds").value(20))
                .andExpect(jsonPath("$.data.totalBudget").value(4000.0))
                .andExpect(jsonPath("$.data.totalSpent").value(2800.0))
                .andExpect(jsonPath("$.data.averageCTR").value(2.8))
                .andExpect(jsonPath("$.data.roi").value(12.4))
                .andExpect(jsonPath("$.data.campaignGrowth").value(10.2))
                .andExpect(jsonPath("$.data.timeRange").value("90d"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetFilterOptions() throws Exception {
        mockMvc.perform(get("/api/analytics/filters")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.timeRanges").isArray())
                .andExpect(jsonPath("$.data.timeRanges[0]").value("7d"))
                .andExpect(jsonPath("$.data.campaignStatuses").isArray())
                .andExpect(jsonPath("$.data.campaignObjectives").isArray())
                .andExpect(jsonPath("$.data.aiProviders").isArray())
                .andExpect(jsonPath("$.data.metrics").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetAIProviderAnalytics() throws Exception {
        AnalyticsResponse.AIProviderAnalytics.ProviderMetrics openAIMetrics = 
            new AnalyticsResponse.AIProviderAnalytics.ProviderMetrics(
                "OPENAI", 25, 92.5, 1800.0, 0.75, 68.0
            );

        AnalyticsResponse.AIProviderAnalytics aiProviderAnalytics = 
            new AnalyticsResponse.AIProviderAnalytics(
                Map.of("OPENAI", openAIMetrics),
                "OPENAI",
                "OPENAI",
                18.75,
                625.0,
                25
            );

        AnalyticsResponse mockResponse = new AnalyticsResponse();
        mockResponse.setAiProviderAnalytics(aiProviderAnalytics);

        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/ai-providers")
                .param("timeRange", "30d")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mostUsedProvider").value("OPENAI"))
                .andExpect(jsonPath("$.data.bestPerformingProvider").value("OPENAI"))
                .andExpect(jsonPath("$.data.totalAICost").value(18.75))
                .andExpect(jsonPath("$.data.estimatedSavings").value(625.0))
                .andExpect(jsonPath("$.data.totalContentGenerated").value(25));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminAccessToAnalytics() throws Exception {
        AnalyticsResponse mockResponse = new AnalyticsResponse();
        when(analyticsService.getAnalytics(anyLong(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/analytics/dashboard")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAnalyticsServiceException() throws Exception {
        when(analyticsService.getAnalytics(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/analytics/dashboard")
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to fetch analytics dashboard"));
    }
}
