<template>
  <div class="ad-table-container">
    <!-- Advanced Filters -->
    <div class="filters-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="12" :md="6">
          <a-input
            v-model:value="searchQuery"
            placeholder="Search ads..."
            size="large"
            allow-clear
            aria-label="Search ads"
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
            <a-select-option value="ARCHIVED">Archived</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="adTypeFilter"
            placeholder="Ad Type"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by ad type"
            @change="handleFilterChange"
          >
            <a-select-option value="">All Types</a-select-option>
            <a-select-option value="IMAGE">Image</a-select-option>
            <a-select-option value="VIDEO">Video</a-select-option>
            <a-select-option value="CAROUSEL">Carousel</a-select-option>
            <a-select-option value="COLLECTION">Collection</a-select-option>
          </a-select>
        </a-col>
        
        <a-col :xs="24" :sm="12" :md="4">
          <a-select
            v-model:value="campaignFilter"
            placeholder="Campaign"
            size="large"
            allow-clear
            style="width: 100%"
            aria-label="Filter by campaign"
            @change="handleFilterChange"
          >
            <a-select-option value="">All Campaigns</a-select-option>
            <a-select-option v-for="campaign in campaigns" :key="campaign.id" :value="campaign.id">
              {{ campaign.name }}
            </a-select-option>
          </a-select>
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

    <!-- Results Summary & Bulk Actions -->
    <div class="results-actions-bar">
      <div id="results-summary" class="results-summary" role="status" aria-live="polite">
        <a-typography-text type="secondary">
          Showing {{ filteredAds.length }} of {{ totalAds }} ads
          <span v-if="hasActiveFilters"> (filtered)</span>
        </a-typography-text>
      </div>

      <!-- Bulk Actions -->
      <div v-if="selectedRowKeys.length > 0" class="bulk-actions">
        <a-space>
          <a-typography-text strong>
            {{ selectedRowKeys.length }} ad(s) selected
          </a-typography-text>
          <a-button size="small" @click="clearSelection">
            Clear Selection
          </a-button>
          <a-button
            type="primary"
            size="small"
            @click="handleBulkExport"
          >
            <template #icon>
              <ExportOutlined />
            </template>
            Export Selected
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="table-section">
      <a-table
        :columns="columns"
        :data-source="paginatedAds"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1400 }"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
        role="table"
        aria-label="Ads table"
        aria-describedby="results-summary"
        @change="handleTableChange"
      >
        <!-- Custom column renderers -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="ad-name-cell">
              <a-typography-title :level="5" style="margin: 0; cursor: pointer" @click="$emit('view-details', record)">
                {{ record.name || 'Untitled Ad' }}
              </a-typography-title>
              <a-typography-text type="secondary" style="font-size: 12px">
                ID: {{ record.id }}
              </a-typography-text>
            </div>
          </template>
          
          <template v-else-if="column.key === 'campaign'">
            <div class="campaign-cell">
              <a-typography-text strong>{{ getCampaignName(record.campaignId) }}</a-typography-text>
              <br>
              <a-typography-text type="secondary" style="font-size: 11px">
                Campaign ID: {{ record.campaignId }}
              </a-typography-text>
            </div>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.status || 'Unknown' }}
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
                <strong>Headline:</strong> {{ truncateText(record.headline, 30) }}
              </div>
              <div v-if="record.primaryText" class="content-item">
                <strong>Text:</strong> {{ truncateText(record.primaryText, 40) }}
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
              <a-tooltip title="View Details">
                <a-button 
                  size="small" 
                  aria-label="View details for {{ record.name || 'Untitled Ad' }}"
                  @click="$emit('view-details', record)"
                >
                  <template #icon>
                    <EyeOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>
              
              <a-tooltip title="Edit Ad">
                <a-button 
                  size="small" 
                  aria-label="Edit {{ record.name || 'Untitled Ad' }}"
                  @click="$emit('edit-ad', record)"
                >
                  <template #icon>
                    <EditOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>
              
              <a-tooltip title="Duplicate Ad">
                <a-button 
                  size="small" 
                  aria-label="Duplicate {{ record.name || 'Untitled Ad' }}"
                  @click="$emit('duplicate-ad', record)"
                >
                  <template #icon>
                    <CopyOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>
              
              <a-tooltip title="Export to Facebook">
                <a-button 
                  size="small" 
                  type="primary" 
                  aria-label="Export {{ record.name || 'Untitled Ad' }} to Facebook"
                  @click="$emit('export-ad', record)"
                >
                  <template #icon>
                    <ExportOutlined aria-hidden="true" />
                  </template>
                </a-button>
              </a-tooltip>
              
              <a-popconfirm
                title="Are you sure you want to delete this ad?"
                ok-text="Yes"
                cancel-text="No"
                @confirm="$emit('delete-ad', record.id)"
              >
                <a-tooltip title="Delete Ad">
                  <a-button 
                    size="small" 
                    danger
                    aria-label="Delete {{ record.name || 'Untitled Ad' }}"
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
                <p>No Media</p>
              </div>
            </div>
            
            <!-- Ad Info -->
            <div class="ad-info">
              <a-typography-title :level="5" :ellipsis="{ rows: 2 }" style="margin-bottom: 8px;">
                {{ ad.name || 'Untitled Ad' }}
              </a-typography-title>
              
              <div class="ad-meta">
                <a-tag :color="getStatusColor(ad.status)" size="small">
                  {{ ad.status || 'Unknown' }}
                </a-tag>
                <a-tag color="blue" size="small" v-if="ad.adType">
                  {{ formatAdType(ad.adType) }}
                </a-tag>
              </div>
              
              <div class="campaign-info">
                <a-typography-text type="secondary" style="font-size: 12px;">
                  Campaign: {{ getCampaignName(ad.campaignId) }}
                </a-typography-text>
              </div>
              
              <div class="ad-content" v-if="ad.headline || ad.primaryText">
                <div v-if="ad.headline" class="content-line">
                  <strong>Headline:</strong> {{ truncateText(ad.headline, 40) }}
                </div>
                <div v-if="ad.primaryText" class="content-line">
                  <strong>Text:</strong> {{ truncateText(ad.primaryText, 60) }}
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
                    Edit
                  </a-button>
                  <a-popconfirm
                    title="Are you sure you want to delete this ad?"
                    ok-text="Yes"
                    cancel-text="No"
                    @confirm="$emit('delete-ad', ad.id)"
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
                <a-button type="primary" size="small" @click="$emit('view-details', ad)">
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
        :total="filteredAds.length"
        :page-size-options="['12', '24', '48', '96']"
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
  DeleteOutlined,
  CopyOutlined,
  ExportOutlined,
  FileImageOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'

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
    }
  },
  emits: ['view-details', 'edit-ad', 'delete-ad', 'duplicate-ad', 'export-ad'],

  setup(props, { emit }) {
    // Reactive data
    const searchQuery = ref('')
    const statusFilter = ref('')
    const adTypeFilter = ref('')
    const campaignFilter = ref('')
    const viewMode = ref('table')
    const currentPage = ref(1)
    const pageSize = ref(24)
    const sortField = ref('createdDate')
    const sortOrder = ref('desc')
    const selectedRowKeys = ref([])
    
    // Table columns configuration
    const columns = ref([
      {
        title: 'Ad Name',
        key: 'name',
        dataIndex: 'name',
        sorter: true,
        width: 200,
        fixed: 'left'
      },
      {
        title: 'Campaign',
        key: 'campaign',
        sorter: true,
        width: 180
      },
      {
        title: 'Status',
        key: 'status',
        dataIndex: 'status',
        sorter: true,
        width: 100,
        filters: [
          { text: 'Active', value: 'ACTIVE' },
          { text: 'Paused', value: 'PAUSED' },
          { text: 'Draft', value: 'DRAFT' },
          { text: 'Archived', value: 'ARCHIVED' }
        ]
      },
      {
        title: 'Type',
        key: 'adType',
        dataIndex: 'adType',
        sorter: true,
        width: 100
      },
      {
        title: 'Content',
        key: 'content',
        width: 300
      },
      {
        title: 'Media',
        key: 'media',
        width: 100,
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
        width: 250,
        fixed: 'right'
      }
    ])
    
    // Computed properties
    const totalAds = computed(() => props.ads.length)
    
    const filteredAds = computed(() => {
      let filtered = [...props.ads]
      
      // Search filter
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(ad =>
          ad.name?.toLowerCase().includes(query) ||
          ad.headline?.toLowerCase().includes(query) ||
          ad.primaryText?.toLowerCase().includes(query) ||
          ad.id?.toString().includes(query)
        )
      }
      
      // Status filter
      if (statusFilter.value) {
        filtered = filtered.filter(ad => ad.status === statusFilter.value)
      }
      
      // Ad type filter
      if (adTypeFilter.value) {
        filtered = filtered.filter(ad => ad.adType === adTypeFilter.value)
      }
      
      // Campaign filter
      if (campaignFilter.value) {
        filtered = filtered.filter(ad => ad.campaignId?.toString() === campaignFilter.value.toString())
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
    
    const totalPages = computed(() => Math.ceil(filteredAds.value.length / pageSize.value))
    
    const paginatedAds = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return filteredAds.value.slice(start, end)
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
        'ARCHIVED': 'purple',
        'DELETED': 'red'
      }
      return colors[status] || 'default'
    }
    
    const formatAdType = (adType) => {
      return adType?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const getCampaignName = (campaignId) => {
      if (!campaignId) return 'No Campaign'

      // Check if campaigns are loaded
      if (!props.campaigns || props.campaigns.length === 0) {
        return 'Loading...'
      }

      // Convert to string for comparison (handle type mismatch)
      const campaign = props.campaigns.find(c => c.id?.toString() === campaignId?.toString())
      return campaign?.name || `Campaign #${campaignId}`
    }
    
    const truncateText = (text, maxLength) => {
      if (!text) return ''
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
    }
    
    const formatDate = (date) => {
      return dayjs(date).format('MMM DD, YYYY')
    }
    
    const formatRelativeTime = (date) => {
      return dayjs(date).fromNow()
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

    // Watchers
    watch([searchQuery, statusFilter, adTypeFilter, campaignFilter], () => {
      currentPage.value = 1
    })

    return {
      searchQuery,
      statusFilter,
      adTypeFilter,
      campaignFilter,
      viewMode,
      currentPage,
      pageSize,
      sortField,
      sortOrder,
      selectedRowKeys,
      rowSelection,
      columns,
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