<template>
  <div class="campaign-table-container">
    <!-- Advanced Filters -->
    <div class="filters-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="12" :md="6">
          <a-input
            v-model:value="searchQuery"
            placeholder="Search campaigns..."
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
            placeholder="Status"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by status"
            @change="handleFilterChange"
          >
            <a-select-option value="">All Status</a-select-option>
            <a-select-option value="ACTIVE">Active</a-select-option>
            <a-select-option value="PAUSED">Paused</a-select-option>
            <a-select-option value="DRAFT">Draft</a-select-option>
            <a-select-option value="COMPLETED">Completed</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="objectiveFilter"
            placeholder="Objective"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by objective"
            @change="handleFilterChange"
          >
            <a-select-option value="">All Objectives</a-select-option>
            <a-select-option value="BRAND_AWARENESS">Brand Awareness</a-select-option>
            <a-select-option value="REACH">Reach</a-select-option>
            <a-select-option value="TRAFFIC">Traffic</a-select-option>
            <a-select-option value="ENGAGEMENT">Engagement</a-select-option>
            <a-select-option value="APP_INSTALLS">App Installs</a-select-option>
            <a-select-option value="VIDEO_VIEWS">Video Views</a-select-option>
            <a-select-option value="LEAD_GENERATION">Lead Generation</a-select-option>
            <a-select-option value="CONVERSIONS">Conversions</a-select-option>
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
            Reset
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
            {{ viewMode === 'table' ? 'Cards' : 'Table' }}
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- Results Summary -->
    <div id="results-summary" class="results-summary" role="status" aria-live="polite">
      <a-typography-text type="secondary">
        Showing {{ filteredCampaigns.length }} of {{ totalCampaigns }} campaigns
        <span v-if="hasActiveFilters"> (filtered)</span>
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
              {{ record.status || 'Unknown' }}
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
                <strong>${{ formatNumber(record.dailyBudget) }}</strong>/day
              </div>
              <div v-else-if="record.totalBudget">
                <strong>${{ formatNumber(record.totalBudget) }}</strong> total
              </div>
              <span v-else class="text-gray-400">Not set</span>
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
              <a-tooltip title="View Details">
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
              
              <a-tooltip title="Edit Campaign">
                <a-button 
                  size="small" 
                  aria-label="Edit {{ record.name }}"
                  @click="$emit('edit-campaign', record)"
                >
                  <template #icon>
                    <EditOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>
              
              <a-tooltip title="View Ads">
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
                title="Are you sure you want to delete this campaign?"
                ok-text="Yes"
                cancel-text="No"
                @confirm="$emit('delete-campaign', record.id)"
              >
                <a-tooltip title="Delete Campaign">
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
                {{ campaign.status || 'Unknown' }}
              </a-tag>
              <a-tag color="blue" v-if="campaign.objective" style="margin-left: 4px;">
                {{ formatObjective(campaign.objective) }}
              </a-tag>
            </div>
            
            <a-row :gutter="[12, 12]" style="margin-bottom: 16px;">
              <a-col :span="12">
                <a-typography-text type="secondary" style="font-size: 12px; font-weight: 500;">
                  Budget
                </a-typography-text>
                <div style="font-size: 14px; font-weight: 600;">
                  <div v-if="campaign.dailyBudget">
                    ${{ formatNumber(campaign.dailyBudget) }}/day
                  </div>
                  <div v-else-if="campaign.totalBudget">
                    ${{ formatNumber(campaign.totalBudget) }} total
                  </div>
                  <span v-else class="text-gray-400">Not set</span>
                </div>
              </a-col>
              <a-col :span="12">
                <a-typography-text type="secondary" style="font-size: 12px; font-weight: 500;">
                  Total Ads
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
                  <a-button size="small" @click="$emit('edit-campaign', campaign)">
                    <template #icon>
                      <EditOutlined />
                    </template>
                    Edit
                  </a-button>
                  <a-popconfirm
                    title="Are you sure you want to delete this campaign?"
                    ok-text="Yes"
                    cancel-text="No"
                    @confirm="$emit('delete-campaign', campaign.id)"
                  >
                    <a-button size="small" danger>
                      <template #icon>
                        <DeleteOutlined />
                      </template>
                      Delete
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </a-col>
              <a-col>
                <a-button type="primary" size="small" @click="$emit('view-details', campaign)">
                  View Details
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
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="filteredCampaigns.length"
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
import { ref, computed, watch } from 'vue'
import {
  SearchOutlined,
  ClearOutlined,
  TableOutlined,
  AppstoreOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined
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
    DeleteOutlined
  },
  props: {
    campaigns: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['view-details', 'edit-campaign', 'delete-campaign', 'view-ads'],
  
  setup(props) {
    // Reactive data
    const searchQuery = ref('')
    const statusFilter = ref('')
    const objectiveFilter = ref('')
    const dateRange = ref([])
    const viewMode = ref('table')
    const currentPage = ref(1)
    const pageSize = ref(20)
    const sortField = ref('createdDate')
    const sortOrder = ref('desc')
    
    // Table columns configuration
    const columns = ref([
      {
        title: 'Campaign Name',
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 250,
        fixed: 'left'
      },
      {
        title: 'Status',
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 120,
        filters: [
          { text: 'Active', value: 'ACTIVE' },
          { text: 'Paused', value: 'PAUSED' },
          { text: 'Draft', value: 'DRAFT' },
          { text: 'Completed', value: 'COMPLETED' }
        ]
      },
      {
        title: 'Objective',
        key: 'objective',
        dataIndex: 'objective',
        sorter: true,
        width: 150
      },
      {
        title: 'Budget',
        key: 'budget',
        sorter: true,
        width: 150
      },
      {
        title: 'Ads',
        key: 'adsCount',
        dataIndex: 'totalAds',
        sorter: true,
        width: 80,
        align: 'center'
      },
      {
        title: 'Created Date',
        key: 'createdDate',
        dataIndex: 'createdDate',
        sorter: true,
        width: 150
      },
      {
        title: 'Actions',
        key: 'actions',
        width: 200,
        fixed: 'right'
      }
    ])
    
    // Computed properties
    const totalCampaigns = computed(() => props.campaigns.length)
    
    const filteredCampaigns = computed(() => {
      let filtered = [...props.campaigns]
      
      // Search filter
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(campaign =>
          campaign.name?.toLowerCase().includes(query) ||
          campaign.id?.toString().includes(query)
        )
      }
      
      // Status filter
      if (statusFilter.value) {
        filtered = filtered.filter(campaign => campaign.status === statusFilter.value)
      }
      
      // Objective filter
      if (objectiveFilter.value) {
        filtered = filtered.filter(campaign => campaign.objective === objectiveFilter.value)
      }
      
      // Date range filter
      if (dateRange.value && dateRange.value.length === 2) {
        const [startDate, endDate] = dateRange.value
        filtered = filtered.filter(campaign => {
          const campaignDate = dayjs(campaign.createdDate)
          return campaignDate.isAfter(startDate) && campaignDate.isBefore(endDate.add(1, 'day'))
        })
      }
      
      // Sorting
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
    
    const totalPages = computed(() => Math.ceil(filteredCampaigns.value.length / pageSize.value))
    
    const paginatedCampaigns = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredCampaigns.value.slice(start, end)
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
      currentPage.value = page
      pageSize.value = size
    }
    
    const handlePageSizeChange = (current, size) => {
      currentPage.value = 1
      pageSize.value = size
    }
    
    // Utility functions
    const getStatusColor = (status) => {
      const colors = {
        'ACTIVE': 'green',
        'PAUSED': 'orange',
        'DRAFT': 'blue',
        'COMPLETED': 'purple',
        'DELETED': 'red'
      }
      return colors[status] || 'default'
    }
    
    const formatObjective = (objective) => {
      return objective?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const formatNumber = (num) => {
      return new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }).format(num)
    }
    
    const formatDate = (date) => {
      return dayjs(date).format('MMM DD, YYYY')
    }
    
    const formatRelativeTime = (date) => {
      return dayjs(date).fromNow()
    }
    
    // Watchers
    watch([searchQuery, statusFilter, objectiveFilter, dateRange], () => {
      currentPage.value = 1
    })
    
    return {
      searchQuery,
      statusFilter,
      objectiveFilter,
      dateRange,
      viewMode,
      currentPage,
      pageSize,
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