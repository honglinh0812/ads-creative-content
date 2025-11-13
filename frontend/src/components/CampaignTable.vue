<template>
  <div class="campaign-table-container">
    <!-- Advanced Filters -->
    <div class="filters-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="12" :md="6">
          <a-input
            v-model:value="searchQuery"
            :placeholder="$t('campaign.table.placeholder.search')"
            size="large"
            allow-clear
            aria-label="Search campaigns"
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
            :placeholder="$t('campaign.table.placeholder.status')"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by status"
            @change="handleFilterChange"
          >
            <a-select-option value="">{{ $t('campaign.table.filter.allStatus') }}</a-select-option>
            <a-select-option value="ACTIVE">{{ $t('campaign.status.active') }}</a-select-option>
            <a-select-option value="PAUSED">{{ $t('campaign.status.paused') }}</a-select-option>
            <a-select-option value="DRAFT">{{ $t('campaign.status.draft') }}</a-select-option>
            <a-select-option value="COMPLETED">{{ $t('campaign.status.completed') }}</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="objectiveFilter"
            :placeholder="$t('campaign.table.placeholder.objective')"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by objective"
            @change="handleFilterChange"
          >
            <a-select-option value="">{{ $t('campaign.table.filter.allObjectives') }}</a-select-option>
            <a-select-option value="BRAND_AWARENESS">{{ $t('campaign.objective.brandAwareness') }}</a-select-option>
            <a-select-option value="REACH">{{ $t('campaign.objective.reach') }}</a-select-option>
            <a-select-option value="TRAFFIC">{{ $t('campaign.objective.traffic') }}</a-select-option>
            <a-select-option value="ENGAGEMENT">{{ $t('campaign.objective.engagement') }}</a-select-option>
            <a-select-option value="APP_INSTALLS">{{ $t('campaign.objective.appInstalls') }}</a-select-option>
            <a-select-option value="VIDEO_VIEWS">{{ $t('campaign.objective.videoViews') }}</a-select-option>
            <a-select-option value="LEAD_GENERATION">{{ $t('campaign.objective.leadGeneration') }}</a-select-option>
            <a-select-option value="CONVERSIONS">{{ $t('campaign.objective.conversions') }}</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-range-picker
            v-model:value="dateRange"
            size="large"
            style="width: 100%"
            aria-label="Filter by date range"
            @change="handleDateRangeChange"
          />
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="3">
          <a-button
            size="large"
            style="width: 100%"
            aria-label="Reset all filters"
            @click="resetFilters"
          >
            <template #icon>
              <ClearOutlined aria-hidden="true" />
            </template>
            {{ $t('campaign.table.action.reset') }}
          </a-button>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="3">
          <a-button
            type="primary"
            size="large"
            style="width: 100%"
            :aria-label="viewMode === 'table' ? 'Switch to cards view' : 'Switch to table view'"
            @click="toggleView"
          >
            <template #icon>
              <component :is="viewMode === 'table' ? 'AppstoreOutlined' : 'TableOutlined'" aria-hidden="true" />
            </template>
            {{ viewMode === 'table' ? $t('campaign.table.view.cards') : $t('campaign.table.view.table') }}
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- Results Summary -->
    <div id="results-summary" class="results-summary" role="status" aria-live="polite">
      <a-typography-text type="secondary">
        {{ $t('campaign.table.results.showing', { filtered: filteredCampaigns.length, total: totalCampaigns }) }}
        <span v-if="hasActiveFilters"> {{ $t('campaign.table.results.filtered') }}</span>
      </a-typography-text>
    </div>

    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="table-section">
      <a-table
        :columns="columns"
        :data-source="paginatedCampaigns"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1200 }"
        row-key="id"
        size="middle"
        role="table"
        aria-label="Campaigns table"
        aria-describedby="results-summary"
        @change="handleTableChange"
      >
        <!-- Custom column renderers -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="campaign-name-cell">
              <a-typography-title :level="5" style="margin: 0; cursor: pointer" @click="$emit('view-details', record)">
                {{ record.name }}
              </a-typography-title>
              <a-typography-text type="secondary" style="font-size: 12px">
                ID: {{ record.id }}
              </a-typography-text>
            </div>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              <component :is="getStatusIcon(record.status)" style="margin-right: 4px;" />
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'objective'">
            <a-tag color="blue" v-if="record.objective">
              {{ formatObjective(record.objective) }}
            </a-tag>
            <span v-else class="text-gray-400">-</span>
          </template>
          
          <template v-else-if="column.key === 'budget'">
            <div class="budget-cell">
              <div v-if="record.dailyBudget">
                <strong>${{ formatNumber(record.dailyBudget) }}</strong> {{ $t('campaign.table.budget.perDay') }}
              </div>
              <div v-else-if="record.totalBudget">
                <strong>${{ formatNumber(record.totalBudget) }}</strong> {{ $t('campaign.table.budget.total') }}
              </div>
              <span v-else class="text-gray-400">{{ $t('campaign.table.budget.notSet') }}</span>
            </div>
          </template>
          
          <template v-else-if="column.key === 'adsCount'">
            <a-badge :count="record.totalAds || 0" :number-style="{ backgroundColor: '#52c41a' }" />
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
              <a-tooltip :title="$t('campaign.table.action.viewDetails')">
                <a-button
                  size="small"
                  aria-label="View details for {{ record.name }}"
                  @click="$emit('view-details', record)"
                >
                  <template #icon>
                    <EyeOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="record.status === 'EXPORTED' ? $t('campaign.table.action.cannotEdit') : $t('campaign.table.action.edit')">
                <a-button
                  size="small"
                  :disabled="record.status === 'EXPORTED'"
                  aria-label="Edit {{ record.name }}"
                  @click="$emit('edit-campaign', record)"
                >
                  <template #icon>
                    <EditOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-tooltip :title="$t('campaign.table.action.viewAds')">
                <a-button
                  size="small"
                  aria-label="View ads for {{ record.name }}"
                  @click="$emit('view-ads', record)"
                >
                  <template #icon>
                    <AppstoreOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>

              <a-popconfirm
                :title="$t('campaign.table.action.confirmDelete')"
                :ok-text="$t('common.action.yes')"
                :cancel-text="$t('common.action.no')"
                @confirm="$emit('delete-campaign', record.id)"
              >
                <a-tooltip :title="$t('campaign.table.action.delete')">
                  <a-button size="small" danger>
                    <template #icon>
                      <DeleteOutlined />
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
        <a-col v-for="campaign in paginatedCampaigns" :key="campaign.id" :xs="24" :sm="12" :md="8" :lg="6">
          <a-card
            hoverable
            class="campaign-card"
            :body-style="{ padding: '16px' }"
          >
            <div class="campaign-header">
              <a-typography-title :level="5" :ellipsis="{ rows: 2 }" style="margin-bottom: 8px;">
                {{ campaign.name }}
              </a-typography-title>
              <a-typography-text type="secondary" style="font-size: 12px;">
                ID: {{ campaign.id }}
              </a-typography-text>
            </div>
            
            <div style="margin: 12px 0;">
              <a-tag :color="getStatusColor(campaign.status)">
                <component :is="getStatusIcon(campaign.status)" style="margin-right: 4px;" />
                {{ getStatusLabel(campaign.status) }}
              </a-tag>
              <a-tag color="blue" v-if="campaign.objective" style="margin-left: 4px;">
                {{ formatObjective(campaign.objective) }}
              </a-tag>
            </div>
            
            <a-row :gutter="[12, 12]" style="margin-bottom: 16px;">
              <a-col :span="12">
                <a-typography-text type="secondary" style="font-size: 12px; font-weight: 500;">
                  {{ $t('campaign.table.label.budget') }}
                </a-typography-text>
                <div style="font-size: 14px; font-weight: 600;">
                  <div v-if="campaign.dailyBudget">
                    ${{ formatNumber(campaign.dailyBudget) }} {{ $t('campaign.table.budget.perDay') }}
                  </div>
                  <div v-else-if="campaign.totalBudget">
                    ${{ formatNumber(campaign.totalBudget) }} {{ $t('campaign.table.budget.total') }}
                  </div>
                  <span v-else class="text-gray-400">{{ $t('campaign.table.budget.notSet') }}</span>
                </div>
              </a-col>
              <a-col :span="12">
                <a-typography-text type="secondary" style="font-size: 12px; font-weight: 500;">
                  {{ $t('campaign.table.label.totalAds') }}
                </a-typography-text>
                <div style="font-size: 14px; font-weight: 600;">
                  {{ campaign.totalAds || 0 }}
                </div>
              </a-col>
            </a-row>
            
            <a-divider style="margin: 12px 0;" />
            
            <a-row justify="space-between" align="middle">
              <a-col>
                <a-space size="small">
                  <a-button
                    size="small"
                    :disabled="campaign.status === 'EXPORTED'"
                    @click="$emit('edit-campaign', campaign)"
                  >
                    <template #icon>
                      <EditOutlined />
                    </template>
                    {{ $t('common.action.edit') }}
                  </a-button>
                  <a-popconfirm
                    :title="$t('campaign.table.action.confirmDelete')"
                    :ok-text="$t('common.action.yes')"
                    :cancel-text="$t('common.action.no')"
                    @confirm="$emit('delete-campaign', campaign.id)"
                  >
                    <a-button size="small" danger>
                      <template #icon>
                        <DeleteOutlined />
                      </template>
                      {{ $t('common.action.delete') }}
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </a-col>
              <a-col>
                <a-button type="primary" size="small" @click="$emit('view-details', campaign)">
                  {{ $t('campaign.table.action.viewDetails') }}
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
        :total="totalCampaigns"
        :page-size-options="['10', '20', '50', '100']"
        show-size-changer
        show-quick-jumper
        show-total
        @change="handlePageChange"
        @showSizeChange="handlePageSizeChange"
      >
        <template #buildOptionText="props">
          <span>{{ props.value }} / page</span>
        </template>
      </a-pagination>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  SearchOutlined,
  ClearOutlined,
  TableOutlined,
  AppstoreOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  FileOutlined,
  CheckCircleOutlined,
  ExportOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)

export default {
  name: 'CampaignTable',
  components: {
    SearchOutlined,
    ClearOutlined,
    TableOutlined,
    AppstoreOutlined,
    EyeOutlined,
    EditOutlined,
    DeleteOutlined,
    FileOutlined,
    CheckCircleOutlined,
    ExportOutlined,
    PlayCircleOutlined,
    PauseCircleOutlined,
    CheckOutlined,
    CloseCircleOutlined,
    ClockCircleOutlined,
    QuestionCircleOutlined
  },
  props: {
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
      default: 20
    }
  },
  emits: ['view-details', 'edit-campaign', 'delete-campaign', 'view-ads', 'page-change', 'page-size-change'],

  setup(props, { emit }) {
    const { t } = useI18n()
    // Reactive data
    const searchQuery = ref('')
    const statusFilter = ref('')
    const objectiveFilter = ref('')
    const dateRange = ref([])
    const viewMode = ref('table')
    const sortField = ref('createdDate')
    const sortOrder = ref('desc')
    
    // Table columns configuration
    const columns = computed(() => [
      {
        title: t('campaign.table.column.name'),
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 250,
        fixed: 'left'
      },
      {
        title: t('campaign.table.column.status'),
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 120,
        filters: [
          { text: t('campaign.status.active'), value: 'ACTIVE' },
          { text: t('campaign.status.paused'), value: 'PAUSED' },
          { text: t('campaign.status.draft'), value: 'DRAFT' },
          { text: t('campaign.status.completed'), value: 'COMPLETED' }
        ]
      },
      {
        title: t('campaign.table.column.objective'),
        key: 'objective',
        dataIndex: 'objective',
        sorter: true,
        width: 150
      },
      {
        title: t('campaign.table.column.budget'),
        key: 'budget',
        sorter: true,
        width: 150
      },
      {
        title: t('campaign.table.column.ads'),
        key: 'adsCount',
        dataIndex: 'totalAds',
        sorter: true,
        width: 80,
        align: 'center'
      },
      {
        title: t('campaign.table.column.createdDate'),
        key: 'createdDate',
        dataIndex: 'createdDate',
        sorter: true,
        width: 150
      },
      {
        title: t('campaign.table.column.actions'),
        key: 'actions',
        width: 200,
        fixed: 'right'
      }
    ])
    
    // Computed properties
    const totalCampaigns = computed(() => props.totalItems || props.campaigns.length)

    const filteredCampaigns = computed(() => {
      let filtered = [...props.campaigns]

      // Search filter (frontend only for displayed page)
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(campaign =>
          campaign.name?.toLowerCase().includes(query) ||
          campaign.id?.toString().includes(query)
        )
      }

      // Status filter (frontend only for displayed page)
      if (statusFilter.value) {
        filtered = filtered.filter(campaign => campaign.status === statusFilter.value)
      }

      // Objective filter (frontend only for displayed page)
      if (objectiveFilter.value) {
        filtered = filtered.filter(campaign => campaign.objective === objectiveFilter.value)
      }

      // Date range filter (frontend only for displayed page)
      if (dateRange.value && dateRange.value.length === 2) {
        const [startDate, endDate] = dateRange.value
        filtered = filtered.filter(campaign => {
          const campaignDate = dayjs(campaign.createdDate)
          return campaignDate.isAfter(startDate) && campaignDate.isBefore(endDate.add(1, 'day'))
        })
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

    const totalPages = computed(() => Math.ceil(totalCampaigns.value / props.pageSize))

    const paginatedCampaigns = computed(() => {
      // When using server-side pagination, just return the filtered campaigns as they are already paginated
      return filteredCampaigns.value
    })
    
    const hasActiveFilters = computed(() => {
      return searchQuery.value || statusFilter.value || objectiveFilter.value || 
             (dateRange.value && dateRange.value.length === 2)
    })
    
    // Methods
    const handleSearch = () => {
      currentPage.value = 1
    }
    
    const handleFilterChange = () => {
      currentPage.value = 1
    }
    
    const handleDateRangeChange = () => {
      currentPage.value = 1
    }
    
    const resetFilters = () => {
      searchQuery.value = ''
      statusFilter.value = ''
      objectiveFilter.value = ''
      dateRange.value = []
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

    const handlePageSizeChange = (_current, size) => {
      emit('page-size-change', 1, size)
    }
    
    // Utility functions
    const getStatusColor = (status) => {
      const colors = {
        'DRAFT': 'default',        // Gray - chưa có ads
        'READY': 'cyan',           // Cyan - có ads, sẵn sàng export
        'EXPORTED': 'green',       // Green - đã export
        'ACTIVE': 'blue',          // Blue - đang chạy
        'PAUSED': 'orange',        // Orange - tạm dừng
        'COMPLETED': 'purple',     // Purple - hoàn thành
        'FAILED': 'red',           // Red - thất bại
        'PENDING': 'gold'          // Gold - đang chờ
      }
      return colors[status] || 'default'
    }

    const getStatusIcon = (status) => {
      const icons = {
        'DRAFT': 'FileOutlined',
        'READY': 'CheckCircleOutlined',
        'EXPORTED': 'ExportOutlined',
        'ACTIVE': 'PlayCircleOutlined',
        'PAUSED': 'PauseCircleOutlined',
        'COMPLETED': 'CheckOutlined',
        'FAILED': 'CloseCircleOutlined',
        'PENDING': 'ClockCircleOutlined'
      }
      return icons[status] || 'QuestionCircleOutlined'
    }

    const getStatusLabel = (status) => {
      const labels = {
        'DRAFT': t('campaign.status.draft'),
        'READY': t('campaign.status.ready'),
        'EXPORTED': t('campaign.status.exported'),
        'ACTIVE': t('campaign.status.active'),
        'PAUSED': t('campaign.status.paused'),
        'COMPLETED': t('campaign.status.completed'),
        'FAILED': t('campaign.status.failed'),
        'PENDING': t('campaign.status.pending')
      }
      return labels[status] || status || t('campaign.status.unknown')
    }
    
    const formatObjective = (objective) => {
      if (!objective) return ''
      const objectiveMap = {
        'BRAND_AWARENESS': t('campaign.objective.brandAwareness'),
        'REACH': t('campaign.objective.reach'),
        'TRAFFIC': t('campaign.objective.traffic'),
        'ENGAGEMENT': t('campaign.objective.engagement'),
        'APP_INSTALLS': t('campaign.objective.appInstalls'),
        'VIDEO_VIEWS': t('campaign.objective.videoViews'),
        'LEAD_GENERATION': t('campaign.objective.leadGeneration'),
        'CONVERSIONS': t('campaign.objective.conversions')
      }
      return objectiveMap[objective] || objective.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const formatNumber = (num) => {
      return new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }).format(num)
    }
    
    const formatDate = (date) => {
      if (!date) return t('campaign.table.date.notAvailable')

      const dayjsDate = dayjs(date)
      if (!dayjsDate.isValid()) {
        console.warn('Invalid date:', date)
        return t('campaign.table.date.invalid')
      }

      return dayjsDate.format('MMM DD, YYYY')
    }

    const formatRelativeTime = (date) => {
      if (!date) return ''

      const dayjsDate = dayjs(date)
      if (!dayjsDate.isValid()) return ''

      return dayjsDate.fromNow()
    }
    
    return {
      searchQuery,
      statusFilter,
      objectiveFilter,
      dateRange,
      viewMode,
      sortField,
      sortOrder,
      columns,
      totalCampaigns,
      filteredCampaigns,
      totalPages,
      paginatedCampaigns,
      hasActiveFilters,
      handleSearch,
      handleFilterChange,
      handleDateRangeChange,
      resetFilters,
      toggleView,
      handleTableChange,
      handlePageChange,
      handlePageSizeChange,
      getStatusColor,
      getStatusIcon,
      getStatusLabel,
      formatObjective,
      formatNumber,
      formatDate,
      formatRelativeTime
    }
  }
}
</script>

<style scoped>
.campaign-table-container {
  @apply space-y-6;
}

.filters-section {
  @apply bg-white dark:bg-gray-800 p-6 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700;
}

.results-summary {
  @apply flex justify-between items-center;
}

.table-section {
  @apply bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden;
}

.cards-section {
  @apply min-h-[400px];
}

.campaign-card {
  @apply transition-all duration-200 hover:shadow-lg border border-gray-200 dark:border-gray-700;
}

.campaign-card:hover {
  @apply transform -translate-y-1;
}

.campaign-header {
  @apply mb-3;
}

.campaign-name-cell {
  @apply space-y-1;
}

.budget-cell {
  @apply space-y-1;
}

.date-cell {
  @apply space-y-1;
}

.pagination-section {
  @apply flex justify-center bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700;
}

/* Dark mode styles */
.dark .campaign-table-container {
  @apply text-white;
}

.dark .filters-section {
  @apply bg-gray-800 border-gray-700;
}

.dark .table-section {
  @apply bg-gray-800 border-gray-700;
}

.dark .campaign-card {
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
  
  .campaign-card {
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
:deep(.ant-select:focus-visible),
:deep(.ant-picker:focus-visible) {
  @apply outline-2 outline-blue-500 outline-offset-2;
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .campaign-card {
    @apply border-2 border-black;
  }
  
  :deep(.ant-table-thead > tr > th) {
    @apply border-2 border-black;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .campaign-card {
    @apply transition-none;
  }
  
  .campaign-card:hover {
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