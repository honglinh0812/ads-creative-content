<template>
  <div class="analytics-container">
    <!-- Page Header - Following Dashboard pattern -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">Analytics Dashboard</h1>
        <p class="page-subtitle">
          Comprehensive insights and performance metrics for your Facebook advertising campaigns
        </p>
      </div>

      <!-- Header Actions -->
      <div class="header-actions">
        <ImportFacebookReport @import-complete="handleImportComplete" />
        <a-button @click="exportData" type="primary" size="large">
          <template #icon>
            <download-outlined />
          </template>
          Export
        </a-button>
      </div>
    </div>

    <!-- Analytics Dashboard Component -->
    <div class="analytics-content">
      <AnalyticsDashboard />
    </div>

    <!-- Export Modal -->
    <a-modal
      v-model:open="showExportModal"
      title="Export Analytics Data"
      :width="600"
      :footer="null"
    >
      <div class="export-modal-content">
        <div class="export-options">
          <div class="form-group">
            <label class="form-label">Data Type</label>
            <a-radio-group v-model:value="selectedExportType" class="export-type-group">
              <a-radio-button
                v-for="type in exportTypes"
                :key="type.value"
                :value="type.value"
              >
                <template #icon>
                  <component :is="type.icon" />
                </template>
                {{ type.label }}
              </a-radio-button>
            </a-radio-group>
          </div>

          <div class="form-group">
            <label class="form-label">Format</label>
            <a-radio-group v-model:value="selectedExportFormat" class="export-format-group">
              <a-radio-button value="csv">
                <file-text-outlined /> CSV
              </a-radio-button>
              <a-radio-button value="xlsx">
                <file-excel-outlined /> Excel
              </a-radio-button>
              <a-radio-button value="pdf">
                <file-pdf-outlined /> PDF
              </a-radio-button>
            </a-radio-group>
          </div>

          <div class="form-group">
            <label class="form-label">Time Range</label>
            <a-select v-model:value="selectedExportTimeRange" style="width: 100%;" size="large">
              <a-select-option value="7d">Last 7 days</a-select-option>
              <a-select-option value="30d">Last 30 days</a-select-option>
              <a-select-option value="90d">Last 90 days</a-select-option>
              <a-select-option value="1y">Last year</a-select-option>
            </a-select>
          </div>
        </div>

        <div class="modal-footer">
          <a-space>
            <a-button @click="closeExportModal">
              Cancel
            </a-button>
            <a-button type="primary" @click="performExport" :loading="exporting">
              <template #icon>
                <download-outlined v-if="!exporting" />
              </template>
              {{ exporting ? 'Exporting...' : 'Export' }}
            </a-button>
          </a-space>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined,
  FileTextOutlined,
  FileExcelOutlined,
  FilePdfOutlined,
  FolderOutlined,
  ThunderboltOutlined,
  FileOutlined,
  DatabaseOutlined
} from '@ant-design/icons-vue'
import AnalyticsDashboard from '@/components/AnalyticsDashboard.vue'
import ImportFacebookReport from '@/components/ImportFacebookReport.vue'
import api from '@/services/api'

export default {
  name: 'AnalyticsView',
  components: {
    AnalyticsDashboard,
    ImportFacebookReport,
    DownloadOutlined,
    FileTextOutlined,
    FileExcelOutlined,
    FilePdfOutlined,
    FolderOutlined,
    ThunderboltOutlined,
    FileOutlined,
    DatabaseOutlined
  },

  setup() {
    // Reactive data
    const loading = ref(false)
    const showExportModal = ref(false)
    const exporting = ref(false)
    const analyticsData = ref(null)

    // Export options
    const selectedExportType = ref('campaigns')
    const selectedExportFormat = ref('csv')
    const selectedExportTimeRange = ref('30d')

    // Export configuration
    const exportTypes = [
      { value: 'campaigns', label: 'Campaigns', icon: FolderOutlined },
      { value: 'ads', label: 'Ads', icon: ThunderboltOutlined },
      { value: 'content', label: 'Content', icon: FileOutlined },
      { value: 'all', label: 'All Data', icon: DatabaseOutlined }
    ]

    // Methods
    const refreshData = async () => {
      loading.value = true
      try {
        const response = await api.analyticsAPI.getDashboard(selectedExportTimeRange.value)
        analyticsData.value = response.data.data
      } catch (error) {
        console.error('Error refreshing data:', error)
        message.error('Failed to refresh analytics data')
      } finally {
        loading.value = false
      }
    }

    const exportData = () => {
      showExportModal.value = true
    }

    const closeExportModal = () => {
      showExportModal.value = false
    }

    const performExport = async () => {
      exporting.value = true
      try {
        // First, try to fetch analytics data if we don't have it
        if (!analyticsData.value) {
          const response = await api.analyticsAPI.getDashboard(selectedExportTimeRange.value)
          analyticsData.value = response.data.data
        }

        // Generate the export file based on format
        const exportResult = await generateExport(
          selectedExportType.value,
          selectedExportFormat.value,
          selectedExportTimeRange.value,
          analyticsData.value
        )

        // Create download link
        const blob = new Blob([exportResult.content], { type: exportResult.mimeType })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', exportResult.filename)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)

        message.success('Analytics data exported successfully!')
        closeExportModal()
      } catch (error) {
        console.error('Export failed:', error)
        message.error('Failed to export analytics data. Please try again.')
      } finally {
        exporting.value = false
      }
    }

    // Generate export file
    const generateExport = async (type, format, timeRange, data) => {
      const timestamp = new Date().toISOString().split('T')[0]
      const filename = `analytics-${type}-${timeRange}-${timestamp}.${format}`

      if (format === 'csv') {
        return {
          content: generateCSV(type, data),
          mimeType: 'text/csv;charset=utf-8;',
          filename
        }
      } else if (format === 'xlsx') {
        // For Excel, we'll generate CSV for now (can enhance with library like xlsx)
        return {
          content: generateCSV(type, data),
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          filename
        }
      } else if (format === 'pdf') {
        // For PDF, generate a simple text format (can enhance with library like jsPDF)
        return {
          content: generatePDF(type, data),
          mimeType: 'application/pdf',
          filename
        }
      }
    }

    const generateCSV = (type, data) => {
      if (!data) return ''

      let rows = []
      let headers = []

      switch (type) {
        case 'campaigns':
          headers = ['Campaign Name', 'Status', 'Budget', 'Spent', 'Impressions', 'Clicks', 'CTR', 'CPC', 'ROI']
          if (data.campaignAnalytics) {
            rows = data.campaignAnalytics.map(c => [
              c.campaignName || '',
              c.status || '',
              c.budget || 0,
              c.spent || 0,
              c.impressions || 0,
              c.clicks || 0,
              c.ctr || 0,
              c.cpc || 0,
              c.roi || 0
            ])
          }
          break

        case 'ads':
          headers = ['Ad Name', 'Campaign', 'Status', 'Impressions', 'Clicks', 'CTR', 'Spend']
          // Extract ads data from analytics if available
          rows = [[]] // Placeholder
          break

        case 'content':
          headers = ['Content ID', 'Type', 'Performance Score', 'Impressions', 'Engagement']
          if (data.contentAnalytics?.topPerformingContent) {
            rows = data.contentAnalytics.topPerformingContent.map(c => [
              c.contentId || '',
              c.type || '',
              c.performanceScore || 0,
              c.impressions || 0,
              c.engagement || 0
            ])
          }
          break

        case 'all':
          // Combine all data
          headers = ['Type', 'Name', 'Metric', 'Value']
          if (data.campaignAnalytics) {
            data.campaignAnalytics.forEach(c => {
              rows.push(['Campaign', c.campaignName, 'Impressions', c.impressions])
              rows.push(['Campaign', c.campaignName, 'Clicks', c.clicks])
              rows.push(['Campaign', c.campaignName, 'Budget', c.budget])
            })
          }
          break
      }

      // Convert to CSV format
      const csvContent = [
        headers.join(','),
        ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
      ].join('\n')

      return csvContent
    }

    const generatePDF = (type, data) => {
      // Simple text-based PDF placeholder
      // In production, use a library like jsPDF
      let content = `Analytics Export - ${type}\n\n`
      content += `Time Range: ${selectedExportTimeRange.value}\n`
      content += `Export Date: ${new Date().toISOString()}\n\n`
      content += `Data:\n${JSON.stringify(data, null, 2)}`
      return content
    }

    // Lifecycle
    const handleImportComplete = (result) => {
      message.success(`Successfully imported ${result.imported} performance reports`)
      // Refresh the analytics dashboard to show new data
      refreshData()
    }

    onMounted(() => {
      // Load initial data
      refreshData()
    })

    return {
      loading,
      showExportModal,
      exporting,
      selectedExportType,
      selectedExportFormat,
      selectedExportTimeRange,
      exportTypes,
      exportData,
      closeExportModal,
      performExport,
      handleImportComplete
    }
  }
}
</script>

<style scoped>
.analytics-container {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* Page Header - Consistent with Dashboard */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 24px;
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px;
  line-height: 1.2;
}

.page-subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  max-width: 600px;
  line-height: 1.5;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
  align-items: center;
}

/* Analytics Content */
.analytics-content {
  background: transparent;
  border-radius: 12px;
}

/* Export Modal */
.export-modal-content {
  padding: 24px 0;
}

.export-options {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.export-type-group,
.export-format-group {
  width: 100%;
}

.export-type-group :deep(.ant-radio-button-wrapper),
.export-format-group :deep(.ant-radio-button-wrapper) {
  height: auto;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb;
}

/* Responsive Design */
@media (max-width: 768px) {
  .analytics-container {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
  }

  .header-actions {
    flex-direction: column;
    gap: 8px;
  }

  .header-actions :deep(.ant-btn) {
    width: 100%;
  }

  .export-type-group,
  .export-format-group {
    display: flex;
    flex-direction: column;
  }

  .export-type-group :deep(.ant-radio-button-wrapper),
  .export-format-group :deep(.ant-radio-button-wrapper) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px;
  }

  .page-subtitle {
    font-size: 14px;
  }
}
</style>
