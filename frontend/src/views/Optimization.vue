<template>
  <div class="optimization-view">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <i class="pi pi-lightbulb"></i>
          Optimization Center
        </h1>
        <p class="page-description">
          AI-powered recommendations to optimize your campaigns, reduce costs, and maximize performance
        </p>
      </div>
      
      <!-- Quick Stats -->
      <div class="quick-stats" v-if="optimizationSummary">
        <div class="stat-item">
          <span class="stat-value">{{ optimizationSummary.totalRecommendations }}</span>
          <span class="stat-label">Active Recommendations</span>
        </div>
        <div class="stat-item high-priority">
          <span class="stat-value">{{ optimizationSummary.highPriorityCount }}</span>
          <span class="stat-label">High Priority</span>
        </div>
        <div class="stat-item potential-impact">
          <span class="stat-value">+{{ optimizationSummary.totalPotentialImpact.toFixed(1) }}%</span>
          <span class="stat-label">Potential Impact</span>
        </div>
      </div>
    </div>

    <!-- Navigation Tabs -->
    <div class="nav-tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        @click="activeTab = tab.id"
        :class="['nav-tab', { active: activeTab === tab.id }]"
      >
        <i :class="tab.icon"></i>
        <span>{{ tab.label }}</span>
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- Tab Content -->
    <div class="tab-content">
      <!-- All Recommendations Tab -->
      <div v-if="activeTab === 'all'" class="tab-panel">
        <OptimizationDashboard />
      </div>

      <!-- High Priority Tab -->
      <div v-else-if="activeTab === 'priority'" class="tab-panel">
        <div class="priority-section">
          <div class="section-header">
            <h2>High Priority Recommendations</h2>
            <p>These recommendations require immediate attention and can significantly impact your performance.</p>
          </div>
          
          <div v-if="highPriorityLoading" class="loading-state">
            <div class="loading-spinner"></div>
            <p>Loading high priority recommendations...</p>
          </div>
          
          <div v-else-if="highPriorityRecommendations.length === 0" class="empty-state">
            <i class="pi pi-check-circle"></i>
            <h3>Great job!</h3>
            <p>You don't have any high priority recommendations at the moment.</p>
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
      </div>

      <!-- By Category Tab -->
      <div v-else-if="activeTab === 'category'" class="tab-panel">
        <div class="category-section">
          <div class="section-header">
            <h2>Recommendations by Category</h2>
            <p>Explore recommendations organized by optimization category.</p>
          </div>
          
          <div class="category-grid">
            <div 
              v-for="category in recommendationCategories" 
              :key="category.name"
              class="category-card"
              @click="selectCategory(category)"
            >
              <div class="category-icon" :style="{ backgroundColor: category.color + '20', color: category.color }">
                <i :class="category.icon"></i>
              </div>
              <div class="category-info">
                <h3>{{ category.name }}</h3>
                <p>{{ category.description }}</p>
                <div class="category-stats">
                  <span class="recommendation-count">{{ category.count }} recommendations</span>
                  <span class="potential-impact">+{{ category.impact.toFixed(1) }}% potential impact</span>
                </div>
              </div>
              <div class="category-arrow">
                <i class="pi pi-chevron-right"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Performance Tracking Tab -->
      <div v-else-if="activeTab === 'tracking'" class="tab-panel">
        <div class="tracking-section">
          <div class="section-header">
            <h2>Implementation Tracking</h2>
            <p>Monitor the effectiveness of implemented recommendations.</p>
          </div>
          
          <!-- Implementation Stats -->
          <div class="implementation-stats">
            <div class="stat-card">
              <div class="stat-icon">
                <i class="pi pi-check-circle"></i>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ implementationStats.totalImplemented }}</span>
                <span class="stat-label">Recommendations Implemented</span>
              </div>
            </div>
            
            <div class="stat-card">
              <div class="stat-icon">
                <i class="pi pi-chart-line"></i>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ implementationStats.averageImpact.toFixed(1) }}%</span>
                <span class="stat-label">Average Impact Achieved</span>
              </div>
            </div>
            
            <div class="stat-card">
              <div class="stat-icon">
                <i class="pi pi-trophy"></i>
              </div>
              <div class="stat-content">
                <span class="stat-number">{{ implementationStats.successRate.toFixed(1) }}%</span>
                <span class="stat-label">Success Rate</span>
              </div>
            </div>
          </div>
          
          <!-- Implementation History -->
          <div class="implementation-history">
            <h3>Recent Implementations</h3>
            <div class="history-list">
              <div 
                v-for="implementation in implementationHistory" 
                :key="implementation.id"
                class="history-item"
              >
                <div class="history-icon" :class="`status-${implementation.status.toLowerCase()}`">
                  <i :class="getImplementationStatusIcon(implementation.status)"></i>
                </div>
                <div class="history-content">
                  <h4>{{ implementation.title }}</h4>
                  <p>{{ implementation.description }}</p>
                  <div class="history-meta">
                    <span class="implementation-date">{{ formatDate(implementation.implementedAt) }}</span>
                    <span class="implementation-impact" :class="getImpactClass(implementation.actualImpact)">
                      {{ implementation.actualImpact > 0 ? '+' : '' }}{{ implementation.actualImpact.toFixed(1) }}% impact
                    </span>
                  </div>
                </div>
                <div class="history-status" :class="`status-${implementation.status.toLowerCase()}`">
                  {{ formatStatus(implementation.status) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Settings Tab -->
      <div v-else-if="activeTab === 'settings'" class="tab-panel">
        <div class="settings-section">
          <div class="section-header">
            <h2>Optimization Settings</h2>
            <p>Configure how recommendations are generated and delivered.</p>
          </div>
          
          <div class="settings-form">
            <div class="setting-group">
              <label class="setting-label">Recommendation Frequency</label>
              <select v-model="settings.frequency" class="setting-select">
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
              <p class="setting-description">How often should we generate new recommendations?</p>
            </div>
            
            <div class="setting-group">
              <label class="setting-label">Minimum Confidence Level</label>
              <div class="confidence-slider">
                <input 
                  type="range" 
                  v-model="settings.minConfidence" 
                  min="0.5" 
                  max="1" 
                  step="0.05"
                  class="slider"
                >
                <span class="confidence-value">{{ (settings.minConfidence * 100).toFixed(0) }}%</span>
              </div>
              <p class="setting-description">Only show recommendations with this confidence level or higher.</p>
            </div>
            
            <div class="setting-group">
              <label class="setting-label">Auto-Accept Low Risk Recommendations</label>
              <div class="setting-toggle">
                <input 
                  type="checkbox" 
                  id="autoAccept" 
                  v-model="settings.autoAccept"
                  class="toggle-input"
                >
                <label for="autoAccept" class="toggle-label">
                  <span class="toggle-slider"></span>
                </label>
                <span class="toggle-text">{{ settings.autoAccept ? 'Enabled' : 'Disabled' }}</span>
              </div>
              <p class="setting-description">Automatically implement low-risk recommendations with high confidence.</p>
            </div>
            
            <div class="setting-group">
              <label class="setting-label">Excluded Recommendation Types</label>
              <div class="checkbox-group">
                <label v-for="type in recommendationTypes" :key="type.value" class="checkbox-item">
                  <input 
                    type="checkbox" 
                    :value="type.value"
                    v-model="settings.excludedTypes"
                  >
                  <span class="checkbox-label">{{ type.label }}</span>
                </label>
              </div>
              <p class="setting-description">Select recommendation types you don't want to receive.</p>
            </div>
            
            <div class="setting-actions">
              <button @click="saveSettings" class="btn btn-primary" :disabled="savingSettings">
                <i v-if="savingSettings" class="pi pi-spin pi-spinner"></i>
                <i v-else class="pi pi-save"></i>
                {{ savingSettings ? 'Saving...' : 'Save Settings' }}
              </button>
              <button @click="resetSettings" class="btn btn-secondary">
                <i class="pi pi-refresh"></i>
                Reset to Defaults
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import OptimizationDashboard from '@/components/optimization/OptimizationDashboard.vue'
import RecommendationCard from '@/components/optimization/RecommendationCard.vue'
import { optimizationAPI } from '@/services/api'

export default {
  name: 'OptimizationView',
  components: {
    OptimizationDashboard,
    RecommendationCard
  },
  
  setup() {
    // Reactive data
    const activeTab = ref('all')
    const optimizationSummary = ref(null)
    const highPriorityRecommendations = ref([])
    const highPriorityLoading = ref(false)
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
    
    // Static data
    const tabs = computed(() => [
      { 
        id: 'all', 
        label: 'All Recommendations', 
        icon: 'pi pi-list',
        count: optimizationSummary.value?.totalRecommendations || 0
      },
      { 
        id: 'priority', 
        label: 'High Priority', 
        icon: 'pi pi-exclamation-triangle',
        count: optimizationSummary.value?.highPriorityCount || 0
      },
      { 
        id: 'category', 
        label: 'By Category', 
        icon: 'pi pi-th-large',
        count: 0
      },
      { 
        id: 'tracking', 
        label: 'Performance', 
        icon: 'pi pi-chart-line',
        count: 0
      },
      { 
        id: 'settings', 
        label: 'Settings', 
        icon: 'pi pi-cog',
        count: 0
      }
    ])
    
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
        const response = await optimizationAPI.getOptimizationSummary()
        optimizationSummary.value = response.data
      } catch (error) {
        console.error('Error fetching optimization summary:', error)
      }
    }
    
    const fetchHighPriorityRecommendations = async () => {
      try {
        highPriorityLoading.value = true
        const response = await optimizationAPI.getHighPriorityRecommendations()
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
        await optimizationAPI.acceptRecommendation(recommendation.id)
        recommendation.status = 'ACCEPTED'
      } catch (error) {
        console.error('Error accepting recommendation:', error)
      }
    }
    
    const handleDismissRecommendation = async (recommendation, reason) => {
      try {
        await optimizationAPI.dismissRecommendation(recommendation.id, reason)
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
        await optimizationAPI.updateRecommendationSettings(settings.value)
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
    
    // Lifecycle
    onMounted(() => {
      fetchOptimizationSummary()
      fetchHighPriorityRecommendations()
      loadImplementationHistory()
    })
    
    return {
      activeTab,
      tabs,
      optimizationSummary,
      highPriorityRecommendations,
      highPriorityLoading,
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
      getImpactClass,
      formatDate,
      formatStatus
    }
  }
}
</script>

<style scoped>
.optimization-view {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

.page-header {
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
  margin: 0 0 8px 0;
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

.quick-stats {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  min-width: 120px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

.high-priority .stat-value {
  color: #dc2626;
}

.potential-impact .stat-value {
  color: #059669;
}

.nav-tabs {
  display: flex;
  gap: 4px;
  background: #f3f4f6;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 24px;
  overflow-x: auto;
}

.nav-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.nav-tab.active {
  background: white;
  color: #374151;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.nav-tab:hover:not(.active) {
  color: #374151;
}

.tab-count {
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.nav-tab.active .tab-count {
  background: #3b82f6;
}

.tab-content {
  background: white;
  border-radius: 12px;
  min-height: 600px;
}

.tab-panel {
  padding: 24px;
}

.section-header {
  margin-bottom: 24px;
}

.section-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
}

.section-header p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px;
  text-align: center;
  color: #6b7280;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state i {
  font-size: 48px;
  color: #10b981;
  margin-bottom: 16px;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  font-size: 20px;
}

.category-info {
  flex: 1;
}

.category-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.category-info p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
}

.category-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.recommendation-count {
  color: #6b7280;
}

.potential-impact {
  color: #059669;
  font-weight: 500;
}

.category-arrow {
  color: #9ca3af;
  font-size: 16px;
}

.implementation-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #3b82f6;
  color: white;
  border-radius: 12px;
  font-size: 20px;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.implementation-history h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.history-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 18px;
}

.history-icon.status-success {
  background: #dcfce7;
  color: #166534;
}

.history-icon.status-partial {
  background: #fef3c7;
  color: #92400e;
}

.history-icon.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.history-content {
  flex: 1;
}

.history-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
}

.history-content p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
}

.history-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.implementation-date {
  color: #9ca3af;
}

.implementation-impact {
  font-weight: 500;
}

.impact-positive {
  color: #059669;
}

.impact-negative {
  color: #dc2626;
}

.impact-neutral {
  color: #6b7280;
}

.history-status {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 4px;
}

.history-status.status-success {
  background: #dcfce7;
  color: #166534;
}

.history-status.status-partial {
  background: #fef3c7;
  color: #92400e;
}

.history-status.status-failed {
  background: #fee2e2;
  color: #991b1b;
}

.settings-form {
  max-width: 600px;
}

.setting-group {
  margin-bottom: 24px;
}

.setting-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.setting-select {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
}

.confidence-slider {
  display: flex;
  align-items: center;
  gap: 12px;
}

.slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
}

.confidence-value {
  font-weight: 600;
  color: #374151;
  min-width: 40px;
}

.setting-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-input {
  display: none;
}

.toggle-label {
  position: relative;
  width: 48px;
  height: 24px;
  background: #d1d5db;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.toggle-slider {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.toggle-input:checked + .toggle-label {
  background: #3b82f6;
}

.toggle-input:checked + .toggle-label .toggle-slider {
  transform: translateX(24px);
}

.toggle-text {
  font-size: 14px;
  color: #6b7280;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label {
  font-size: 14px;
  color: #374151;
}

.setting-description {
  font-size: 13px;
  color: #6b7280;
  margin: 8px 0 0 0;
}

.setting-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

/* Responsive Design */
@media (max-width: 768px) {
  .optimization-view {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .quick-stats {
    justify-content: space-between;
  }
  
  .stat-item {
    min-width: auto;
    flex: 1;
  }
  
  .nav-tabs {
    overflow-x: auto;
  }
  
  .tab-panel {
    padding: 16px;
  }
  
  .recommendations-grid,
  .category-grid {
    grid-template-columns: 1fr;
  }
  
  .implementation-stats {
    grid-template-columns: 1fr;
  }
  
  .history-item {
    flex-direction: column;
    gap: 12px;
  }
  
  .history-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
