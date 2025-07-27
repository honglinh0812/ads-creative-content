<template>
  <div class="performance-trends-chart">
    <div v-if="loading" class="chart-loading">
      <div class="loading-spinner"></div>
      <p>Loading chart data...</p>
    </div>
    
    <div v-else-if="!data || data.length === 0" class="chart-empty">
      <i class="pi pi-chart-line"></i>
      <h3>No Data Available</h3>
      <p>No performance data found for the selected time period.</p>
    </div>
    
    <div v-else class="chart-container">
      <canvas ref="chartCanvas" :width="chartWidth" :height="chartHeight"></canvas>
      
      <!-- Chart Legend -->
      <div class="chart-legend">
        <div class="legend-item">
          <div class="legend-color" :style="{ backgroundColor: chartColor }"></div>
          <span class="legend-label">{{ metricLabel }}</span>
        </div>
      </div>
      
      <!-- Chart Stats -->
      <div class="chart-stats">
        <div class="stat-item">
          <span class="stat-label">Peak</span>
          <span class="stat-value">{{ formatValue(maxValue) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">Average</span>
          <span class="stat-value">{{ formatValue(avgValue) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">Trend</span>
          <span class="stat-value" :class="trendClass">
            <i :class="trendIcon"></i>
            {{ trendPercentage }}%
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'

export default {
  name: 'PerformanceTrendsChart',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    metric: {
      type: String,
      default: 'impressions'
    },
    loading: {
      type: Boolean,
      default: false
    },
    height: {
      type: Number,
      default: 300
    }
  },
  
  setup(props) {
    const chartCanvas = ref(null)
    const chartWidth = ref(800)
    const chartHeight = ref(300)
    let resizeObserver = null
    
    // Computed properties
    const chartColor = computed(() => {
      const colors = {
        impressions: '#3b82f6',
        clicks: '#10b981',
        ctr: '#f59e0b',
        cpc: '#8b5cf6',
        conversions: '#ef4444',
        spend: '#06b6d4'
      }
      return colors[props.metric] || '#6b7280'
    })
    
    const metricLabel = computed(() => {
      const labels = {
        impressions: 'Impressions',
        clicks: 'Clicks',
        ctr: 'CTR (%)',
        cpc: 'CPC ($)',
        conversions: 'Conversions',
        spend: 'Spend ($)'
      }
      return labels[props.metric] || props.metric
    })
    
    const chartData = computed(() => {
      return props.data.map(item => ({
        x: new Date(item.timestamp),
        y: item.value || 0
      }))
    })
    
    const maxValue = computed(() => {
      if (!props.data.length) return 0
      return Math.max(...props.data.map(item => item.value || 0))
    })
    
    const avgValue = computed(() => {
      if (!props.data.length) return 0
      const sum = props.data.reduce((acc, item) => acc + (item.value || 0), 0)
      return sum / props.data.length
    })
    
    const trendPercentage = computed(() => {
      if (props.data.length < 2) return 0
      
      const firstValue = props.data[0]?.value || 0
      const lastValue = props.data[props.data.length - 1]?.value || 0
      
      if (firstValue === 0) return 0
      
      return (((lastValue - firstValue) / firstValue) * 100).toFixed(1)
    })
    
    const trendClass = computed(() => {
      const trend = parseFloat(trendPercentage.value)
      if (trend > 0) return 'trend-positive'
      if (trend < 0) return 'trend-negative'
      return 'trend-neutral'
    })
    
    const trendIcon = computed(() => {
      const trend = parseFloat(trendPercentage.value)
      if (trend > 0) return 'pi pi-arrow-up'
      if (trend < 0) return 'pi pi-arrow-down'
      return 'pi pi-minus'
    })
    
    // Methods
    const formatValue = (value) => {
      if (props.metric === 'ctr' || props.metric === 'conversions') {
        return `${value.toFixed(1)}%`
      } else if (props.metric === 'cpc' || props.metric === 'spend') {
        return `$${value.toFixed(2)}`
      } else if (value >= 1000000) {
        return `${(value / 1000000).toFixed(1)}M`
      } else if (value >= 1000) {
        return `${(value / 1000).toFixed(1)}K`
      }
      return value.toLocaleString()
    }
    
    const createChart = () => {
      if (!chartCanvas.value || !props.data.length) return
      
      const ctx = chartCanvas.value.getContext('2d')
      const canvas = chartCanvas.value
      
      // Clear canvas
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      // Set up chart dimensions
      const padding = 60
      const chartArea = {
        left: padding,
        top: padding / 2,
        right: canvas.width - padding,
        bottom: canvas.height - padding
      }
      
      const chartAreaWidth = chartArea.right - chartArea.left
      const chartAreaHeight = chartArea.bottom - chartArea.top
      
      // Prepare data
      const dataPoints = chartData.value
      const minValue = Math.min(...dataPoints.map(p => p.y))
      const maxVal = Math.max(...dataPoints.map(p => p.y))
      const valueRange = maxVal - minValue || 1
      
      // Draw grid lines
      ctx.strokeStyle = '#f3f4f6'
      ctx.lineWidth = 1
      
      // Horizontal grid lines
      for (let i = 0; i <= 5; i++) {
        const y = chartArea.top + (chartAreaHeight / 5) * i
        ctx.beginPath()
        ctx.moveTo(chartArea.left, y)
        ctx.lineTo(chartArea.right, y)
        ctx.stroke()
      }
      
      // Vertical grid lines
      const timeStep = Math.max(1, Math.floor(dataPoints.length / 6))
      for (let i = 0; i < dataPoints.length; i += timeStep) {
        const x = chartArea.left + (chartAreaWidth / (dataPoints.length - 1)) * i
        ctx.beginPath()
        ctx.moveTo(x, chartArea.top)
        ctx.lineTo(x, chartArea.bottom)
        ctx.stroke()
      }
      
      // Draw the line chart
      if (dataPoints.length > 1) {
        ctx.strokeStyle = chartColor.value
        ctx.lineWidth = 3
        ctx.lineCap = 'round'
        ctx.lineJoin = 'round'
        
        // Create gradient
        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
        gradient.addColorStop(0, chartColor.value + '20')
        gradient.addColorStop(1, chartColor.value + '05')
        
        // Draw area fill
        ctx.beginPath()
        dataPoints.forEach((point, index) => {
          const x = chartArea.left + (chartAreaWidth / (dataPoints.length - 1)) * index
          const y = chartArea.bottom - ((point.y - minValue) / valueRange) * chartAreaHeight
          
          if (index === 0) {
            ctx.moveTo(x, chartArea.bottom)
            ctx.lineTo(x, y)
          } else {
            ctx.lineTo(x, y)
          }
        })
        ctx.lineTo(chartArea.right, chartArea.bottom)
        ctx.closePath()
        ctx.fillStyle = gradient
        ctx.fill()
        
        // Draw line
        ctx.beginPath()
        dataPoints.forEach((point, index) => {
          const x = chartArea.left + (chartAreaWidth / (dataPoints.length - 1)) * index
          const y = chartArea.bottom - ((point.y - minValue) / valueRange) * chartAreaHeight
          
          if (index === 0) {
            ctx.moveTo(x, y)
          } else {
            ctx.lineTo(x, y)
          }
        })
        ctx.stroke()
        
        // Draw data points
        ctx.fillStyle = chartColor.value
        dataPoints.forEach((point, index) => {
          const x = chartArea.left + (chartAreaWidth / (dataPoints.length - 1)) * index
          const y = chartArea.bottom - ((point.y - minValue) / valueRange) * chartAreaHeight
          
          ctx.beginPath()
          ctx.arc(x, y, 4, 0, 2 * Math.PI)
          ctx.fill()
          
          // Add white border
          ctx.strokeStyle = '#ffffff'
          ctx.lineWidth = 2
          ctx.stroke()
          ctx.strokeStyle = chartColor.value
          ctx.lineWidth = 3
        })
      }
      
      // Draw axes labels
      ctx.fillStyle = '#6b7280'
      ctx.font = '12px system-ui'
      ctx.textAlign = 'center'
      
      // Y-axis labels
      ctx.textAlign = 'right'
      for (let i = 0; i <= 5; i++) {
        const value = minValue + (valueRange / 5) * (5 - i)
        const y = chartArea.top + (chartAreaHeight / 5) * i
        ctx.fillText(formatValue(value), chartArea.left - 10, y + 4)
      }
      
      // X-axis labels
      ctx.textAlign = 'center'
      for (let i = 0; i < dataPoints.length; i += timeStep) {
        const point = dataPoints[i]
        const x = chartArea.left + (chartAreaWidth / (dataPoints.length - 1)) * i
        const dateStr = point.x.toLocaleDateString('en-US', { 
          month: 'short', 
          day: 'numeric' 
        })
        ctx.fillText(dateStr, x, chartArea.bottom + 20)
      }
    }
    
    const handleResize = () => {
      if (!chartCanvas.value) return
      
      const container = chartCanvas.value.parentElement
      if (container) {
        chartWidth.value = container.clientWidth
        chartHeight.value = props.height
        
        nextTick(() => {
          createChart()
        })
      }
    }
    
    // Lifecycle
    onMounted(() => {
      handleResize()
      
      // Set up resize observer
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
    watch([() => props.data, () => props.metric], () => {
      nextTick(() => {
        createChart()
      })
    }, { deep: true })
    
    return {
      chartCanvas,
      chartWidth,
      chartHeight,
      chartColor,
      metricLabel,
      maxValue,
      avgValue,
      trendPercentage,
      trendClass,
      trendIcon,
      formatValue
    }
  }
}
</script>

<style scoped>
.performance-trends-chart {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chart-loading,
.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
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

.chart-empty h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
}

.chart-empty p {
  margin: 0;
  font-size: 14px;
}

.chart-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

canvas {
  max-width: 100%;
  height: auto;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.chart-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-positive {
  color: #059669;
}

.trend-negative {
  color: #dc2626;
}

.trend-neutral {
  color: #6b7280;
}

/* Responsive Design */
@media (max-width: 768px) {
  .chart-stats {
    flex-direction: column;
    gap: 12px;
  }
  
  .stat-item {
    flex-direction: row;
    justify-content: space-between;
  }
  
  .chart-legend {
    justify-content: flex-start;
  }
}
</style>
