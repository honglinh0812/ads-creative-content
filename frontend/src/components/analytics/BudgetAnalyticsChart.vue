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

.budget-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.overview-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  font-size: 20px;
}

.total-budget .card-icon {
  background: #eff6ff;
  color: #2563eb;
}

.spent-budget .card-icon {
  background: #ecfdf5;
  color: #059669;
}

.utilization .card-icon {
  background: #fef3c7;
  color: #d97706;
}

.daily-spend .card-icon {
  background: #f5f3ff;
  color: #7c3aed;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.card-value {
  font-size: 20px;
  color: #111827;
  font-weight: 700;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 16px 0;
}

.utilization-bar {
  margin-bottom: 12px;
}

.progress-background {
  height: 12px;
  background: #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s ease;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.utilization-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
}

.utilization-low {
  background: #dcfce7;
  color: #166534;
}

.utilization-medium {
  background: #fef3c7;
  color: #92400e;
}

.utilization-high {
  background: #fee2e2;
  color: #991b1b;
}

.breakdown-container {
  display: flex;
  align-items: center;
  gap: 32px;
}

.pie-chart-container {
  flex-shrink: 0;
}

.breakdown-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
}

.legend-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.legend-label {
  font-size: 14px;
  color: #111827;
  font-weight: 500;
}

.legend-value {
  font-size: 13px;
  color: #6b7280;
}

.legend-percentage {
  font-size: 12px;
  color: #9ca3af;
}

.trends-chart-container {
  padding: 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.projections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.projection-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.projection-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #f0fdf4;
  color: #16a34a;
  border-radius: 12px;
  font-size: 20px;
}

.projection-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.projection-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.projection-value {
  font-size: 18px;
  color: #111827;
  font-weight: 700;
}

.projection-note {
  font-size: 12px;
  color: #9ca3af;
}

/* Responsive Design */
@media (max-width: 768px) {
  .budget-overview {
    grid-template-columns: 1fr;
  }
  
  .breakdown-container {
    flex-direction: column;
    gap: 20px;
  }
  
  .projections-grid {
    grid-template-columns: 1fr;
  }
  
  .overview-card,
  .projection-card {
    padding: 16px;
  }
  
  .card-icon,
  .projection-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}
</style>
