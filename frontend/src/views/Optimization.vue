<template>
  <div class="optimization-lite">
    <div class="lite-card selection-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">{{ t('optimizationLite.title') }}</p>
          <h2>{{ t('optimizationLite.subtitle') }}</h2>
          <p class="hint">{{ t('optimizationLite.description') }}</p>
        </div>
        <div class="panel-actions">
          <a-input-search
            v-model:value="searchTerm"
            :placeholder="t('optimizationLite.searchPlaceholder')"
            allow-clear
            @search="onSearch"
          />
          <a-button @click="refreshAds" :loading="adsLoading">
            {{ t('optimizationLite.actions.refreshAds') }}
          </a-button>
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
        :data-source="filteredAds"
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

    <div class="lite-card rewrite-panel">
      <div class="panel-header rewrite-header">
        <div>
          <p class="eyebrow">{{ t('optimizationLite.rewrite.title') }}</p>
          <h3>{{ t('optimizationLite.rewrite.subtitle') }}</h3>
        </div>
        <a-button
          type="primary"
          :loading="rewriteLoading"
          :disabled="!rewriteForm.adId"
          @click="generateRewrite"
        >
          {{ t('optimizationLite.rewrite.generate') }}
        </a-button>
      </div>

      <div v-if="!ads.length" class="empty-text">
        {{ t('optimizationLite.rewrite.noAd') }}
      </div>

      <div v-else class="rewrite-content">
        <a-form layout="vertical" class="rewrite-form">
          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('optimizationLite.rewrite.selectAd')">
                <a-select v-model:value="rewriteForm.adId" show-search option-filter-prop="children">
                  <a-select-option
                    v-for="ad in ads"
                    :key="ad.id"
                    :value="ad.id"
                  >
                    {{ ad.name || t('optimizationLite.table.untitled') }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item :label="t('optimizationLite.rewrite.tone')">
                <a-select v-model:value="rewriteForm.tone">
                  <a-select-option v-for="tone in toneOptions" :key="tone.value" :value="tone.value">
                    {{ tone.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item :label="t('optimizationLite.rewrite.length')">
                <a-select v-model:value="rewriteForm.length">
                  <a-select-option v-for="length in lengthOptions" :key="length.value" :value="length.value">
                    {{ length.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('optimizationLite.rewrite.focus')">
                <a-input v-model:value="rewriteForm.focus" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item :label="t('optimizationLite.rewrite.keywords')">
                <a-input
                  v-model:value="rewriteForm.keywords"
                  :placeholder="t('optimizationLite.rewrite.keywordsHint')"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="[16, 16]">
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('optimizationLite.rewrite.provider')">
                <a-select v-model:value="rewriteForm.provider">
                  <a-select-option v-for="provider in providerOptions" :key="provider.value" :value="provider.value">
                    {{ provider.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('optimizationLite.rewrite.variations')">
                <a-input-number v-model:value="rewriteForm.variations" :min="1" :max="5" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('optimizationLite.rewrite.guidance')">
                <a-input
                  v-model:value="rewriteForm.guidance"
                  :placeholder="t('optimizationLite.rewrite.guidancePlaceholder')"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>

        <div v-if="rewriteSelectedAd" class="current-ad-context">
          <p class="eyebrow">{{ t('optimizationLite.rewrite.currentCopy') }}</p>
          <p class="context-block">
            {{ rewriteSelectedAd.primaryText || rewriteSelectedAd.description || rewriteSelectedAd.headline || '—' }}
          </p>
        </div>

        <div class="rewrite-results">
          <p class="eyebrow">{{ t('optimizationLite.rewrite.resultsTitle') }}</p>
          <div v-if="rewriteResults.length" class="rewrite-result-list">
            <div
              v-for="variation in rewriteResults"
              :key="variation.id"
              class="rewrite-result-card"
            >
              <h4>{{ variation.headline || t('optimizationLite.table.untitled') }}</h4>
              <p class="hint">{{ variation.description }}</p>
              <p>{{ variation.primaryText }}</p>
              <div class="rewrite-card-footer">
                <a-tag v-if="variation.callToAction">{{ variation.callToAction }}</a-tag>
                <a-button size="small" @click="copyVariation(variation)">
                  {{ t('optimizationLite.rewrite.copy') }}
                </a-button>
              </div>
            </div>
          </div>
          <p v-else class="hint">{{ t('optimizationLite.rewrite.empty') }}</p>
        </div>

        <a-alert
          v-if="rewriteError"
          type="error"
          show-icon
          :message="rewriteError"
          class="rewrite-alert"
        />
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
import { ref, computed, onMounted, reactive, watch } from 'vue'
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
    const rewriteForm = reactive({
      adId: null,
      tone: 'confident',
      length: 'medium',
      focus: '',
      keywords: '',
      guidance: '',
      provider: 'openai',
      variations: 2
    })
    const rewriteResults = ref([])
    const rewriteLoading = ref(false)
    const rewriteError = ref(null)

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
      campaignId: ad.campaignId,
      callToAction: ad.callToAction,
      createdDate: ad.createdDate,
      primaryText: ad.primaryText,
      headline: ad.headline,
      description: ad.description,
      status: ad.status,
      websiteUrl: ad.websiteUrl,
      adStyle: ad.adStyle,
      prompt: ad.prompt
    })

    const loadAds = async (reset = false) => {
      if (reset) {
        pagination.value.page = 0
        ads.value = []
        selectedRowKeys.value = []
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

    const toneOptions = [
      { label: 'Confident', value: 'confident' },
      { label: 'Friendly', value: 'friendly' },
      { label: 'Urgent', value: 'urgent' },
      { label: 'Playful', value: 'playful' }
    ]

    const lengthOptions = [
      { label: 'Short', value: 'short' },
      { label: 'Medium', value: 'medium' },
      { label: 'Long', value: 'long' }
    ]

    const providerOptions = [
      { label: 'OpenAI', value: 'openai' },
      { label: 'Anthropic', value: 'anthropic' },
      { label: 'Gemini', value: 'gemini' }
    ]

    const rewriteSelectedAd = computed(() =>
      ads.value.find(ad => ad.id === rewriteForm.adId) || null
    )

    const parseKeywords = (value) => {
      if (!value) return []
      return value.split(',').map(item => item.trim()).filter(Boolean)
    }

    const buildRewriteContext = (ad) => {
      const sections = []
      if (ad.primaryText) {
        sections.push(`Primary Text: ${ad.primaryText}`)
      }
      if (ad.headline) {
        sections.push(`Headline: ${ad.headline}`)
      }
      if (ad.description) {
        sections.push(`Description: ${ad.description}`)
      }
      if (rewriteForm.focus) {
        sections.push(`USP: ${rewriteForm.focus}`)
      }
      return sections.join('\n')
    }

    const generateRewrite = async () => {
      if (!rewriteForm.adId) {
        message.warning(t('optimizationLite.rewrite.noAd'))
        return
      }
      const ad = rewriteSelectedAd.value
      if (!ad) {
        message.error(t('optimizationLite.rewrite.noAd'))
        return
      }
      if (!ad.campaignId) {
        message.warning(t('optimizationLite.rewrite.missingCampaign'))
        return
      }

      rewriteLoading.value = true
      rewriteError.value = null
      try {
        const payload = {
          campaignId: ad.campaignId,
          adType: ad.adType || 'SINGLE_IMAGE',
          name: `${ad.name || 'Ad'} rewrite`,
          productDescription: buildRewriteContext(ad) || rewriteForm.guidance,
          referenceContent: rewriteForm.guidance || ad.primaryText || ad.description || '',
          creativeStyle: rewriteForm.tone,
          textProvider: rewriteForm.provider,
          numberOfVariations: rewriteForm.variations,
          language: locale.value,
          callToAction: ad.callToAction || null,
          trendingKeywords: parseKeywords(rewriteForm.keywords),
          websiteUrl: ad.websiteUrl || null
        }
        if (!payload.productDescription) {
          payload.productDescription = 'Rewrite this ad to be clearer and more persuasive.'
        }

        const response = await api.post('/ads/learn/generate', payload)
        if (response.data.status === 'success') {
          rewriteResults.value = (response.data.variations || []).map((variation, index) => ({
            id: variation.id || index,
            headline: variation.headline,
            primaryText: variation.primaryText,
            description: variation.description,
            callToAction: variation.callToAction
          }))
          message.success(t('optimizationLite.rewrite.success'))
        } else {
          throw new Error(response.data.message || t('optimizationLite.rewrite.error'))
        }
      } catch (err) {
        console.error(err)
        rewriteError.value = err?.message || t('optimizationLite.rewrite.error')
        message.error(rewriteError.value)
      } finally {
        rewriteLoading.value = false
      }
    }

    const copyVariation = async (variation) => {
      const content = [variation.headline, variation.primaryText, variation.description]
        .filter(Boolean)
        .join('\n\n')
      if (!content) return
      try {
        await navigator.clipboard.writeText(content)
        message.success(t('optimizationLite.rewrite.copied'))
      } catch (err) {
        console.error(err)
        message.error(t('optimizationLite.rewrite.error'))
      }
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

    watch(ads, (newAds) => {
      if (!rewriteForm.adId && newAds.length) {
        rewriteForm.adId = newAds[0].id
      }
    })

    watch(() => rewriteForm.adId, () => {
      rewriteResults.value = []
    })

    return {
      t,
      ads,
      adsLoading,
      filteredAds,
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
      rewriteForm,
      rewriteResults,
      rewriteLoading,
      rewriteError,
      generateRewrite,
      copyVariation,
      rewriteSelectedAd,
      toneOptions,
      lengthOptions,
      providerOptions
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

.rewrite-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rewrite-header {
  align-items: center;
}

.rewrite-form {
  margin-bottom: 8px;
}

.current-ad-context {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

.context-block {
  margin: 0;
  color: #1f2937;
}

.rewrite-results {
  margin-top: 12px;
}

.rewrite-result-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.rewrite-result-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rewrite-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rewrite-alert {
  margin-top: 12px;
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
