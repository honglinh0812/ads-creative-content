<template>
  <div class="optimization-lite">
    <div class="lite-card selection-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">{{ t('optimizationLite.title') }}</p>
          <h2>{{ t('optimizationLite.subtitle') }}</h2>
          <p class="hint">{{ t('optimizationLite.description') }}</p>
        </div>
      </div>

      <div class="table-actions">
        <div class="selection-info">
          <p>{{ t('optimizationLite.selectionCount', { count: selectedRowKeys.length }) }}</p>
        </div>
        <a-space>
          <a-button
            type="primary"
            :disabled="!selectedRowKeys.length || !adInsightsSupported"
            :loading="analyzeLoading"
            @click="analyzeSelected"
          >
            {{ t('optimizationLite.actions.analyze') }}
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="tableColumns"
        :data-source="paginatedAds"
        :loading="adsLoading"
        :pagination="false"
        :row-selection="rowSelection"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="ad-name-cell">
              <strong>{{ record.name || t('optimizationLite.table.untitled') }}</strong>
              <p class="hint">{{ record.campaignName || t('optimizationLite.table.noCampaign') }}</p>
            </div>
          </template>
          <template v-else-if="column.key === 'cta'">
            <a-tag v-if="record.callToAction">{{ record.callToAction }}</a-tag>
            <span v-else>—</span>
          </template>
          <template v-else-if="column.key === 'created'">
            {{ formatDate(record.createdDate) }}
          </template>
        </template>
      </a-table>

      <div class="table-pagination">
        <a-pagination
          :current="currentPage"
          :total="filteredAds.length"
          :page-size="pageSize"
          :hide-on-single-page="filteredAds.length <= pageSize"
          :page-size-options="pageSizeOptions"
          show-size-changer
          @change="handlePaginationChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>

      <div class="table-footer">
        <a-button v-if="hasMoreAds" @click="loadMoreAds" :loading="adsLoading">
          {{ t('optimizationLite.actions.loadMoreAds') }}
        </a-button>
        <p v-else class="hint">{{ t('optimizationLite.noMoreAds') }}</p>
      </div>
    </div>

    <div class="insights-grid">
      <div class="lite-card empty-state" v-if="!adInsightsSupported">
        <p class="eyebrow">{{ t('optimizationLite.empty.title') }}</p>
        <p class="hint">{{ t('optimizationLite.messages.insightsUnavailable') }}</p>
      </div>
      <div class="lite-card empty-state" v-else-if="!analyzedInsights.length && !analyzeLoading">
        <p class="eyebrow">{{ t('optimizationLite.empty.title') }}</p>
        <p class="hint">{{ t('optimizationLite.empty.description') }}</p>
      </div>

      <div
        v-for="insight in analyzedInsights"
        :key="insight.adId"
        class="lite-card insight-card"
      >
        <div class="insight-header">
          <div>
            <p class="eyebrow">{{ insight.campaignName || t('optimizationLite.table.noCampaign') }}</p>
            <h3>{{ insight.adName || t('optimizationLite.table.untitled') }}</h3>
            <p class="hint">
              {{ t('optimizationLite.card.type', { type: insight.adType || '—' }) }} · {{ formatDate(insight.createdDate) }}
            </p>
          </div>
          <div class="score-pill">
            <span>{{ Math.round(insight.scorecard.total) }}</span>
            <p>{{ t('optimizationLite.card.score') }}</p>
          </div>
        </div>

        <div class="suggestions">
          <div
            v-for="category in categoryOrder"
            :key="category"
            class="suggestion-column"
          >
            <h4>{{ categoryLabels[category] }}</h4>
            <ul>
              <li v-for="(suggestion, index) in (insight.suggestions[category] || [])" :key="index">
                {{ suggestion }}
              </li>
            </ul>
          </div>
        </div>

        <div class="card-footer">
          <div class="scores">
            <div>
              <p class="label">{{ t('optimizationLite.card.compliance') }}</p>
              <p class="value">{{ Math.round(insight.scorecard.compliance) }}</p>
            </div>
            <div>
              <p class="label">{{ t('optimizationLite.card.linguistic') }}</p>
              <p class="value">{{ Math.round(insight.scorecard.linguistic) }}</p>
            </div>
            <div>
              <p class="label">{{ t('optimizationLite.card.persuasiveness') }}</p>
              <p class="value">{{ Math.round(insight.scorecard.persuasiveness) }}</p>
            </div>
          </div>
          <a-button
            type="primary"
            :loading="savingInsightId === insight.adId"
            :disabled="insight.saved"
            @click="saveInsight(insight)"
          >
            {{ insight.saved ? t('optimizationLite.actions.saved') : t('optimizationLite.actions.save') }}
          </a-button>
        </div>
      </div>
    </div>

    <div class="lite-card history-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">{{ t('optimizationLite.history.title') }}</p>
          <h3>{{ t('optimizationLite.history.subtitle') }}</h3>
        </div>
        <a-button @click="refreshHistory" :loading="historyLoading">
          {{ t('optimizationLite.actions.refreshHistory') }}
        </a-button>
      </div>

      <div v-if="history.length" class="history-list">
        <div v-for="entry in history" :key="entry.id" class="history-item">
          <div>
            <h4>{{ entry.adName }}</h4>
            <p class="hint">
              {{ entry.campaignName || t('optimizationLite.table.noCampaign') }}
              · {{ formatDate(entry.createdAt) }}
            </p>
            <div class="history-tags" v-if="entry.suggestions">
              <a-tag v-for="(items, category) in entry.suggestions" :key="category">
                {{ categoryLabels[category] }} · {{ items.length }}
              </a-tag>
            </div>
          </div>
          <p class="score">{{ Math.round(entry.scorecard?.total || 0) }}</p>
        </div>
        <div class="history-footer">
          <a-button v-if="historyHasMore" @click="loadHistory" :loading="historyLoading">
            {{ t('optimizationLite.actions.loadMoreHistory') }}
          </a-button>
          <p v-else class="hint">{{ t('optimizationLite.history.end') }}</p>
        </div>
      </div>
      <p v-else-if="!historySupported" class="empty-text">
        {{ t('optimizationLite.messages.historyUnavailable') }}
      </p>
      <p v-else class="empty-text">{{ t('optimizationLite.history.empty') }}</p>
    </div>

    <a-alert
      v-if="error"
      type="error"
      show-icon
      :message="t('optimizationLite.error.title')"
      :description="error"
    />
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import api from '@/services/api'

export default {
  name: 'OptimizationLiteView',
  setup() {
    const { t, locale } = useI18n()
    const ads = ref([])
    const adsLoading = ref(false)
    const searchTerm = ref('')
    const pagination = ref({ page: 0, size: 25, total: 0 })
    const currentPage = ref(1)
    const pageSize = ref(5)
    const pageSizeOptions = ['5', '10']
    const selectedRowKeys = ref([])
    const analyzedInsights = ref([])
    const analyzeLoading = ref(false)
    const savingInsightId = ref(null)
    const history = ref([])
    const historyPage = ref(0)
    const historyHasMore = ref(true)
    const historyLoading = ref(false)
    const error = ref(null)
    const adInsightsSupported = ref(true)
    const historySupported = ref(true)

    const tableColumns = computed(() => [
      {
        title: t('optimizationLite.table.columns.name'),
        key: 'name',
        dataIndex: 'name'
      },
      {
        title: t('optimizationLite.table.columns.type'),
        key: 'type',
        dataIndex: 'adType'
      },
      {
        title: t('optimizationLite.table.columns.cta'),
        key: 'cta',
        dataIndex: 'callToAction'
      },
      {
        title: t('optimizationLite.table.columns.created'),
        key: 'created',
        dataIndex: 'createdDate'
      }
    ])

    const rowSelection = computed(() => ({
      selectedRowKeys: selectedRowKeys.value,
      onChange: (keys) => {
        selectedRowKeys.value = keys
      }
    }))

    const filteredAds = computed(() => {
      if (!searchTerm.value) return ads.value
      const term = searchTerm.value.toLowerCase()
      return ads.value.filter(ad =>
        ad.name?.toLowerCase().includes(term) ||
        ad.campaignName?.toLowerCase().includes(term)
      )
    })

    const paginatedAds = computed(() => {
      const start = (currentPage.value - 1) * pageSize.value
      return filteredAds.value.slice(start, start + pageSize.value)
    })

    const hasMoreAds = computed(() => ads.value.length < pagination.value.total)

    const categoryOrder = ['copy', 'visual', 'cta', 'hook', 'length', 'target']
    const categoryLabels = computed(() => ({
      copy: t('optimizationLite.categories.copy'),
      visual: t('optimizationLite.categories.visual'),
      cta: t('optimizationLite.categories.cta'),
      hook: t('optimizationLite.categories.hook'),
      length: t('optimizationLite.categories.length'),
      target: t('optimizationLite.categories.target')
    }))

    const mapDtoToRow = (ad) => ({
      id: ad.id,
      name: ad.name,
      adType: ad.adType,
      campaignName: ad.campaignName,
      callToAction: ad.callToAction,
      createdDate: ad.createdDate
    })

    const loadAds = async (reset = false) => {
      if (reset) {
        pagination.value.page = 0
        ads.value = []
        selectedRowKeys.value = []
        currentPage.value = 1
      }
      adsLoading.value = true
      error.value = null
      try {
        const { data } = await api.ads.getAll(pagination.value.page, pagination.value.size)
        pagination.value.total = data.totalElements || 0
        const newAds = (data.content || []).map(mapDtoToRow)
        ads.value = reset ? newAds : [...ads.value, ...newAds]
      } catch (err) {
        error.value = err?.message || 'Unable to load ads'
      } finally {
        adsLoading.value = false
      }
    }

    const refreshAds = () => loadAds(true)
    const loadMoreAds = () => {
      pagination.value.page += 1
      loadAds()
    }

    const shouldDisableFeature = (err) => {
      const status = err?.response?.status
      return status === 404 || status === 501 || status === 503
    }

    const analyzeSelected = async () => {
      if (!adInsightsSupported.value) {
        message.info(t('optimizationLite.messages.insightsUnavailable'))
        return
      }
      analyzeLoading.value = true
      error.value = null
      try {
        const payload = {
          adIds: selectedRowKeys.value,
          language: locale.value
        }
        const { data } = await api.optimizationAPI.analyzeAds(payload)
        analyzedInsights.value = data.data || []
        if (!analyzedInsights.value.length) {
          message.info(t('optimizationLite.messages.noInsights'))
        }
      } catch (err) {
        console.error(err)
        if (shouldDisableFeature(err)) {
          adInsightsSupported.value = false
          message.info(t('optimizationLite.messages.insightsUnavailable'))
        } else {
          error.value = err?.message || 'Unable to analyze ads'
          message.error(error.value)
        }
      } finally {
        analyzeLoading.value = false
      }
    }

    const saveInsight = async (insight) => {
      savingInsightId.value = insight.adId
      try {
        await api.optimizationAPI.saveAdInsights({
          adId: insight.adId,
          adName: insight.adName,
          campaignName: insight.campaignName,
          language: locale.value,
          suggestions: insight.suggestions,
          scorecard: insight.scorecard
        })
        insight.saved = true
        message.success(t('optimizationLite.messages.saved'))
        refreshHistory()
      } catch (err) {
        console.error(err)
        message.error(err?.message || 'Unable to save insight')
      } finally {
        savingInsightId.value = null
      }
    }

    const loadHistory = async (reset = false) => {
      if (!historySupported.value) return
      if (reset) {
        history.value = []
        historyPage.value = 0
        historyHasMore.value = true
      }
      if (!historyHasMore.value) return

      historyLoading.value = true
      try {
        const { data } = await api.optimizationAPI.getInsightHistory(historyPage.value, 5)
        const pageData = data.data || { content: [], last: true }
        const entries = pageData.content || []
        history.value = reset ? entries : [...history.value, ...entries]
        historyHasMore.value = !pageData.last
        historyPage.value += 1
      } catch (err) {
        console.error(err)
        if (shouldDisableFeature(err)) {
          historySupported.value = false
          message.info(t('optimizationLite.messages.historyUnavailable'))
        } else {
          message.error(err?.message || 'Unable to load history')
        }
      } finally {
        historyLoading.value = false
      }
    }

    const refreshHistory = () => {
      historySupported.value = true
      loadHistory(true)
    }

    const onSearch = () => {
      // search is reactive, but we keep handler for keyboard enter
    }

    const formatDate = (value) => {
      if (!value) return '—'
      return new Intl.DateTimeFormat(undefined, {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
      }).format(new Date(value))
    }

    onMounted(() => {
      loadAds(true)
      loadHistory(true)
    })

    watch([filteredAds, pageSize], () => {
      const maxPage = Math.max(1, Math.ceil(filteredAds.value.length / pageSize.value) || 1)
      if (currentPage.value > maxPage) {
        currentPage.value = maxPage
      }
    })

    watch(searchTerm, () => {
      currentPage.value = 1
    })

    const handlePaginationChange = (page, size) => {
      currentPage.value = page
      if (size && size !== pageSize.value) {
        pageSize.value = Number(size)
      }
    }

    const handlePageSizeChange = (_, size) => {
      pageSize.value = Number(size)
      currentPage.value = 1
    }

    return {
      t,
      adsLoading,
      filteredAds,
      paginatedAds,
      tableColumns,
      rowSelection,
      selectedRowKeys,
      searchTerm,
      analyzedInsights,
      analyzeLoading,
      savingInsightId,
      categoryOrder,
      categoryLabels,
      hasMoreAds,
      history,
      historyLoading,
      historyHasMore,
      error,
      loadMoreAds,
      refreshAds,
      analyzeSelected,
      saveInsight,
      loadHistory,
      refreshHistory,
      onSearch,
      formatDate,
      adInsightsSupported,
      historySupported,
      currentPage,
      pageSize,
      pageSizeOptions,
      handlePaginationChange,
      handlePageSizeChange
    }
  }
}
</script>

<style scoped>
.optimization-lite {
  padding: 32px;
  background: #f5f6fa;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.lite-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.selection-panel .panel-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
}

.panel-actions {
  display: flex;
  gap: 12px;
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #64748b;
  font-size: 12px;
  margin-bottom: 4px;
}

.hint {
  color: #94a3b8;
}

.table-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
}

.table-footer {
  margin-top: 16px;
  text-align: center;
}

.table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.insight-card .suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin: 16px 0;
}

.suggestion-column h4 {
  margin-bottom: 8px;
}

.suggestion-column ul {
  list-style: disc;
  padding-left: 18px;
  color: #0f172a;
}

.score-pill {
  background: #ebe9fe;
  color: #5b21b6;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
}

.score-pill span {
  font-size: 28px;
  font-weight: 700;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.scores {
  display: flex;
  gap: 24px;
}

.scores .label {
  color: #94a3b8;
  margin-bottom: 4px;
}

.scores .value {
  font-size: 20px;
  font-weight: 600;
}

.history-panel .history-item {
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.history-item .score {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.history-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.empty-state {
  text-align: center;
}

.empty-text {
  color: #94a3b8;
}

@media (max-width: 768px) {
  .optimization-lite {
    padding: 16px;
  }

  .selection-panel .panel-header {
    flex-direction: column;
  }

  .panel-actions {
    flex-direction: column;
  }

  .table-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .scores {
    flex-direction: column;
  }
}
</style>
