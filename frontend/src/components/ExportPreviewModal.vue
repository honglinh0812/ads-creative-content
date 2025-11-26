<template>
  <a-modal
    :open="visible"
    :title="t('ads.export.preview.modalTitle', { count: ads.length })"
    :width="900"
    :footer="null"
    @cancel="handleClose"
    class="export-preview-modal"
  >
    <div class="preview-container">
      <p class="text-sm text-gray-600 mb-4">
        {{ t('ads.export.preview.description', { count: ads.length }) }}
      </p>

      <!-- Format Selection -->
      <div v-if="!autoUpload" class="format-selection mb-4">
        <label class="block text-sm font-medium mb-2">{{ t('ads.export.preview.formatLabel') }}</label>
        <a-radio-group v-model:value="selectedFormat" button-style="solid">
          <a-radio-button value="csv">
            <file-text-outlined /> CSV
          </a-radio-button>
          <a-radio-button value="excel">
            <file-excel-outlined /> Excel (.xlsx)
          </a-radio-button>
        </a-radio-group>
      </div>

      <!-- Preview Table -->
      <div class="preview-table-container">
        <a-table
          :columns="columns"
          :data-source="previewData"
          :loading="loading"
          :pagination="{ pageSize: 5, showSizeChanger: false }"
          size="small"
          :scroll="{ x: 800 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'imageUrl'">
              <img
                v-if="record.imageUrl"
                :src="record.imageUrl"
                alt="Ad preview"
                class="preview-image"
              />
              <span v-else class="text-gray-400">No image</span>
            </template>
            <template v-else-if="column.key === 'headline'">
              <div class="truncate max-w-xs" :title="record.headline">
                {{ record.headline }}
              </div>
            </template>
            <template v-else-if="column.key === 'primaryText'">
              <div class="truncate max-w-xs" :title="record.primaryText">
                {{ record.primaryText }}
              </div>
            </template>
            <template v-else-if="column.key === 'validation'">
              <a-badge
                :status="record.valid ? 'success' : 'error'"
                :text="record.valid ? t('ads.export.preview.validation.validLabel') : t('ads.export.preview.validation.invalidLabel')"
              />
            </template>
          </template>
        </a-table>
      </div>

      <!-- Validation Summary -->
      <div v-if="validationSummary" class="validation-summary mt-4">
        <a-alert
          :type="validationSummary.hasErrors ? 'warning' : 'success'"
          :message="validationSummary.message"
          show-icon
        >
          <template #description v-if="validationSummary.errors && validationSummary.errors.length">
            <ul class="text-sm">
              <li v-for="(error, index) in validationSummary.errors" :key="index">
                {{ error }}
              </li>
            </ul>
          </template>
        </a-alert>
      </div>

      <!-- Export Progress -->
      <div v-if="exporting" class="export-progress mt-4">
        <a-progress
          :percent="exportProgress"
          :status="exportProgress === 100 ? 'success' : 'active'"
        />
        <p class="text-center text-sm mt-2">
          {{ exportStatusMessage }}
        </p>
      </div>

      <!-- Action Buttons -->
      <div class="modal-actions mt-6">
        <a-space>
          <a-button @click="handleClose" :disabled="exporting">
            {{ t('common.actions.cancel') }}
          </a-button>
          <a-button
            type="primary"
            @click="handleExport"
            :loading="exporting"
            :disabled="!canExport"
          >
            <download-outlined />
            <span v-if="autoUpload">
              {{ t('ads.export.preview.actions.upload') }}
            </span>
            <span v-else>
              {{ t('ads.export.preview.actions.download', { format: selectedFormat.toUpperCase() }) }}
            </span>
          </a-button>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FileTextOutlined, FileExcelOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import api from '@/services/api'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'

const MIME_TYPES = {
  csv: 'text/csv;charset=utf-8;',
  excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
}

function base64ToBlob(base64Data, contentType) {
  if (!base64Data) return null
  const sliceSize = 512
  const byteCharacters = atob(base64Data)
  const byteArrays = []

  for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
    const slice = byteCharacters.slice(offset, offset + sliceSize)
    const byteNumbers = new Array(slice.length)
    for (let i = 0; i < slice.length; i++) {
      byteNumbers[i] = slice.charCodeAt(i)
    }
    const byteArray = new Uint8Array(byteNumbers)
    byteArrays.push(byteArray)
  }

  return new Blob(byteArrays, { type: contentType })
}

export default {
  name: 'ExportPreviewModal',
  components: {
    FileTextOutlined,
    FileExcelOutlined,
    DownloadOutlined
  },
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    ads: {
      type: Array,
      required: true
    },
    autoUpload: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible', 'export-complete', 'export-error'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const store = useStore()
    const selectedFormat = ref('csv')
    const loading = ref(false)
    const exporting = ref(false)
    const exportProgress = ref(0)
    const exportStatusMessage = ref('')
    const previewData = ref([])
    const validationSummary = ref(null)

    const columns = computed(() => ([
      {
        title: t('ads.export.preview.table.image'),
        key: 'imageUrl',
        width: 80
      },
      {
        title: t('ads.export.preview.table.headline'),
        dataIndex: 'headline',
        key: 'headline',
        width: 150
      },
      {
        title: t('ads.export.preview.table.primaryText'),
        dataIndex: 'primaryText',
        key: 'primaryText',
        width: 200
      },
      {
        title: t('ads.export.preview.table.description'),
        dataIndex: 'description',
        key: 'description',
        width: 150
      },
      {
        title: t('ads.export.preview.table.cta'),
        dataIndex: 'callToAction',
        key: 'callToAction',
        width: 100
      },
      {
        title: t('ads.export.preview.table.validation'),
        key: 'validation',
        width: 120
      }
    ]))

    const canExport = computed(() => previewData.value.length > 0 && !exporting.value)

    const normalizePreviewData = (data) => {
      if (!data) return []
      if (Array.isArray(data)) return data
      if (Array.isArray(data.ads)) return data.ads
      if (Array.isArray(data.data)) return data.data
      if (Array.isArray(data.items)) return data.items
      if (Array.isArray(data.rows)) return data.rows
      if (data.ad) return [data.ad]
      if (Array.isArray(data.preview)) return data.preview
      if (data.preview) return [data.preview]
      return []
    }

    const ensureSelection = async () => {
      const ids = (props.ads || []).map(ad => ad?.id).filter(Boolean)
      if (!ids.length || store.state.fbExport.selectedAdIds.length) {
        return ids
      }
      await store.dispatch('fbExport/setSelectedAds', ids)
      return ids
    }

    const buildPreviewRow = (row) => {
      const adId = row?.adId || row?.id
      const localAd = (props.ads || []).find(ad => ad.id === adId) || {}
      const headline = row?.headline || localAd.headline || ''
      const primaryText = row?.primaryText || row?.body || localAd.primaryText || ''
      const description = row?.description || row?.linkDescription || localAd.description || ''
      const callToAction = row?.callToAction || localAd.callToAction || ''
      const imageUrl = row?.imageUrl || localAd.imageUrl || ''

      const validation = validateAd({ headline, primaryText, callToAction })

      return {
        ...row,
        id: adId,
        imageUrl,
        headline,
        primaryText,
        description,
        callToAction,
        adName: row?.adName || localAd.name || '',
        campaignName: row?.campaignName || localAd.campaignName || localAd.campaign?.name || '',
        valid: validation.valid,
        warnings: validation.warnings || []
      }
    }

    const refreshValidationSummary = () => {
      if (!previewData.value.length) {
        validationSummary.value = null
        return
      }

      const invalidAds = previewData.value.filter(ad => !ad.valid)
      validationSummary.value = {
        hasErrors: invalidAds.length > 0,
        message: invalidAds.length > 0
          ? t('ads.export.preview.validation.hasIssues', { count: invalidAds.length })
          : t('ads.export.preview.validation.allValid', { count: previewData.value.length }),
        errors: invalidAds.map(ad => ad.adName || ad.headline || t('ads.export.preview.validation.unknown'))
      }
    }

    const loadPreview = async () => {
      if (!props.ads.length) return
      await ensureSelection()
      loading.value = true

      try {
        const response = await store.dispatch('fbExport/previewExport')
        const rows = normalizePreviewData(response)
        const safeRows = Array.isArray(rows) ? rows : []
        const hydratedRows = safeRows.length ? safeRows : props.ads

        previewData.value = hydratedRows.map(row => buildPreviewRow(row))
        refreshValidationSummary()
      } catch (error) {
        console.error('Failed to load preview:', error)
        const msg = error.response?.data?.message || error.message
        message.error(t('ads.export.preview.errors.previewFailed', { error: msg }))
        previewData.value = []
        validationSummary.value = null
      } finally {
        loading.value = false
      }
    }

    const validateAd = (ad) => {
      const warnings = []
      if (!ad.headline) warnings.push('headline')
      if (!ad.primaryText) warnings.push('primaryText')
      if (!ad.callToAction) warnings.push('cta')
      return {
        valid: warnings.length === 0,
        warnings
      }
    }

    const handleExport = async () => {
      if (!canExport.value) return

      exporting.value = true
      exportProgress.value = 0
      exportStatusMessage.value = t('ads.export.preview.status.preparing')

      const adIds = props.ads.map(ad => ad.id)

      const progressInterval = setInterval(() => {
        if (exportProgress.value < 90) {
          exportProgress.value += 10
          exportStatusMessage.value = props.autoUpload
            ? t('ads.export.preview.status.uploading')
            : t('ads.export.preview.status.downloading', { format: selectedFormat.value.toUpperCase() })
        }
      }, 250)

      try {
        if (props.autoUpload) {
          const result = await store.dispatch('fbExport/exportToFacebook', { autoUpload: true })
          clearInterval(progressInterval)
          exportProgress.value = 100
          exportStatusMessage.value = t('ads.export.preview.status.completed')

          if (result?.autoUpload?.status === 'UPLOADED') {
            message.success(t('ads.export.preview.messages.uploaded'))
          } else {
            const reason = result?.autoUpload?.message || t('ads.export.preview.messages.uploaded')
            message.info(reason)
          }

          emit('export-complete', result)
          handleClose()
        } else {
          const response = await api.facebookExport.exportMultipleAds(
            adIds,
            selectedFormat.value,
            { autoUpload: false }
          )

          clearInterval(progressInterval)
          exportProgress.value = 100
          exportStatusMessage.value = t('ads.export.preview.status.completed')

          const data = response.data || {}
          const format = (data.format || selectedFormat.value).toLowerCase()
          const mimeType = MIME_TYPES[format] || MIME_TYPES.csv
          const blob = base64ToBlob(data.fileContent, mimeType)
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          const extension = format === 'excel' || format === 'xlsx' ? 'xlsx' : 'csv'
          link.setAttribute('download', `ads_export_${Date.now()}.${extension}`)
          document.body.appendChild(link)
          link.click()
          link.remove()
          window.URL.revokeObjectURL(url)

          message.success(t('ads.export.preview.messages.downloaded', { count: adIds.length, format: selectedFormat.value.toUpperCase() }))

          setTimeout(() => {
            emit('export-complete')
            handleClose()
          }, 500)
        }
      } catch (error) {
        console.error('Export failed:', error)
        message.error(t('ads.export.preview.errors.uploadFailed', { error: error.response?.data?.message || error.message }))
        exportProgress.value = 0
        exportStatusMessage.value = ''
        emit('export-error', error)
      } finally {
        clearInterval(progressInterval)
        exporting.value = false
      }
    }

    const handleClose = () => {
      if (!exporting.value) {
        emit('update:visible', false)
      }
    }

    watch(() => props.visible, (newVal) => {
      if (newVal) {
        loadPreview()
      } else {
        // Reset state
        previewData.value = []
        validationSummary.value = null
        exportProgress.value = 0
        exportStatusMessage.value = ''
      }
    })

    watch(() => props.ads, () => {
      if (props.visible) {
        loadPreview()
      }
    }, { deep: true })

    return {
      t,
      selectedFormat,
      loading,
      exporting,
      exportProgress,
      exportStatusMessage,
      previewData,
      validationSummary,
      columns,
      canExport,
      handleExport,
      handleClose
    }
  }
}
</script>

<style scoped>
.export-preview-modal :deep(.ant-modal-body) {
  max-height: 70vh;
  overflow-y: auto;
}

.preview-container {
  padding: 8px 0;
}

.format-selection {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.preview-table-container {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.preview-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 4px;
}

.validation-summary {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.validation-summary ul {
  margin: 8px 0 0 0;
  padding-left: 20px;
}

.export-progress {
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* Dark mode support */
.dark .format-selection,
.dark .validation-summary,
.dark .export-progress {
  background: #1f1f1f;
}

.dark .preview-table-container {
  border-color: #303030;
}

.dark .modal-actions {
  border-top-color: #303030;
}

/* Responsive */
@media (max-width: 768px) {
  .preview-table-container {
    overflow-x: auto;
  }

  .modal-actions {
    flex-direction: column;
    gap: 8px;
  }

  .modal-actions :deep(.ant-space) {
    width: 100%;
    justify-content: stretch;
  }

  .modal-actions :deep(.ant-space-item) {
    flex: 1;
  }

  .modal-actions :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
