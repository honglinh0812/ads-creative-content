<template>
  <div class="import-facebook-report">
    <!-- Import Button -->
    <a-button
      type="primary"
      @click="showModal"
      :icon="h(UploadOutlined)"
    >
      {{ $t('import.importReports', 'Import Facebook Reports') }}
    </a-button>

    <!-- Import Modal -->
    <a-modal
      v-model:open="visible"
      :title="$t('import.modalTitle', 'Import Facebook Performance Reports')"
      :width="1000"
      :footer="null"
      :maskClosable="false"
      @cancel="handleCancel"
      class="import-report-modal"
    >
      <div class="import-container">
        <!-- Step 1: File Upload -->
        <div v-if="!previewData" class="upload-section">
          <a-alert
            :message="$t('import.instructions', 'Import Instructions')"
            type="info"
            show-icon
            class="mb-4"
          >
            <template #description>
              <ol class="text-sm list-decimal pl-4">
                <li>Download your Facebook Ads Manager performance report (CSV or Excel)</li>
                <li>Ensure report includes: Ad Name, Impressions, Clicks, Spend</li>
                <li>Upload the file below to preview matches</li>
              </ol>
            </template>
          </a-alert>

          <a-upload-dragger
            v-model:fileList="fileList"
            :before-upload="beforeUpload"
            :accept="'.csv,.xlsx,.xls'"
            :max-count="1"
            @remove="handleRemove"
          >
            <p class="ant-upload-drag-icon">
              <inbox-outlined></inbox-outlined>
            </p>
            <p class="ant-upload-text">
              {{ $t('import.uploadText', 'Click or drag file to upload') }}
            </p>
            <p class="ant-upload-hint">
              {{ $t('import.uploadHint', 'Support CSV or Excel (.xlsx) files. Max size: 10MB') }}
            </p>
          </a-upload-dragger>

          <div class="upload-actions mt-4">
            <a-space>
              <a-button @click="handleCancel">
                {{ $t('common.cancel', 'Cancel') }}
              </a-button>
              <a-button
                type="primary"
                @click="handlePreview"
                :loading="isPreviewLoading"
                :disabled="fileList.length === 0"
              >
                {{ $t('import.preview', 'Preview Matches') }}
              </a-button>
            </a-space>
          </div>
        </div>

        <!-- Step 2: Preview Matches -->
        <div v-else class="preview-section">
          <!-- Match Summary -->
          <div class="match-summary mb-4">
            <a-row :gutter="16">
              <a-col :span="6">
                <a-statistic
                  :title="$t('import.totalRows', 'Total Rows')"
                  :value="totalRows"
                  :value-style="{ color: '#3f8600' }"
                >
                  <template #prefix>
                    <file-text-outlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="6">
                <a-statistic
                  :title="$t('import.matched', 'Matched')"
                  :value="matchedCount"
                  :value-style="{ color: '#52c41a' }"
                >
                  <template #prefix>
                    <check-circle-outlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="6">
                <a-statistic
                  :title="$t('import.unmatched', 'Unmatched')"
                  :value="unmatchedCount"
                  :value-style="{ color: '#cf1322' }"
                >
                  <template #prefix>
                    <close-circle-outlined />
                  </template>
                </a-statistic>
              </a-col>
              <a-col :span="6">
                <a-statistic
                  :title="$t('import.matchRate', 'Match Rate')"
                  :value="matchRate"
                  :suffix="'%'"
                  :precision="1"
                  :value-style="{ color: matchRate >= 80 ? '#52c41a' : '#faad14' }"
                >
                  <template #prefix>
                    <pie-chart-outlined />
                  </template>
                </a-statistic>
              </a-col>
            </a-row>
          </div>

          <!-- Tabs for Matched/Unmatched -->
          <a-tabs v-model:activeKey="activeTab">
            <!-- Matched Reports Tab -->
            <a-tab-pane key="matched" :tab="`Matched (${matchedCount})`">
              <a-table
                :columns="matchedColumns"
                :data-source="matchedReports"
                :pagination="{ pageSize: 10, showSizeChanger: true }"
                size="small"
                :scroll="{ x: 1200 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'matchConfidence'">
                    <a-tag :color="getConfidenceColor(record.matchConfidence)">
                      {{ (record.matchConfidence * 100).toFixed(0) }}%
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'matchType'">
                    <a-tag :color="getMatchTypeColor(record.matchType)">
                      {{ record.matchType }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'impressions'">
                    {{ formatNumber(record.metrics.impressions) }}
                  </template>
                  <template v-else-if="column.key === 'clicks'">
                    {{ formatNumber(record.metrics.clicks) }}
                  </template>
                  <template v-else-if="column.key === 'spend'">
                    ${{ formatNumber(record.metrics.spend, 2) }}
                  </template>
                  <template v-else-if="column.key === 'ctr'">
                    {{ formatNumber(record.metrics.ctr, 2) }}%
                  </template>
                </template>
              </a-table>
            </a-tab-pane>

            <!-- Unmatched Reports Tab -->
            <a-tab-pane key="unmatched" :tab="`Unmatched (${unmatchedCount})`">
              <a-alert
                v-if="unmatchedCount > 0"
                message="These ads could not be matched automatically"
                type="warning"
                show-icon
                class="mb-3"
              />
              <a-table
                :columns="unmatchedColumns"
                :data-source="unmatchedReports"
                :pagination="{ pageSize: 10 }"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'reason'">
                    <a-tag color="orange">{{ record.reason }}</a-tag>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>

          <!-- Action Buttons -->
          <div class="preview-actions mt-4">
            <a-space>
              <a-button @click="handleBack">
                {{ $t('common.back', 'Back') }}
              </a-button>
              <a-button
                type="primary"
                @click="handleConfirmImport"
                :loading="isImporting"
                :disabled="matchedCount === 0"
              >
                {{ $t('import.confirm', 'Import') }} {{ matchedCount }} {{ $t('import.reports', 'Reports') }}
              </a-button>
            </a-space>
          </div>
        </div>

        <!-- Error Display -->
        <a-alert
          v-if="error"
          :message="error"
          type="error"
          closable
          @close="clearError"
          class="mt-4"
        />
      </div>
    </a-modal>

    <!-- Success Modal -->
    <a-modal
      v-model:open="showSuccessModal"
      title="Import Successful"
      :footer="null"
      @cancel="showSuccessModal = false"
    >
      <a-result
        status="success"
        :title="`Successfully imported ${importResult?.imported || 0} reports`"
      >
        <template #extra>
          <a-button type="primary" @click="handleSuccessClose">
            {{ $t('common.close', 'Close') }}
          </a-button>
        </template>
      </a-result>
    </a-modal>
  </div>
</template>

<script>
import { ref, computed, h } from 'vue'
import { useStore } from 'vuex'
import { message, UploadOutlined } from 'ant-design-vue'
import {
  InboxOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  PieChartOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'ImportFacebookReport',
  components: {
    InboxOutlined,
    FileTextOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    PieChartOutlined
  },
  emits: ['import-complete'],
  setup(props, { emit }) {
    const store = useStore()

    // Local state
    const visible = ref(false)
    const fileList = ref([])
    const activeTab = ref('matched')
    const showSuccessModal = ref(false)

    // Computed from store
    const previewData = computed(() => store.getters['fbImport/previewData'])
    const matchedReports = computed(() => store.getters['fbImport/matchedReports'])
    const unmatchedReports = computed(() => store.getters['fbImport/unmatchedReports'])
    const matchedCount = computed(() => store.getters['fbImport/matchedCount'])
    const unmatchedCount = computed(() => store.getters['fbImport/unmatchedCount'])
    const totalRows = computed(() => store.getters['fbImport/totalRows'])
    const matchRate = computed(() => store.getters['fbImport/matchRate'])
    const isPreviewLoading = computed(() => store.getters['fbImport/isPreviewLoading'])
    const isImporting = computed(() => store.getters['fbImport/isImporting'])
    const importResult = computed(() => store.getters['fbImport/importResult'])
    const error = computed(() => store.getters['fbImport/error'])

    // Table columns for matched reports
    const matchedColumns = [
      {
        title: 'Facebook Ad Name',
        dataIndex: 'facebookAdName',
        key: 'facebookAdName',
        width: 200,
        ellipsis: true
      },
      {
        title: 'Matched Ad',
        dataIndex: 'adName',
        key: 'adName',
        width: 180,
        ellipsis: true
      },
      {
        title: 'Campaign',
        dataIndex: 'campaignName',
        key: 'campaignName',
        width: 150,
        ellipsis: true
      },
      {
        title: 'Match Type',
        key: 'matchType',
        width: 100
      },
      {
        title: 'Confidence',
        key: 'matchConfidence',
        width: 100
      },
      {
        title: 'Impressions',
        key: 'impressions',
        width: 120,
        align: 'right'
      },
      {
        title: 'Clicks',
        key: 'clicks',
        width: 100,
        align: 'right'
      },
      {
        title: 'CTR',
        key: 'ctr',
        width: 80,
        align: 'right'
      },
      {
        title: 'Spend',
        key: 'spend',
        width: 100,
        align: 'right'
      }
    ]

    // Table columns for unmatched reports
    const unmatchedColumns = [
      {
        title: 'Row #',
        dataIndex: 'rowNumber',
        key: 'rowNumber',
        width: 80
      },
      {
        title: 'Facebook Ad Name',
        dataIndex: 'facebookAdName',
        key: 'facebookAdName',
        width: 300,
        ellipsis: true
      },
      {
        title: 'Reason',
        key: 'reason',
        width: 200
      }
    ]

    // File upload handlers
    const beforeUpload = (file) => {
      const isValidType = file.type === 'text/csv' ||
                          file.type === 'application/vnd.ms-excel' ||
                          file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

      if (!isValidType) {
        message.error('You can only upload CSV or Excel files!')
        return false
      }

      const isLt10M = file.size / 1024 / 1024 < 10
      if (!isLt10M) {
        message.error('File must be smaller than 10MB!')
        return false
      }

      // Determine file type
      const fileType = file.name.toLowerCase().endsWith('.xlsx') ||
                       file.name.toLowerCase().endsWith('.xls') ? 'excel' : 'csv'

      store.dispatch('fbImport/selectFile', { file, type: fileType })

      return false // Prevent auto upload
    }

    const handleRemove = () => {
      store.dispatch('fbImport/reset')
      fileList.value = []
    }

    // Modal handlers
    const showModal = () => {
      visible.value = true
      store.dispatch('fbImport/reset')
    }

    const handleCancel = () => {
      visible.value = false
      store.dispatch('fbImport/reset')
      fileList.value = []
    }

    const handleBack = () => {
      store.dispatch('fbImport/reset')
      fileList.value = []
    }

    // Preview import
    const handlePreview = async () => {
      try {
        await store.dispatch('fbImport/previewImport')
        message.success(`Found ${matchedCount.value} matches out of ${totalRows.value} rows`)
      } catch (error) {
        message.error(error.message || 'Failed to preview import')
      }
    }

    // Confirm import
    const handleConfirmImport = async () => {
      try {
        const result = await store.dispatch('fbImport/confirmImport', {
          matchedReports: matchedReports.value,
          source: 'FACEBOOK'
        })

        showSuccessModal.value = true
        message.success(`Successfully imported ${result.imported} reports`)

        // Emit event to parent
        emit('import-complete', result)
      } catch (error) {
        message.error(error.message || 'Failed to import reports')
      }
    }

    const handleSuccessClose = () => {
      showSuccessModal.value = false
      visible.value = false
      store.dispatch('fbImport/reset')
      fileList.value = []
    }

    const clearError = () => {
      store.dispatch('fbImport/clearError')
    }

    // Utility functions
    const formatNumber = (value, decimals = 0) => {
      if (value == null) return '-'
      return Number(value).toLocaleString('en-US', {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals
      })
    }

    const getConfidenceColor = (confidence) => {
      if (confidence >= 0.95) return 'green'
      if (confidence >= 0.85) return 'blue'
      return 'orange'
    }

    const getMatchTypeColor = (matchType) => {
      switch (matchType) {
        case 'EXACT': return 'green'
        case 'ID_EXTRACTION': return 'blue'
        case 'FUZZY': return 'orange'
        default: return 'default'
      }
    }

    return {
      h,
      UploadOutlined,
      visible,
      fileList,
      activeTab,
      showSuccessModal,
      previewData,
      matchedReports,
      unmatchedReports,
      matchedCount,
      unmatchedCount,
      totalRows,
      matchRate,
      isPreviewLoading,
      isImporting,
      importResult,
      error,
      matchedColumns,
      unmatchedColumns,
      beforeUpload,
      handleRemove,
      showModal,
      handleCancel,
      handleBack,
      handlePreview,
      handleConfirmImport,
      handleSuccessClose,
      clearError,
      formatNumber,
      getConfidenceColor,
      getMatchTypeColor
    }
  }
}
</script>

<style scoped lang="scss">
.import-facebook-report {
  display: inline-block;
}

.import-container {
  min-height: 400px;
}

.upload-section {
  padding: 20px 0;
}

.preview-section {
  padding: 20px 0;
}

.match-summary {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.preview-actions,
.upload-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.ant-statistic {
  text-align: center;
}

:deep(.ant-table-cell) {
  font-size: 12px;
}

:deep(.ant-upload-drag) {
  background: #fafafa;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
}

:deep(.ant-upload-drag:hover) {
  border-color: #1890ff;
}
</style>
