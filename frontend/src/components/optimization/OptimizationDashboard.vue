<template>
  <div class="optimization-dashboard">
    <!-- Dashboard Header -->
    <div class="dashboard-header">
      <div class="header-content">
        <h1 class="dashboard-title">
          <i class="pi pi-lightbulb"></i>
          Optimization Recommendations
        </h1>
        <p class="dashboard-subtitle">
          AI-powered insights to improve your campaign performance and maximize ROI
        </p>
      </div>
      
      <!-- Quick Actions -->
      <div class="header-actions">
        <div class="time-range-selector">
          <label for="timeRange" class="sr-only">Select time range</label>
          <select 
            id="timeRange"
            v-model="selectedTimeRange" 
            @change="fetchRecommendations"
            class="time-range-select"
          >
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
            <option value="90d">Last 90 days</option>
          </select>
        </div>
        
        <button @click="refreshRecommendations" class="action-btn refresh-btn" :disabled="loading">
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          Refresh
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>Analyzing your campaigns and generating recommendations...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <i class="pi pi-exclamation-triangle"></i>
      <h3>Failed to load recommendations</h3>
      <p>{{ error }}</p>
      <button @click="fetchRecommendations" class="retry-btn">
        <i class="pi pi-refresh"></i>
        Retry
      </button>
    </div>

    <!-- Optimization Content -->
    <div v-else-if="optimizationData" class="optimization-content">
      <!-- Summary Cards -->
      <div class="summary-section">
        <div class="summary-grid">
          <div class="summary-card total-recommendations">
            <div class="card-icon">
              <i class="pi pi-list"></i>
            </div>
            <div class="card-content">
              <span class="card-value">{{ optimizationData.summary.totalRecommendations }}</span>
              <span class="card-label">Total Recommendations</span>
            </div>
          </div>
          
          <div class="summary-card high-priority">
            <div class="card-icon">
              <i class="pi pi-exclamation-circle"></i>
            </div>
            <div class="card-content">
              <span class="card-value">{{ optimizationData.summary.highPriorityRecommendations }}</span>
              <span class="card-label">High Priority</span>
            </div>
          </div>
          
          <div class="summary-card potential-impact">
            <div class="card-icon">
              <i class="pi pi-chart-line"></i>
            </div>
            <div class="card-content">
              <span class="card-value">+{{ optimizationData.summary.totalPotentialImpact.toFixed(1) }}%</span>
              <span class="card-label">Potential Impact</span>
            </div>
          </div>
          
          <div class="summary-card implementable">
            <div class="card-icon">
              <i class="pi pi-cog"></i>
            </div>
            <div class="card-content">
              <span class="card-value">{{ optimizationData.summary.implementableRecommendations }}</span>
              <span class="card-label">Ready to Implement</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Filter and Sort Controls -->
      <div class="controls-section">
        <div class="filter-controls">
          <div class="filter-group">
            <label for="priorityFilter">Priority</label>
            <select id="priorityFilter" v-model="selectedPriority" @change="applyFilters">
              <option value="">All Priorities</option>
              <option value="HIGH">High Priority</option>
              <option value="MEDIUM">Medium Priority</option>
              <option value="LOW">Low Priority</option>
            </select>
          </div>
          
          <div class="filter-group">
            <label for="typeFilter">Type</label>
            <select id="typeFilter" v-model="selectedType" @change="applyFilters">
              <option value="">All Types</option>
              <option value="BUDGET_REALLOCATION">Budget Optimization</option>
              <option value="AI_PROVIDER_SWITCH">AI Provider</option>
              <option value="CAMPAIGN_OBJECTIVE_OPTIMIZATION">Campaign Objective</option>
              <option value="AD_SCHEDULING">Ad Scheduling</option>
              <option value="CONTENT_TYPE_OPTIMIZATION">Content Type</option>
              <option value="AD_CREATIVE_REFRESH">Creative Refresh</option>
            </select>
          </div>
          
          <div class="filter-group">
            <label for="categoryFilter">Category</label>
            <select id="categoryFilter" v-model="selectedCategory" @change="applyFilters">
              <option value="">All Categories</option>
              <option value="Budget Optimization">Budget Optimization</option>
              <option value="AI Optimization">AI Optimization</option>
              <option value="Campaign Strategy">Campaign Strategy</option>
              <option value="Creative Optimization">Creative Optimization</option>
              <option value="Timing Optimization">Timing Optimization</option>
            </select>
          </div>
        </div>
        
        <div class="view-controls">
          <button 
            @click="viewMode = 'cards'" 
            :class="['view-btn', { active: viewMode === 'cards' }]"
          >
            <i class="pi pi-th-large"></i>
            Cards
          </button>
          <button 
            @click="viewMode = 'list'" 
            :class="['view-btn', { active: viewMode === 'list' }]"
          >
            <i class="pi pi-list"></i>
            List
          </button>
        </div>
      </div>

      <!-- Recommendations Display -->
      <div class="recommendations-section">
        <div v-if="filteredRecommendations.length === 0" class="no-recommendations">
          <i class="pi pi-info-circle"></i>
          <h3>No recommendations found</h3>
          <p>Try adjusting your filters or check back later for new recommendations.</p>
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
        <div v-if="totalPages > 1" class="pagination">
          <button
            @click="currentPage = Math.max(1, currentPage - 1)"
            :disabled="currentPage === 1"
            class="pagination-btn"
          >
            <i class="pi pi-chevron-left"></i>
          </button>
          
          <span class="pagination-info">
            Page {{ currentPage }} of {{ totalPages }}
            ({{ filteredRecommendations.length }} recommendations)
          </span>
          
          <button
            @click="currentPage = Math.min(totalPages, currentPage + 1)"
            :disabled="currentPage === totalPages"
            class="pagination-btn"
          >
            <i class="pi pi-chevron-right"></i>
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import RecommendationCard from './RecommendationCard.vue'
import RecommendationListItem from './RecommendationListItem.vue'
import { optimizationAPI } from '@/services/api'

export default {
  name: 'OptimizationDashboard',
  components: {
    RecommendationCard,
    RecommendationListItem
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
  background: #f8fafc;
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

.header-content h1 i {
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

.time-range-select {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  color: #374151;
  font-size: 14px;
  min-width: 150px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  color: #374151;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-btn:hover:not(:disabled) {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #3b82f6;
}

.pi-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e5e7eb;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.error-container i {
  font-size: 48px;
  color: #ef4444;
  margin-bottom: 16px;
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
  margin-top: 16px;
}

.retry-btn:hover {
  background: #2563eb;
}

.summary-section {
  margin-bottom: 32px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
  transform: translateY(-2px);
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  font-size: 24px;
}

.total-recommendations .card-icon {
  background: #eff6ff;
  color: #2563eb;
}

.high-priority .card-icon {
  background: #fef2f2;
  color: #dc2626;
}

.potential-impact .card-icon {
  background: #f0fdf4;
  color: #16a34a;
}

.implementable .card-icon {
  background: #fef3c7;
  color: #d97706;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.card-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.controls-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 24px;
  flex-wrap: wrap;
}

.filter-controls {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.filter-group select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  font-size: 14px;
  min-width: 150px;
}

.view-controls {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  padding: 4px;
  border-radius: 8px;
}

.view-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-btn.active {
  background: white;
  color: #374151;
  box-shadow: 0 1px 3px rgb(0 0 0 / 10%);
}

.view-btn:hover:not(.active) {
  color: #374151;
}

.no-recommendations {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  text-align: center;
  color: #6b7280;
}

.no-recommendations i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #d1d5db;
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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding: 24px;
}

.pagination-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #6b7280;
}

/* Responsive Design */
@media (width <= 768px) {
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
  
  .summary-grid {
    grid-template-columns: 1fr;
  }
  
  .controls-section {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-controls {
    justify-content: space-between;
  }
  
  .filter-group select {
    min-width: auto;
    flex: 1;
  }
  
  .recommendations-grid {
    grid-template-columns: 1fr;
  }
}
</style>
