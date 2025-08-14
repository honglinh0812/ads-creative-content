<template>
  <div class="campaign-analytics-table">
    <!-- Table Controls -->
    <div class="table-controls">
      <div class="search-box">
        <i class="pi pi-search"></i>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search campaigns..."
          class="search-input"
        />
      </div>
      
      <div class="filter-controls">
        <select v-model="statusFilter" class="filter-select">
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="PAUSED">Paused</option>
          <option value="DRAFT">Draft</option>
        </select>
        
        <select v-model="sortBy" class="filter-select">
          <option value="campaignName">Name</option>
          <option value="impressions">Impressions</option>
          <option value="clicks">Clicks</option>
          <option value="ctr">CTR</option>
          <option value="spent">Spent</option>
          <option value="roi">ROI</option>
        </select>
        
        <button @click="toggleSortOrder" class="sort-btn">
          <i :class="sortOrder === 'asc' ? 'pi pi-sort-up' : 'pi pi-sort-down'"></i>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="table-loading">
      <div class="loading-spinner"></div>
      <p>Loading campaign data...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="filteredCampaigns.length === 0" class="table-empty">
      <i class="pi pi-briefcase"></i>
      <h3>No Campaigns Found</h3>
      <p v-if="searchQuery">No campaigns match your search criteria.</p>
      <p v-else>No campaign data available for the selected period.</p>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="analytics-table">
        <thead>
          <tr>
            <th class="sortable" @click="setSortBy('campaignName')">
              Campaign Name
              <i v-if="sortBy === 'campaignName'" :class="sortIcon"></i>
            </th>
            <th class="sortable" @click="setSortBy('status')">
              Status
              <i v-if="sortBy === 'status'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('budget')">
              Budget
              <i v-if="sortBy === 'budget'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('spent')">
              Spent
              <i v-if="sortBy === 'spent'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('impressions')">
              Impressions
              <i v-if="sortBy === 'impressions'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('clicks')">
              Clicks
              <i v-if="sortBy === 'clicks'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('ctr')">
              CTR
              <i v-if="sortBy === 'ctr'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('cpc')">
              CPC
              <i v-if="sortBy === 'cpc'" :class="sortIcon"></i>
            </th>
            <th class="sortable numeric" @click="setSortBy('roi')">
              ROI
              <i v-if="sortBy === 'roi'" :class="sortIcon"></i>
            </th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="campaign in paginatedCampaigns"
            :key="campaign.campaignId"
            class="table-row"
            @click="$emit('campaign-click', campaign)"
          >
            <td class="campaign-name">
              <div class="campaign-info">
                <span class="name">{{ campaign.campaignName }}</span>
                <span class="objective">{{ campaign.objective }}</span>
              </div>
            </td>
            <td>
              <span :class="getStatusClass(campaign.status)">
                {{ campaign.status }}
              </span>
            </td>
            <td class="numeric">{{ formatCurrency(campaign.budget) }}</td>
            <td class="numeric">
              <div class="spent-info">
                <span class="amount">{{ formatCurrency(campaign.spent) }}</span>
                <span class="utilization" :class="getUtilizationClass(campaign.budgetUtilization)">
                  {{ campaign.budgetUtilization.toFixed(1) }}%
                </span>
              </div>
            </td>
            <td class="numeric">{{ formatNumber(campaign.impressions) }}</td>
            <td class="numeric">{{ formatNumber(campaign.clicks) }}</td>
            <td class="numeric">{{ campaign.ctr.toFixed(2) }}%</td>
            <td class="numeric">{{ formatCurrency(campaign.cpc) }}</td>
            <td class="numeric" :class="getRoiClass(campaign.roi)">
              {{ campaign.roi > 0 ? '+' : '' }}{{ campaign.roi.toFixed(1) }}%
            </td>
            <td>
              <div class="action-buttons">
                <button
                  @click.stop="viewCampaign(campaign)"
                  class="action-btn view-btn"
                  title="View Details"
                >
                  <i class="pi pi-eye"></i>
                </button>
                <button
                  @click.stop="editCampaign(campaign)"
                  class="action-btn edit-btn"
                  title="Edit Campaign"
                >
                  <i class="pi pi-pencil"></i>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        @click="currentPage = Math.max(1, currentPage - 1)"
        :disabled="currentPage === 1"
        class="pagination-btn"
      >
        <i class="pi pi-chevron-left"></i>
      </button>
      
      <span class="pagination-info">
        Page {{ currentPage }} of {{ totalPages }}
        ({{ filteredCampaigns.length }} campaigns)
      </span>
      
      <button
        @click="currentPage = Math.min(totalPages, currentPage + 1)"
        :disabled="currentPage === totalPages"
        class="pagination-btn"
      >
        <i class="pi pi-chevron-right"></i>
      </button>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'

export default {
  name: 'CampaignAnalyticsTable',
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
  emits: ['campaign-click'],
  
  setup(props, { emit }) {
    // Reactive data
    const searchQuery = ref('')
    const statusFilter = ref('')
    const sortBy = ref('campaignName')
    const sortOrder = ref('asc')
    const currentPage = ref(1)
    const itemsPerPage = ref(10)
    
    // Computed properties
    const filteredCampaigns = computed(() => {
      let filtered = [...props.campaigns]
      
      // Apply search filter
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(campaign =>
          campaign.campaignName.toLowerCase().includes(query) ||
          campaign.objective.toLowerCase().includes(query)
        )
      }
      
      // Apply status filter
      if (statusFilter.value) {
        filtered = filtered.filter(campaign =>
          campaign.status === statusFilter.value
        )
      }
      
      // Apply sorting
      filtered.sort((a, b) => {
        let aVal = a[sortBy.value]
        let bVal = b[sortBy.value]
        
        // Handle string sorting
        if (typeof aVal === 'string') {
          aVal = aVal.toLowerCase()
          bVal = bVal.toLowerCase()
        }
        
        let comparison = 0
        if (aVal > bVal) comparison = 1
        if (aVal < bVal) comparison = -1
        
        return sortOrder.value === 'asc' ? comparison : -comparison
      })
      
      return filtered
    })
    
    const totalPages = computed(() => {
      return Math.ceil(filteredCampaigns.value.length / itemsPerPage.value)
    })
    
    const paginatedCampaigns = computed(() => {
      const start = (currentPage.value - 1) * itemsPerPage.value
      const end = start + itemsPerPage.value
      return filteredCampaigns.value.slice(start, end)
    })
    
    const sortIcon = computed(() => {
      return sortOrder.value === 'asc' ? 'pi pi-sort-up' : 'pi pi-sort-down'
    })
    
    // Methods
    const setSortBy = (field) => {
      if (sortBy.value === field) {
        sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
      } else {
        sortBy.value = field
        sortOrder.value = 'asc'
      }
    }
    
    const toggleSortOrder = () => {
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
    }
    
    const formatCurrency = (value) => {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
      }).format(value)
    }
    
    const formatNumber = (value) => {
      if (value >= 1000000) {
        return `${(value / 1000000).toFixed(1)}M`
      } else if (value >= 1000) {
        return `${(value / 1000).toFixed(1)}K`
      }
      return value.toLocaleString()
    }
    
    const getStatusClass = (status) => {
      const baseClass = 'status-badge'
      switch (status?.toLowerCase()) {
        case 'active':
          return `${baseClass} status-active`
        case 'paused':
          return `${baseClass} status-paused`
        case 'draft':
          return `${baseClass} status-draft`
        default:
          return `${baseClass} status-unknown`
      }
    }
    
    const getUtilizationClass = (utilization) => {
      if (utilization >= 90) return 'utilization-high'
      if (utilization >= 70) return 'utilization-medium'
      return 'utilization-low'
    }
    
    const getRoiClass = (roi) => {
      if (roi > 0) return 'roi-positive'
      if (roi < 0) return 'roi-negative'
      return 'roi-neutral'
    }
    
    const viewCampaign = (campaign) => {
      emit('campaign-click', campaign)
    }
    
    const editCampaign = (campaign) => {
      // Emit edit event or handle navigation
      console.log('Edit campaign:', campaign.campaignId)
    }
    
    // Watchers
    watch([searchQuery, statusFilter], () => {
      currentPage.value = 1
    })
    
    return {
      searchQuery,
      statusFilter,
      sortBy,
      sortOrder,
      currentPage,
      itemsPerPage,
      filteredCampaigns,
      totalPages,
      paginatedCampaigns,
      sortIcon,
      setSortBy,
      toggleSortOrder,
      formatCurrency,
      formatNumber,
      getStatusClass,
      getUtilizationClass,
      getRoiClass,
      viewCampaign,
      editCampaign
    }
  }
}
</script>

<style scoped>
.campaign-analytics-table {
  @apply flex flex-col gap-5;
}

.table-controls {
  @apply flex justify-between items-center gap-4 flex-wrap;
}

.search-box {
  @apply relative flex-1 min-w-[200px];
}

.search-box i {
  @apply absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 text-sm;
}

.search-input {
  @apply w-full py-2 pl-9 pr-3 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors;
}

.filter-controls {
  @apply flex items-center gap-2;
}

.filter-select {
  @apply py-2 px-3 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors;
}

.sort-btn {
  @apply p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 cursor-pointer transition-all duration-200 hover:bg-gray-50 dark:hover:bg-gray-700;
}

.table-loading,
.table-empty {
  @apply flex flex-col items-center justify-center p-12 text-gray-500 dark:text-gray-400;
}

.loading-spinner {
  @apply w-8 h-8 border-4 border-gray-200 border-t-blue-500 rounded-full animate-spin mb-4;
}

.table-empty i {
  @apply text-5xl mb-4 text-gray-300 dark:text-gray-600;
}

.table-container {
  @apply overflow-x-auto border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm;
}

.analytics-table {
  @apply w-full border-collapse bg-white dark:bg-gray-800;
}

.analytics-table th {
  @apply bg-gray-50 dark:bg-gray-700 p-3 text-left font-semibold text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-gray-600 whitespace-nowrap;
}

.analytics-table th.numeric {
  @apply text-right;
}

.analytics-table th.sortable {
  @apply cursor-pointer select-none relative hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors;
}

.analytics-table th i {
  @apply ml-1 text-xs;
}

.analytics-table td {
  @apply p-3 border-b border-gray-100 dark:border-gray-700 text-gray-900 dark:text-white;
}

.analytics-table td.numeric {
  @apply text-right;
}

.table-row {
  @apply cursor-pointer transition-colors hover:bg-gray-50 dark:hover:bg-gray-700/50;
}

.campaign-info {
  @apply flex flex-col gap-0.5;
}

.campaign-info .name {
  @apply font-medium text-gray-900 dark:text-white;
}

.campaign-info .objective {
  @apply text-xs text-gray-500 dark:text-gray-400;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
}

.status-active {
  background: #dcfce7;
  color: #166534;
}

.status-paused {
  background: #fef3c7;
  color: #92400e;
}

.status-draft {
  background: #f3f4f6;
  color: #374151;
}

.spent-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.utilization {
  font-size: 11px;
  font-weight: 500;
}

.utilization-high { color: #dc2626; }
.utilization-medium { color: #f59e0b; }
.utilization-low { color: #059669; }

.roi-positive { color: #059669; }
.roi-negative { color: #dc2626; }
.roi-neutral { color: #6b7280; }

.action-buttons {
  display: flex;
  gap: 4px;
}

.action-btn {
  padding: 6px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
}

.view-btn {
  background: #eff6ff;
  color: #2563eb;
}

.view-btn:hover {
  background: #dbeafe;
}

.edit-btn {
  background: #f0fdf4;
  color: #16a34a;
}

.edit-btn:hover {
  background: #dcfce7;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 16px;
}

.pagination-btn {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  background: #f3f4f6;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #6b7280;
}

/* Responsive Design */
@media (width <= 768px) {
  .table-controls {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-box {
    min-width: auto;
  }
  
  .filter-controls {
    justify-content: space-between;
  }
  
  .analytics-table {
    font-size: 12px;
  }
  
  .analytics-table th,
  .analytics-table td {
    padding: 8px;
  }
  
  .campaign-info .name {
    font-size: 13px;
  }
  
  .campaign-info .objective {
    font-size: 11px;
  }
}
</style>
