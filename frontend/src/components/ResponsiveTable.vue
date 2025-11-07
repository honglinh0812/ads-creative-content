<template>
  <div class="responsive-table-wrapper">
    <!-- Desktop/Tablet: Table View -->
    <a-table
      v-if="!isMobile"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :row-selection="rowSelection"
      :scroll="scroll"
      :row-key="rowKey"
      @change="handleTableChange"
      class="desktop-table"
    >
      <template v-for="(_, slot) in $slots" v-slot:[slot]="scope">
        <slot :name="slot" v-bind="scope" />
      </template>
    </a-table>

    <!-- Mobile: Card View -->
    <div v-else class="mobile-card-view">
      <!-- Loading State -->
      <div v-if="loading" class="mobile-loading">
        <a-spin size="large" />
        <p class="mt-4 text-gray-500">{{ $t('common.loading') }}</p>
      </div>

      <!-- Empty State -->
      <a-empty
        v-else-if="!dataSource || dataSource.length === 0"
        :description="emptyText || $t('common.noData')"
      />

      <!-- Cards -->
      <div v-else class="mobile-cards-container">
        <div
          v-for="(item, index) in paginatedData"
          :key="item[rowKey] || index"
          class="mobile-card"
          :class="{ selected: isSelected(item) }"
          @click="handleCardClick(item)"
        >
          <!-- Card Header with Selection -->
          <div class="card-header">
            <a-checkbox
              v-if="rowSelection"
              :checked="isSelected(item)"
              @click.stop="handleSelect(item)"
            />
            <slot name="cardTitle" :record="item">
              <h3 class="card-title">{{ getCardTitle(item) }}</h3>
            </slot>
          </div>

          <!-- Card Body -->
          <div class="card-body">
            <slot name="cardContent" :record="item">
              <div
                v-for="col in visibleColumns"
                :key="col.key"
                class="card-field"
              >
                <label class="field-label">{{ col.title }}:</label>
                <div class="field-value">
                  <slot
                    :name="`bodyCell-${col.key}`"
                    :record="item"
                    :column="col"
                  >
                    {{ getFieldValue(item, col) }}
                  </slot>
                </div>
              </div>
            </slot>
          </div>

          <!-- Card Actions -->
          <div v-if="$slots.actions" class="card-actions">
            <slot name="actions" :record="item" />
          </div>
        </div>
      </div>

      <!-- Mobile Pagination -->
      <div v-if="pagination && dataSource.length > 0" class="mobile-pagination">
        <a-pagination
          v-model:current="currentPage"
          v-model:page-size="pageSize"
          :total="dataSource.length"
          :show-size-changer="false"
          size="small"
          @change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'

export default {
  name: 'ResponsiveTable',
  props: {
    columns: {
      type: Array,
      required: true
    },
    dataSource: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    pagination: {
      type: [Object, Boolean],
      default: () => ({ pageSize: 10, showSizeChanger: true })
    },
    rowSelection: {
      type: Object,
      default: null
    },
    scroll: {
      type: Object,
      default: null
    },
    rowKey: {
      type: String,
      default: 'id'
    },
    mobileBreakpoint: {
      type: Number,
      default: 768
    },
    emptyText: {
      type: String,
      default: null
    },
    cardTitleKey: {
      type: String,
      default: null
    }
  },
  emits: ['change', 'selection-change'],
  setup(props, { emit }) {
    const isMobile = ref(false)
    const currentPage = ref(1)
    const pageSize = ref(props.pagination?.pageSize || 10)
    const selectedRowKeys = ref(props.rowSelection?.selectedRowKeys || [])

    // Detect mobile viewport
    const checkMobile = () => {
      isMobile.value = window.innerWidth <= props.mobileBreakpoint
    }

    // Initialize and watch for resize
    checkMobile()
    window.addEventListener('resize', checkMobile)

    // Computed: Visible columns (exclude hidden ones)
    const visibleColumns = computed(() => {
      return props.columns.filter(col => !col.hidden)
    })

    // Computed: Paginated data for mobile view
    const paginatedData = computed(() => {
      if (!props.pagination) return props.dataSource
      const start = (currentPage.value - 1) * pageSize.value
      const end = start + pageSize.value
      return props.dataSource.slice(start, end)
    })

    // Get card title
    const getCardTitle = (item) => {
      if (props.cardTitleKey) {
        return item[props.cardTitleKey]
      }
      // Try common title fields
      return item.name || item.title || item.headline || item[props.rowKey]
    }

    // Get field value
    const getFieldValue = (item, col) => {
      if (col.dataIndex) {
        return item[col.dataIndex]
      }
      if (col.key) {
        return item[col.key]
      }
      return ''
    }

    // Check if item is selected
    const isSelected = (item) => {
      return selectedRowKeys.value.includes(item[props.rowKey])
    }

    // Handle card click
    const handleCardClick = (item) => {
      if (props.rowSelection) {
        handleSelect(item)
      }
    }

    // Handle selection
    const handleSelect = (item) => {
      const key = item[props.rowKey]
      const index = selectedRowKeys.value.indexOf(key)

      if (index > -1) {
        selectedRowKeys.value.splice(index, 1)
      } else {
        selectedRowKeys.value.push(key)
      }

      if (props.rowSelection?.onChange) {
        props.rowSelection.onChange(selectedRowKeys.value, getSelectedRows())
      }

      emit('selection-change', selectedRowKeys.value, getSelectedRows())
    }

    // Get selected rows
    const getSelectedRows = () => {
      return props.dataSource.filter(item =>
        selectedRowKeys.value.includes(item[props.rowKey])
      )
    }

    // Handle table change (sorting, pagination, etc.)
    const handleTableChange = (pagination, filters, sorter) => {
      emit('change', pagination, filters, sorter)
    }

    // Handle page change (mobile)
    const handlePageChange = (page, size) => {
      currentPage.value = page
      pageSize.value = size
    }

    // Watch for external selection changes
    watch(() => props.rowSelection?.selectedRowKeys, (newKeys) => {
      if (newKeys) {
        selectedRowKeys.value = [...newKeys]
      }
    }, { deep: true })

    return {
      isMobile,
      currentPage,
      pageSize,
      selectedRowKeys,
      visibleColumns,
      paginatedData,
      getCardTitle,
      getFieldValue,
      isSelected,
      handleCardClick,
      handleSelect,
      handleTableChange,
      handlePageChange
    }
  }
}
</script>

<style scoped>
.responsive-table-wrapper {
  width: 100%;
}

/* Desktop Table Enhancements */
.desktop-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.desktop-table :deep(.ant-table) {
  font-size: 14px;
}

.desktop-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #262626;
}

/* Mobile Card View */
.mobile-card-view {
  width: 100%;
}

.mobile-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.mobile-cards-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px;
}

.mobile-card {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
  cursor: pointer;
}

.mobile-card:active {
  transform: scale(0.98);
}

.mobile-card.selected {
  border-color: #1890ff;
  background: #f0f9ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 0;
  line-height: 1.4;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.field-value {
  font-size: 14px;
  color: #262626;
  line-height: 1.5;
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.card-actions :deep(.ant-btn) {
  flex: 1;
}

.mobile-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding: 16px;
}

/* Dark Mode Support */
.dark .desktop-table :deep(.ant-table-thead > tr > th) {
  background: #1f1f1f;
  color: #fafafa;
}

.dark .mobile-card {
  background: #1f1f1f;
  border-color: #303030;
}

.dark .mobile-card.selected {
  border-color: #1890ff;
  background: #0c2340;
}

.dark .card-title {
  color: #fafafa;
}

.dark .field-label {
  color: #8c8c8c;
}

.dark .field-value {
  color: #d9d9d9;
}

.dark .card-header,
.dark .card-actions {
  border-color: #303030;
}

/* Touch-friendly enhancements */
@media (pointer: coarse) {
  .mobile-card {
    padding: 20px;
    min-height: 44px;
  }

  .card-actions :deep(.ant-btn) {
    min-height: 44px;
    padding: 12px 16px;
  }
}

/* Tablet optimization (768px - 1024px) */
@media (min-width: 769px) and (max-width: 1024px) {
  .desktop-table :deep(.ant-table) {
    font-size: 13px;
  }

  .desktop-table :deep(.ant-table-thead > tr > th) {
    padding: 12px 8px;
  }

  .desktop-table :deep(.ant-table-tbody > tr > td) {
    padding: 12px 8px;
  }
}

/* Small mobile (< 375px) */
@media (max-width: 374px) {
  .mobile-cards-container {
    gap: 8px;
  }

  .mobile-card {
    padding: 12px;
    border-radius: 8px;
  }

  .card-title {
    font-size: 14px;
  }

  .field-label {
    font-size: 11px;
  }

  .field-value {
    font-size: 13px;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .mobile-card {
    border-width: 2px;
  }

  .mobile-card.selected {
    border-width: 3px;
  }
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .mobile-card {
    transition: none;
  }

  .mobile-card:active {
    transform: none;
  }
}
</style>
