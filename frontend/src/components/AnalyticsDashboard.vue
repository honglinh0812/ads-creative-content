<template>
  <div class="analytics-dashboard">
    <!-- Dashboard Header -->
    <div class="dashboard-header">
      <div class="header-content">
        <h1 class="dashboard-title">Analytics Dashboard</h1>
        <p class="dashboard-subtitle">Comprehensive insights into your Facebook advertising performance</p>
      </div>
      
      <!-- Time Range Selector -->
      <div class="time-range-selector">
        <label for="timeRange" class="sr-only">Select time range</label>
        <select 
          id="timeRange"
          v-model="selectedTimeRange" 
          @change="fetchAnalytics"
          class="time-range-select"
        >
          <option value="7d">Last 7 days</option>
          <option value="30d">Last 30 days</option>
          <option value="90d">Last 90 days</option>
          <option value="1y">Last year</option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>Loading analytics data...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Failed to load analytics</h3>
      <p>{{ error }}</p>
      <button @click="fetchAnalytics" class="retry-btn">
        <i class="pi pi-refresh"></i>
        Retry
      </button>
    </div>

    <!-- Analytics Content -->
    <div v-else-if="analytics" class="analytics-content">
      <!-- KPI Cards -->
      <section class="kpi-section">
        <h2 class="section-title">Key Performance Indicators</h2>
        <div class="kpi-grid">
          <Card
            v-for="kpi in kpiCards"
            :key="kpi.key"
            :title="kpi.title"
            variant="elevated"
            class="kpi-card"
          >
            <div class="kpi-content">
              <div class="kpi-icon" :class="`kpi-${kpi.color}`">
                <i :class="`pi ${kpi.icon}`"></i>
              </div>
              <div class="kpi-details">
                <div class="kpi-value">{{ formatValue(kpi.value, kpi.format) }}</div>
                <div class="kpi-change" :class="kpi.change >= 0 ? 'positive' : 'negative'">
                  <i :class="kpi.change >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"></i>
                  {{ Math.abs(kpi.change) }}%
                </div>
              </div>
            </div>
          </Card>
        </div>
      </section>

      <!-- Performance Trends Chart -->
      <section class="trends-section">
        <div class="section-header">
          <h2 class="section-title">Performance Trends</h2>
          <div class="metric-selector">
            <label for="trendMetric" class="sr-only">Select metric</label>
            <select 
              id="trendMetric"
              v-model="selectedTrendMetric" 
              @change="updateTrendsChart"
              class="metric-select"
            >
              <option value="impressions">Impressions</option>
              <option value="clicks">Clicks</option>
              <option value="ctr">CTR (%)</option>
              <option value="cpc">CPC ($)</option>
              <option value="conversions">Conversions</option>
              <option value="spend">Spend ($)</option>
            </select>
          </div>
        </div>
        <div class="chart-container">
          <PerformanceTrendsChart
            :data="trendsChartData"
            :metric="selectedTrendMetric"
            :loading="trendsLoading"
          />
        </div>
      </section>

      <!-- Analytics Grid -->
      <div class="analytics-grid">
        <!-- Campaign Analytics -->
        <section class="analytics-card campaign-analytics">
          <div class="card-header">
            <h3 class="card-title">
              <i class="pi pi-briefcase"></i>
              Campaign Performance
            </h3>
            <button @click="exportCampaignData" class="export-btn">
              <i class="pi pi-download"></i>
              Export
            </button>
          </div>
          <div class="card-content">
            <CampaignAnalyticsTable
              :campaigns="analytics.campaignAnalytics"
              :loading="false"
              @campaign-click="onCampaignClick"
            />
          </div>
        </section>

        <!-- AI Provider Analytics -->
        <section class="analytics-card ai-provider-analytics">
          <div class="card-header">
            <h3 class="card-title">
              <i class="pi pi-cog"></i>
              AI Provider Performance
            </h3>
          </div>
          <div class="card-content">
            <AIProviderChart
              :data="analytics.aiProviderAnalytics"
              :loading="false"
            />
          </div>
        </section>

        <!-- Budget Analytics -->
        <section class="analytics-card budget-analytics">
          <div class="card-header">
            <h3 class="card-title">
              <i class="pi pi-dollar"></i>
              Budget Analysis
            </h3>
          </div>
          <div class="card-content">
            <BudgetAnalyticsChart
              :data="analytics.budgetAnalytics"
              :loading="false"
            />
          </div>
        </section>

        <!-- Content Analytics -->
        <section class="analytics-card content-analytics">
          <div class="card-header">
            <h3 class="card-title">
              <i class="pi pi-file-edit"></i>
              Content Performance
            </h3>
          </div>
          <div class="card-content">
            <ContentAnalyticsChart
              :data="analytics.contentAnalytics"
              :loading="false"
            />
          </div>
        </section>
      </div>

      <!-- Top Performing Content -->
      <section class="top-content-section">
        <h2 class="section-title">Top Performing Content</h2>
        <div class="top-content-grid">
          <TopContentCard
            v-for="content in topPerformingContent"
            :key="content.contentId"
            :content="content"
            @content-click="onContentClick"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import Card from './ui/Card.vue'
import PerformanceTrendsChart from './analytics/PerformanceTrendsChart.vue'
import CampaignAnalyticsTable from './analytics/CampaignAnalyticsTable.vue'
import AIProviderChart from './analytics/AIProviderChart.vue'
import BudgetAnalyticsChart from './analytics/BudgetAnalyticsChart.vue'
import ContentAnalyticsChart from './analytics/ContentAnalyticsChart.vue'
import TopContentCard from './analytics/TopContentCard.vue'
import api from '@/services/api'

export default {
  name: 'AnalyticsDashboard',
  components: {
    Card,
    PerformanceTrendsChart,
    CampaignAnalyticsTable,
    AIProviderChart,
    BudgetAnalyticsChart,
    ContentAnalyticsChart,
    TopContentCard
  },
  
  setup() {
    const store = useStore()
    
    // Reactive data
    const loading = ref(true)
    const error = ref(null)
    const analytics = ref(null)
    const selectedTimeRange = ref('30d')
    const selectedTrendMetric = ref('impressions')
    const trendsLoading = ref(false)
    
    // Computed properties
    const kpiCards = computed(() => {
      if (!analytics.value?.kpiMetrics) return []
      
      const kpis = analytics.value.kpiMetrics
      
      return [
        {
          key: 'campaigns',
          title: 'Total Campaigns',
          value: kpis.totalCampaigns,
          change: kpis.campaignGrowth,
          icon: 'pi-briefcase',
          color: 'blue',
          format: 'number'
        },
        {
          key: 'ads',
          title: 'Total Ads',
          value: kpis.totalAds,
          change: kpis.adGrowth,
          icon: 'pi-megaphone',
          color: 'green',
          format: 'number'
        },
        {
          key: 'impressions',
          title: 'Total Impressions',
          value: kpis.totalImpressions,
          change: kpis.impressionGrowth,
          icon: 'pi-eye',
          color: 'purple',
          format: 'number'
        },
        {
          key: 'ctr',
          title: 'Average CTR',
          value: kpis.averageCTR,
          change: 0, // Would be calculated from trends
          icon: 'pi-chart-line',
          color: 'orange',
          format: 'percentage'
        },
        {
          key: 'budget',
          title: 'Total Budget',
          value: kpis.totalBudget,
          change: kpis.budgetGrowth,
          icon: 'pi-dollar',
          color: 'teal',
          format: 'currency'
        },
        {
          key: 'roi',
          title: 'ROI',
          value: kpis.roi,
          change: 0, // Would be calculated from trends
          icon: 'pi-chart-bar',
          color: 'indigo',
          format: 'percentage'
        },
        {
          key: 'content',
          title: 'AI Content Generated',
          value: kpis.contentGenerated,
          change: 0,
          icon: 'pi-cog',
          color: 'pink',
          format: 'number'
        },
        {
          key: 'savings',
          title: 'AI Cost Savings',
          value: kpis.aiCostSavings,
          change: 0,
          icon: 'pi-money-bill',
          color: 'cyan',
          format: 'currency'
        }
      ]
    })
    
    const trendsChartData = computed(() => {
      if (!analytics.value?.performanceTrends) return []
      
      return analytics.value.performanceTrends.map(trend => ({
        timestamp: trend.timestamp,
        value: trend.metrics[selectedTrendMetric.value] || 0,
        period: trend.period
      }))
    })
    
    /* const topPerformingContent = computed(() => analytics.value?.contentAnalytics?.topPerformingContent || [])
    */
    // Helper methods
    const formatValue = (value, format) => {
      if (!value && value !== 0) return 'N/A'
      
      switch (format) {
        case 'currency':
          return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
          }).format(value)
        case 'percentage':
          return `${value.toFixed(1)}%`
        case 'number':
          return new Intl.NumberFormat('en-US').format(value)
        default:
          return value.toString()
      }
    }

    // Methods
    const fetchAnalytics = async () => {
      try {
        loading.value = true
        error.value = null
        
        const response = await api.analyticsAPI.getDashboard(selectedTimeRange.value)
        analytics.value = response.data
        
      } catch (err) {
        console.error('Error fetching analytics:', err)
        error.value = err.response?.data?.message || 'Failed to load analytics data'
      } finally {
        loading.value = false
      }
    }
    
    const updateTrendsChart = async () => {
      trendsLoading.value = true
      // In a real app, you might fetch specific trend data here
      setTimeout(() => {
        trendsLoading.value = false
      }, 500)
    }
    
    const exportCampaignData = () => {
      // Implement CSV export functionality
      const csvData = analytics.value.campaignAnalytics.map(campaign => ({
        'Campaign Name': campaign.campaignName,
        'Status': campaign.status,
        'Budget': campaign.budget,
        'Spent': campaign.spent,
        'Impressions': campaign.impressions,
        'Clicks': campaign.clicks,
        'CTR': campaign.ctr,
        'CPC': campaign.cpc,
        'ROI': campaign.roi
      }))
      
      // Convert to CSV and download
      const csv = convertToCSV(csvData)
      downloadCSV(csv, `campaign-analytics-${selectedTimeRange.value}.csv`)
    }
    
    const onCampaignClick = (campaign) => {
      // Navigate to campaign details
      store.dispatch('analytics/setSelectedCampaign', campaign)
      // You could emit an event or use router here
    }
    
    const onContentClick = (content) => {
      // Navigate to content details
      store.dispatch('analytics/setSelectedContent', content)
    }
    
    // Helper functions
    const convertToCSV = (data) => {
      if (!data.length) return ''
      
      const headers = Object.keys(data[0])
      const csvContent = [
        headers.join(','),
        ...data.map(row => headers.map(header => `"${row[header]}"`).join(','))
      ].join('\n')
      
      return csvContent
    }
    
    const downloadCSV = (csv, filename) => {
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      const url = URL.createObjectURL(blob)
      
      link.setAttribute('href', url)
      link.setAttribute('download', filename)
      link.style.visibility = 'hidden'
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
    
    // Lifecycle
    onMounted(() => {
      fetchAnalytics()
    })
    
    // Watchers
    watch(selectedTimeRange, () => {
      fetchAnalytics()
    })
    
    return {
      loading,
      error,
      analytics,
      selectedTimeRange,
      selectedTrendMetric,
      trendsLoading,
      kpiCards,
      trendsChartData,
      fetchAnalytics,
      updateTrendsChart,
      exportCampaignData,
      onCampaignClick,
      onContentClick,
      formatValue
    }
  }
}
</script>

<style scoped>
.analytics-dashboard {
  padding: var(--space-6);
  background: var(--color-bg-primary);
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-8);
  gap: var(--space-4);
}

.header-content h1 {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  margin: 0 0 var(--space-2) 0;
}

.header-content p {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin: 0;
}

.time-range-selector {
  flex-shrink: 0;
}

.time-range-select {
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-secondary);
  color: var(--color-text);
  font-size: var(--text-sm);
  min-width: 150px;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top: 3px solid var(--brand-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--space-4);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container i {
  font-size: 3rem;
  color: var(--error-500);
  margin-bottom: var(--space-4);
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  background: var(--brand-primary);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: var(--transition-colors);
}

.retry-btn:hover {
  background: var(--brand-primary-hover);
}

.analytics-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.section-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0 0 var(--space-4) 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.kpi-card {
  transition: transform 0.2s ease;
}

.kpi-card:hover {
  transform: translateY(-2px);
}

.kpi-content {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.kpi-icon.kpi-blue {
  background: #2d5aa0;
}

.kpi-icon.kpi-green {
  background: #16a085;
}

.kpi-icon.kpi-purple {
  background: #8e44ad;
}

.kpi-icon.kpi-orange {
  background: #f4a261;
}

.kpi-icon.kpi-teal {
  background: #06b6d4;
}

.kpi-icon.kpi-indigo {
  background: #4c51bf;
}

.kpi-icon.kpi-pink {
  background: #e76f51;
}

.kpi-icon.kpi-cyan {
  background: #0891b2;
}

.kpi-details {
  flex: 1;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.kpi-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
}

.kpi-change.positive {
  color: #10b981;
}

.kpi-change.negative {
  color: #ef4444;
}

.trends-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.metric-select {
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-secondary);
  color: var(--color-text);
  font-size: var(--text-sm);
}

.chart-container {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  height: 400px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: var(--space-6);
}

.analytics-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0;
}

.export-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: var(--transition-colors);
}

.export-btn:hover {
  background: var(--color-hover);
}

.card-content {
  padding: var(--space-6);
}

.top-content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-4);
}

/* Responsive Design */
@media (width <= 768px) {
  .analytics-dashboard {
    padding: var(--space-4);
  }
  
  .dashboard-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  
  .analytics-grid {
    grid-template-columns: 1fr;
  }
  
  .trends-section .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  
  .chart-container {
    height: 300px;
  }
  
  .top-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
