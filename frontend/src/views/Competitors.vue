<template>
  <div class="competitors-page">
    <a-card :title="$t('competitors.form.title')" :bordered="false" class="search-card">
      <a-form layout="vertical" @submit.prevent="handleSearch">
        <a-row :gutter="[12, 12]">
          <a-col :xs="24" :md="10">
            <a-form-item :label="$t('competitors.searchCard.keywordLabel')">
              <a-input
                v-model:value="searchForm.query"
                :placeholder="$t('competitors.searchCard.keywordPlaceholder')"
                size="large"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="7">
            <a-form-item :label="$t('competitors.searchCard.engineLabel')">
              <a-select v-model:value="searchForm.engine" size="large">
                <a-select-option value="google">{{ $t('competitors.platformLabels.google') }}</a-select-option>
                <a-select-option value="bing">{{ $t('competitors.platformLabels.bing') }}</a-select-option>
                <a-select-option value="youtube">YouTube</a-select-option>
                <a-select-option value="tiktok">TikTok</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="7">
            <a-form-item :label="$t('competitors.searchCard.locationLabel')">
              <a-select v-model:value="searchForm.location" size="large">
                <a-select-option
                  v-for="option in locationOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-space>
          <a-button type="primary" size="large" :loading="isSearching" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            {{ $t('competitors.buttons.search') }}
          </a-button>
          <a-button size="large" @click="clearSelection" :disabled="!hasSelectedAds">
            {{ $t('competitors.buttons.clearSelection') }}
          </a-button>
        </a-space>
      </a-form>
    </a-card>

    <div class="watchlist-section">
      <a-card :title="$t('competitors.watchlist.title')" :bordered="false">
        <p class="watchlist-subtitle">{{ $t('competitors.watchlist.subtitle') }}</p>
        <a-form layout="vertical" class="watchlist-form" @submit.prevent="handleAddWatchlist">
          <a-row :gutter="[8, 8]" align="bottom">
            <a-col :xs="24" :md="10">
              <a-form-item :label="$t('competitors.watchlist.brandLabel')">
                <a-input
                  v-model:value="watchlistForm.query"
                  :placeholder="$t('competitors.watchlist.brandPlaceholder')"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="4">
              <a-form-item :label="$t('competitors.watchlist.platformLabel')">
                <a-select v-model:value="watchlistForm.engine">
                  <a-select-option value="google">{{ $t('competitors.platformLabels.google') }}</a-select-option>
                  <a-select-option value="youtube">YouTube</a-select-option>
                  <a-select-option value="tiktok">TikTok</a-select-option>
                  <a-select-option value="bing">{{ $t('competitors.platformLabels.bing') }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="4">
              <a-form-item :label="$t('competitors.watchlist.regionLabel')">
                <a-select v-model:value="watchlistForm.location">
                  <a-select-option
                    v-for="option in locationOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="8" :md="3">
              <a-form-item :label="$t('competitors.watchlist.limitLabel')">
                <a-input-number v-model:value="watchlistForm.limit" :min="1" :max="50" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :xs="8" :md="3">
              <a-form-item label=" ">
                <a-button type="primary" block @click="handleAddWatchlist">
                  {{ $t('competitors.watchlist.actions.add') }}
                </a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
        <div class="watchlist-actions">
          <a-button :disabled="!watchlist.length" @click="handleRefreshAllWatchlist">
            {{ $t('competitors.watchlist.actions.refreshAll') }}
          </a-button>
        </div>
        <div v-if="watchlist.length" class="watchlist-grid">
          <div v-for="item in watchlist" :key="item.id" class="watchlist-card">
            <div class="watchlist-card-header">
              <h4>{{ item.query }}</h4>
              <a-tag color="blue">{{ item.engine }}</a-tag>
              <a-tag v-if="item.hasNew" color="green">{{ $t('competitors.watchlist.newLabel') }}</a-tag>
            </div>
            <p class="watchlist-meta">
              {{ getLocationLabel(item) }} ·
              {{ item.lastChecked ? formatWatchlistTimestamp(item.lastChecked) : $t('competitors.watchlist.neverChecked') }}
            </p>
            <p class="watchlist-count">
              {{ $t('competitors.watchlist.adsTracked', { count: item.lastResultCount ?? 0 }) }}
            </p>
            <p class="watchlist-hint" v-if="item.lastMessage">{{ item.lastMessage }}</p>
            <div class="watchlist-card-actions">
              <a-button type="link" size="small" :loading="isEntryRefreshing(item.id)" @click="handleRefreshWatchlist(item)">
                {{ $t('competitors.watchlist.actions.refresh') }}
              </a-button>
              <a-button type="link" size="small" danger @click="handleRemoveWatchlist(item)">
                {{ $t('competitors.watchlist.actions.remove') }}
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else :description="$t('competitors.watchlist.empty')" />
        <div v-if="watchlistActivity.length" class="watchlist-activity">
          <p class="eyebrow">{{ $t('competitors.watchlist.activityTitle') }}</p>
          <ul>
            <li v-for="activity in watchlistActivity" :key="activity.id">
              <strong>{{ activity.query }}</strong> · {{ activity.message }} ·
              <span class="watchlist-time">{{ formatWatchlistTimestamp(activity.timestamp) }}</span>
            </li>
          </ul>
        </div>
      </a-card>
    </div>

    <a-alert
      v-if="searchError"
      type="error"
      :message="searchError"
      show-icon
      class="mb-3"
    />

    <div v-if="isSearching" class="loading-section">
      <a-spin size="large" />
      <p>{{ $t('competitors.results.searching', { platform: searchForm.engine }) }}</p>
    </div>

    <div v-else>
      <div v-if="hasAds" class="results-section">
        <div class="section-header">
          <h3>{{ $t('competitors.sections.ads') }}</h3>
          <a-space>
            <a-button type="primary" :disabled="!hasSelectedAds" @click="showComparisonModal = true">
              {{ $t('competitors.compareSelected', { count: selectedCount }) }}
            </a-button>
            <a-button @click="clearSelection">
              {{ $t('competitors.buttons.clearSelection') }}
            </a-button>
          </a-space>
        </div>
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="ad in ads"
            :key="ad.id"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <a-card
              hoverable
              class="competitor-ad-card"
              :class="{ selected: isAdSelected(ad) }"
              @click="toggleAd(ad)"
            >
              <div class="ad-card-body">
                <h4>{{ ad.title }}</h4>
                <p class="ad-description">{{ ad.description }}</p>
                <p class="ad-link">{{ ad.displayed_link }}</p>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <div v-if="hasVideos" class="results-section">
        <div class="section-header">
          <h3>{{ $t('competitors.sections.videos') }}</h3>
        </div>
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="video in videos"
            :key="video.link"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <a-card hoverable>
              <img :src="video.thumbnail" class="video-thumb" />
              <h4>{{ video.title }}</h4>
              <p class="hint">{{ video.channel }} · {{ video.views }}</p>
              <a :href="video.link" target="_blank" rel="noopener">{{ $t('competitors.buttons.watch') }}</a>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <div v-if="hasContent" class="results-section">
        <div class="section-header">
          <h3>{{ $t('competitors.sections.tiktok') }}</h3>
        </div>
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="item in content"
            :key="item.link"
            :xs="24"
            :sm="12"
            :md="8"
          >
            <a-card hoverable>
              <img :src="item.thumbnail" class="video-thumb" />
              <h4>{{ item.title }}</h4>
              <p class="hint">{{ item.author }} · {{ item.stats }}</p>
              <a :href="item.link" target="_blank" rel="noopener">{{ $t('competitors.buttons.open') }}</a>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <a-empty
        v-if="!hasAds && !hasVideos && !hasContent && !searchError"
        :description="$t('competitors.noResults')"
      />
    </div>

    <a-modal
      v-model:open="showComparisonModal"
      :title="$t('competitors.modals.comparison.title')"
      width="80%"
      :footer="null"
    >
      <div class="comparison-content">
        <a-tabs v-model:activeKey="comparisonTab">
          <a-tab-pane key="compare" :tab="$t('competitors.modals.comparison.tabs.compare')">
            <a-form layout="vertical">
              <a-form-item :label="$t('competitors.modals.comparison.fields.myAd')">
                <a-textarea v-model:value="myAdText" rows="4" :placeholder="$t('competitors.modals.comparison.fields.placeholderMyAd')" />
              </a-form-item>

              <a-form-item :label="$t('competitors.modals.comparison.fields.selectAd')">
                <a-select v-model:value="selectedCompetitorId">
                  <a-select-option v-for="ad in selectedCompetitorAds" :key="ad.id" :value="ad.id">
                    {{ ad.title }}
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-button
                type="primary"
                :loading="isAnalyzing"
                :disabled="!selectedCompetitorId || !myAdText"
                @click="generateComparison"
              >
                {{ $t('competitors.modals.comparison.generate') }}
              </a-button>
            </a-form>

            <div v-if="aiSuggestion" class="ai-result-section">
              <pre>{{ aiSuggestion }}</pre>
            </div>
          </a-tab-pane>

          <a-tab-pane key="patterns" :tab="$t('competitors.modals.comparison.tabs.patterns')">
            <p>{{ $t('competitors.modals.comparison.patternsHint', { count: selectedCount }) }}</p>
            <a-button
              type="primary"
              :disabled="selectedCount < 2"
              :loading="isAnalyzing"
              @click="identifyPatterns"
            >
              {{ $t('competitors.modals.comparison.patternsButton') }}
            </a-button>
            <div v-if="patterns" class="ai-result-section">
              <pre>{{ patterns }}</pre>
            </div>
            <a-alert v-if="selectedCount < 2" type="info" :message="$t('competitors.modals.comparison.selectTwoNotice')" show-icon class="mt-4" />
          </a-tab-pane>

          <a-tab-pane key="abtest" :tab="$t('competitors.modals.comparison.tabs.abtest')">
            <a-form layout="vertical">
              <a-form-item :label="$t('competitors.modals.abTest.myAd')">
                <a-textarea v-model:value="myAdForABTest" rows="4" />
              </a-form-item>
              <a-form-item :label="$t('competitors.modals.abTest.competitorAd')">
                <a-select v-model:value="competitorForABTest">
                  <a-select-option v-for="ad in selectedCompetitorAds" :key="ad.id" :value="ad.id">
                    {{ ad.title }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item :label="$t('competitors.modals.abTest.variationCount')">
                <a-input-number v-model:value="abTestVariationCount" :min="1" :max="5" />
              </a-form-item>
              <a-button
                type="primary"
                :loading="isAnalyzing"
                :disabled="!myAdForABTest || !competitorForABTest"
                @click="generateABTest"
              >
                {{ $t('competitors.modals.abTest.button') }}
              </a-button>
            </a-form>
            <div v-if="abTestVariations.length" class="ai-result-section">
              <a-row :gutter="[16, 16]">
                <a-col v-for="(variation, index) in abTestVariations" :key="index" :xs="24" :md="12">
                  <a-card :title="$t('competitors.modals.abTest.variation', { index: index + 1 })">
                    <pre>{{ variation }}</pre>
                  </a-card>
                </a-col>
              </a-row>
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex'
import {
  SearchOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const LOCATION_PRESETS = [
  { key: 'global', gl: '', hl: '', labelKey: 'competitors.locations.global', fallback: 'Global' },
  { key: 'us', gl: 'us', hl: 'en', labelKey: 'competitors.locations.us', fallback: 'United States' },
  { key: 'gb', gl: 'gb', hl: 'en', labelKey: 'competitors.locations.gb', fallback: 'United Kingdom' },
  { key: 'vn', gl: 'vn', hl: 'vi', labelKey: 'competitors.locations.vn', fallback: 'Vietnam' },
  { key: 'au', gl: 'au', hl: 'en', labelKey: 'competitors.locations.au', fallback: 'Australia' },
  { key: 'de', gl: 'de', hl: 'de', labelKey: 'competitors.locations.de', fallback: 'Germany' },
  { key: 'fr', gl: 'fr', hl: 'fr', labelKey: 'competitors.locations.fr', fallback: 'France' },
  { key: 'jp', gl: 'jp', hl: 'ja', labelKey: 'competitors.locations.jp', fallback: 'Japan' },
  { key: 'sg', gl: 'sg', hl: 'en', labelKey: 'competitors.locations.sg', fallback: 'Singapore' }
]

export default {
  name: 'CompetitorsView',
  components: {
    SearchOutlined
  },
  data() {
    return {
      searchForm: {
        query: '',
        engine: 'google',
        location: 'global',
        limit: 10
      },
      watchlistForm: {
        query: '',
        engine: 'google',
        location: 'global',
        limit: 10
      },
      showComparisonModal: false,
      comparisonTab: 'compare',
      myAdText: '',
      selectedCompetitorId: null,
      myAdForABTest: '',
      competitorForABTest: null,
      abTestVariationCount: 3
    }
  },
  computed: {
    ...mapState('competitor', [
      'searchResults',
      'isSearching',
      'searchError',
      'selectedCompetitorAds',
      'aiSuggestion',
      'patterns',
      'abTestVariations',
      'isAnalyzing',
      'watchlist',
      'watchlistActivity'
    ]),
    ...mapGetters('competitor', [
      'ads',
      'videos',
      'content',
      'hasAds',
      'hasVideos',
      'hasContent',
      'hasSelectedAds',
      'selectedCount',
      'isWatchlistRefreshing'
    ]),
    locationOptions() {
      return LOCATION_PRESETS.map(preset => ({
        value: preset.key,
        label: this.$te(preset.labelKey) ? this.$t(preset.labelKey) : preset.fallback
      }))
    }
  },
  created() {
    this.initWatchlist()
  },
  methods: {
    ...mapActions('competitor', {
      searchAcrossWeb: 'searchAcrossWeb',
      toggleAdSelection: 'toggleAdSelection',
      clearSelectionAction: 'clearSelection',
      generateSuggestionAction: 'generateSuggestion',
      identifyPatternsAction: 'identifyPatterns',
      generateABTestAction: 'generateABTest',
      addWatchlistItemAction: 'addWatchlistItem',
      removeWatchlistItemAction: 'removeWatchlistItem',
      refreshWatchlistItemAction: 'refreshWatchlistItem',
      refreshAllWatchlistAction: 'refreshAllWatchlist',
      initWatchlist: 'initWatchlist'
    }),
    resolveLocationPreset(key) {
      return LOCATION_PRESETS.find(item => item.key === key) || LOCATION_PRESETS[0]
    },
    buildLocationPayload(key) {
      const preset = this.resolveLocationPreset(key)
      const label = this.$te(preset.labelKey) ? this.$t(preset.labelKey) : preset.fallback
      return { ...preset, label }
    },
    getLocationLabel(item) {
      const key = item.locationKey || item.location || 'global'
      const preset = this.resolveLocationPreset(key)
      return this.$te(preset.labelKey) ? this.$t(preset.labelKey) : (item.location || preset.fallback)
    },
    async handleSearch() {
      if (!this.searchForm.query.trim()) {
        message.warning(this.$t('competitors.messages.keywordRequired'))
        return
      }
      try {
        const preset = this.buildLocationPayload(this.searchForm.location)
        await this.searchAcrossWeb({
          query: this.searchForm.query.trim(),
          engine: this.searchForm.engine,
          location: preset.label,
          gl: preset.gl,
          hl: preset.hl,
          limit: this.searchForm.limit
        })
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.searchFailed'))
      }
    },
    toggleAd(ad) {
      this.toggleAdSelection(ad)
    },
    clearSelection() {
      this.clearSelectionAction()
      this.selectedCompetitorId = null
      this.competitorForABTest = null
    },
    async generateComparison() {
      const competitor = this.selectedCompetitorAds.find(ad => ad.id === this.selectedCompetitorId)
      if (!competitor) {
        message.warning(this.$t('competitors.messages.selectCompetitor'))
        return
      }
      try {
        await this.generateSuggestionAction({
          competitorAd: competitor,
          myAd: this.myAdText
        })
        message.success(this.$t('competitors.messages.suggestionGenerated'))
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.actionFailed'))
      }
    },
    async identifyPatterns() {
      try {
        await this.identifyPatternsAction({
          competitorAds: this.selectedCompetitorAds
        })
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.actionFailed'))
      }
    },
    async generateABTest() {
      const competitor = this.selectedCompetitorAds.find(ad => ad.id === this.competitorForABTest)
      if (!competitor) {
        message.warning(this.$t('competitors.messages.selectCompetitor'))
        return
      }
      try {
        await this.generateABTestAction({
          competitorAd: competitor,
          myAd: this.myAdForABTest,
          variationCount: this.abTestVariationCount
        })
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.actionFailed'))
      }
    },
    async handleAddWatchlist() {
      if (!this.watchlistForm.query.trim()) {
        message.warning(this.$t('competitors.messages.watchlistKeywordRequired'))
        return
      }
      try {
        const preset = this.buildLocationPayload(this.watchlistForm.location)
        await this.addWatchlistItemAction({
          query: this.watchlistForm.query.trim(),
          engine: this.watchlistForm.engine,
          locationKey: preset.key,
          locationLabel: preset.label,
          gl: preset.gl,
          hl: preset.hl,
          limit: this.watchlistForm.limit
        })
        this.watchlistForm.query = ''
        message.success(this.$t('competitors.messages.watchlistAdded'))
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.actionFailed'))
      }
    },
    handleRemoveWatchlist(item) {
      this.removeWatchlistItemAction(item.id)
      message.info(this.$t('competitors.messages.watchlistRemoved'))
    },
    handleRefreshWatchlist(item) {
      this.refreshWatchlistItemAction(item).catch(() => {
        message.error(this.$t('competitors.messages.actionFailed'))
      })
    },
    handleRefreshAllWatchlist() {
      this.refreshAllWatchlistAction()
    }
  }
}
</script>

<style scoped>
.competitors-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.search-card {
  background: #fff;
}

.results-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.competitor-ad-card {
  cursor: pointer;
}

.competitor-ad-card.selected {
  border: 1px solid #52c41a;
}

.ad-card-body h4 {
  margin-bottom: 8px;
}

.ad-description {
  color: #555;
}

.video-thumb {
  width: 100%;
  border-radius: 6px;
  margin-bottom: 8px;
}

.watchlist-section {
  margin-top: 4px;
}

.watchlist-form {
  margin-bottom: 12px;
}

.watchlist-subtitle {
  margin-top: -8px;
  margin-bottom: 12px;
  color: #6b7280;
}

.watchlist-actions {
  text-align: right;
  margin-bottom: 12px;
}

.watchlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.watchlist-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.watchlist-card-header {
  display: flex;
  gap: 6px;
  align-items: center;
}

.watchlist-meta,
.watchlist-hint {
  color: #6b7280;
}

.watchlist-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.loading-section {
  text-align: center;
  padding: 40px 0;
}

.ai-result-section {
  margin-top: 16px;
  background: #fafafa;
  padding: 12px;
  border-radius: 8px;
}

@media (max-width: 768px) {
  .competitors-page {
    padding: 12px;
  }
}
</style>
