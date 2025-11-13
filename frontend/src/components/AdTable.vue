<template>
  <div class="ad-table-container">
    <!-- Advanced Filters -->
    <div class="filters-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="12" :md="6">
          <a-input
            v-model:value="searchQuery"
            :placeholder="$t('adTable.filters.searchPlaceholder')"
            size="large"
            allow-clear
            :aria-label="$t('adTable.filters.searchLabel')"
            role="searchbox"
            @input="handleSearch"
            @keydown.enter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined aria-hidden="true" />
            </template>
          </a-input>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="statusFilter"
            :placeholder="$t('adTable.filters.statusPlaceholder')"
            size="large"
            allow-clear
            style="width: 100%"
            :aria-label="$t('adTable.filters.statusLabel')"
            @change="handleFilterChange"
          >
            <a-select-option value="">{{ $t('adTable.filterOptions.allStatus') }}</a-select-option>
            <a-select-option value="ACTIVE">{{ $t('adTable.filterOptions.active') }}</a-select-option>
            <a-select-option value="PAUSED">{{ $t('adTable.filterOptions.paused') }}</a-select-option>
            <a-select-option value="DRAFT">{{ $t('adTable.filterOptions.draft') }}</a-select-option>
            <a-select-option value="ARCHIVED">{{ $t('adTable.filterOptions.archived') }}</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="adTypeFilter"
            :placeholder="$t('adTable.filters.adTypePlaceholder')"
            size="large"
            allow-clear
            style="width: 100%"
            :aria-label="$t('adTable.filters.adTypeLabel')"
            @change="handleFilterChange"
          >
            <a-select-option value="">{{ $t('adTable.filterOptions.allTypes') }}</a-select-option>
            <a-select-option value="IMAGE">{{ $t('adTable.filterOptions.image') }}</a-select-option>
            <a-select-option value="VIDEO">{{ $t('adTable.filterOptions.video') }}</a-select-option>
            <a-select-option value="CAROUSEL">{{ $t('adTable.filterOptions.carousel') }}</a-select-option>
            <a-select-option value="COLLECTION">{{ $t('adTable.filterOptions.collection') }}</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="campaignFilter"
            :placeholder="$t('adTable.filters.campaignPlaceholder')"
            size="large"
            allow-clear
            style="width: 100%"
            :aria-label="$t('adTable.filters.campaignLabel')"
            @change="handleFilterChange"
          >
            <a-select-option value="">{{ $t('adTable.filterOptions.allCampaigns') }}</a-select-option>
            <a-select-option v-for="campaign in campaigns" :key="campaign.id" :value="campaign.id">
              {{ campaign.name }}
            </a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="3">
          <a-button
            size="large"
            style="width: 100%"
            :aria-label="$t('adTable.filters.resetLabel')"
            @click="resetFilters"
          >
            <template #icon>
              <ClearOutlined aria-hidden="true" />
            </template>
            {{ $t('adTable.filters.resetButton') }}
          </a-button>
        </a-col>

        <a-col :xs="24" :sm="12" :md="3">
          <a-button
            type="primary"
            size="large"
            style="width: 100%"
            :aria-label="viewMode === 'table' ? $t('adTable.filters.viewToggleTable') : $t('adTable.filters.viewToggleCards')"
            @click="toggleView"
          >
            <template #icon>
              <component :is="viewMode === 'table' ? 'AppstoreOutlined' : 'TableOutlined'" aria-hidden="true" />
            </template>
            {{ viewMode === 'table' ? $t('adTable.filters.cardsButton') : $t('adTable.filters.tableButton') }}
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- Results Summary & Bulk Actions -->
    <div class="results-actions-bar">
      <div id="results-summary" class="results-summary" role="status" aria-live="polite">
        <a-typography-text type="secondary">
          {{ $t('adTable.results.showing', { count: filteredAds.length, total: totalAds }) }}
          <span v-if="hasActiveFilters"> {{ $t('adTable.results.filtered') }}</span>
        </a-typography-text>
      </div>

      <!-- Bulk Actions -->
      <div v-if="selectedRowKeys.length > 0" class="bulk-actions">
        <a-space>
          <a-typography-text strong>
            {{ $t('adTable.bulkActions.selected', { count: selectedRowKeys.length }) }}
          </a-typography-text>
          <a-button size="small" @click="clearSelection">
            {{ $t('adTable.bulkActions.clearSelection') }}
          </a-button>
          <a-button
            type="primary"
            size="small"
            @click="handleBulkExport"
          >
            <template #icon>
              <ExportOutlined />
            </template>
            {{ $t('adTable.bulkActions.exportSelected') }}
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="table-section">
      <a-table
        :columns="translatedColumns"
        :data-source="paginatedAds"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1400 }"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
        role="table"
        :aria-label="$t('adTable.table.ariaLabel')"
        aria-describedby="results-summary"
        @change="handleTableChange"
      >
        <!-- Custom column renderers -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="ad-name-cell">
              <a-typography-title :level="5" style="margin: 0; cursor: pointer" @click="$emit('view-details', record)">
                {{ record.name || $t('adTable.cells.untitledAd') }}
              </a-typography-title>
              <a-typography-text type="secondary" style="font-size: 12px">
                {{ $t('adTable.cells.adId', { id: record.id }) }}
              </a-typography-text>
            </div>
          </template>

          <template v-else-if="column.key === 'campaign'">
            <div class="campaign-cell">
              <a-typography-text strong>{{ getCampaignName(record.campaignId) }}</a-typography-text>
              <br>
              <a-typography-text type="secondary" style="font-size: 11px">
                {{ $t('adTable.cells.campaignId', { id: record.campaignId }) }}
              </a-typography-text>
            </div>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.status || $t('adTable.cells.unknown') }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'adType'">
            <a-tag color="blue" v-if="record.adType">
              {{ formatAdType(record.adType) }}
            </a-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
          
          <template v-else-if="column.key === 'content'">
            <div class="content-cell">
              <div v-if="record.headline" class="content-item">
                <strong>{{ $t('adTable.cells.headline') }}</strong> {{ truncateText(record.headline, 30) }}
              </div>
              <div v-if="record.primaryText" class="content-item">
                <strong>{{ $t('adTable.cells.text') }}</strong> {{ truncateText(record.primaryText, 40) }}
              </div>
              <div v-if="record.callToAction" class="content-item">
                <a-tag size="small" color="green">{{ record.callToAction }}</a-tag>
              </div>
            </div>
          </template>
          
          <template v-else-if="column.key === 'media'">
            <div class="media-cell">
              <div v-if="record.imageUrl" class="media-item">
                <img :src="record.imageUrl" alt="Ad Image" class="media-thumbnail" />
              </div>
              <div v-else-if="record.videoUrl" class="media-item">
                <video :src="record.videoUrl" class="media-thumbnail" controls />
              </div>
              <div v-else class="no-media">
                <FileImageOutlined style="font-size: 24px; color: #d9d9d9;" />
              </div>
            </div>
          </template>
          
          <template v-else-if="column.key === 'createdDate'">
            <div class="date-cell">
              <div>{{ formatDate(record.createdDate) }}</div>
              <a-typography-text type="secondary" style="font-size: 11px">
                {{ formatRelativeTime(record.createdDate) }}
              </a-typography-text>
            </div>
          </template>
          
          <template v-else-if="column.key === 'actions'">
            <a-space size="small">
              <a-tooltip :title="$t('adTable.actions.viewDetails')">
                <a-button
                  size="small"
                  :aria-label="$t('adTable.actions.viewDetailsLabel', { name: record.name || $t('adTable.cells.untitledAd') })"
                  @click="$emit('view-details', record)"
                >
                  <template #icon>
                    <EyeOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="$t('adTable.actions.editAd')">
                <a-button
                  size="small"
                  :aria-label="$t('adTable.actions.editLabel', { name: record.name || $t('adTable.cells.untitledAd') })"
                  @click="$emit('edit-ad', record)"
                >
                  <template #icon>
                    <EditOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="$t('adTable.actions.duplicateAd')">
                <a-button
                  size="small"
                  :aria-label="$t('adTable.actions.duplicateLabel', { name: record.name || $t('adTable.cells.untitledAd') })"
                  @click="$emit('duplicate-ad', record)"
                >
                  <template #icon>
                    <CopyOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="$t('adTable.actions.exportToFacebook')">
                <a-button
                  size="small"
                  type="primary"
                  :aria-label="$t('adTable.actions.exportLabel', { name: record.name || $t('adTable.cells.untitledAd') })"
                  @click="$emit('export-ad', record)"
                >
                  <template #icon>
                    <ExportOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-popconfirm
                :title="$t('adTable.actions.deleteConfirm')"
                :ok-text="$t('adTable.actions.deleteYes')"
                :cancel-text="$t('adTable.actions.deleteNo')"
                @confirm="$emit('delete-ad', record.id)"
              >
                <a-tooltip :title="$t('adTable.actions.deleteAd')">
                  <a-button
                    size="small"
                    danger
                    :aria-label="$t('adTable.actions.deleteLabel', { name: record.name || $t('adTable.cells.untitledAd') })"
                  >
                    <template #icon>
                      <DeleteOutlined aria-hidden="true" />
                    </template>
                  </a-button>
                </a-tooltip>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- Card View -->
    <div v-else class="cards-section">
      <a-row :gutter="[24, 24]">
        <a-col v-for="ad in paginatedAds" :key="ad.id" :xs="24" :sm="12" :md="8" :lg="6">
          <a-card
            hoverable
            class="ad-card"
            :body-style="{ padding: '16px' }"
          >
            <!-- Ad Media -->
            <div class="ad-media">
              <div v-if="ad.imageUrl" class="media-container">
                <img :src="ad.imageUrl" alt="Ad Image" class="ad-image" />
              </div>
              <div v-else-if="ad.videoUrl" class="media-container">
                <video :src="ad.videoUrl" class="ad-video" controls />
              </div>
              <div v-else class="no-media-placeholder">
                <FileImageOutlined style="font-size: 48px; color: #d9d9d9;" />
                <p>{{ $t('adTable.card.noMedia') }}</p>
              </div>
            </div>
            
            <!-- Ad Info -->
            <div class="ad-info">
              <a-typography-title :level="5" :ellipsis="{ rows: 2 }" style="margin-bottom: 8px;">
                {{ ad.name || $t('adTable.card.untitledAd') }}
              </a-typography-title>

              <div class="ad-meta">
                <a-tag :color="getStatusColor(ad.status)" size="small">
                  {{ ad.status || $t('adTable.card.unknown') }}
                </a-tag>
                <a-tag color="blue" size="small" v-if="ad.adType">
                  {{ formatAdType(ad.adType) }}
                </a-tag>
              </div>

              <div class="campaign-info">
                <a-typography-text type="secondary" style="font-size: 12px;">
                  {{ $t('adTable.card.campaign', { name: getCampaignName(ad.campaignId) }) }}
                </a-typography-text>
              </div>

              <div class="ad-content" v-if="ad.headline || ad.primaryText">
                <div v-if="ad.headline" class="content-line">
                  <strong>{{ $t('adTable.card.headline') }}</strong> {{ truncateText(ad.headline, 40) }}
                </div>
                <div v-if="ad.primaryText" class="content-line">
                  <strong>{{ $t('adTable.card.text') }}</strong> {{ truncateText(ad.primaryText, 60) }}
                </div>
              </div>
            </div>
            
            <a-divider style="margin: 12px 0;" />
            
            <!-- Actions -->
            <a-row justify="space-between" align="middle">
              <a-col>
                <a-space size="small">
                  <a-button size="small" @click="$emit('edit-ad', ad)">
                    <template #icon>
                      <EditOutlined />
                    </template>
                    {{ $t('adTable.card.edit') }}
                  </a-button>
                  <a-popconfirm
                    :title="$t('adTable.card.deleteConfirm')"
                    :ok-text="$t('adTable.card.deleteYes')"
                    :cancel-text="$t('adTable.card.deleteNo')"
                    @confirm="$emit('delete-ad', ad.id)"
                  >
                    <a-button size="small" danger>
                      <template #icon>
                        <DeleteOutlined />
                      </template>
                      {{ $t('adTable.card.delete') }}
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </a-col>
              <a-col>
                <a-button type="primary" size="small" @click="$emit('view-details', ad)">
                  {{ $t('adTable.card.viewDetails') }}
                </a-button>
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination-section">
      <a-pagination
        :current="currentPage"
        :page-size="pageSize"
        :total="totalAds"
        :page-size-options="['12', '24', '48', '96']"
        show-size-changer
        show-quick-jumper
        show-total
        @change="handlePageChange"
        @showSizeChange="handlePageSizeChange"
      >
        <template #buildOptionText="props">
          <span>{{ $t('adTable.pagination.perPage', { value: props.value }) }}</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  SearchOutlined,
  ClearOutlined,
  TableOutlined,
  AppstoreOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  ExportOutlined,
  FileImageOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/vi'
import 'dayjs/locale/en'

dayjs.extend(relativeTime)

export default {
  name: 'AdTable',
  components: {
    SearchOutlined,
    ClearOutlined,
    TableOutlined,
    AppstoreOutlined,
    EyeOutlined,
    EditOutlined,
    DeleteOutlined,
    CopyOutlined,
    ExportOutlined,
    FileImageOutlined
  },
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
  emits: ['view-details', 'edit-ad', 'delete-ad', 'duplicate-ad', 'export-ad', 'page-change', 'page-size-change'],

  setup(props, { emit }) {
    const { t, locale } = useI18n()

    // Reactive data
    const searchQuery = ref('')
    const statusFilter = ref('')
    const adTypeFilter = ref('')
    const campaignFilter = ref('')
    const viewMode = ref('table')
    const sortField = ref('createdDate')
    const sortOrder = ref('desc')
    const selectedRowKeys = ref([])

    // Table columns configuration with i18n
    const translatedColumns = computed(() => [
      {
        title: t('adTable.table.columns.adName'),
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 200,
        fixed: 'left'
      },
      {
        title: t('adTable.table.columns.campaign'),
        key: 'campaign',
        sorter: true,
        width: 180
      },
      {
        title: t('adTable.table.columns.status'),
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 100,
        filters: [
          { text: t('adTable.table.statusFilters.active'), value: 'ACTIVE' },
          { text: t('adTable.table.statusFilters.paused'), value: 'PAUSED' },
          { text: t('adTable.table.statusFilters.draft'), value: 'DRAFT' },
          { text: t('adTable.table.statusFilters.archived'), value: 'ARCHIVED' }
        ]
      },
      {
        title: t('adTable.table.columns.type'),
        key: 'adType',
        dataIndex: 'adType',
        sorter: true,
        width: 100
      },
      {
        title: t('adTable.table.columns.content'),
        key: 'content',
        width: 300
      },
      {
        title: t('adTable.table.columns.media'),
        key: 'media',
        width: 100,
        align: 'center'
      },
      {
        title: t('adTable.table.columns.createdDate'),
        key: 'createdDate',
        dataIndex: 'createdDate',
        sorter: true,
        width: 150
      },
      {
        title: t('adTable.table.columns.actions'),
        key: 'actions',
        width: 250,
        fixed: 'right'
      }
    ])
    
    // Computed properties
    const totalAds = computed(() => props.totalItems || props.ads.length)

    const filteredAds = computed(() => {
      let filtered = [...props.ads]

      // Search filter (frontend only for displayed page)
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(ad =>
          ad.name?.toLowerCase().includes(query) ||
          ad.headline?.toLowerCase().includes(query) ||
          ad.primaryText?.toLowerCase().includes(query) ||
          ad.id?.toString().includes(query)
        )
      }

      // Status filter (frontend only for displayed page)
      if (statusFilter.value) {
        filtered = filtered.filter(ad => ad.status === statusFilter.value)
      }

      // Ad type filter (frontend only for displayed page)
      if (adTypeFilter.value) {
        filtered = filtered.filter(ad => ad.adType === adTypeFilter.value)
      }

      // Campaign filter (frontend only for displayed page)
      if (campaignFilter.value) {
        filtered = filtered.filter(ad => ad.campaignId?.toString() === campaignFilter.value.toString())
      }

      // Sorting (frontend only for displayed page)
      filtered.sort((a, b) => {
        let aValue = a[sortField.value]
        let bValue = b[sortField.value]

        if (sortField.value === 'createdDate') {
          aValue = new Date(aValue)
          bValue = new Date(bValue)
        }

        if (sortOrder.value === 'asc') {
          return aValue > bValue ? 1 : -1
        } else {
          return aValue < bValue ? 1 : -1
        }
      })

      return filtered
    })

    const totalPages = computed(() => Math.ceil(totalAds.value / props.pageSize))

    const paginatedAds = computed(() => {
      // When using server-side pagination, just return the filtered ads as they are already paginated
      return filteredAds.value
    })
    
    const hasActiveFilters = computed(() => {
      return searchQuery.value || statusFilter.value || adTypeFilter.value || campaignFilter.value
    })
    
    // Methods
    const handleSearch = () => {
      currentPage.value = 1
    }
    
    const handleFilterChange = () => {
      currentPage.value = 1
    }
    
    const resetFilters = () => {
      searchQuery.value = ''
      statusFilter.value = ''
      adTypeFilter.value = ''
      campaignFilter.value = ''
      currentPage.value = 1
    }
    
    const toggleView = () => {
      viewMode.value = viewMode.value === 'table' ? 'cards' : 'table'
    }
    
    const handleTableChange = (pagination, filters, sorter) => {
      if (sorter.field) {
        sortField.value = sorter.field
        sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
      }
    }
    
    const handlePageChange = (page, size) => {
      emit('page-change', page, size)
    }

    const handlePageSizeChange = (current, size) => {
      emit('page-size-change', current, size)
    }
    
    // Utility functions
    const getStatusColor = (status) => {
      const colors = {
        'ACTIVE': 'green',
        'PAUSED': 'orange',
        'DRAFT': 'blue',
        'ARCHIVED': 'purple',
        'DELETED': 'red'
      }
      return colors[status] || 'default'
    }
    
    const formatAdType = (adType) => {
      return adType?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const getCampaignName = (campaignId) => {
      if (!campaignId) return t('adTable.cells.noCampaign')

      // Check if campaigns are loaded
      if (!props.campaigns || props.campaigns.length === 0) {
        return t('adTable.cells.loading')
      }

      // Convert to string for comparison (handle type mismatch)
      const campaign = props.campaigns.find(c => c.id?.toString() === campaignId?.toString())
      return campaign?.name || t('adTable.cells.campaignNumber', { id: campaignId })
    }

    const truncateText = (text, maxLength) => {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    }

    const formatDate = (date) => {
      // Set dayjs locale based on i18n locale
      const dayjsLocale = locale.value === 'vi' ? 'vi' : 'en'
      return dayjs(date).locale(dayjsLocale).format('MMM DD, YYYY')
    }

    const formatRelativeTime = (date) => {
      // Set dayjs locale based on i18n locale
      const dayjsLocale = locale.value === 'vi' ? 'vi' : 'en'
      return dayjs(date).locale(dayjsLocale).fromNow()
    }

    // Bulk selection handlers
    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (selectedKeys) => {
        selectedRowKeys.value = selectedKeys
      },
      getCheckboxProps: (record) => ({
        name: record.name
      })
    }))

    const clearSelection = () => {
      selectedRowKeys.value = []
    }

    const handleBulkExport = () => {
      if (selectedRowKeys.value.length === 0) {
        return
      }
      // Emit the export-ad event with array of selected ad IDs
      emit('export-ad', selectedRowKeys.value)
    }

    return {
      searchQuery,
      statusFilter,
      adTypeFilter,
      campaignFilter,
      viewMode,
      sortField,
      sortOrder,
      selectedRowKeys,
      rowSelection,
      translatedColumns,
      totalAds,
      filteredAds,
      totalPages,
      paginatedAds,
      hasActiveFilters,
      handleSearch,
      handleFilterChange,
      resetFilters,
      toggleView,
      handleTableChange,
      handlePageChange,
      handlePageSizeChange,
      clearSelection,
      handleBulkExport,
      getStatusColor,
      formatAdType,
      getCampaignName,
      truncateText,
      formatDate,
      formatRelativeTime
    }
  }
}
</script>

<style scoped>
.ad-table-container {
  @apply space-y-6;
}

.filters-section {
  @apply bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700;
}

.results-actions-bar {
  @apply flex justify-between items-center bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 mb-4;
}

.results-summary {
  @apply flex items-center;
}

.bulk-actions {
  @apply flex items-center gap-2;
}

.table-section {
  @apply bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden;
}

.cards-section {
  @apply min-h-[400px];
}

.ad-card {
  @apply transition-all duration-200 hover:shadow-lg border border-gray-200 dark:border-gray-700;
}

.ad-card:hover {
  @apply transform -translate-y-1;
}

.ad-media {
  @apply mb-4;
}

.media-container {
  @apply relative overflow-hidden rounded-lg;
}

.ad-image, .ad-video {
  @apply w-full h-32 object-cover;
}

.no-media-placeholder {
  @apply flex flex-col items-center justify-center h-32 bg-gray-100 dark:bg-gray-700 rounded-lg;
}

.ad-info {
  @apply space-y-2;
}

.ad-meta {
  @apply flex gap-2 flex-wrap;
}

.campaign-info {
  @apply mt-2;
}

.ad-content {
  @apply mt-3 space-y-1;
}

.content-line {
  @apply text-sm text-gray-600 dark:text-gray-300;
}

.ad-name-cell {
  @apply space-y-1;
}

.campaign-cell {
  @apply space-y-1;
}

.content-cell {
  @apply space-y-2;
}

.content-item {
  @apply text-sm;
}

.media-cell {
  @apply flex justify-center;
}

.media-thumbnail {
  @apply w-12 h-12 object-cover rounded;
}

.no-media {
  @apply flex justify-center items-center w-12 h-12 bg-gray-100 dark:bg-gray-700 rounded;
}

.date-cell {
  @apply space-y-1;
}

.pagination-section {
  @apply flex justify-center bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700;
}

/* Dark mode styles */
.dark .ad-table-container {
  @apply text-white;
}

.dark .filters-section {
  @apply bg-gray-800 border-gray-700;
}

.dark .table-section {
  @apply bg-gray-800 border-gray-700;
}

.dark .ad-card {
  @apply bg-gray-800 border-gray-700;
}

.dark .pagination-section {
  @apply bg-gray-800 border-gray-700;
}

/* Responsive styles */
@media (max-width: 768px) {
  .filters-section {
    @apply p-4;
  }
  
  .ad-card {
    @apply mb-4;
  }
}

/* Table responsive styles */
:deep(.ant-table-wrapper) {
  @apply overflow-x-auto;
}

:deep(.ant-table-thead > tr > th) {
  @apply bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600;
}

/* Accessibility improvements */
.sr-only {
  @apply absolute w-px h-px p-0 -m-px overflow-hidden whitespace-nowrap border-0;
  clip: rect(0, 0, 0, 0);
}

/* Focus styles for better keyboard navigation */
:deep(.ant-btn:focus-visible),
:deep(.ant-input:focus-visible),
:deep(.ant-select:focus-visible) {
  @apply outline-2 outline-blue-500 outline-offset-2;
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .ad-card {
    @apply border-2 border-black;
  }
  
  :deep(.ant-table-thead > tr > th) {
    @apply border-2 border-black;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .ad-card {
    @apply transition-none;
  }
  
  .ad-card:hover {
    @apply transform-none;
  }
}

/* Touch target improvements for mobile */
@media (max-width: 768px) {
  :deep(.ant-btn) {
    @apply min-h-[44px] min-w-[44px];
  }
  
  .filters-section .ant-col {
    @apply mb-3;
  }
  
  .view-toggle {
    @apply w-full;
  }
}

/* Improved responsive table */
@media (max-width: 1024px) {
  :deep(.ant-table-wrapper) {
    @apply text-sm;
  }
  
  .media-thumbnail {
    @apply w-8 h-8;
  }
}

/* Better spacing for small screens */
@media (max-width: 480px) {
  .filters-section {
    @apply p-3;
  }
  
  .results-summary {
    @apply text-sm;
  }
  
  :deep(.ant-space-item) {
    @apply mb-2;
  }
}

:deep(.ant-table-tbody > tr > td) {
  @apply border-b border-gray-100 dark:border-gray-700;
}

:deep(.ant-table-tbody > tr:hover > td) {
  @apply bg-gray-50 dark:bg-gray-700;
}

/* Custom scrollbar */
:deep(.ant-table-body) {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e0 #f7fafc;
}

:deep(.ant-table-body::-webkit-scrollbar) {
  height: 8px;
}

:deep(.ant-table-body::-webkit-scrollbar-track) {
  @apply bg-gray-100 dark:bg-gray-700;
}

:deep(.ant-table-body::-webkit-scrollbar-thumb) {
  @apply bg-gray-300 dark:bg-gray-500 rounded;
}

:deep(.ant-table-body::-webkit-scrollbar-thumb:hover) {
  @apply bg-gray-400 dark:bg-gray-400;
}
</style>