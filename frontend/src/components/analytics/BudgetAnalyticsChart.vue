<template>
  <div class="budget-analytics-chart">
    <div v-if="loading" class="chart-loading">
      <div class="loading-spinner"></div>
      <p>Loading budget data...</p>
    </div>
    
    <div v-else-if="!data" class="chart-empty">
      <i class="pi pi-dollar"></i>
      <h3>No Budget Data</h3>
      <p>No budget analytics available for the selected period.</p>
    </div>
    
    <div v-else class="chart-content">
      <!-- Budget Overview -->
      <div class="budget-overview">
        <div class="overview-card total-budget">
          <div class="card-icon">
            <i class="pi pi-wallet"></i>
          </div>
          <div class="card-content">
            <span class="card-label">Total Budget</span>
            <span class="card-value">{{ formatCurrency(data.totalBudgetAllocated) }}</span>
          </div>
        </div>
        
        <div class="overview-card spent-budget">
          <div class="card-icon">
            <i class="pi pi-credit-card"></i>
          </div>
          <div class="card-content">
            <span class="card-label">Total Spent</span>
            <span class="card-value">{{ formatCurrency(data.totalBudgetSpent) }}</span>
          </div>
        </div>
        
        <div class="overview-card utilization">
          <div class="card-icon">
            <i class="pi pi-chart-pie"></i>
          </div>
          <div class="card-content">
            <span class="card-label">Utilization Rate</span>
            <span class="card-value">{{ data.budgetUtilizationRate.toFixed(1) }}%</span>
          </div>
        </div>
        
        <div class="overview-card daily-spend">
          <div class="card-icon">
            <i class="pi pi-calendar"></i>
          </div>
          <div class="card-content">
            <span class="card-label">Avg Daily Spend</span>
            <span class="card-value">{{ formatCurrency(data.averageDailySpend) }}</span>
          </div>
        </div>
      </div>

      <!-- Budget Utilization Progress -->
      <div class="utilization-section">
        <h4 class="section-title">Budget Utilization</h4>
        <div class="utilization-bar">
          <div class="progress-background">
            <div 
              class="progress-fill" 
              :style="{ 
                width: `${Math.min(data.budgetUtilizationRate, 100)}%`,
                backgroundColor: getUtilizationColor(data.budgetUtilizationRate)
              }"
            ></div>
          </div>
          <div class="progress-labels">
            <span class="progress-spent">{{ formatCurrency(data.totalBudgetSpent) }}</span>
            <span class="progress-total">{{ formatCurrency(data.totalBudgetAllocated) }}</span>
          </div>
        </div>
        
        <div class="utilization-status" :class="getUtilizationClass(data.budgetUtilizationRate)">
          <i :class="getUtilizationIcon(data.budgetUtilizationRate)"></i>
          <span>{{ getUtilizationMessage(data.budgetUtilizationRate) }}</span>
        </div>
      </div>

      <!-- Budget Breakdown Pie Chart -->
      <div class="breakdown-section">
        <h4 class="section-title">Budget Breakdown</h4>
        <div class="breakdown-container">
          <div class="pie-chart-container">
            <canvas ref="pieChartCanvas" :width="pieChartSize" :height="pieChartSize"></canvas>
          </div>
          <div class="breakdown-legend">
            <div
              v-for="(item, index) in data.budgetBreakdown"
              :key="item.category"
              class="legend-item"
            >
              <div 
                class="legend-color" 
                :style="{ backgroundColor: breakdownColors[index % breakdownColors.length] }"
              ></div>
              <div class="legend-info">
                <span class="legend-label">{{ item.category }}</span>
                <span class="legend-value">{{ formatCurrency(item.amount) }}</span>
                <span class="legend-percentage">{{ item.percentage.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Spending Trends -->
      <div class="trends-section">
        <h4 class="section-title">Spending Trends</h4>
        <div class="trends-chart-container">
          <canvas ref="trendsChartCanvas" :width="trendsChartWidth" :height="trendsChartHeight"></canvas>
        </div>
      </div>

      <!-- Projections -->
      <div class="projections-section">
        <h4 class="section-title">Budget Projections</h4>
        <div class="projections-grid">
          <div class="projection-card">
            <div class="projection-icon">
              <i class="pi pi-calendar-plus"></i>
            </div>
            <div class="projection-content">
              <span class="projection-label">Projected Monthly Spend</span>
              <span class="projection-value">{{ formatCurrency(data.projectedMonthlySpend) }}</span>
              <span class="projection-note">Based on current trends</span>
            </div>
          </div>
          
          <div class="projection-card">
            <div class="projection-icon">
              <i class="pi pi-clock"></i>
            </div>
            <div class="projection-content">
              <span class="projection-label">Budget Depletion</span>
              <span class="projection-value">{{ getBudgetDepletionTime() }}</span>
              <span class="projection-note">At current spend rate</span>
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
  name: 'BudgetAnalyticsChart',
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
    const pieChartCanvas = ref(null)
    const trendsChartCanvas = ref(null)
    const pieChartSize = ref(200)
    const trendsChartWidth = ref(600)
    const trendsChartHeight = ref(200)
    let resizeObserver = null
    
    const breakdownColors = [
      '#3b82f6', // Blue
      '#10b981', // Green
      '#f59e0b', // Yellow
      '#ef4444', // Red
      '#8b5cf6', // Purple
      '#06b6d4'  // Cyan
    ]
    
    // Methods
    const formatCurrency = (value) => {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
      }).format(value || 0)
    }
    
    const getUtilizationColor = (rate) => {
      if (rate >= 90) return '#ef4444' // Red
      if (rate >= 70) return '#f59e0b' // Yellow
      return '#10b981' // Green
    }
    
    const getUtilizationClass = (rate) => {
      if (rate >= 90) return 'utilization-high'
      if (rate >= 70) return 'utilization-medium'
      return 'utilization-low'
    }
    
    const getUtilizationIcon = (rate) => {
      if (rate >= 90) return 'pi pi-exclamation-triangle'
      if (rate >= 70) return 'pi pi-info-circle'
      return 'pi pi-check-circle'
    }
    
    const getUtilizationMessage = (rate) => {
      if (rate >= 90) return 'High utilization - Monitor spending closely'
      if (rate >= 70) return 'Moderate utilization - On track'
      return 'Low utilization - Room for increased spending'
    }
    
    const getBudgetDepletionTime = () => {
      if (!props.data || props.data.averageDailySpend === 0) return 'N/A'
      
      const remaining = props.data.totalBudgetAllocated - props.data.totalBudgetSpent
      const daysRemaining = Math.floor(remaining / props.data.averageDailySpend)
      
      if (daysRemaining <= 0) return 'Budget exhausted'
      if (daysRemaining === 1) return '1 day'
      if (daysRemaining < 30) return `${daysRemaining} days`
      if (daysRemaining < 365) return `${Math.floor(daysRemaining / 30)} months`
      return `${Math.floor(daysRemaining / 365)} years`
    }
    
    const createPieChart = () => {
      if (!pieChartCanvas.value || !props.data?.budgetBreakdown) return
      
      const ctx = pieChartCanvas.value.getContext('2d')
      const canvas = pieChartCanvas.value
      const centerX = canvas.width / 2
      const centerY = canvas.height / 2
      const radius = Math.min(centerX, centerY) - 20
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      let currentAngle = -Math.PI / 2 // Start from top
      
      props.data.budgetBreakdown.forEach((item, index) => {
        const sliceAngle = (item.percentage / 100) * 2 * Math.PI
        const color = breakdownColors[index % breakdownColors.length]
        
        // Draw slice
        ctx.beginPath()
        ctx.moveTo(centerX, centerY)
        ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle)
        ctx.closePath()
        ctx.fillStyle = color
        ctx.fill()
        
        // Draw border
        ctx.strokeStyle = '#ffffff'
        ctx.lineWidth = 2
        ctx.stroke()
        
        currentAngle += sliceAngle
      })
      
      // Draw center circle for donut effect
      ctx.beginPath()
      ctx.arc(centerX, centerY, radius * 0.5, 0, 2 * Math.PI)
      ctx.fillStyle = '#ffffff'
      ctx.fill()
      ctx.strokeStyle = '#e5e7eb'
      ctx.lineWidth = 1
      ctx.stroke()
      
      // Draw center text
      ctx.fillStyle = '#111827'
      ctx.font = 'bold 16px system-ui'
      ctx.textAlign = 'center'
      ctx.fillText('Budget', centerX, centerY - 5)
      ctx.font = '12px system-ui'
      ctx.fillStyle = '#6b7280'
      ctx.fillText('Breakdown', centerX, centerY + 10)
    }
    
    const createTrendsChart = () => {
      if (!trendsChartCanvas.value || !props.data?.spendingTrends) return
      
      const ctx = trendsChartCanvas.value.getContext('2d')
      const canvas = trendsChartCanvas.value
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      const padding = 40
      const chartArea = {
        left: padding,
        top: padding / 2,
        right: canvas.width - padding,
        bottom: canvas.height - padding
      }
      
      const chartWidth = chartArea.right - chartArea.left
      const chartHeight = chartArea.bottom - chartArea.top
      
      const trends = props.data.spendingTrends.slice(-30) // Last 30 days
      if (trends.length === 0) return
      
      // Find min/max values
      const amounts = trends.map(t => t.amount)
      const minAmount = Math.min(...amounts)
      const maxAmount = Math.max(...amounts)
      const range = maxAmount - minAmount || 1
      
      // Draw grid lines
      ctx.strokeStyle = '#f3f4f6'
      ctx.lineWidth = 1
      
      for (let i = 0; i <= 4; i++) {
        const y = chartArea.top + (chartHeight / 4) * i
        ctx.beginPath()
        ctx.moveTo(chartArea.left, y)
        ctx.lineTo(chartArea.right, y)
        ctx.stroke()
      }
      
      // Draw trend line
      if (trends.length > 1) {
        ctx.strokeStyle = '#3b82f6'
        ctx.lineWidth = 2
        ctx.beginPath()
        
        trends.forEach((trend, index) => {
          const x = chartArea.left + (chartWidth / (trends.length - 1)) * index
          const y = chartArea.bottom - ((trend.amount - minAmount) / range) * chartHeight
          
          if (index === 0) {
            ctx.moveTo(x, y)
          } else {
            ctx.lineTo(x, y)
          }
        })
        
        ctx.stroke()
        
        // Draw data points
        ctx.fillStyle = '#3b82f6'
        trends.forEach((trend, index) => {
          const x = chartArea.left + (chartWidth / (trends.length - 1)) * index
          const y = chartArea.bottom - ((trend.amount - minAmount) / range) * chartHeight
          
          ctx.beginPath()
          ctx.arc(x, y, 3, 0, 2 * Math.PI)
          ctx.fill()
        })
      }
      
      // Draw Y-axis labels
      ctx.fillStyle = '#6b7280'
      ctx.font = '10px system-ui'
      ctx.textAlign = 'right'
      
      for (let i = 0; i <= 4; i++) {
        const value = minAmount + (range / 4) * (4 - i)
        const y = chartArea.top + (chartHeight / 4) * i
        ctx.fillText(`$${Math.round(value)}`, chartArea.left - 5, y + 3)
      }
    }
    
    const handleResize = () => {
      nextTick(() => {
        createPieChart()
        createTrendsChart()
      })
    }
    
    // Lifecycle
    onMounted(() => {
      handleResize()
      
      if (pieChartCanvas.value?.parentElement) {
        resizeObserver = new ResizeObserver(handleResize)
        resizeObserver.observe(pieChartCanvas.value.parentElement)
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
        createPieChart()
        createTrendsChart()
      })
    }, { deep: true })
    
    return {
      pieChartCanvas,
      trendsChartCanvas,
      pieChartSize,
      trendsChartWidth,
      trendsChartHeight,
      breakdownColors,
      formatCurrency,
      getUtilizationColor,
      getUtilizationClass,
      getUtilizationIcon,
      getUtilizationMessage,
      getBudgetDepletionTime
    }
  }
}
</script>

<style scoped>
.budget-analytics-chart {
  @apply flex flex-col gap-6;
}

.chart-loading,
.chart-empty {
  @apply flex flex-col items-center justify-center p-12 text-gray-500 dark:text-gray-400;
}

.loading-spinner {
  @apply w-8 h-8 border-4 border-gray-200 border-t-blue-500 rounded-full animate-spin mb-4;
}

.chart-empty i {
  @apply text-5xl mb-4 text-gray-300 dark:text-gray-600;
}

.budget-overview {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4;
}

.overview-card {
  @apply flex items-center gap-3 p-5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl transition-all duration-300 hover:shadow-lg hover:shadow-gray-200/50 dark:hover:shadow-gray-900/50;
}

.card-icon {
  @apply flex items-center justify-center w-12 h-12 rounded-xl text-xl;
}

.total-budget .card-icon {
  @apply bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400;
}

.spent-budget .card-icon {
  @apply bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400;
}

.utilization .card-icon {
  @apply bg-yellow-50 dark:bg-yellow-900/20 text-yellow-600 dark:text-yellow-400;
}

.daily-spend .card-icon {
  @apply bg-purple-50 dark:bg-purple-900/20 text-purple-600 dark:text-purple-400;
}

.card-content {
  @apply flex flex-col gap-1;
}

.card-label {
  @apply text-sm text-gray-500 dark:text-gray-400 font-medium;
}

.card-value {
  @apply text-xl text-gray-900 dark:text-white font-bold;
}

.section-title {
  @apply text-lg font-semibold text-gray-900 dark:text-white mb-4;
}

.utilization-bar {
  @apply mb-3;
}

.progress-background {
  @apply h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-2;
}

.progress-fill {
  @apply h-full rounded-full transition-all duration-500 ease-out;
}

.progress-labels {
  @apply flex justify-between text-xs text-gray-600 dark:text-gray-400;
}

.utilization-status {
  @apply flex items-center gap-2 p-3 rounded-lg text-sm font-medium;
}

.utilization-low {
  @apply bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-800;
}

.utilization-medium {
  @apply bg-yellow-50 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-400 border border-yellow-200 dark:border-yellow-800;
}

.utilization-high {
  @apply bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-400 border border-red-200 dark:border-red-800;
}

.breakdown-container {
  @apply flex items-center gap-8;
}

.pie-chart-container {
  @apply flex-shrink-0;
}

.breakdown-legend {
  @apply flex-1 flex flex-col gap-3;
}

.legend-item {
  @apply flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg;
}

.legend-color {
  @apply w-4 h-4 rounded flex-shrink-0;
}

.legend-info {
  @apply flex flex-col gap-0.5 flex-1;
}

.legend-label {
  @apply text-sm text-gray-900 dark:text-white font-medium;
}

.legend-value {
  @apply text-xs text-gray-600 dark:text-gray-400;
}

.legend-percentage {
  @apply text-xs text-gray-400 dark:text-gray-500;
}

.trends-chart-container {
  @apply p-5 bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 rounded-lg;
}

.projections-grid {
  @apply grid grid-cols-1 lg:grid-cols-2 gap-4;
}

.projection-card {
  @apply flex items-center gap-4 p-5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-sm hover:shadow-lg transition-all duration-300;
}

.projection-icon {
  @apply flex items-center justify-center w-12 h-12 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 rounded-xl text-xl;
}

.projection-content {
  @apply flex flex-col gap-1;
}

.projection-label {
  @apply text-sm text-gray-500 dark:text-gray-400 font-medium;
}

.projection-value {
  @apply text-lg text-gray-900 dark:text-white font-bold;
}

.projection-note {
  @apply text-xs text-gray-400 dark:text-gray-500;
}

/* Responsive Design */
@media (width <= 768px) {
  .budget-overview {
    @apply grid-cols-1;
  }
  
  .breakdown-container {
    @apply flex-col gap-5;
  }
  
  .projections-grid {
    @apply grid-cols-1;
  }
  
  .overview-card,
  .projection-card {
    @apply p-4;
  }
  
  .card-icon,
  .projection-icon {
    @apply w-10 h-10 text-lg;
  }
}
</style>
