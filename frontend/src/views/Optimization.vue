<template>
  <div class="optimization-view">
    <!-- Mobile Header -->
    <MobileHeader 
      v-if="isMobile" 
      @toggle-mobile-menu="toggleMobileMenu"
    />
    
    <!-- Desktop Page Header -->
    <div v-if="!isMobile" class="page-header">
      <a-page-header
        title="Optimization Center"
        sub-title="AI-powered recommendations to optimize your campaigns, reduce costs, and maximize performance"
      >
        <template #extra>
        </template>
      </a-page-header>
    </div>

    <!-- Mobile Page Header -->
    <div v-if="isMobile" class="mobile-header">
      <h1>Optimization Center</h1>
    </div>

    <!-- Navigation Tabs -->
    <div class="nav-tabs">
      <a-tabs v-model:activeKey="activeTab" type="card" class="optimization-tabs">
        <a-tab-pane key="all" tab="All Recommendations">
          <OptimizationDashboard />
        </a-tab-pane>
        
        <a-tab-pane key="priority" tab="High Priority">
          <div class="priority-section">
            <div class="section-header">
              <h2>High Priority Recommendations</h2>
              <p>These recommendations require immediate attention and can significantly impact your performance.</p>
            </div>
            
            <div v-if="highPriorityLoading" class="loading-state">
              <a-spin size="large" />
              <p>Loading high priority recommendations...</p>
            </div>
            
            <div v-else-if="highPriorityRecommendations.length === 0" class="empty-state">
              <a-empty description="Great job! You don't have any high priority recommendations at the moment.">
                <template #image>
                  <check-circle-outlined style="font-size: 48px; color: #52c41a;" />
                </template>
              </a-empty>
            </div>
            
            <div v-else class="recommendations-grid">
              <RecommendationCard
                v-for="recommendation in highPriorityRecommendations"
                :key="recommendation.id"
                :recommendation="recommendation"
                @accept="handleAcceptRecommendation"
                @dismiss="handleDismissRecommendation"
                @schedule="handleScheduleRecommendation"
                @view-details="handleViewDetails"
              />
            </div>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="category" tab="By Category">
          <div class="category-section">
            <div class="section-header">
              <h2>Recommendations by Category</h2>
              <p>Explore recommendations organized by optimization category.</p>
            </div>
            
            <a-row :gutter="[16, 16]">
              <a-col 
                v-for="category in recommendationCategories" 
                :key="category.name"
                :xs="24" :sm="12" :lg="8"
              >
                <a-card 
                  class="category-card" 
                  hoverable
                  @click="selectCategory(category)"
                >
                  <template #title>
                    <div class="category-title">
                      <div class="category-icon" :style="{ backgroundColor: category.color + '20', color: category.color }">
                        <i :class="category.icon"></i>
                      </div>
                      <span>{{ category.name }}</span>
                    </div>
                  </template>
                  <template #extra>
                    <right-outlined />
                  </template>
                  <p>{{ category.description }}</p>
                  <div class="category-stats">
                    <a-statistic 
                      :value="category.count" 
                      suffix="recommendations"
                      :value-style="{ fontSize: '16px', color: category.color }"
                    />
                    <a-statistic 
                      :value="category.impact" 
                      suffix="% potential impact"
                      :value-style="{ fontSize: '16px', color: '#52c41a' }"
                    />
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="tracking" tab="Performance">
          <div class="tracking-section">
            <div class="section-header">
              <h2>Implementation Tracking</h2>
              <p>Monitor the effectiveness of implemented recommendations.</p>
            </div>
            
            <!-- Implementation Stats -->
            <a-row :gutter="[16, 16]" style="margin-bottom: 24px;">
              <a-col :xs="24" :sm="8">
                <a-card class="stat-card">
                  <a-statistic
                    title="Recommendations Implemented"
                    :value="implementationStats.totalImplemented"
                    :value-style="{ color: '#1890ff' }"
                  >
                    <template #prefix>
                      <check-circle-outlined />
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
              
              <a-col :xs="24" :sm="8">
                <a-card class="stat-card">
                  <a-statistic
                    title="Average Impact Achieved"
                    :value="implementationStats.averageImpact"
                    suffix="%"
                    :value-style="{ color: '#52c41a' }"
                  >
                    <template #prefix>
                      <line-chart-outlined />
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
              
              <a-col :xs="24" :sm="8">
                <a-card class="stat-card">
                  <a-statistic
                    title="Success Rate"
                    :value="implementationStats.successRate"
                    suffix="%"
                    :value-style="{ color: '#fa8c16' }"
                  >
                    <template #prefix>
                      <trophy-outlined />
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
            </a-row>
            
            <!-- Implementation History -->
            <a-card title="Recent Implementations">
              <a-list
                :data-source="implementationHistory"
                item-layout="horizontal"
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                <template #actions>
                  <a-tag :color="getStatusColor(item.status)">
                    {{ formatStatus(item.status) }}
                  </a-tag>
                </template>
                <a-list-item-meta>
                  <template #avatar>
                    <a-avatar :style="{ backgroundColor: getStatusColor(item.status) }">
                      <i :class="getImplementationStatusIcon(item.status)"></i>
                    </a-avatar>
                  </template>
                  <template #title>
                    {{ item.title }}
                  </template>
                  <template #description>
                    {{ item.description }}
                    <div class="history-meta">
                      <span class="implementation-date">{{ formatDate(item.implementedAt) }}</span>
                      <span class="implementation-impact" :class="getImpactClass(item.actualImpact)">
                        {{ item.actualImpact > 0 ? '+' : '' }}{{ item.actualImpact.toFixed(1) }}% impact
                      </span>
                    </div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </div>
    </a-tab-pane>
    
    <a-tab-pane key="settings" tab="Settings">
      <div class="settings-section">
        <div class="section-header">
          <h2>Optimization Settings</h2>
          <p>Configure how recommendations are generated and delivered.</p>
        </div>
        
        <a-card>
          <a-form
            :model="settings"
            layout="vertical"
            @finish="saveSettings"
          >
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item label="Recommendation Frequency">
                  <a-select v-model:value="settings.frequency">
                    <a-select-option value="daily">Daily</a-select-option>
                    <a-select-option value="weekly">Weekly</a-select-option>
                    <a-select-option value="monthly">Monthly</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              
              <a-col :xs="24" :sm="12">
                <a-form-item label="Minimum Confidence Level">
                  <a-slider
                    v-model:value="settings.minConfidence"
                    :min="0.5"
                    :max="1"
                    :step="0.05"
                    :marks="{ 0.5: '50%', 0.7: '70%', 0.9: '90%', 1: '100%' }"
                  />
                </a-form-item>
              </a-col>
            </a-row>
            
            <a-form-item label="Auto-Accept Low Risk Recommendations">
              <a-switch v-model:checked="settings.autoAccept" />
              <span style="margin-left: 8px;">Automatically implement low-risk recommendations with high confidence</span>
            </a-form-item>
            
            <a-form-item label="Excluded Recommendation Types">
              <a-checkbox-group v-model:value="settings.excludedTypes">
                <a-row>
                  <a-col :span="8" v-for="type in recommendationTypes" :key="type.value">
                    <a-checkbox :value="type.value">{{ type.label }}</a-checkbox>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </a-form-item>
            
            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit" :loading="savingSettings">
                  <template #icon>
                    <save-outlined />
                  </template>
                  Save Settings
                </a-button>
                <a-button @click="resetSettings">
                  <template #icon>
                    <reload-outlined />
                  </template>
                  Reset to Defaults
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </div>
    </a-tab-pane>
  </a-tabs>
</div>
</div>
</template>

<script>
import { ref, onMounted } from 'vue'
import OptimizationDashboard from '@/components/optimization/OptimizationDashboard.vue'
import RecommendationCard from '@/components/optimization/RecommendationCard.vue'
import MobileHeader from '@/components/MobileHeader.vue'
import {
  ReloadOutlined,
  CheckCircleOutlined,
  RightOutlined,
  LineChartOutlined,
  TrophyOutlined,
  SaveOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'OptimizationView',
  components: {
    OptimizationDashboard,
    RecommendationCard,
    MobileHeader,
    ReloadOutlined,
    CheckCircleOutlined,
    RightOutlined,
    LineChartOutlined,
    TrophyOutlined,
    SaveOutlined
  },
  
  setup() {
    // Reactive data
    const activeTab = ref('all')
    const loading = ref(false)
    const optimizationSummary = ref(null)
    const highPriorityRecommendations = ref([])
    const highPriorityLoading = ref(false)
    const isMobile = ref(false)
    const implementationStats = ref({
      totalImplemented: 15,
      averageImpact: 12.5,
      successRate: 78.3
    })
    const implementationHistory = ref([])
    const settings = ref({
      frequency: 'daily',
      minConfidence: 0.7,
      autoAccept: false,
      excludedTypes: []
    })
    const savingSettings = ref(false)
    
    const recommendationCategories = ref([
      {
        name: 'Budget Optimization',
        description: 'Optimize budget allocation across campaigns',
        icon: 'pi pi-dollar',
        color: '#10b981',
        count: 3,
        impact: 18.5
      },
      {
        name: 'AI Optimization',
        description: 'Improve AI provider selection and content quality',
        icon: 'pi pi-cog',
        color: '#3b82f6',
        count: 2,
        impact: 12.3
      },
      {
        name: 'Campaign Strategy',
        description: 'Optimize campaign objectives and targeting',
        icon: 'pi pi-target',
        color: '#f59e0b',
        count: 4,
        impact: 15.7
      },
      {
        name: 'Creative Optimization',
        description: 'Refresh ad creatives and improve engagement',
        icon: 'pi pi-image',
        color: '#8b5cf6',
        count: 2,
        impact: 9.2
      },
      {
        name: 'Timing Optimization',
        description: 'Optimize ad scheduling and delivery timing',
        icon: 'pi pi-clock',
        color: '#ef4444',
        count: 1,
        impact: 6.8
      }
    ])
    
    const recommendationTypes = ref([
      { value: 'BUDGET_REALLOCATION', label: 'Budget Reallocation' },
      { value: 'AI_PROVIDER_SWITCH', label: 'AI Provider Switch' },
      { value: 'CAMPAIGN_OBJECTIVE_OPTIMIZATION', label: 'Campaign Objective' },
      { value: 'AD_SCHEDULING', label: 'Ad Scheduling' },
      { value: 'CONTENT_TYPE_OPTIMIZATION', label: 'Content Type' },
      { value: 'AD_CREATIVE_REFRESH', label: 'Creative Refresh' }
    ])
    
    // Methods


    
    const fetchOptimizationSummary = async () => {
      try {
        const response = await api.optimizationAPI.getOptimizationSummary()
        optimizationSummary.value = response.data
      } catch (error) {
        console.error('Error fetching optimization summary:', error)
      }
    }
    
    const fetchHighPriorityRecommendations = async () => {
      try {
        highPriorityLoading.value = true
        const response = await api.optimizationAPI.getHighPriorityRecommendations()
        highPriorityRecommendations.value = response.data
      } catch (error) {
        console.error('Error fetching high priority recommendations:', error)
      } finally {
        highPriorityLoading.value = false
      }
    }
    
    const loadImplementationHistory = () => {
      // Mock implementation history
      implementationHistory.value = [
        {
          id: '1',
          title: 'Increased Budget for Campaign A',
          description: 'Budget increased from $500 to $750 daily',
          implementedAt: new Date('2024-01-10'),
          actualImpact: 22.3,
          status: 'SUCCESS'
        },
        {
          id: '2',
          title: 'Switched to OpenAI Provider',
          description: 'Changed AI provider for content generation',
          implementedAt: new Date('2024-01-08'),
          actualImpact: 15.7,
          status: 'SUCCESS'
        },
        {
          id: '3',
          title: 'Optimized Ad Scheduling',
          description: 'Adjusted delivery schedule for better performance',
          implementedAt: new Date('2024-01-05'),
          actualImpact: -2.1,
          status: 'PARTIAL'
        }
      ]
    }
    
    const handleAcceptRecommendation = async (recommendation) => {
      try {
        await api.optimizationAPI.acceptRecommendation(recommendation.id)
        recommendation.status = 'ACCEPTED'
      } catch (error) {
        console.error('Error accepting recommendation:', error)
      }
    }
    
    const handleDismissRecommendation = async (recommendation, reason) => {
      try {
        await api.optimizationAPI.dismissRecommendation(recommendation.id, reason)
        recommendation.status = 'DISMISSED'
      } catch (error) {
        console.error('Error dismissing recommendation:', error)
      }
    }
    
    const handleScheduleRecommendation = (recommendation) => {
      console.log('Schedule recommendation:', recommendation.id)
    }
    
    const handleViewDetails = (recommendation) => {
      console.log('View details:', recommendation.id)
    }
    
    const selectCategory = (category) => {
      console.log('Selected category:', category.name)
    }
    
    const saveSettings = async () => {
      try {
        savingSettings.value = true
        await api.optimizationAPI.updateRecommendationSettings(settings.value)
        // Show success message
      } catch (error) {
        console.error('Error saving settings:', error)
      } finally {
        savingSettings.value = false
      }
    }
    
    const resetSettings = () => {
      settings.value = {
        frequency: 'daily',
        minConfidence: 0.7,
        autoAccept: false,
        excludedTypes: []
      }
    }
    
    const getImplementationStatusIcon = (status) => {
      const icons = {
        'SUCCESS': 'pi pi-check-circle',
        'PARTIAL': 'pi pi-exclamation-circle',
        'FAILED': 'pi pi-times-circle'
      }
      return icons[status] || 'pi pi-question-circle'
    }
    
    const getStatusColor = (status) => {
      const colors = {
        'SUCCESS': 'green',
        'PARTIAL': 'orange',
        'FAILED': 'red'
      }
      return colors[status] || 'default'
    }
    
    const getImpactClass = (impact) => {
      if (impact > 0) return 'impact-positive'
      if (impact < 0) return 'impact-negative'
      return 'impact-neutral'
    }
    
    const formatDate = (date) => {
      return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      }).format(date)
    }
    
    const formatStatus = (status) => {
      return status.toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const toggleMobileMenu = () => {
      console.log('Toggle mobile menu')
    }
    
    const checkMobile = () => {
      isMobile.value = window.innerWidth <= 768
    }
    
    // Lifecycle
    onMounted(() => {
      checkMobile()
      window.addEventListener('resize', checkMobile)
      fetchOptimizationSummary()
      fetchHighPriorityRecommendations()
      loadImplementationHistory()
    })
    
    return {
      activeTab,
      loading,
      optimizationSummary,
      highPriorityRecommendations,
      highPriorityLoading,
      isMobile,
      recommendationCategories,
      implementationStats,
      implementationHistory,
      settings,
      savingSettings,
      recommendationTypes,
      handleAcceptRecommendation,
      handleDismissRecommendation,
      handleScheduleRecommendation,
      handleViewDetails,
      selectCategory,
      saveSettings,
      resetSettings,
      getImplementationStatusIcon,
      getStatusColor,
      getImpactClass,
      formatDate,
      formatStatus,
      toggleMobileMenu
    }
  }
}
</script>

<style scoped>
.optimization-view {
  background: #f5f5f5;
  min-height: 100vh;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.mobile-header h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.nav-tabs {
  padding: 24px;
}

.optimization-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section-header {
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px;
}

.section-header p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  text-align: center;
  color: #6b7280;
}

.loading-state p {
  margin-top: 16px;
  font-size: 16px;
}

.empty-state {
  padding: 64px;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.category-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-card:hover {
  border-color: #dae4eb;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 18px;
}

.category-stats {
  display: flex;
  gap: 16px;
  margin-top: 12px;
}

.stat-card {
  text-align: center;
}

.history-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  margin-top: 8px;
}

.implementation-date {
  color: #9ca3af;
}

.implementation-impact {
  font-weight: 500;
}

.impact-positive {
  color: #52c41a;
}

.impact-negative {
  color: #ff4d4f;
}

.impact-neutral {
  color: #6b7280;
}

/* Responsive Design */
@media (max-width: 768px) {
  .nav-tabs {
    padding: 16px;
  }
  
  .recommendations-grid {
    grid-template-columns: 1fr;
  }
  
  .category-stats {
    flex-direction: column;
    gap: 8px;
  }
  
  .history-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
