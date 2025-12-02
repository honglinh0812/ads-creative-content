<template>
  <section class="ad-table">
    <a-card class="panel" :bordered="false" :body-style="{ padding: '24px' }">
      <header class="panel__header">
        <div>
          <p class="panel__eyebrow">{{ t('adTable.header.eyebrow') }}</p>
          <h2 class="panel__title">{{ t('adTable.header.title') }}</h2>
          <p class="panel__subtitle">{{ t('adTable.header.subtitle') }}</p>
        </div>
        <div class="view-toggle-wrapper">
          <a-segmented
            v-model:value="viewMode"
            class="view-toggle"
            :options="viewOptions"
            size="large"
          />
          <p v-if="isCompact" class="view-toggle__hint">
            {{ t('adTable.view.compactHint') }}
          </p>
        </div>
      </header>

      <a-form layout="vertical" class="filter-form">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :md="8">
            <a-form-item :label="t('adTable.filters.searchLabel')">
              <a-input
                v-model:value="searchQuery"
                :placeholder="t('adTable.filters.searchPlaceholder')"
                allow-clear
                @input="handleSearch"
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="5">
            <a-form-item :label="t('adTable.filters.statusLabel')">
              <a-select
                v-model:value="statusFilter"
                :placeholder="t('adTable.filters.statusPlaceholder')"
                allow-clear
                :options="statusSelectOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="5">
            <a-form-item :label="t('adTable.filters.adTypeLabel')">
              <a-select
                v-model:value="adTypeFilter"
                :placeholder="t('adTable.filters.adTypePlaceholder')"
                allow-clear
                :options="adTypeSelectOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="4">
            <a-form-item :label="t('adTable.filters.campaignLabel')">
              <a-select
                v-model:value="campaignFilter"
                :placeholder="t('adTable.filters.campaignPlaceholder')"
                allow-clear
                :options="campaignOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="2">
            <a-form-item :label="t('adTable.filters.resetLabel')">
              <a-button block ghost @click="resetFilters">
                {{ t('adTable.filters.resetButton') }}
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="summary-card" :bordered="false" :body-style="{ padding: '20px' }">
      <div class="summary-card__content">
        <div>
          <p class="summary-card__title">
            {{ t('adTable.results.showing', { count: filteredAds.length, total: totalAds }) }}
          </p>
          <p class="summary-card__muted">{{ t('adTable.summary.syncHint') }}</p>
        </div>
        <div class="summary-card__badges">
          <span v-if="hasActiveFilters" class="badge">{{ t('adTable.summary.filtersActive') }}</span>
          <span v-if="selectedRowKeys.length" class="badge">
            {{ t('adTable.bulkActions.selected', { count: selectedRowKeys.length }) }}
          </span>
        </div>
        <div class="summary-card__actions">
          <a-button type="link" v-if="selectedRowKeys.length" @click="clearSelection">
            {{ t('adTable.bulkActions.clearSelection') }}
          </a-button>
          <a-button
            type="primary"
            :disabled="!selectedRowKeys.length"
            @click="handleBulkExport"
          >
            {{ t('adTable.bulkActions.exportSelected') }}
          </a-button>
        </div>
      </div>
    </a-card>

    <div v-if="isTableView" class="table-view">
      <a-table
        :columns="columns"
        :data-source="paginatedAds"
        :loading="loading"
        :pagination="false"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
        :scroll="tableScroll"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <button type="button" class="link-button" @click="$emit('view-details', record)">
              {{ record.name || t('adTable.cells.untitledAd') }}
            </button>
            <p class="cell-muted">
              {{ t('adTable.cells.adId', { id: record.id || t('common.default.notAvailable') }) }}
            </p>
          </template>

          <template v-else-if="column.key === 'campaign'">
            <p class="cell-strong">{{ getCampaignName(record.campaignId) }}</p>
            <p class="cell-muted">
              {{ t('adTable.cells.campaignId', { id: record.campaignId || t('common.default.notAvailable') }) }}
            </p>
          </template>

          <template v-else-if="column.key === 'status'">
            <span class="status-pill" :data-state="record.status">
              {{ formatStatusLabel(record.status) }}
            </span>
          </template>

          <template v-else-if="column.key === 'adType'">
            <span class="type-pill" v-if="record.adType">{{ formatAdType(record.adType) }}</span>
            <span v-else class="cell-muted">—</span>
          </template>

          <template v-else-if="column.key === 'content'">
            <div class="content-preview">
              <div v-if="record.headline" class="content-section">
                <span class="label">{{ t('adTable.cells.headline') }}</span>
                <a-typography-paragraph
                  class="content-text"
                  :ellipsis="{ rows: 2, tooltip: record.headline }"
                >
                  {{ record.headline }}
                </a-typography-paragraph>
              </div>
              <div v-if="record.primaryText" class="content-section">
                <span class="label">{{ t('adTable.cells.text') }}</span>
                <a-typography-paragraph
                  class="content-text"
                  :ellipsis="{ rows: 2, tooltip: record.primaryText }"
                >
                  {{ record.primaryText }}
                </a-typography-paragraph>
              </div>
              <p v-if="record.callToAction" class="content-section">
                <span class="label">{{ t('adTable.cells.cta') }}</span>
                {{ record.callToAction }}
              </p>
              <p v-if="!record.headline && !record.primaryText && !record.callToAction" class="cell-muted">
                {{ t('adTable.cells.noContent') }}
              </p>
              <a-button
                type="link"
                size="small"
                class="content-expand"
                @click.stop="openContentModal(record)"
              >
                {{ t('adTable.cells.viewFullContent') }}
              </a-button>
            </div>
          </template>

          <template v-else-if="column.key === 'media'">
            <div class="media-thumb" v-if="record.imageUrl || record.videoUrl">
              <img v-if="record.imageUrl" :src="record.imageUrl" alt="Ad media" />
              <video v-else :src="record.videoUrl" muted />
            </div>
            <span v-else class="cell-muted">{{ t('adTable.cells.noMedia') }}</span>
          </template>

          <template v-else-if="column.key === 'createdDate'">
            <p>{{ formatDate(record.createdDate) }}</p>
            <p class="cell-muted">{{ formatRelativeTime(record.createdDate) }}</p>
          </template>

          <template v-else-if="column.key === 'actions'">
            <div class="row-actions">
              <a-button type="link" size="small" @click="$emit('view-details', record)">
                {{ t('adTable.actions.viewDetails') }}
              </a-button>
              <a-button type="link" size="small" @click="$emit('edit-ad', record)">
                {{ t('adTable.actions.edit') }}
              </a-button>
              <a-dropdown trigger="click">
                <a-button type="link" size="small">
                  {{ t('adTable.actions.more') }}
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="duplicate" @click="handleRowAction('duplicate-ad', record)">
                      {{ t('adTable.actions.duplicate') }}
                    </a-menu-item>
                    <a-menu-item key="export" @click="handleRowAction('export-ad', record)">
                      {{ t('adTable.actions.exportToFacebook') }}
                    </a-menu-item>
                    <a-menu-item key="download" @click="handleRowAction('download-ad', record)">
                      {{ t('adTable.actions.download') }}
                    </a-menu-item>
                    <a-menu-item key="delete">
                      <a-popconfirm
                        :title="t('adTable.actions.deleteConfirm')"
                        :ok-text="t('adTable.actions.deleteYes')"
                        :cancel-text="t('adTable.actions.deleteNo')"
                        @confirm="handleRowAction('delete-ad', record.id)"
                      >
                        <span class="danger-link">{{ t('adTable.actions.delete') }}</span>
                      </a-popconfirm>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <div v-else class="card-view">
      <a-empty
        v-if="!paginatedAds.length && !loading"
        :description="t('adTable.card.emptyDescription')"
      />
      <div v-else class="card-grid">
        <a-card
          v-for="ad in paginatedAds"
          :key="ad.id"
          class="ad-card"
          :bordered="false"
          hoverable
        >
          <div class="ad-card__media">
            <div v-if="ad.imageUrl || ad.videoUrl" class="media-frame">
              <img v-if="ad.imageUrl" :src="ad.imageUrl" alt="Ad visual" />
              <video v-else :src="ad.videoUrl" muted controls />
            </div>
            <div v-else class="media-placeholder">
              <span>{{ t('adTable.card.noMedia') }}</span>
            </div>
          </div>

          <div class="ad-card__body">
            <p class="ad-card__name">{{ ad.name || t('adTable.card.untitledAd') }}</p>
            <div class="ad-card__meta">
              <span class="status-pill" :data-state="ad.status">
                {{ formatStatusLabel(ad.status) }}
              </span>
              <span v-if="ad.adType" class="type-pill">
                {{ formatAdType(ad.adType) }}
              </span>
            </div>
            <p class="ad-card__campaign">
              {{ t('adTable.card.campaign', { name: getCampaignName(ad.campaignId) }) }}
            </p>
            <p v-if="ad.headline" class="ad-card__text">
              <span class="label">{{ t('adTable.card.headline') }}</span>
              {{ ad.headline }}
            </p>
            <p v-if="ad.primaryText" class="ad-card__text">
              <span class="label">{{ t('adTable.card.text') }}</span>
              {{ ad.primaryText }}
            </p>
            <a-button type="link" size="small" class="content-expand" @click="openContentModal(ad)">
              {{ t('adTable.cells.viewFullContent') }}
            </a-button>
          </div>

          <div class="ad-card__actions">
            <a-button block @click="$emit('view-details', ad)">{{ t('adTable.card.viewDetails') }}</a-button>
            <a-button block type="primary" ghost @click="$emit('edit-ad', ad)">{{ t('adTable.card.edit') }}</a-button>
            <a-popconfirm
              :title="t('adTable.card.deleteConfirm')"
              :ok-text="t('adTable.card.deleteYes')"
              :cancel-text="t('adTable.card.deleteNo')"
              @confirm="$emit('delete-ad', ad.id)"
            >
              <a-button block danger ghost>{{ t('adTable.card.delete') }}</a-button>
            </a-popconfirm>
          </div>
        </a-card>
      </div>
    </div>

    <div v-if="totalPages > 1" class="pagination-wrapper">
      <a-pagination
        :current="internalPage"
        :page-size="pageSizeValue"
        :total="totalAds"
        :show-total="renderTotal"
        :page-size-options="['12', '24', '48', '96']"
        show-size-changer
        show-quick-jumper
        @change="handlePageChange"
        @showSizeChange="handlePageSizeChange"
      />
    </div>

    <a-modal
      v-model:visible="contentModalVisible"
      :title="t('adTable.contentModal.title')"
      :footer="null"
      width="520px"
      destroy-on-close
    >
      <div v-if="contentModalData" class="content-modal">
        <div v-if="contentModalData.name" class="content-modal__section">
          <p class="content-modal__label">{{ t('adTable.contentModal.name') }}</p>
          <p class="content-modal__value">{{ contentModalData.name }}</p>
        </div>
        <div v-if="contentModalData.headline" class="content-modal__section">
          <p class="content-modal__label">{{ t('adTable.card.headline') }}</p>
          <p class="content-modal__value">{{ contentModalData.headline }}</p>
        </div>
        <div v-if="contentModalData.primaryText" class="content-modal__section">
          <p class="content-modal__label">{{ t('adTable.card.text') }}</p>
          <p class="content-modal__value">{{ contentModalData.primaryText }}</p>
        </div>
        <div v-if="contentModalData.callToAction" class="content-modal__section">
          <p class="content-modal__label">{{ t('adTable.cells.cta') }}</p>
          <p class="content-modal__value">{{ contentModalData.callToAction }}</p>
        </div>
      </div>
    </a-modal>
  </section>
</template>

<script>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

export default {
  name: 'AdTable',
  props: {
    ads: {
      type: Array,
      default: () => []
    },
    campaigns: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    totalItems: {
      type: Number,
      default: 0
    },
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 24
    },
    adTypeOptions: {
      type: Array,
      default: () => [
        { label: 'Website Conversion', value: 'WEBSITE_CONVERSION_AD' },
        { label: 'Page Post', value: 'PAGE_POST_AD' },
        { label: 'Lead Form', value: 'LEAD_FORM_AD' }
      ]
    },
    statusOptions: {
      type: Array,
      default: () => [
        { label: 'Draft', value: 'DRAFT' },
        { label: 'Ready', value: 'READY' },
        { label: 'Active', value: 'ACTIVE' },
        { label: 'Paused', value: 'PAUSED' },
        { label: 'Completed', value: 'COMPLETED' },
        { label: 'Failed', value: 'FAILED' }
      ]
    }
  },
  emits: [
    'view-details',
    'edit-ad',
    'delete-ad',
    'duplicate-ad',
    'export-ad',
    'download-ad',
    'page-change',
    'page-size-change'
  ],
  setup(props, { emit }) {
    const { t } = useI18n()
    const searchQuery = ref('')
    const statusFilter = ref('')
    const adTypeFilter = ref('')
    const campaignFilter = ref('')
    const viewMode = ref('table')
    const sortField = ref('createdDate')
    const sortOrder = ref('desc')
    const selectedRowKeys = ref([])
    const internalPage = ref(props.currentPage || 1)
    const pageSizeValue = computed(() => props.pageSize)
    const isCompact = ref(false)
    const contentModalVisible = ref(false)
    const contentModalData = ref(null)

    watch(
      () => props.currentPage,
      (value) => {
        internalPage.value = value || 1
      }
    )

    const statusSelectOptions = computed(() => {
      if (props.statusOptions && props.statusOptions.length) {
        return props.statusOptions
      }
      return Array.from(
        new Set(safeAds.value.map(ad => ad.status).filter(Boolean))
      ).map(status => ({
        label: formatStatusLabel(status),
        value: status
      }))
    })

    const adTypeSelectOptions = computed(() => {
      if (props.adTypeOptions && props.adTypeOptions.length) {
        return props.adTypeOptions
      }
      return Array.from(
        new Set(safeAds.value.map(ad => ad.adType).filter(Boolean))
      ).map(type => ({
        label: formatAdType(type),
        value: type
      }))
    })

    const safeCampaigns = computed(() => (Array.isArray(props.campaigns) ? props.campaigns : []))

    const campaignOptions = computed(() => {
      if (!safeCampaigns.value.length) {
        return []
      }

      return safeCampaigns.value.map((campaign) => ({
        label: campaign.name,
        value: campaign.id
      }))
    })

    const updateViewportState = () => {
      if (typeof window === 'undefined') return
      const compact = window.innerWidth < 992
      isCompact.value = compact
      if (compact) {
        viewMode.value = 'cards'
      }
    }

    onMounted(() => {
      updateViewportState()
      window.addEventListener('resize', updateViewportState)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', updateViewportState)
    })

    const viewOptions = computed(() => [
      { label: t('adTable.view.table'), value: 'table', disabled: isCompact.value },
      { label: t('adTable.view.cards'), value: 'cards' }
    ])

    const safeAds = computed(() => (Array.isArray(props.ads) ? props.ads : []))

    const totalAds = computed(() => props.totalItems || safeAds.value.length)

    const filteredAds = computed(() => {
      let filtered = [...safeAds.value]

      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter((ad) =>
          [
            ad.name,
            ad.headline,
            ad.primaryText,
            ad.id?.toString(),
            getCampaignName(ad.campaignId)
          ]
            .filter(Boolean)
            .some((field) => field.toLowerCase().includes(query))
        )
      }

      if (statusFilter.value) {
        filtered = filtered.filter((ad) => ad.status === statusFilter.value)
      }

      if (adTypeFilter.value) {
        filtered = filtered.filter((ad) => ad.adType === adTypeFilter.value)
      }

      if (campaignFilter.value) {
        filtered = filtered.filter(
          (ad) => ad.campaignId?.toString() === campaignFilter.value.toString()
        )
      }

      filtered.sort((a, b) => {
        let aValue = a[sortField.value]
        let bValue = b[sortField.value]

        if (sortField.value === 'createdDate') {
          aValue = new Date(aValue)
          bValue = new Date(bValue)
        }

        if (sortOrder.value === 'asc') {
          return aValue > bValue ? 1 : -1
        }
        return aValue < bValue ? 1 : -1
      })

      return filtered
    })

    const paginatedAds = computed(() => filteredAds.value)
    const totalPages = computed(() =>
      props.pageSize ? Math.ceil(totalAds.value / props.pageSize) : 1
    )

    const hasActiveFilters = computed(() =>
      Boolean(searchQuery.value || statusFilter.value || adTypeFilter.value || campaignFilter.value)
    )

    const columns = computed(() => [
      {
        title: t('adTable.table.columns.adName'),
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 220,
        fixed: 'left'
      },
      {
        title: t('adTable.table.columns.campaign'),
        key: 'campaign',
        width: 170,
        responsive: ['md']
      },
      {
        title: t('adTable.table.columns.status'),
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 110,
        responsive: ['lg'],
        filters: statusSelectOptions.value.map((item) => ({
          text: item.label,
          value: item.value
        }))
      },
      {
        title: t('adTable.table.columns.type'),
        key: 'adType',
        dataIndex: 'adType',
        sorter: true,
        width: 130,
        responsive: ['lg'],
        filters: adTypeSelectOptions.value.map((item) => ({
          text: item.label,
          value: item.value
        }))
      },
      {
        title: t('adTable.table.columns.content'),
        key: 'content',
        width: 360,
        responsive: ['md']
      },
      {
        title: t('adTable.table.columns.media'),
        key: 'media',
        width: 120,
        align: 'center',
        responsive: ['lg']
      },
      {
        title: t('adTable.table.columns.createdDate'),
        key: 'createdDate',
        dataIndex: 'createdDate',
        sorter: true,
        width: 150,
        responsive: ['md']
      },
      {
        title: t('adTable.table.columns.actions'),
        key: 'actions',
        width: 200,
        fixed: 'right'
      }
    ])

    const isTableView = computed(() => viewMode.value === 'table' && !isCompact.value)
    const tableScroll = computed(() => (isTableView.value ? { x: 1000 } : undefined))

    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (selectedKeys) => {
        selectedRowKeys.value = selectedKeys
      }
    }))

    const goToFirstPage = () => {
      internalPage.value = 1
      emit('page-change', 1, props.pageSize)
    }

    const handleSearch = () => {
      goToFirstPage()
    }

    const handleFilterChange = () => {
      goToFirstPage()
    }

    const resetFilters = () => {
      searchQuery.value = ''
      statusFilter.value = ''
      adTypeFilter.value = ''
      campaignFilter.value = ''
      goToFirstPage()
    }

    const handleTableChange = (pagination, filters, sorter) => {
      if (sorter?.field) {
        sortField.value = sorter.field
        sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
      }

      if (filters?.status?.length) {
        statusFilter.value = filters.status[0]
      }

      if (filters?.adType?.length) {
        adTypeFilter.value = filters.adType[0]
      }
    }

    const handlePageChange = (page, size) => {
      internalPage.value = page
      emit('page-change', page, size)
    }

    const handlePageSizeChange = (page, size) => {
      internalPage.value = 1
      emit('page-size-change', page, size)
    }

    const clearSelection = () => {
      selectedRowKeys.value = []
    }

    const handleBulkExport = () => {
      if (!selectedRowKeys.value.length) {
        return
      }
      emit('export-ad', selectedRowKeys.value)
    }

    const getCampaignName = (campaignId) => {
      if (!campaignId) return t('adTable.cells.noCampaign')
      const campaign = safeCampaigns.value.find(
        (entry) => entry.id?.toString() === campaignId?.toString()
      )
      return campaign?.name || t('adTable.cells.campaignNumber', { id: campaignId })
    }

    const formatStatusLabel = (status) => {
      if (!status) return t('adTable.cells.unknown')
      const map = {
        ACTIVE: t('adTable.table.statusFilters.active'),
        PAUSED: t('adTable.table.statusFilters.paused'),
        DRAFT: t('adTable.table.statusFilters.draft'),
        ARCHIVED: t('adTable.table.statusFilters.archived'),
        COMPLETED: t('ads.statusLabel.completed'),
        FAILED: t('ads.statusLabel.failed'),
        READY: t('ads.statusLabel.ready')
      }
      return map[status] || status
    }

    const formatEnumValue = (value) =>
      value?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())

    const formatAdType = (adType) => {
      if (!adType) return t('common.default.notAvailable')
      const map = {
        WEBSITE_CONVERSION_AD: t('ads.adTypeLabel.websiteConversion'),
        PAGE_POST_AD: t('ads.adTypeLabel.pagePost'),
        LEAD_FORM_AD: t('ads.adTypeLabel.leadForm')
      }
      return map[adType] || formatEnumValue(adType)
    }

    const formatDate = (date) => {
      if (!date) return t('common.default.notAvailable')
      return dayjs(date).format('MMM DD, YYYY')
    }

    const formatRelativeTime = (date) => {
      if (!date) return t('common.default.notAvailable')
      return dayjs(date).fromNow()
    }

    const renderTotal = (total, range) => {
      if (!range || !range.length) {
        return t('adTable.results.showing', { count: total, total })
      }
      return t('adTable.pagination.range', { start: range[0], end: range[1], total })
    }

    const openContentModal = (record) => {
      contentModalData.value = {
        name: record.name || t('adTable.cells.untitledAd'),
        headline: record.headline,
        primaryText: record.primaryText,
        callToAction: record.callToAction
      }
      contentModalVisible.value = true
    }

    const handleRowAction = (action, payload) => {
      emit(action, payload)
    }

    return {
      t,
      searchQuery,
      statusFilter,
      adTypeFilter,
      campaignFilter,
      viewMode,
      filteredAds,
      columns,
      paginatedAds,
      totalAds,
      totalPages,
      handleSearch,
      handleFilterChange,
      resetFilters,
      getCampaignName,
      formatAdType,
      formatStatusLabel,
      formatDate,
      formatRelativeTime,
      hasActiveFilters,
      selectedRowKeys,
      rowSelection,
      clearSelection,
      handleBulkExport,
      statusSelectOptions,
      adTypeSelectOptions,
      campaignOptions,
      viewOptions,
      handleTableChange,
      handlePageChange,
      handlePageSizeChange,
      internalPage,
      pageSizeValue,
      renderTotal,
      isCompact,
      isTableView,
      tableScroll,
      openContentModal,
      contentModalVisible,
      contentModalData,
      handleRowAction
    }
  }
}
</script>

<style scoped>
.ad-table {
  display: flex;
  flex-direction: column;
  gap: 24px;
  color: var(--text-primary);
  --surface: #ffffff;
  --surface-muted: #f8fafc;
  --border: #e2e8f0;
  --text-primary: #0f172a;
  --text-muted: #64748b;
  --primary: #1d4ed8;
  --primary-soft: #dbeafe;
  --shadow: 0px 12px 30px rgba(15, 23, 42, 0.08);
}

.panel,
.summary-card {
  background: var(--surface);
  border-radius: 20px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 24px;
}

.view-toggle-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.view-toggle__hint {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.panel__eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.panel__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.panel__subtitle {
  margin: 6px 0 0;
  color: var(--text-muted);
}

.view-toggle {
  min-width: 220px;
}

.filter-form {
  margin-top: 8px;
}

.summary-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.summary-card__title {
  font-weight: 600;
  margin-bottom: 4px;
}

.summary-card__muted {
  color: var(--text-muted);
  margin: 0;
}

.summary-card__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-card__actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.badge {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 13px;
}

.table-view {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: var(--shadow);
  padding: 12px;
}

.link-button {
  border: none;
  background: transparent;
  color: var(--primary);
  font-weight: 600;
  padding: 0;
  cursor: pointer;
  transition: color 0.2s ease;
}

.link-button:hover,
.row-actions .ant-btn-link:hover {
  color: #0b3b9b;
}

.cell-muted {
  color: var(--text-muted);
  margin: 0;
  font-size: 12px;
}

.cell-strong {
  font-weight: 600;
  margin-bottom: 2px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: #e2e8f0;
  color: var(--text-primary);
  text-transform: capitalize;
}

.status-pill[data-state='ACTIVE'] {
  background: #dcfce7;
  color: #166534;
}

.status-pill[data-state='PAUSED'] {
  background: #fef9c3;
  color: #854d0e;
}

.status-pill[data-state='DRAFT'] {
  background: #e0e7ff;
  color: #3730a3;
}

.status-pill[data-state='ARCHIVED'] {
  background: #f1f5f9;
  color: #475569;
}

.type-pill {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--surface-muted);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 500;
}

.content-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--text-primary);
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.content-preview .label {
  font-weight: 600;
  margin-right: 6px;
  color: var(--text-muted);
  text-transform: uppercase;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.content-text {
  margin-bottom: 0;
  color: var(--text-primary);
}

.content-expand {
  padding-left: 0;
}

.media-thumb {
  width: 72px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border);
}

.media-thumb img,
.media-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.danger-link {
  color: #dc2626;
  cursor: pointer;
}

.card-view {
  background: transparent;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.ad-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: var(--shadow);
}

.ad-card__media .media-frame {
  border-radius: 14px;
  border: 1px solid var(--border);
  overflow: hidden;
  height: 160px;
  background: var(--surface-muted);
}

.ad-card__media img,
.ad-card__media video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 160px;
  border-radius: 14px;
  background: var(--surface-muted);
  color: var(--text-muted);
  font-size: 14px;
}

.ad-card__name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.ad-card__meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ad-card__campaign {
  color: var(--text-muted);
  margin: 8px 0;
}

.ad-card__text {
  margin-bottom: 6px;
  font-size: 14px;
  white-space: pre-line;
}

.ad-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: var(--surface);
  border-radius: 20px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

:deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--text-primary);
}

:deep(.ant-input),
:deep(.ant-select-selector) {
  border-radius: 12px;
  border-color: var(--border);
  padding: 8px 12px;
}

:deep(.ant-input:focus),
:deep(.ant-select-focused .ant-select-selector) {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.18);
}

:deep(.ant-table) {
  font-size: 14px;
}

:deep(.ant-table-thead > tr > th) {
  background: var(--surface-muted);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  color: var(--text-muted);
}

:deep(.ant-table-cell) {
  vertical-align: middle;
}

:deep(.ant-segmented) {
  border-radius: 999px;
  background: var(--surface-muted);
  padding: 4px;
}

:deep(.ant-segmented-item-selected) {
  background: var(--surface);
  color: var(--primary);
  font-weight: 600;
  box-shadow: var(--shadow);
}

:deep(.ant-btn) {
  border-radius: 999px;
  transition: all 0.2s ease;
}

:deep(.ant-btn-primary) {
  background: var(--primary);
  border-color: var(--primary);
}

:deep(.ant-btn-primary:hover) {
  background: #153eaa;
  border-color: #153eaa;
}

:deep(.ant-typography) {
  margin-bottom: 0;
}

:deep(.ant-btn-link) {
  padding-inline: 0;
}

@media (max-width: 992px) {
  .panel__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-card__content {
    flex-direction: column;
    align-items: flex-start;
  }

  .row-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}

.content-modal__section + .content-modal__section {
  margin-top: 16px;
}

.content-modal__label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.content-modal__value {
  white-space: pre-line;
  margin: 0;
  color: var(--text-primary);
}
</style>
