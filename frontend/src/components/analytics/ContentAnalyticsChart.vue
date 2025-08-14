<template>
  <div class="content-analytics-chart">
    <div v-if="loading" class="chart-loading">
      <div class="loading-spinner"></div>
      <p>Loading content data...</p>
    </div>
    
    <div v-else-if="!data" class="chart-empty">
      <i class="pi pi-file-edit"></i>
      <h3>No Content Data</h3>
      <p>No content analytics available for the selected period.</p>
    </div>
    
    <div v-else class="chart-content">
      <!-- Content Overview -->
      <div class="content-overview">
        <div class="overview-stat">
          <div class="stat-icon">
            <i class="pi pi-file"></i>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ data.totalContentGenerated.toLocaleString() }}</span>
            <span class="stat-label">Total Generated</span>
          </div>
        </div>
        
        <div class="overview-stat">
          <div class="stat-icon">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ data.selectedContent.toLocaleString() }}</span>
            <span class="stat-label">Selected</span>
          </div>
        </div>
        
        <div class="overview-stat">
          <div class="stat-icon">
            <i class="pi pi-percentage"></i>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ data.selectionRate.toFixed(1) }}%</span>
            <span class="stat-label">Selection Rate</span>
          </div>
        </div>
      </div>

      <!-- Content Type Distribution -->
      <div class="distribution-section">
        <h4 class="section-title">Content Type Distribution</h4>
        <div class="distribution-chart">
          <canvas ref="distributionCanvas" :width="chartWidth" :height="chartHeight"></canvas>
          <div class="distribution-legend">
            <div
              v-for="(count, type) in data.contentTypeDistribution"
              :key="type"
              class="legend-item"
            >
              <div 
                class="legend-color" 
                :style="{ backgroundColor: getTypeColor(type) }"
              ></div>
              <span class="legend-label">{{ formatContentType(type) }}</span>
              <span class="legend-count">{{ count.toLocaleString() }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- AI Provider Performance -->
      <div class="provider-performance-section">
        <h4 class="section-title">AI Provider Performance</h4>
        <div class="provider-bars">
          <div
            v-for="(performance, provider) in data.aiProviderPerformance"
            :key="provider"
            class="provider-bar"
          >
            <div class="provider-info">
              <span class="provider-name">{{ formatProviderName(provider) }}</span>
              <span class="provider-score">{{ performance.toFixed(1) }}%</span>
            </div>
            <div class="bar-container">
              <div class="bar-background">
                <div 
                  class="bar-fill" 
                  :style="{ 
                    width: `${performance}%`,
                    backgroundColor: getProviderColor(provider)
                  }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Content Performance Metrics -->
      <div class="performance-metrics">
        <div class="metric-card">
          <div class="metric-header">
            <i class="pi pi-star"></i>
            <span>Top Performing Content</span>
          </div>
          <div class="metric-value">
            {{ data.topPerformingContent.length }}
          </div>
          <div class="metric-description">
            High-performing content pieces
          </div>
        </div>
        
        <div class="metric-card">
          <div class="metric-header">
            <i class="pi pi-chart-line"></i>
            <span>Avg Performance Score</span>
          </div>
          <div class="metric-value">
            {{ getAveragePerformanceScore().toFixed(1) }}
          </div>
          <div class="metric-description">
            Based on impressions and CTR
          </div>
        </div>
        
        <div class="metric-card">
          <div class="metric-header">
            <i class="pi pi-eye"></i>
            <span>Total Impressions</span>
          </div>
          <div class="metric-value">
            {{ getTotalImpressions().toLocaleString() }}
          </div>
          <div class="metric-description">
            From selected content
          </div>
        </div>
        
        <div class="metric-card">
          <div class="metric-header">
            <i class="pi pi-mouse"></i>
            <span>Avg CTR</span>
          </div>
          <div class="metric-value">
            {{ getAverageCTR().toFixed(2) }}%
          </div>
          <div class="metric-description">
            Click-through rate
          </div>
        </div>
      </div>

      <!-- Content Quality Insights -->
      <div class="quality-insights">
        <h4 class="section-title">Content Quality Insights</h4>
        <div class="insights-grid">
          <div class="insight-card" :class="getSelectionRateClass(data.selectionRate)">
            <div class="insight-icon">
              <i :class="getSelectionRateIcon(data.selectionRate)"></i>
            </div>
            <div class="insight-content">
              <span class="insight-title">Selection Rate</span>
              <span class="insight-message">{{ getSelectionRateMessage(data.selectionRate) }}</span>
            </div>
          </div>
          
          <div class="insight-card">
            <div class="insight-icon">
              <i class="pi pi-lightbulb"></i>
            </div>
            <div class="insight-content">
              <span class="insight-title">Best Provider</span>
              <span class="insight-message">
                {{ getBestProvider() }} shows highest selection rate
              </span>
            </div>
          </div>
          
          <div class="insight-card">
            <div class="insight-icon">
              <i class="pi pi-chart-bar"></i>
            </div>
            <div class="insight-content">
              <span class="insight-title">Content Volume</span>
              <span class="insight-message">
                {{ getVolumeInsight() }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'

export default {
  name: 'ContentAnalyticsChart',
  props: {
    data: {
      type: Object,
      default: () => null
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  
  setup(props) {
    const distributionCanvas = ref(null)
    const chartWidth = ref(300)
    const chartHeight = ref(200)
    let resizeObserver = null
    
    const contentTypeColors = {
      'headline': '#3b82f6',
      'primary_text': '#10b981',
      'description': '#f59e0b',
      'call_to_action': '#ef4444',
      'image': '#8b5cf6',
      'video': '#06b6d4'
    }
    
    const providerColors = {
      'OPENAI': '#10b981',
      'GEMINI': '#3b82f6',
      'ANTHROPIC': '#8b5cf6',
      'HUGGINGFACE': '#f59e0b',
      'FAL_AI': '#ef4444',
      'STABLE_DIFFUSION': '#06b6d4'
    }
    
    // Methods
    const formatContentType = (type) => {
      return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const formatProviderName = (name) => {
      return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const getTypeColor = (type) => {
      return contentTypeColors[type.toLowerCase()] || '#6b7280'
    }
    
    const getProviderColor = (provider) => {
      return providerColors[provider] || '#6b7280'
    }
    
    const getAveragePerformanceScore = () => {
      if (!props.data?.topPerformingContent?.length) return 0
      
      const total = props.data.topPerformingContent.reduce((sum, content) => 
        sum + content.performanceScore, 0)
      return total / props.data.topPerformingContent.length
    }
    
    const getTotalImpressions = () => {
      if (!props.data?.topPerformingContent?.length) return 0
      
      return props.data.topPerformingContent.reduce((sum, content) => 
        sum + content.impressions, 0)
    }
    
    const getAverageCTR = () => {
      if (!props.data?.topPerformingContent?.length) return 0
      
      const total = props.data.topPerformingContent.reduce((sum, content) => 
        sum + content.ctr, 0)
      return total / props.data.topPerformingContent.length
    }
    
    const getSelectionRateClass = (rate) => {
      if (rate >= 70) return 'insight-excellent'
      if (rate >= 50) return 'insight-good'
      if (rate >= 30) return 'insight-average'
      return 'insight-poor'
    }
    
    const getSelectionRateIcon = (rate) => {
      if (rate >= 70) return 'pi pi-check-circle'
      if (rate >= 50) return 'pi pi-info-circle'
      if (rate >= 30) return 'pi pi-exclamation-triangle'
      return 'pi pi-times-circle'
    }
    
    const getSelectionRateMessage = (rate) => {
      if (rate >= 70) return 'Excellent content quality and relevance'
      if (rate >= 50) return 'Good content performance'
      if (rate >= 30) return 'Room for improvement in content quality'
      return 'Consider reviewing content generation strategy'
    }
    
    const getBestProvider = () => {
      if (!props.data?.aiProviderPerformance) return 'N/A'
      
      const providers = Object.entries(props.data.aiProviderPerformance)
      if (providers.length === 0) return 'N/A'
      
      const best = providers.reduce((max, current) => 
        current[1] > max[1] ? current : max)
      
      return formatProviderName(best[0])
    }
    
    const getVolumeInsight = () => {
      const total = props.data?.totalContentGenerated || 0
      if (total > 1000) return 'High content generation volume'
      if (total > 500) return 'Moderate content generation'
      if (total > 100) return 'Growing content library'
      return 'Building content foundation'
    }
    
    const createDistributionChart = () => {
      if (!distributionCanvas.value || !props.data?.contentTypeDistribution) return
      
      const ctx = distributionCanvas.value.getContext('2d')
      const canvas = distributionCanvas.value
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      const types = Object.entries(props.data.contentTypeDistribution)
      if (types.length === 0) return
      
      // Chart dimensions
      const padding = 30
      const chartArea = {
        left: padding,
        top: padding,
        right: canvas.width - padding,
        bottom: canvas.height - padding
      }
      
      const chartWidth = chartArea.right - chartArea.left
      const chartHeight = chartArea.bottom - chartArea.top
      
      // Calculate bar dimensions
      const barHeight = chartHeight / types.length * 0.7
      const barSpacing = chartHeight / types.length
      
      // Find max value for scaling
      const maxValue = Math.max(...types.map(([, count]) => count))
      const scale = chartWidth / (maxValue * 1.1)
      
      // Draw bars
      types.forEach(([type, count], index) => {
        const y = chartArea.top + index * barSpacing + (barSpacing - barHeight) / 2
        const barWidth = count * scale
        
        // Draw bar
        ctx.fillStyle = getTypeColor(type)
        ctx.fillRect(chartArea.left, y, barWidth, barHeight)
        
        // Draw value label
        ctx.fillStyle = '#374151'
        ctx.font = '12px system-ui'
        ctx.textAlign = 'left'
        ctx.fillText(count.toString(), chartArea.left + barWidth + 5, y + barHeight / 2 + 4)
        
        // Draw type label
        ctx.fillStyle = '#6b7280'
        ctx.font = '11px system-ui'
        ctx.textAlign = 'right'
        ctx.fillText(formatContentType(type), chartArea.left - 5, y + barHeight / 2 + 4)
      })
    }
    
    const handleResize = () => {
      nextTick(() => {
        createDistributionChart()
      })
    }
    
    // Lifecycle
    onMounted(() => {
      handleResize()
      
      if (distributionCanvas.value?.parentElement) {
        resizeObserver = new ResizeObserver(handleResize)
        resizeObserver.observe(distributionCanvas.value.parentElement)
      }
    })
    
    onUnmounted(() => {
      if (resizeObserver) {
        resizeObserver.disconnect()
      }
    })
    
    // Watchers
    watch(() => props.data, () => {
      nextTick(() => {
        createDistributionChart()
      })
    }, { deep: true })
    
    return {
      distributionCanvas,
      chartWidth,
      chartHeight,
      formatContentType,
      formatProviderName,
      getTypeColor,
      getProviderColor,
      getAveragePerformanceScore,
      getTotalImpressions,
      getAverageCTR,
      getSelectionRateClass,
      getSelectionRateIcon,
      getSelectionRateMessage,
      getBestProvider,
      getVolumeInsight
    }
  }
}
</script>

<style scoped>
.content-analytics-chart {
  @apply flex flex-col gap-6;
}

.chart-loading,
.chart-empty {
  @apply flex flex-col items-center justify-center p-12 text-gray-500 dark:text-gray-400;
}

.loading-spinner {
  @apply w-8 h-8 border-4 border-gray-200 dark:border-gray-700 border-t-blue-500 rounded-full animate-spin mb-4;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.chart-empty i {
  @apply text-5xl mb-4 text-gray-300 dark:text-gray-600;
}

.content-overview {
  @apply grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4;
}

.overview-stat {
  @apply flex items-center gap-3 p-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm hover:shadow-md transition-all duration-300;
}

.stat-icon {
  @apply flex items-center justify-center w-10 h-10 bg-gradient-to-br from-blue-50 to-blue-100 dark:from-blue-900 dark:to-blue-800 text-blue-600 dark:text-blue-400 rounded-lg text-lg;
}

.stat-info {
  @apply flex flex-col gap-0.5;
}

.stat-value {
  @apply text-xl font-bold text-gray-900 dark:text-white;
}

.stat-label {
  @apply text-xs text-gray-500 dark:text-gray-400 font-medium;
}

.section-title {
  @apply text-lg font-semibold text-gray-900 dark:text-white mb-4;
}

.distribution-chart {
  @apply flex gap-6 items-center;
}

.distribution-legend {
  @apply flex flex-col gap-2;
}

.legend-item {
  @apply flex items-center gap-2;
}

.legend-color {
  @apply w-3 h-3 rounded-sm;
}

.legend-label {
  @apply text-sm text-gray-700 dark:text-gray-300 font-medium flex-1;
}

.legend-count {
  @apply text-sm text-gray-500 dark:text-gray-400 font-semibold;
}

.provider-bars {
  @apply flex flex-col gap-3;
}

.provider-bar {
  @apply flex flex-col gap-1.5;
}

.provider-info {
  @apply flex justify-between items-center;
}

.provider-name {
  @apply text-sm text-gray-700 dark:text-gray-300 font-medium;
}

.provider-score {
  @apply text-sm text-gray-900 dark:text-white font-semibold;
}

.bar-container {
  @apply w-full;
}

.bar-background {
  @apply h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden;
}

.bar-fill {
  @apply h-full rounded-full transition-all duration-500 ease-out;
}

.performance-metrics {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4;
}

.metric-card {
  @apply p-5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl text-center shadow-sm hover:shadow-lg transition-all duration-300;
}

.metric-header {
  @apply flex items-center justify-center gap-2 mb-3 text-sm text-gray-500 dark:text-gray-400 font-medium;
}

.metric-header i {
  @apply text-base;
}

.metric-value {
  @apply text-2xl font-bold text-gray-900 dark:text-white mb-1;
}

.metric-description {
  @apply text-xs text-gray-400 dark:text-gray-500;
}

.insights-grid {
  @apply grid grid-cols-1 lg:grid-cols-2 gap-4;
}

.insight-card {
  @apply flex items-center gap-4 p-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm hover:shadow-md transition-all duration-300;
}

.insight-icon {
  @apply flex items-center justify-center w-10 h-10 rounded-lg text-lg;
}

.insight-excellent .insight-icon {
  background: #dcfce7;
  color: #166534;
}

.insight-good .insight-icon {
  background: #dbeafe;
  color: #1d4ed8;
}

.insight-average .insight-icon {
  background: #fef3c7;
  color: #92400e;
}

.insight-poor .insight-icon {
  background: #fee2e2;
  color: #991b1b;
}

.insight-card:not(.insight-excellent, .insight-good, .insight-average, .insight-poor) .insight-icon {
  background: #f3f4f6;
  color: #6b7280;
}

.insight-content {
  @apply flex-1;
}

.insight-title {
  @apply text-sm font-semibold text-gray-900 dark:text-white mb-1;
}

.insight-message {
  @apply text-xs text-gray-600 dark:text-gray-400 leading-relaxed;
}

/* Responsive Design */
@media (width <= 768px) {
  .content-analytics-chart {
    @apply p-4;
  }
  
  .section-title {
    @apply text-base;
  }
  
  .overview-stat {
    @apply p-4;
  }
  
  .stat-value {
    @apply text-xl;
  }
  
  .content-overview {
    @apply grid-cols-1;
  }
  
  .distribution-chart {
    @apply flex-col gap-4;
  }
  
  .performance-metrics {
    @apply grid-cols-1;
  }
  
  .insights-grid {
    @apply grid-cols-1;
  }
  
  .insight-card {
    @apply p-3 flex-col text-center;
  }
  
  .insight-icon {
    @apply w-8 h-8 text-base;
  }
}
</style>
