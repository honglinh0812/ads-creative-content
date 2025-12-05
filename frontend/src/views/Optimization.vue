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

        <div v-if="insight.persona" class="persona-summary">
          <div class="persona-summary-header">
            <p class="eyebrow">{{ t('optimizationLite.persona.attachedTitle') }}</p>
            <a-tag color="cyan">{{ t('optimizationLite.persona.autoApplied') }}</a-tag>
          </div>
          <h4>{{ insight.persona.name }}</h4>
          <div class="persona-tags">
            <a-tag color="blue" v-if="insight.persona.age">
              {{ t('optimizationLite.persona.age', { age: insight.persona.age }) }}
            </a-tag>
            <a-tag color="purple" v-if="insight.persona.tone">
              {{ formatPersonaTone(insight.persona.tone) }}
            </a-tag>
            <a-tag color="green" v-if="insight.persona.gender">
              {{ formatPersonaGender(insight.persona.gender) }}
            </a-tag>
          </div>
          <p class="hint" v-if="insight.persona.desiredOutcome">{{ insight.persona.desiredOutcome }}</p>
          <div class="persona-interests" v-if="insight.persona.interests?.length">
            <a-tag v-for="interest in insight.persona.interests.slice(0, 4)" :key="interest">
              {{ interest }}
            </a-tag>
          </div>
        </div>
        <p class="hint persona-missing" v-else>
          {{ t('optimizationLite.persona.noPersona') }}
        </p>

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

        <div v-if="insight.copyReview" class="copy-review">
          <div class="copy-review-header">
            <h4>{{ t('optimizationLite.copyReview.title') }}</h4>
            <a-tag color="purple">{{ Math.round(insight.copyReview.overallScore || insight.scorecard?.total || 0) }}/100</a-tag>
          </div>
          <p class="hint" v-if="insight.copyReview.personaSummary">
            {{ t('optimizationLite.copyReview.persona', { persona: insight.copyReview.personaSummary }) }}
          </p>
          <div
            v-for="section in insight.copyReview.sections"
            :key="section.section"
            class="copy-review-section"
          >
            <div class="section-header">
              <h5>{{ formatSectionLabel(section.section) }}</h5>
              <a-tag>{{ Math.round(section.score || 0) }}/100</a-tag>
            </div>
            <p class="verdict">{{ section.verdict }}</p>
            <div v-if="section.strengths?.length" class="strengths">
              <strong>{{ t('optimizationLite.copyReview.strengths') }}</strong>
              <ul>
                <li v-for="(item, index) in section.strengths" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div v-if="section.improvements?.length" class="improvements">
              <strong>{{ t('optimizationLite.copyReview.improvements') }}</strong>
              <ul>
                <li v-for="(item, index) in section.improvements" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div v-if="section.rewrite" class="rewrite-preview">
              <strong>{{ t('optimizationLite.copyReview.rewritePreview') }}</strong>
              <p>{{ section.rewrite }}</p>
            </div>
            <a-button
              type="link"
              size="small"
              :loading="isRewriteLoading(insight.adId, section.section)"
              @click="handleRewriteSection(insight, section)"
            >
              {{ t('optimizationLite.actions.rewrite') }}
            </a-button>
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
    const rewriteLoading = ref({})

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

    const formatPersonaGender = (gender) => {
      switch ((gender || '').toUpperCase()) {
        case 'MALE':
          return t('personas.male')
        case 'FEMALE':
          return t('personas.female')
        case 'ALL':
          return t('personas.all')
        default:
          return gender || '—'
      }
    }

    const formatPersonaTone = (tone) => {
      if (!tone) return '—'
      const toneKey = tone.toLowerCase()
      const translationKey = `personas.toneLabels.${toneKey}`
      const translated = t(translationKey, tone)
      return translated || tone
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

    const formatSectionLabel = (section) => {
      switch ((section || '').toUpperCase()) {
        case 'HEADLINE':
          return t('optimizationLite.copyReview.sections.headline')
        case 'DESCRIPTION':
          return t('optimizationLite.copyReview.sections.description')
        default:
          return t('optimizationLite.copyReview.sections.primaryText')
      }
    }

    const rewriteKey = (adId, section) => `${adId}-${section}`

    const isRewriteLoading = (adId, section) => {
      return !!rewriteLoading.value[rewriteKey(adId, section)]
    }

    const handleRewriteSection = async (insight, section) => {
      const key = rewriteKey(insight.adId, section.section)
      rewriteLoading.value = { ...rewriteLoading.value, [key]: true }
      try {
        const response = await api.optimizationAPI.rewriteAdCopy(insight.adId, {
          section: section.section,
          additionalGuidance: section.improvements?.join(' ') || '',
          language: locale.value
        })
        const rewritten = response.data?.data?.rewrittenText || response.data?.rewrittenText
        if (rewritten) {
          section.rewrite = rewritten
          message.success(t('optimizationLite.messages.rewriteSuccess'))
        } else {
          throw new Error('Missing rewrite text')
        }
      } catch (error) {
        console.error('Rewrite failed', error)
        message.error(t('optimizationLite.messages.rewriteFailed'))
      } finally {
        rewriteLoading.value = { ...rewriteLoading.value, [key]: false }
      }
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
      handlePageSizeChange,
      formatSectionLabel,
      handleRewriteSection,
      isRewriteLoading,
      formatPersonaGender,
      formatPersonaTone
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

.persona-hint {
  margin-bottom: 16px;
}

.persona-tags,
.persona-interests {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.persona-summary {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  margin: 12px 0;
  background: #f8fafc;
}

.persona-summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.persona-missing {
  margin: 12px 0;
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

.copy-review {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.copy-review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.copy-review-section {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  margin-top: 12px;
  background: #f9fafb;
}

.copy-review-section .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.copy-review-section .verdict {
  font-weight: 500;
  margin-bottom: 6px;
}

.copy-review-section ul {
  padding-left: 18px;
  margin: 4px 0 8px;
}

.rewrite-preview {
  padding: 8px;
  background: #fff;
  border-radius: 8px;
  border: 1px dashed #cbd5f5;
  margin-bottom: 8px;
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
