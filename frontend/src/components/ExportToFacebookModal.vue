<template>
  <a-modal
    :open="visible"
    title="Export to Facebook Ads Manager"
    width="900px"
    :confirm-loading="exporting"
    :ok-button-props="{ disabled: !canExport }"
    @ok="handleExportAndRedirect"
    @cancel="handleCancel"
  >
    <template #footer>
      <a-button key="cancel" @click="handleCancel">Cancel</a-button>
      <a-button key="download" @click="handleDownloadOnly" :loading="exporting">
        Download Only
      </a-button>
      <a-button
        key="export"
        type="primary"
        @click="handleExportAndRedirect"
        :loading="exporting"
        :disabled="!canExport"
      >
        Upload & Open Ads Manager
      </a-button>
    </template>

    <div class="export-modal-content">
      <!-- Format Selector -->
      <div class="format-selector mb-6">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Export Format
        </label>
        <a-radio-group v-model:value="selectedFormat" size="large" button-style="solid">
          <a-radio-button value="csv">
            <file-text-outlined class="mr-1" />
            CSV (Recommended)
          </a-radio-button>
          <a-radio-button value="excel">
            <file-excel-outlined class="mr-1" />
            Excel (.xlsx)
          </a-radio-button>
        </a-radio-group>
        <p class="text-xs text-gray-500 mt-2">
          {{ formatDescription }}
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-12 space-y-4">
        <a-spin size="large">
          <template #indicator>
            <loading-outlined style="font-size: 48px" spin />
          </template>
        </a-spin>
        <p class="text-gray-600">Loading preview...</p>
      </div>

      <!-- Exporting Progress -->
      <div v-if="exporting" class="export-progress-section">
        <div class="flex flex-col items-center justify-center py-8 space-y-4">
          <a-spin size="large">
            <template #indicator>
              <loading-outlined style="font-size: 48px" spin />
            </template>
          </a-spin>
          <div class="text-center">
            <p class="text-lg font-medium">Exporting ads...</p>
            <p class="text-sm text-gray-500">
              Processing {{ selectedCount }} ad(s) in {{ selectedFormat.toUpperCase() }} format
            </p>
          </div>
          <a-progress
            :percent="exportProgress"
            :status="exportProgress === 100 ? 'success' : 'active'"
            :show-info="true"
            stroke-color="#1890ff"
          />
        </div>
      </div>

      <!-- Error State -->
      <a-alert
        v-if="error && !loading"
        type="error"
        :message="error"
        show-icon
        closable
        @close="clearError"
        class="mb-4"
      />

      <!-- Preview Data -->
      <div v-if="previewData && !loading" class="preview-section">
        <!-- Summary Stats -->
        <div class="stats-grid mb-6">
          <div class="stat-card">
            <div class="stat-value">{{ selectedCount }}</div>
            <div class="stat-label">Ads Selected</div>
          </div>
          <div class="stat-card">
            <div class="stat-value text-green-600">{{ validCount }}</div>
            <div class="stat-label">Valid</div>
          </div>
          <div class="stat-card">
            <div class="stat-value text-orange-600">{{ warningCount }}</div>
            <div class="stat-label">Warnings</div>
          </div>
        </div>

        <!-- Validation Warnings -->
        <a-alert
          v-if="hasWarnings"
          type="warning"
          show-icon
          class="mb-4"
        >
          <template #message>
            <strong>{{ warningCount }} ad(s) have validation warnings</strong>
          </template>
          <template #description>
            <div class="text-sm">
              <p>These ads may not perform optimally on Facebook:</p>
              <ul class="list-disc list-inside mt-2">
                <li v-for="(warning, idx) in validationWarnings.slice(0, 3)" :key="idx">
                  {{ warning }}
                </li>
                <li v-if="validationWarnings.length > 3">
                  ... and {{ validationWarnings.length - 3 }} more
                </li>
              </ul>
            </div>
          </template>
        </a-alert>

        <!-- Preview Table -->
        <div class="preview-table-container">
          <h3 class="text-sm font-medium text-gray-700 mb-3">Preview</h3>
          <a-table
            :columns="previewColumns"
            :data-source="previewTableData"
            :pagination="false"
            :scroll="{ x: 1000 }"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'adName'">
                <div class="font-medium">{{ record.adName }}</div>
              </template>
              <template v-else-if="column.key === 'validation'">
                <a-tag v-if="record.isValid" color="green">
                  <check-circle-outlined class="mr-1" />
                  Valid
                </a-tag>
                <a-tooltip v-else placement="top">
                  <template #title>
                    <div v-for="(warn, idx) in record.warnings" :key="idx" class="text-xs">
                      • {{ warn }}
                    </div>
                  </template>
                  <a-tag color="orange">
                    <warning-outlined class="mr-1" />
                    {{ record.warnings.length }} Warning(s)
                  </a-tag>
                </a-tooltip>
              </template>
              <template v-else-if="column.key === 'headline'">
                <div class="text-xs">
                  {{ truncate(record.headline, 40) }}
                  <span :class="getCharCountClass(record.headline.length, 40)" class="ml-1">
                    ({{ record.headline.length }}/40)
                  </span>
                </div>
              </template>
              <template v-else-if="column.key === 'primaryText'">
                <div class="text-xs">
                  {{ truncate(record.primaryText, 50) }}
                  <span :class="getCharCountClass(record.primaryText.length, 125)" class="ml-1">
                    ({{ record.primaryText.length }}/125)
                  </span>
                </div>
              </template>
            </template>
          </a-table>
        </div>
      </div>

      <!-- Help Text -->
      <div class="help-text mt-6">
        <a-alert type="info" show-icon>
          <template #message>
            <strong>What happens next?</strong>
          </template>
          <template #description>
            <ol class="list-decimal list-inside text-sm space-y-1 mt-2">
              <li>Your selected ads will be uploaded to Facebook via the Marketing API (if configured)</li>
              <li>Facebook Ads Manager opens in a new tab so you can review/publish</li>
              <li>If auto-upload isn’t available, we automatically download the CSV so you can import it manually</li>
            </ol>
          </template>
        </a-alert>
      </div>
    </div>
  </a-modal>
</template>

<script>
import {
  FileTextOutlined,
  FileExcelOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import { mapState, mapGetters, mapActions } from 'vuex'

export default {
  name: 'ExportToFacebookModal',

  components: {
    FileTextOutlined,
    FileExcelOutlined,
    LoadingOutlined,
    CheckCircleOutlined,
    WarningOutlined
  },

  props: {
    visible: {
      type: Boolean,
      required: true
    },
    showAutoUpload: {
      type: Boolean,
      default: true
    },
    adIds: {
      type: Array,
      default: () => []
    }
  },

  emits: ['update:visible', 'success', 'error'],

  data() {
    return {
      previewColumns: [
        {
          title: 'Ad Name',
          dataIndex: 'adName',
          key: 'adName',
          width: 150,
          fixed: 'left'
        },
        {
          title: 'Campaign',
          dataIndex: 'campaignName',
          key: 'campaignName',
          width: 150
        },
        {
          title: 'Headline',
          dataIndex: 'headline',
          key: 'headline',
          width: 200
        },
        {
          title: 'Primary Text',
          dataIndex: 'primaryText',
          key: 'primaryText',
          width: 250
        },
        {
          title: 'Validation',
          key: 'validation',
          width: 120,
          align: 'center'
        }
      ],
      exportProgress: 0
    }
  },

  computed: {
    ...mapState('fbExport', ['format', 'isLoading', 'isExporting', 'error', 'previewData']),
    ...mapGetters('fbExport', ['selectedAdIds', 'selectedCount']),

    selectedFormat: {
      get() {
        return this.format
      },
      set(value) {
        this.setFormat(value)
      }
    },

    loading() {
      return this.isLoading
    },

    exporting() {
      return this.isExporting
    },

    formatDescription() {
      if (this.selectedFormat === 'csv') {
        return 'CSV format is recommended for maximum compatibility with Facebook Ads Manager'
      }
      return 'Excel format provides better formatting and is easier to review before uploading'
    },

    canExport() {
      return this.selectedCount > 0 && !this.loading && !this.exporting
    },

    canUploadDirect() {
      return this.selectedCount > 0 && !this.loading && !this.exporting
    },

    previewTableData() {
      if (!this.previewData) return []

      // Handle single ad preview
      if (this.previewData.ad) {
        return [this.buildPreviewRow(this.previewData)]
      }

      // Handle bulk preview
      if (this.previewData.ads && Array.isArray(this.previewData.ads)) {
        return this.previewData.ads.slice(0, 5).map(ad => this.buildPreviewRow(ad))
      }

      return []
    },

    validCount() {
      return this.previewTableData.filter(row => row.isValid).length
    },

    warningCount() {
      return this.previewTableData.filter(row => !row.isValid).length
    },

    hasWarnings() {
      return this.warningCount > 0
    },

    validationWarnings() {
      const warnings = []
      this.previewTableData.forEach(row => {
        if (!row.isValid) {
          row.warnings.forEach(warn => {
            if (!warnings.includes(warn)) {
              warnings.push(`${row.adName}: ${warn}`)
            }
          })
        }
      })
      return warnings
    }
  },

  watch: {
    visible: {
      immediate: true,
      handler(newVal) {
        if (newVal && this.adIds.length > 0) {
          this.initializeExport()
        }
      }
    },

    adIds: {
      immediate: true,
      handler(newIds) {
        if (newIds && newIds.length > 0) {
          this.setSelectedAds(newIds)
        }
      }
    }
  },

  methods: {
    ...mapActions('fbExport', [
      'setSelectedAds',
      'setFormat',
      'previewExport',
      'exportToFacebook',
      'downloadOnly',
      'clearError',
      'clearSelection'
    ]),

    async initializeExport() {
      try {
        await this.previewExport()
      } catch (error) {
        this.$message.error('Failed to load preview: ' + error.message)
        this.$emit('error', error)
      }
    },

    async handleExportAndRedirect() {
      try {
        this.exportProgress = 0
        this.simulateProgress()
        const result = await this.exportToFacebook({ autoUpload: true })
        this.exportProgress = 100
        const status = result?.autoUpload?.status
        const redirectUrl = result?.redirectUrl || 'https://business.facebook.com/adsmanager/manage/ads'
        window.open(redirectUrl, '_blank', 'noopener,noreferrer')
        if (status === 'UPLOADED') {
          this.$message.success('Ads uploaded to Facebook!')
        } else {
          const reason = result?.autoUpload?.message || 'Auto upload unavailable. Downloaded file instead.'
          this.$message.info(reason)
        }
        this.$emit('success', result)
        this.handleClose()
      } catch (error) {
        this.exportProgress = 0
        this.$message.error('Export failed: ' + error.message)
        this.$emit('error', error)
      }
    },

    async handleDownloadOnly() {
      try {
        this.exportProgress = 0
        this.simulateProgress()
        await this.downloadOnly()
        this.exportProgress = 100
        this.$message.success('File downloaded successfully!')
        this.$emit('success')
        this.handleClose()
      } catch (error) {
        this.exportProgress = 0
        this.$message.error('Download failed: ' + error.message)
        this.$emit('error', error)
      }
    },

    simulateProgress() {
      // Simulate progress for user feedback
      const interval = setInterval(() => {
        if (this.exportProgress < 90) {
          this.exportProgress += 10
        } else {
          clearInterval(interval)
        }
      }, 200)
    },

    handleCancel() {
      this.handleClose()
    },

    handleClose() {
      this.$emit('update:visible', false)
      this.clearError()
    },

    buildPreviewRow(adData) {
      const ad = adData.ad || adData
      const warnings = this.validateAd(ad)

      return {
        adName: ad.name || 'Unnamed Ad',
        campaignName: ad.campaignName || adData.campaign?.name || 'N/A',
        headline: ad.headline || '',
        primaryText: ad.primaryText || '',
        isValid: warnings.length === 0,
        warnings
      }
    },

    validateAd(ad) {
      const warnings = []

      // Headline length (Facebook limit: 40 chars)
      if (ad.headline && ad.headline.length > 40) {
        warnings.push('Headline exceeds 40 characters')
      } else if (!ad.headline) {
        warnings.push('Missing headline')
      }

      // Primary text length (Facebook limit: 125 chars for most placements)
      if (ad.primaryText && ad.primaryText.length > 125) {
        warnings.push('Primary text may be truncated (>125 chars)')
      } else if (!ad.primaryText) {
        warnings.push('Missing primary text')
      }

      // Media check
      if (!ad.imageUrl && !ad.videoUrl) {
        warnings.push('No media (image or video)')
      }

      return warnings
    },

    truncate(text, maxLength) {
      if (!text) return ''
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    },

    getCharCountClass(length, limit) {
      const percentage = (length / limit) * 100
      if (percentage > 100) return 'text-red-600 font-medium'
      if (percentage > 90) return 'text-orange-600'
      return 'text-gray-500'
    }
  }
}
</script>

<style scoped>
.export-modal-content {
  max-height: 70vh;
  overflow-y: auto;
}

.format-selector {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  padding: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.preview-table-container {
  background: white;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.help-text {
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .format-selector {
    background: #1f2937;
  }

  .stat-card {
    background: #111827;
    border-color: #374151;
  }

  .stat-value {
    color: #f3f4f6;
  }

  .preview-table-container {
    background: #111827;
    border-color: #374151;
  }
}
</style>
