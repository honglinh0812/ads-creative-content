<template>
  <section class="ad-table">
    <a-card class="panel" :bordered="false" :body-style="{ padding: '24px' }">
      <header class="panel__header">
        <div>
          <p class="panel__eyebrow">Creative library</p>
          <h2 class="panel__title">Ad performance overview</h2>
          <p class="panel__subtitle">Use filters to focus on the assets that matter today.</p>
        </div>
        <a-segmented
          v-model:value="viewMode"
          class="view-toggle"
          :options="viewOptions"
          size="large"
        />
      </header>

      <a-form layout="vertical" class="filter-form">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :md="8">
            <a-form-item label="Search ads">
              <a-input
                v-model:value="searchQuery"
                placeholder="Search by ad name, campaign, or text"
                allow-clear
                @input="handleSearch"
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="5">
            <a-form-item label="Status">
              <a-select
                v-model:value="statusFilter"
                placeholder="Select status"
                allow-clear
                :options="statusSelectOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="5">
            <a-form-item label="Ad type">
              <a-select
                v-model:value="adTypeFilter"
                placeholder="Select type"
                allow-clear
                :options="adTypeSelectOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="4">
            <a-form-item label="Campaign">
              <a-select
                v-model:value="campaignFilter"
                placeholder="Select campaign"
                allow-clear
                :options="campaignOptions"
                @change="handleFilterChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="2">
            <a-form-item label=" ">
              <a-button block ghost @click="resetFilters">
                Reset
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="summary-card" :bordered="false" :body-style="{ padding: '20px' }">
      <div class="summary-card__content">
        <div>
          <p class="summary-card__title">Showing {{ filteredAds.length }} of {{ totalAds }} ads</p>
          <p class="summary-card__muted">Live data synced with Facebook every few minutes.</p>
        </div>
        <div class="summary-card__badges">
          <span v-if="hasActiveFilters" class="badge">Filters active</span>
          <span v-if="selectedRowKeys.length" class="badge">Selected {{ selectedRowKeys.length }}</span>
        </div>
        <div class="summary-card__actions">
          <a-button type="link" v-if="selectedRowKeys.length" @click="clearSelection">
            Clear selection
          </a-button>
          <a-button
            type="primary"
            :disabled="!selectedRowKeys.length"
            @click="handleBulkExport"
          >
            Export selected
          </a-button>
        </div>
      </div>
    </a-card>

    <div v-if="viewMode === 'table'" class="table-view">
      <a-table
        :columns="columns"
        :data-source="paginatedAds"
        :loading="loading"
        :pagination="false"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <button type="button" class="link-button" @click="$emit('view-details', record)">
              {{ record.name || 'Untitled ad' }}
            </button>
            <p class="cell-muted">Ad ID • {{ record.id }}</p>
          </template>

          <template v-else-if="column.key === 'campaign'">
            <p class="cell-strong">{{ getCampaignName(record.campaignId) }}</p>
            <p class="cell-muted">Campaign ID • {{ record.campaignId || 'N/A' }}</p>
          </template>

          <template v-else-if="column.key === 'status'">
            <span class="status-pill" :data-state="record.status">
              {{ record.status || 'Unknown' }}
            </span>
          </template>

          <template v-else-if="column.key === 'adType'">
            <span class="type-pill" v-if="record.adType">{{ formatAdType(record.adType) }}</span>
            <span v-else class="cell-muted">—</span>
          </template>

          <template v-else-if="column.key === 'content'">
            <div class="content-preview">
              <p v-if="record.headline">
                <span class="label">Headline</span>
                {{ truncateText(record.headline, 80) }}
              </p>
              <p v-if="record.primaryText">
                <span class="label">Text</span>
                {{ truncateText(record.primaryText, 110) }}
              </p>
              <p v-if="record.callToAction">
                <span class="label">CTA</span>
                {{ record.callToAction }}
              </p>
              <p v-if="!record.headline && !record.primaryText && !record.callToAction" class="cell-muted">
                Content unavailable
              </p>
            </div>
          </template>

          <template v-else-if="column.key === 'media'">
            <div class="media-thumb" v-if="record.imageUrl || record.videoUrl">
              <img v-if="record.imageUrl" :src="record.imageUrl" alt="Ad media" />
              <video v-else :src="record.videoUrl" muted />
            </div>
            <span v-else class="cell-muted">No media</span>
          </template>

          <template v-else-if="column.key === 'createdDate'">
            <p>{{ formatDate(record.createdDate) }}</p>
            <p class="cell-muted">{{ formatRelativeTime(record.createdDate) }}</p>
          </template>

          <template v-else-if="column.key === 'actions'">
            <div class="row-actions">
              <a-button type="link" size="small" @click="$emit('view-details', record)">
                View
              </a-button>
              <a-button type="link" size="small" @click="$emit('edit-ad', record)">
                Edit
              </a-button>
              <a-button type="link" size="small" @click="$emit('duplicate-ad', record)">
                Duplicate
              </a-button>
              <a-button type="link" size="small" @click="$emit('export-ad', record)">
                Export
              </a-button>
              <a-button type="link" size="small" @click="$emit('download-ad', record)">
                Download
              </a-button>
              <a-popconfirm
                title="Delete this ad?"
                ok-text="Delete"
                cancel-text="Cancel"
                @confirm="$emit('delete-ad', record.id)"
              >
                <a-button type="link" danger size="small">
                  Delete
                </a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
    </div>

    <div v-else class="card-view">
      <a-empty
        v-if="!paginatedAds.length && !loading"
        description="No ads match the selected filters."
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
              <span>No media provided</span>
            </div>
          </div>

          <div class="ad-card__body">
            <p class="ad-card__name">{{ ad.name || 'Untitled ad' }}</p>
            <div class="ad-card__meta">
              <span class="status-pill" :data-state="ad.status">
                {{ ad.status || 'Unknown' }}
              </span>
              <span v-if="ad.adType" class="type-pill">
                {{ formatAdType(ad.adType) }}
              </span>
            </div>
            <p class="ad-card__campaign">
              {{ getCampaignName(ad.campaignId) }}
            </p>
            <p v-if="ad.headline" class="ad-card__text">
              <span class="label">Headline</span>
              {{ truncateText(ad.headline, 90) }}
            </p>
            <p v-if="ad.primaryText" class="ad-card__text">
              <span class="label">Text</span>
              {{ truncateText(ad.primaryText, 120) }}
            </p>
          </div>

          <div class="ad-card__actions">
            <a-button block @click="$emit('view-details', ad)">View details</a-button>
            <a-button block type="primary" ghost @click="$emit('edit-ad', ad)">Edit ad</a-button>
            <a-popconfirm
              title="Delete this ad?"
              ok-text="Delete"
              cancel-text="Cancel"
              @confirm="$emit('delete-ad', ad.id)"
            >
              <a-button block danger ghost>Delete</a-button>
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
  </section>
</template>

<script>
import { ref, computed, watch } from 'vue'
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

    watch(
      () => props.currentPage,
      (value) => {
        internalPage.value = value || 1
      }
    )

    const statusSelectOptions = [
      { label: 'Active', value: 'ACTIVE' },
      { label: 'Paused', value: 'PAUSED' },
      { label: 'Draft', value: 'DRAFT' },
      { label: 'Archived', value: 'ARCHIVED' }
    ]

    const adTypeSelectOptions = [
      { label: 'Image', value: 'IMAGE' },
      { label: 'Video', value: 'VIDEO' },
      { label: 'Carousel', value: 'CAROUSEL' },
      { label: 'Collection', value: 'COLLECTION' }
    ]

    const campaignOptions = computed(() => {
      if (!props.campaigns || !props.campaigns.length) {
        return []
      }

      return props.campaigns.map((campaign) => ({
        label: campaign.name,
        value: campaign.id
      }))
    })

    const viewOptions = [
      { label: 'Table view', value: 'table' },
      { label: 'Card view', value: 'cards' }
    ]

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
        title: 'Ad name',
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 220,
        fixed: 'left'
      },
      {
        title: 'Campaign',
        key: 'campaign',
        width: 180
      },
      {
        title: 'Status',
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 120,
        filters: statusSelectOptions.map((item) => ({
          text: item.label,
          value: item.value
        }))
      },
      {
        title: 'Type',
        key: 'adType',
        dataIndex: 'adType',
        sorter: true,
        width: 120
      },
      {
        title: 'Content',
        key: 'content',
        width: 320
      },
      {
        title: 'Media',
        key: 'media',
        width: 120,
        align: 'center'
      },
      {
        title: 'Created',
        key: 'createdDate',
        dataIndex: 'createdDate',
        sorter: true,
        width: 160
      },
      {
        title: 'Actions',
        key: 'actions',
        width: 240,
        fixed: 'right'
      }
    ])

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
      if (!campaignId) return 'No campaign'
      const campaign = props.campaigns.find(
        (entry) => entry.id?.toString() === campaignId?.toString()
      )
      return campaign?.name || `Campaign ${campaignId}`
    }

    const formatAdType = (adType) =>
      adType?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())

    const truncateText = (text, maxLength) => {
      if (!text) return ''
      return text.length > maxLength ? `${text.substring(0, maxLength)}…` : text
    }

    const formatDate = (date) => {
      if (!date) return 'Not set'
      return dayjs(date).format('MMM DD, YYYY')
    }

    const formatRelativeTime = (date) => {
      if (!date) return 'Unavailable'
      return dayjs(date).fromNow()
    }

    const renderTotal = (total, range) => {
      if (!range || !range.length) {
        return `${total} ads`
      }
      return `${range[0]}-${range[1]} of ${total} ads`
    }

    return {
      searchQuery,
      statusFilter,
      adTypeFilter,
      campaignFilter,
      viewMode,
      columns,
      paginatedAds,
      totalAds,
      totalPages,
      handleSearch,
      handleFilterChange,
      resetFilters,
      getCampaignName,
      formatAdType,
      truncateText,
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
      renderTotal
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

.content-preview .label {
  font-weight: 600;
  margin-right: 6px;
  color: var(--text-muted);
  text-transform: uppercase;
  font-size: 11px;
  letter-spacing: 0.08em;
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
</style>
