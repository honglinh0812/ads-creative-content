<template>
  <div class="optimization-dashboard">
    <!-- Dashboard Header -->
    <div class="dashboard-header">
      <div class="header-content">
        <h1 class="dashboard-title">
          <bulb-outlined />
          Optimization Recommendations
        </h1>
        <p class="dashboard-subtitle">
          AI-powered insights to improve your campaign performance and maximize ROI
        </p>
      </div>
      
      <!-- Quick Actions -->
      <div class="header-actions">
        <a-select 
          v-model:value="selectedTimeRange" 
          @change="fetchRecommendations"
          style="width: 150px;"
        >
          <a-select-option value="7d">Last 7 days</a-select-option>
          <a-select-option value="30d">Last 30 days</a-select-option>
          <a-select-option value="90d">Last 90 days</a-select-option>
        </a-select>
        
        <a-button @click="refreshRecommendations" :loading="loading">
          <template #icon>
            <reload-outlined />
          </template>
          Refresh
        </a-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <a-spin size="large" />
      <p>Analyzing your campaigns and generating recommendations...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <a-result
        status="error"
        title="Failed to load recommendations"
        :sub-title="error"
      >
        <template #extra>
          <a-button type="primary" @click="fetchRecommendations">
            <template #icon>
              <reload-outlined />
            </template>
            Retry
          </a-button>
        </template>
      </a-result>
    </div>

    <!-- Optimization Content -->
    <div v-else-if="optimizationData" class="optimization-content">
      <!-- Summary Cards -->
      <a-row :gutter="[16, 16]" style="margin-bottom: 24px;">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="summary-card" hoverable>
            <a-statistic
              title="Total Recommendations"
              :value="optimizationData.summary.totalRecommendations"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <unordered-list-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="summary-card" hoverable>
            <a-statistic
              title="High Priority"
              :value="optimizationData.summary.highPriorityRecommendations"
              :value-style="{ color: '#ff4d4f' }"
            >
              <template #prefix>
                <exclamation-circle-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="summary-card" hoverable>
            <a-statistic
              title="Potential Impact"
              :value="optimizationData.summary.totalPotentialImpact"
              suffix="%"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix>
                <line-chart-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="summary-card" hoverable>
            <a-statistic
              title="Ready to Implement"
              :value="optimizationData.summary.implementableRecommendations"
              :value-style="{ color: '#fa8c16' }"
            >
              <template #prefix>
                <setting-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- Filter and Sort Controls -->
      <a-card style="margin-bottom: 24px;">
        <a-row :gutter="16" align="middle">
          <a-col :xs="24" :sm="8" :md="6">
            <a-select 
              v-model:value="selectedPriority" 
              @change="applyFilters"
              placeholder="All Priorities"
              style="width: 100%"
            >
              <a-select-option value="">All Priorities</a-select-option>
              <a-select-option value="HIGH">High Priority</a-select-option>
              <a-select-option value="MEDIUM">Medium Priority</a-select-option>
              <a-select-option value="LOW">Low Priority</a-select-option>
            </a-select>
          </a-col>
          
          <a-col :xs="24" :sm="8" :md="6">
            <a-select 
              v-model:value="selectedType" 
              @change="applyFilters"
              placeholder="All Types"
              style="width: 100%"
            >
              <a-select-option value="">All Types</a-select-option>
              <a-select-option value="BUDGET_REALLOCATION">Budget Optimization</a-select-option>
              <a-select-option value="AI_PROVIDER_SWITCH">AI Provider</a-select-option>
              <a-select-option value="CAMPAIGN_OBJECTIVE_OPTIMIZATION">Campaign Objective</a-select-option>
              <a-select-option value="AD_SCHEDULING">Ad Scheduling</a-select-option>
              <a-select-option value="CONTENT_TYPE_OPTIMIZATION">Content Type</a-select-option>
              <a-select-option value="AD_CREATIVE_REFRESH">Creative Refresh</a-select-option>
            </a-select>
          </a-col>
          
          <a-col :xs="24" :sm="8" :md="6">
            <a-select 
              v-model:value="selectedCategory" 
              @change="applyFilters"
              placeholder="All Categories"
              style="width: 100%"
            >
              <a-select-option value="">All Categories</a-select-option>
              <a-select-option value="Budget Optimization">Budget Optimization</a-select-option>
              <a-select-option value="AI Optimization">AI Optimization</a-select-option>
              <a-select-option value="Campaign Strategy">Campaign Strategy</a-select-option>
              <a-select-option value="Creative Optimization">Creative Optimization</a-select-option>
              <a-select-option value="Timing Optimization">Timing Optimization</a-select-option>
            </a-select>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="6">
            <a-radio-group v-model:value="viewMode" button-style="solid">
              <a-radio-button value="cards">
                <template #icon>
                  <appstore-outlined />
                </template>
                Cards
              </a-radio-button>
              <a-radio-button value="list">
                <template #icon>
                  <unordered-list-outlined />
                </template>
                List
              </a-radio-button>
            </a-radio-group>
          </a-col>
        </a-row>
      </a-card>

      <!-- Recommendations Display -->
      <div class="recommendations-section">
        <div v-if="filteredRecommendations.length === 0" class="no-recommendations">
          <a-empty description="No recommendations found">
            <template #image>
              <info-circle-outlined style="font-size: 48px; color: #d9d9d9;" />
            </template>
            <template #description>
              <span>Try adjusting your filters or check back later for new recommendations.</span>
            </template>
          </a-empty>
        </div>
        
        <div v-else>
          <!-- Cards View -->
          <div v-if="viewMode === 'cards'" class="recommendations-grid">
            <RecommendationCard
              v-for="recommendation in paginatedRecommendations"
              :key="recommendation.id"
              :recommendation="recommendation"
              @accept="handleAcceptRecommendation"
              @dismiss="handleDismissRecommendation"
              @schedule="handleScheduleRecommendation"
              @view-details="handleViewDetails"
            />
          </div>
          
          <!-- List View -->
          <div v-else class="recommendations-list">
            <RecommendationListItem
              v-for="recommendation in paginatedRecommendations"
              :key="recommendation.id"
              :recommendation="recommendation"
              @accept="handleAcceptRecommendation"
              @dismiss="handleDismissRecommendation"
              @schedule="handleScheduleRecommendation"
              @view-details="handleViewDetails"
            />
          </div>
        </div>
        
        <!-- Pagination -->
        <div v-if="totalPages > 1" style="margin-top: 24px; text-align: center;">
          <a-pagination
            v-model:current="currentPage"
            :total="filteredRecommendations.length"
            :page-size="itemsPerPage"
            show-size-changer
            show-quick-jumper
            :show-total="(total, range) => `${range[0]}-${range[1]} of ${total} recommendations`"
          />
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import RecommendationCard from './RecommendationCard.vue'
import RecommendationListItem from './RecommendationListItem.vue'
import {
  BulbOutlined,
  ReloadOutlined,
  UnorderedListOutlined,
  ExclamationCircleOutlined,
  LineChartOutlined,
  SettingOutlined,
  AppstoreOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import { optimizationAPI } from '@/services/optimizationAPI'

export default {
  name: 'OptimizationDashboard',
  components: {
    RecommendationCard,
    RecommendationListItem,
    BulbOutlined,
    ReloadOutlined,
    UnorderedListOutlined,
    ExclamationCircleOutlined,
    LineChartOutlined,
    SettingOutlined,
    AppstoreOutlined,
    InfoCircleOutlined
  },
  
  setup() {
    // Reactive data
    const loading = ref(true)
    const error = ref(null)
    const optimizationData = ref(null)
    const selectedTimeRange = ref('30d')
    const selectedPriority = ref('')
    const selectedType = ref('')
    const selectedCategory = ref('')
    const viewMode = ref('cards')
    const currentPage = ref(1)
    const itemsPerPage = ref(12)
    const selectedRecommendation = ref(null)
    const showScheduleModal = ref(false)
    const recommendationToSchedule = ref(null)
    
    // Computed properties
    const filteredRecommendations = computed(() => {
      if (!optimizationData.value?.recommendations) return []
      
      let filtered = [...optimizationData.value.recommendations]
      
      if (selectedPriority.value) {
        filtered = filtered.filter(r => r.priority === selectedPriority.value)
      }
      
      if (selectedType.value) {
        filtered = filtered.filter(r => r.type === selectedType.value)
      }
      
      if (selectedCategory.value) {
        filtered = filtered.filter(r => r.category === selectedCategory.value)
      }
      
      return filtered
    })
    
    const totalPages = computed(() => {
      return Math.ceil(filteredRecommendations.value.length / itemsPerPage.value)
    })
    
    const paginatedRecommendations = computed(() => {
      const start = (currentPage.value - 1) * itemsPerPage.value
      const end = start + itemsPerPage.value
      return filteredRecommendations.value.slice(start, end)
    })
    
    // Methods
    const fetchRecommendations = async () => {
      try {
        loading.value = true
        error.value = null
        
        const response = await optimizationAPI.getRecommendations(selectedTimeRange.value)
        optimizationData.value = response.data
        
      } catch (err) {
        console.error('Error fetching optimization recommendations:', err)
        error.value = err.response?.data?.message || 'Failed to load optimization recommendations'
      } finally {
        loading.value = false
      }
    }
    
    const refreshRecommendations = () => {
      fetchRecommendations()
    }
    
    const applyFilters = () => {
      currentPage.value = 1
    }
    
    const handleAcceptRecommendation = async (recommendation) => {
      try {
        await optimizationAPI.acceptRecommendation(recommendation.id)
        // Update recommendation status locally
        recommendation.status = 'ACCEPTED'
        // You could show a success toast here
      } catch (error) {
        console.error('Error accepting recommendation:', error)
        // You could show an error toast here
      }
    }
    
    const handleDismissRecommendation = async (recommendation, reason) => {
      try {
        await optimizationAPI.dismissRecommendation(recommendation.id, reason)
        // Remove recommendation from list or update status
        const index = optimizationData.value.recommendations.findIndex(r => r.id === recommendation.id)
        if (index !== -1) {
          optimizationData.value.recommendations[index].status = 'DISMISSED'
        }
      } catch (error) {
        console.error('Error dismissing recommendation:', error)
      }
    }
    
    const handleScheduleRecommendation = (recommendation) => {
      recommendationToSchedule.value = recommendation
      showScheduleModal.value = true
    }
    
    const closeScheduleModal = () => {
      showScheduleModal.value = false
      recommendationToSchedule.value = null
    }
    
    const confirmScheduleRecommendation = async (scheduleData) => {
      try {
        await optimizationAPI.scheduleRecommendation(recommendationToSchedule.value.id, scheduleData)
        // Update recommendation status
        recommendationToSchedule.value.status = 'SCHEDULED'
        closeScheduleModal()
      } catch (error) {
        console.error('Error scheduling recommendation:', error)
      }
    }
    
    const handleViewDetails = (recommendation) => {
      selectedRecommendation.value = recommendation
    }
    
    // Lifecycle
    onMounted(() => {
      fetchRecommendations()
    })
    
    // Watchers
    watch(selectedTimeRange, () => {
      fetchRecommendations()
    })
    
    watch([selectedPriority, selectedType, selectedCategory], () => {
      currentPage.value = 1
    })
    
    return {
      loading,
      error,
      optimizationData,
      selectedTimeRange,
      selectedPriority,
      selectedType,
      selectedCategory,
      viewMode,
      currentPage,
      totalPages,
      filteredRecommendations,
      paginatedRecommendations,
      selectedRecommendation,
      showScheduleModal,
      recommendationToSchedule,
      fetchRecommendations,
      refreshRecommendations,
      applyFilters,
      handleAcceptRecommendation,
      handleDismissRecommendation,
      handleScheduleRecommendation,
      closeScheduleModal,
      confirmScheduleRecommendation,
      handleViewDetails
    }
  }
}
</script>

<style scoped>
.optimization-dashboard {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 24px;
}

.header-content h1 {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
}

.header-content h1 .anticon {
  color: #f59e0b;
}

.header-content p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  max-width: 600px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  text-align: center;
  color: #6b7280;
}

.loading-container p {
  margin-top: 16px;
  font-size: 16px;
}

.summary-card {
  text-align: center;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.no-recommendations {
  padding: 64px;
}

/* Responsive Design */
@media (max-width: 768px) {
  .optimization-dashboard {
    padding: 16px;
  }
  
  .dashboard-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .header-actions {
    justify-content: space-between;
  }
  
  .recommendations-grid {
    grid-template-columns: 1fr;
  }
}
</style>
