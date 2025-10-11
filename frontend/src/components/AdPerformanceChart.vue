<template>
  <div class="ad-performance-chart">
    <a-card :title="title" :loading="loading">
      <!-- Metric Selector -->
      <template #extra>
        <a-select
          v-model:value="selectedMetric"
          style="width: 150px"
          @change="updateChart"
        >
          <a-select-option value="impressions">Impressions</a-select-option>
          <a-select-option value="clicks">Clicks</a-select-option>
          <a-select-option value="ctr">CTR (%)</a-select-option>
          <a-select-option value="spend">Spend ($)</a-select-option>
          <a-select-option value="cpc">CPC ($)</a-select-option>
          <a-select-option value="cpm">CPM ($)</a-select-option>
          <a-select-option value="conversions">Conversions</a-select-option>
          <a-select-option value="conversionRate">Conversion Rate (%)</a-select-option>
        </a-select>
      </template>

      <!-- Chart Display -->
      <div class="chart-container">
        <canvas ref="chartCanvas"></canvas>
      </div>

      <!-- Summary Statistics -->
      <div v-if="statistics" class="statistics-summary mt-4">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic
              title="Total Impressions"
              :value="statistics.totalImpressions"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Total Clicks"
              :value="statistics.totalClicks"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Avg CTR"
              :value="statistics.avgCtr"
              suffix="%"
              :precision="2"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Total Spend"
              :value="statistics.totalSpend"
              prefix="$"
              :precision="2"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
        </a-row>
        <a-row :gutter="16" class="mt-3">
          <a-col :span="6">
            <a-statistic
              title="Avg CPC"
              :value="statistics.avgCpc"
              prefix="$"
              :precision="2"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Avg CPM"
              :value="statistics.avgCpm"
              prefix="$"
              :precision="2"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Total Conversions"
              :value="statistics.totalConversions"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="Avg Conv. Rate"
              :value="statistics.avgConversionRate"
              suffix="%"
              :precision="2"
              :value-style="{ fontSize: '16px' }"
            />
          </a-col>
        </a-row>
      </div>

      <!-- Empty State -->
      <a-empty
        v-if="!loading && (!reports || reports.length === 0)"
        description="No performance data available"
        class="mt-8"
      >
        <template #image>
          <line-chart-outlined style="font-size: 48px; color: #d9d9d9;" />
        </template>
      </a-empty>
    </a-card>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { LineChartOutlined } from '@ant-design/icons-vue'
import Chart from 'chart.js/auto'

export default {
  name: 'AdPerformanceChart',
  components: {
    LineChartOutlined
  },
  props: {
    reports: {
      type: Array,
      required: true,
      default: () => []
    },
    title: {
      type: String,
      default: 'Performance Over Time'
    },
    loading: {
      type: Boolean,
      default: false
    },
    chartType: {
      type: String,
      default: 'line', // 'line', 'bar'
      validator: (value) => ['line', 'bar'].includes(value)
    }
  },
  setup(props) {
    const chartCanvas = ref(null)
    const chartInstance = ref(null)
    const selectedMetric = ref('impressions')

    // Compute statistics from reports
    const statistics = computed(() => {
      if (!props.reports || props.reports.length === 0) return null

      const total = props.reports.reduce(
        (acc, report) => {
          acc.impressions += report.impressions || 0
          acc.clicks += report.clicks || 0
          acc.spend += report.spend || 0
          acc.conversions += report.conversions || 0

          if (report.ctr != null) {
            acc.ctrSum += report.ctr
            acc.ctrCount++
          }
          if (report.cpc != null) {
            acc.cpcSum += report.cpc
            acc.cpcCount++
          }
          if (report.cpm != null) {
            acc.cpmSum += report.cpm
            acc.cpmCount++
          }
          if (report.conversionRate != null) {
            acc.conversionRateSum += report.conversionRate
            acc.conversionRateCount++
          }

          return acc
        },
        {
          impressions: 0,
          clicks: 0,
          spend: 0,
          conversions: 0,
          ctrSum: 0,
          ctrCount: 0,
          cpcSum: 0,
          cpcCount: 0,
          cpmSum: 0,
          cpmCount: 0,
          conversionRateSum: 0,
          conversionRateCount: 0
        }
      )

      return {
        totalImpressions: total.impressions,
        totalClicks: total.clicks,
        totalSpend: total.spend,
        totalConversions: total.conversions,
        avgCtr: total.ctrCount > 0 ? total.ctrSum / total.ctrCount : 0,
        avgCpc: total.cpcCount > 0 ? total.cpcSum / total.cpcCount : 0,
        avgCpm: total.cpmCount > 0 ? total.cpmSum / total.cpmCount : 0,
        avgConversionRate: total.conversionRateCount > 0 ? total.conversionRateSum / total.conversionRateCount : 0
      }
    })

    // Prepare chart data
    const chartData = computed(() => {
      if (!props.reports || props.reports.length === 0) return null

      // Sort reports by date
      const sortedReports = [...props.reports].sort((a, b) => {
        return new Date(a.reportDate) - new Date(b.reportDate)
      })

      const labels = sortedReports.map(report => {
        const date = new Date(report.reportDate)
        return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
      })

      const datasets = {
        impressions: {
          label: 'Impressions',
          data: sortedReports.map(r => r.impressions || 0),
          borderColor: 'rgb(75, 192, 192)',
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          tension: 0.1
        },
        clicks: {
          label: 'Clicks',
          data: sortedReports.map(r => r.clicks || 0),
          borderColor: 'rgb(54, 162, 235)',
          backgroundColor: 'rgba(54, 162, 235, 0.2)',
          tension: 0.1
        },
        ctr: {
          label: 'CTR (%)',
          data: sortedReports.map(r => r.ctr || 0),
          borderColor: 'rgb(255, 206, 86)',
          backgroundColor: 'rgba(255, 206, 86, 0.2)',
          tension: 0.1
        },
        spend: {
          label: 'Spend ($)',
          data: sortedReports.map(r => r.spend || 0),
          borderColor: 'rgb(255, 99, 132)',
          backgroundColor: 'rgba(255, 99, 132, 0.2)',
          tension: 0.1
        },
        cpc: {
          label: 'CPC ($)',
          data: sortedReports.map(r => r.cpc || 0),
          borderColor: 'rgb(153, 102, 255)',
          backgroundColor: 'rgba(153, 102, 255, 0.2)',
          tension: 0.1
        },
        cpm: {
          label: 'CPM ($)',
          data: sortedReports.map(r => r.cpm || 0),
          borderColor: 'rgb(255, 159, 64)',
          backgroundColor: 'rgba(255, 159, 64, 0.2)',
          tension: 0.1
        },
        conversions: {
          label: 'Conversions',
          data: sortedReports.map(r => r.conversions || 0),
          borderColor: 'rgb(75, 192, 192)',
          backgroundColor: 'rgba(75, 192, 192, 0.2)',
          tension: 0.1
        },
        conversionRate: {
          label: 'Conversion Rate (%)',
          data: sortedReports.map(r => r.conversionRate || 0),
          borderColor: 'rgb(201, 203, 207)',
          backgroundColor: 'rgba(201, 203, 207, 0.2)',
          tension: 0.1
        }
      }

      return {
        labels,
        datasets: [datasets[selectedMetric.value]]
      }
    })

    // Create or update chart
    const updateChart = () => {
      if (!chartCanvas.value || !chartData.value) return

      // Destroy existing chart
      if (chartInstance.value) {
        chartInstance.value.destroy()
      }

      // Create new chart
      const ctx = chartCanvas.value.getContext('2d')
      chartInstance.value = new Chart(ctx, {
        type: props.chartType,
        data: chartData.value,
        options: {
          responsive: true,
          maintainAspectRatio: true,
          aspectRatio: 2,
          plugins: {
            legend: {
              display: true,
              position: 'top'
            },
            tooltip: {
              mode: 'index',
              intersect: false
            }
          },
          scales: {
            x: {
              display: true,
              title: {
                display: true,
                text: 'Date'
              }
            },
            y: {
              display: true,
              title: {
                display: true,
                text: chartData.value.datasets[0].label
              },
              beginAtZero: true
            }
          }
        }
      })
    }

    // Watch for data changes
    watch(() => props.reports, () => {
      if (!props.loading) {
        updateChart()
      }
    }, { deep: true })

    watch(() => props.loading, (newVal) => {
      if (!newVal) {
        updateChart()
      }
    })

    // Lifecycle hooks
    onMounted(() => {
      if (!props.loading && props.reports && props.reports.length > 0) {
        updateChart()
      }
    })

    onBeforeUnmount(() => {
      if (chartInstance.value) {
        chartInstance.value.destroy()
      }
    })

    return {
      chartCanvas,
      selectedMetric,
      statistics,
      updateChart
    }
  }
}
</script>

<style scoped lang="scss">
.ad-performance-chart {
  width: 100%;
}

.chart-container {
  position: relative;
  height: 400px;
  width: 100%;

  canvas {
    max-height: 400px;
  }
}

.statistics-summary {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

:deep(.ant-statistic-title) {
  font-size: 13px;
  color: #666;
}

:deep(.ant-statistic-content) {
  font-size: 16px;
  font-weight: 600;
}
</style>
