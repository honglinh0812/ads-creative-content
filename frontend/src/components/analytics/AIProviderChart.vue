<template>
  <div class="ai-provider-chart">
    <div v-if="loading" class="chart-loading">
      <div class="loading-spinner"></div>
      <p>Loading AI provider data...</p>
    </div>
    
    <div v-else-if="!data || !data.providerMetrics" class="chart-empty">
      <i class="pi pi-cog"></i>
      <h3>No AI Provider Data</h3>
      <p>No AI provider analytics available for the selected period.</p>
    </div>
    
    <div v-else class="chart-content">
      <!-- Summary Stats -->
      <div class="provider-summary">
        <div class="summary-card">
          <div class="summary-icon">
            <i class="pi pi-star"></i>
          </div>
          <div class="summary-info">
            <span class="summary-label">Most Used</span>
            <span class="summary-value">{{ data.mostUsedProvider || 'N/A' }}</span>
          </div>
        </div>
        
        <div class="summary-card">
          <div class="summary-icon">
            <i class="pi pi-trophy"></i>
          </div>
          <div class="summary-info">
            <span class="summary-label">Best Performing</span>
            <span class="summary-value">{{ data.bestPerformingProvider || 'N/A' }}</span>
          </div>
        </div>
        
        <div class="summary-card">
          <div class="summary-icon">
            <i class="pi pi-dollar"></i>
          </div>
          <div class="summary-info">
            <span class="summary-label">Total Cost</span>
            <span class="summary-value">{{ formatCurrency(data.totalAICost) }}</span>
          </div>
        </div>
        
        <div class="summary-card">
          <div class="summary-icon">
            <i class="pi pi-money-bill"></i>
          </div>
          <div class="summary-info">
            <span class="summary-label">Estimated Savings</span>
            <span class="summary-value">{{ formatCurrency(data.estimatedSavings) }}</span>
          </div>
        </div>
      </div>

      <!-- Provider Performance Chart -->
      <div class="chart-section">
        <h4 class="chart-title">Provider Performance Comparison</h4>
        <div class="chart-container">
          <canvas ref="chartCanvas" :width="chartWidth" :height="chartHeight"></canvas>
        </div>
      </div>

      <!-- Provider Details Table -->
      <div class="provider-details">
        <h4 class="section-title">Provider Details</h4>
        <div class="provider-grid">
          <div
            v-for="(metrics, providerName) in data.providerMetrics"
            :key="providerName"
            class="provider-card"
          >
            <div class="provider-header">
              <div class="provider-name">
                <i :class="getProviderIcon(providerName)"></i>
                <span>{{ formatProviderName(providerName) }}</span>
              </div>
              <div class="provider-score" :class="getScoreClass(metrics.selectionRate)">
                {{ metrics.selectionRate.toFixed(1) }}%
              </div>
            </div>
            
            <div class="provider-metrics">
              <div class="metric-row">
                <span class="metric-label">Content Generated</span>
                <span class="metric-value">{{ metrics.contentCount.toLocaleString() }}</span>
              </div>
              <div class="metric-row">
                <span class="metric-label">Success Rate</span>
                <span class="metric-value">{{ metrics.successRate.toFixed(1) }}%</span>
              </div>
              <div class="metric-row">
                <span class="metric-label">Avg Response Time</span>
                <span class="metric-value">{{ formatResponseTime(metrics.averageResponseTime) }}</span>
              </div>
              <div class="metric-row">
                <span class="metric-label">Total Cost</span>
                <span class="metric-value">{{ formatCurrency(metrics.totalCost) }}</span>
              </div>
            </div>
            
            <!-- Performance Bar -->
            <div class="performance-bar">
              <div class="bar-background">
                <div 
                  class="bar-fill" 
                  :style="{ 
                    width: `${metrics.selectionRate}%`,
                    backgroundColor: getProviderColor(providerName)
                  }"
                ></div>
              </div>
              <span class="bar-label">Selection Rate</span>
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
  name: 'AIProviderChart',
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
    const chartCanvas = ref(null)
    const chartWidth = ref(400)
    const chartHeight = ref(300)
    let resizeObserver = null
    
    // Provider colors mapping
    const providerColors = {
      'OPENAI': '#10b981',
      'GEMINI': '#3b82f6',
      'ANTHROPIC': '#8b5cf6',
      'HUGGINGFACE': '#f59e0b',
      'FAL_AI': '#ef4444',
      'STABLE_DIFFUSION': '#06b6d4'
    }
    
    // Methods
    const formatCurrency = (value) => {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(value || 0)
    }
    
    const formatProviderName = (name) => {
      return name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const formatResponseTime = (time) => {
      if (time < 1000) {
        return `${Math.round(time)}ms`
      }
      return `${(time / 1000).toFixed(1)}s`
    }
    
    const getProviderIcon = (providerName) => {
      const icons = {
        'OPENAI': 'pi pi-bolt',
        'GEMINI': 'pi pi-star',
        'ANTHROPIC': 'pi pi-brain',
        'HUGGINGFACE': 'pi pi-heart',
        'FAL_AI': 'pi pi-flash',
        'STABLE_DIFFUSION': 'pi pi-image'
      }
      return icons[providerName] || 'pi pi-cog'
    }
    
    const getProviderColor = (providerName) => {
      return providerColors[providerName] || '#6b7280'
    }
    
    const getScoreClass = (score) => {
      if (score >= 80) return 'score-excellent'
      if (score >= 60) return 'score-good'
      if (score >= 40) return 'score-average'
      return 'score-poor'
    }
    
    const createChart = () => {
      if (!chartCanvas.value || !props.data?.providerMetrics) return
      
      const ctx = chartCanvas.value.getContext('2d')
      const canvas = chartCanvas.value
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      const providers = Object.entries(props.data.providerMetrics)
      if (providers.length === 0) return
      
      // Chart dimensions
      const padding = 40
      const chartArea = {
        left: padding,
        top: padding,
        right: canvas.width - padding,
        bottom: canvas.height - padding
      }
      
      const chartWidth = chartArea.right - chartArea.left
      const chartHeight = chartArea.bottom - chartArea.top
      
      // Calculate bar dimensions
      const barWidth = chartWidth / providers.length * 0.6
      const barSpacing = chartWidth / providers.length
      
      // Find max value for scaling
      const maxValue = Math.max(...providers.map(([, metrics]) => metrics.selectionRate))
      const scale = chartHeight / (maxValue * 1.1) // Add 10% padding
      
      // Draw bars
      providers.forEach(([providerName, metrics], index) => {
        const x = chartArea.left + index * barSpacing + (barSpacing - barWidth) / 2
        const barHeight = metrics.selectionRate * scale
        const y = chartArea.bottom - barHeight
        
        // Draw bar
        ctx.fillStyle = getProviderColor(providerName)
        ctx.fillRect(x, y, barWidth, barHeight)
        
        // Draw value label on top of bar
        ctx.fillStyle = '#374151'
        ctx.font = '12px system-ui'
        ctx.textAlign = 'center'
        ctx.fillText(
          `${metrics.selectionRate.toFixed(1)}%`,
          x + barWidth / 2,
          y - 5
        )
        
        // Draw provider name at bottom
        ctx.fillStyle = '#6b7280'
        ctx.font = '11px system-ui'
        const shortName = formatProviderName(providerName).split(' ')[0]
        ctx.fillText(
          shortName,
          x + barWidth / 2,
          chartArea.bottom + 15
        )
      })
      
      // Draw Y-axis labels
      ctx.fillStyle = '#6b7280'
      ctx.font = '10px system-ui'
      ctx.textAlign = 'right'
      
      for (let i = 0; i <= 5; i++) {
        const value = (maxValue / 5) * i
        const y = chartArea.bottom - (value * scale)
        ctx.fillText(`${value.toFixed(0)}%`, chartArea.left - 5, y + 3)
        
        // Draw grid line
        ctx.strokeStyle = '#f3f4f6'
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(chartArea.left, y)
        ctx.lineTo(chartArea.right, y)
        ctx.stroke()
      }
    }
    
    const handleResize = () => {
      if (!chartCanvas.value) return
      
      const container = chartCanvas.value.parentElement
      if (container) {
        chartWidth.value = Math.min(container.clientWidth, 500)
        chartHeight.value = 300
        
        nextTick(() => {
          createChart()
        })
      }
    }
    
    // Lifecycle
    onMounted(() => {
      handleResize()
      
      if (chartCanvas.value?.parentElement) {
        resizeObserver = new ResizeObserver(handleResize)
        resizeObserver.observe(chartCanvas.value.parentElement)
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
        createChart()
      })
    }, { deep: true })
    
    return {
      chartCanvas,
      chartWidth,
      chartHeight,
      formatCurrency,
      formatProviderName,
      formatResponseTime,
      getProviderIcon,
      getProviderColor,
      getScoreClass
    }
  }
}
</script>

<style scoped>
.ai-provider-chart {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-loading,
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
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

.chart-empty i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #d1d5db;
}

.provider-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.summary-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #3b82f6;
  color: white;
  border-radius: 8px;
}

.summary-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.summary-value {
  font-size: 16px;
  color: #111827;
  font-weight: 600;
}

.chart-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-title,
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.chart-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.provider-card {
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.provider-card:hover {
  box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
  transform: translateY(-2px);
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.provider-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #111827;
}

.provider-name i {
  font-size: 18px;
  color: #6b7280;
}

.provider-score {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.score-excellent {
  background: #dcfce7;
  color: #166534;
}

.score-good {
  background: #dbeafe;
  color: #1d4ed8;
}

.score-average {
  background: #fef3c7;
  color: #92400e;
}

.score-poor {
  background: #fee2e2;
  color: #991b1b;
}

.provider-metrics {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-label {
  font-size: 13px;
  color: #6b7280;
}

.metric-value {
  font-size: 13px;
  color: #111827;
  font-weight: 500;
}

.performance-bar {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bar-background {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.bar-label {
  font-size: 11px;
  color: #6b7280;
  text-align: center;
}

/* Responsive Design */
@media (width <= 768px) {
  .provider-summary {
    grid-template-columns: 1fr;
  }
  
  .summary-card {
    padding: 12px;
  }
  
  .summary-icon {
    width: 32px;
    height: 32px;
  }
  
  .provider-grid {
    grid-template-columns: 1fr;
  }
  
  .provider-card {
    padding: 16px;
  }
  
  .chart-container {
    padding: 12px;
  }
}
</style>
