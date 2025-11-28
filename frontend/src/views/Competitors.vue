<template>
  <div class="competitors-page">
    <a-card title="Search Competitor Footprint" :bordered="false" class="search-card">
      <a-form layout="vertical" @submit.prevent="handleSearch">
        <a-row :gutter="[12, 12]">
          <a-col :xs="24" :md="10">
            <a-form-item label="Keyword / Brand">
              <a-input
                v-model:value="searchForm.query"
                placeholder="e.g. vinfast electric car"
                size="large"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="5">
            <a-form-item label="Engine">
              <a-select v-model:value="searchForm.engine" size="large">
                <a-select-option value="google">Google Ads</a-select-option>
                <a-select-option value="bing">Bing Ads</a-select-option>
                <a-select-option value="youtube">YouTube</a-select-option>
                <a-select-option value="tiktok">TikTok</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="3">
            <a-form-item label="Location">
              <a-input v-model:value="searchForm.location" size="large" placeholder="United States" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="3">
            <a-form-item label="gl">
              <a-input v-model:value="searchForm.gl" size="large" placeholder="US" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="3">
            <a-form-item label="hl">
              <a-input v-model:value="searchForm.hl" size="large" placeholder="en" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-space>
          <a-button type="primary" size="large" :loading="isSearching" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            Search
          </a-button>
          <a-button size="large" @click="clearSelection" :disabled="!hasSelectedAds">
            Clear Selection
          </a-button>
        </a-space>
      </a-form>
    </a-card>

    <div class="watchlist-section">
      <a-card title="Competitor Watchlist" :bordered="false">
        <a-form layout="vertical" class="watchlist-form" @submit.prevent="handleAddWatchlist">
          <a-row :gutter="[8, 8]" align="bottom">
            <a-col :xs="24" :md="10">
              <a-form-item label="Query">
                <a-input v-model:value="watchlistForm.query" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="4">
              <a-form-item label="Engine">
                <a-select v-model:value="watchlistForm.engine">
                  <a-select-option value="google">Google</a-select-option>
                  <a-select-option value="youtube">YouTube</a-select-option>
                  <a-select-option value="tiktok">TikTok</a-select-option>
                  <a-select-option value="bing">Bing</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="4">
              <a-form-item label="Location">
                <a-input v-model:value="watchlistForm.location" />
              </a-form-item>
            </a-col>
            <a-col :xs="8" :md="2">
              <a-form-item label="gl">
                <a-input v-model:value="watchlistForm.gl" />
              </a-form-item>
            </a-col>
            <a-col :xs="8" :md="2">
              <a-form-item label="hl">
                <a-input v-model:value="watchlistForm.hl" />
              </a-form-item>
            </a-col>
            <a-col :xs="8" :md="2">
              <a-form-item label=" ">
                <a-button type="primary" block @click="handleAddWatchlist">Add</a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
        <div class="watchlist-actions">
          <a-button :disabled="!watchlist.length" @click="handleRefreshAllWatchlist">
            Refresh all
          </a-button>
        </div>
        <div v-if="watchlist.length" class="watchlist-grid">
          <div v-for="item in watchlist" :key="item.id" class="watchlist-card">
            <div class="watchlist-card-header">
              <h4>{{ item.query }}</h4>
              <a-tag color="blue">{{ item.engine }}</a-tag>
              <a-tag v-if="item.hasNew" color="green">New</a-tag>
            </div>
            <p class="watchlist-meta">
              {{ item.location || 'global' }} ·
              {{ item.lastChecked ? formatWatchlistTimestamp(item.lastChecked) : 'Never checked' }}
            </p>
            <p class="watchlist-count">{{ item.lastResultCount ?? 0 }} results</p>
            <p class="watchlist-hint" v-if="item.lastMessage">{{ item.lastMessage }}</p>
            <div class="watchlist-card-actions">
              <a-button type="link" size="small" :loading="isEntryRefreshing(item.id)" @click="handleRefreshWatchlist(item)">
                Refresh
              </a-button>
              <a-button type="link" size="small" danger @click="handleRemoveWatchlist(item)">
                Remove
              </a-button>
            </div>
          </div>
        </div>
        <a-empty v-else description="No brands in watchlist" />
        <div v-if="watchlistActivity.length" class="watchlist-activity">
          <p class="eyebrow">Recent activity</p>
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
      <p>Searching {{ searchForm.engine }}...</p>
    </div>

    <div v-else>
      <div v-if="hasAds" class="results-section">
        <div class="section-header">
          <h3>Ad Results</h3>
          <a-space>
            <a-button type="primary" :disabled="!hasSelectedAds" @click="showComparisonModal = true">
              Compare selected ({{ selectedCount }})
            </a-button>
            <a-button @click="clearSelection">Clear selection</a-button>
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
          <h3>Video Results</h3>
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
              <a :href="video.link" target="_blank" rel="noopener">Watch</a>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <div v-if="hasContent" class="results-section">
        <div class="section-header">
          <h3>TikTok Content</h3>
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
              <a :href="item.link" target="_blank" rel="noopener">Open</a>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <a-empty v-if="!hasAds && !hasVideos && !hasContent && !searchError" description="No data yet. Try another query." />
    </div>

    <!-- Comparison Modal -->
    <a-modal
      v-model:open="showComparisonModal"
      title="AI-Powered Comparison"
      width="80%"
      :footer="null"
    >
      <div class="comparison-content">
        <a-tabs v-model:activeKey="comparisonTab">
          <a-tab-pane tab="Compare with my ad" key="compare">
            <a-form layout="vertical">
              <a-form-item label="Your current ad">
                <a-textarea v-model:value="myAdText" rows="4" />
              </a-form-item>
              <a-form-item label="Competitor ad">
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
                Generate suggestion
              </a-button>
            </a-form>
            <div v-if="aiSuggestion" class="ai-result-section">
              <pre>{{ aiSuggestion }}</pre>
            </div>
          </a-tab-pane>
          <a-tab-pane tab="Identify patterns" key="patterns">
            <p>Select at least two ads to identify patterns.</p>
            <a-button
              type="primary"
              :disabled="selectedCount < 2"
              :loading="isAnalyzing"
              @click="identifyPatterns"
            >
              Analyze patterns
            </a-button>
            <div v-if="patterns" class="ai-result-section">
              <pre>{{ patterns }}</pre>
            </div>
          </a-tab-pane>
          <a-tab-pane tab="A/B test ideas" key="abtest">
            <a-form layout="vertical">
              <a-form-item label="Your current ad">
                <a-textarea v-model:value="myAdForABTest" rows="4" />
              </a-form-item>
              <a-form-item label="Competitor reference">
                <a-select v-model:value="competitorForABTest">
                  <a-select-option v-for="ad in selectedCompetitorAds" :key="ad.id" :value="ad.id">
                    {{ ad.title }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="Variations">
                <a-input-number v-model:value="abTestVariationCount" :min="1" :max="5" />
              </a-form-item>
              <a-button
                type="primary"
                :loading="isAnalyzing"
                :disabled="!myAdForABTest || !competitorForABTest"
                @click="generateABTest"
              >
                Generate variations
              </a-button>
            </a-form>
            <div v-if="abTestVariations.length" class="ai-result-section">
              <a-row :gutter="[16, 16]">
                <a-col v-for="(variation, index) in abTestVariations" :key="index" :xs="24" :md="12">
                  <a-card :title="`Variation ${index + 1}`">
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
        location: '',
        gl: '',
        hl: '',
        limit: 10
      },
      watchlistForm: {
        query: '',
        engine: 'google',
        location: '',
        gl: '',
        hl: '',
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
    ])
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
    async handleSearch() {
      if (!this.searchForm.query.trim()) {
        message.warning('Please enter a keyword')
        return
      }
      try {
        await this.searchAcrossWeb({ ...this.searchForm })
      } catch (error) {
        message.error(error?.message || 'Search failed')
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
        message.warning('Select a competitor ad')
        return
      }
      try {
        await this.generateSuggestionAction({
          competitorAd: competitor,
          myAd: this.myAdText
        })
        message.success('Suggestion generated')
      } catch (error) {
        message.error(error?.message || 'Unable to generate suggestion')
      }
    },
    async identifyPatterns() {
      try {
        await this.identifyPatternsAction({
          competitorAds: this.selectedCompetitorAds
        })
      } catch (error) {
        message.error(error?.message || 'Unable to identify patterns')
      }
    },
    async generateABTest() {
      const competitor = this.selectedCompetitorAds.find(ad => ad.id === this.competitorForABTest)
      if (!competitor) {
        message.warning('Select a competitor ad')
        return
      }
      try {
        await this.generateABTestAction({
          competitorAd: competitor,
          myAd: this.myAdForABTest,
          variationCount: this.abTestVariationCount
        })
      } catch (error) {
        message.error(error?.message || 'Unable to generate variations')
      }
    },
    isAdSelected(ad) {
      return this.selectedCompetitorAds.some(item => item.id === ad.id)
    },
    async handleAddWatchlist() {
      if (!this.watchlistForm.query.trim()) {
        message.warning('Enter query')
        return
      }
      try {
        await this.addWatchlistItemAction({ ...this.watchlistForm })
        this.watchlistForm.query = ''
        message.success('Added to watchlist')
      } catch (error) {
        message.error(error?.message || 'Failed to add watchlist item')
      }
    },
    handleRemoveWatchlist(item) {
      this.removeWatchlistItemAction(item.id)
      message.info('Removed')
    },
    handleRefreshWatchlist(item) {
      this.refreshWatchlistItemAction(item).catch(() => {
        message.error('Failed to refresh item')
      })
    },
    handleRefreshAllWatchlist() {
      this.refreshAllWatchlistAction()
    },
    formatWatchlistTimestamp(value) {
      if (!value) return ''
      return new Intl.DateTimeFormat(undefined, {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(value))
    },
    isEntryRefreshing(id) {
      return this.isWatchlistRefreshing(id)
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
