<template>
  <div class="competitors-view">
    <section class="hero-card surface-card">
      <div class="hero-text">
        <p class="eyebrow">{{ $t('competitors.eyebrow') }}</p>
        <h1>{{ $t('competitors.title') }}</h1>
        <p class="hero-description">{{ $t('competitors.hero.description') }}</p>
        <div class="hero-actions">
          <a-radio-group
            v-model:value="selectedPlatform"
            button-style="solid"
            size="large"
            @change="handlePlatformChange"
          >
            <a-radio-button value="facebook">
              <facebook-outlined /> {{ $t('competitors.platformLabels.facebook') }}
            </a-radio-button>
            <a-radio-button value="google">
              <google-outlined /> {{ $t('competitors.platformLabels.google') }}
            </a-radio-button>
            <a-radio-button value="tiktok">
              <play-circle-outlined /> {{ $t('competitors.platformLabels.tiktok') }}
            </a-radio-button>
          </a-radio-group>
        </div>
      </div>
      <div class="hero-meta">
        <div class="hero-stat">
          <p class="hero-stat-label">{{ $t('competitors.hero.watchlistLabel') }}</p>
          <p class="hero-stat-value">{{ watchlist.length }}</p>
        </div>
        <div class="hero-stat">
          <p class="hero-stat-label">{{ $t('competitors.hero.platformLabel') }}</p>
          <p class="hero-stat-value">{{ getPlatformLabel(selectedPlatform) }}</p>
        </div>
      </div>
    </section>

    <section class="surface-card section-card">
      <div class="section-heading">
        <div>
          <h2>{{ $t('competitors.form.title') }}</h2>
          <p class="section-subtitle">{{ $t('competitors.form.description') }}</p>
        </div>
      </div>
      <a-form layout="vertical" @submit.prevent="handleSearch">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :md="12">
            <a-form-item :label="$t('competitors.form.brandLabel')">
              <a-input
                v-model:value="searchForm.brandName"
                :placeholder="$t('competitors.form.brandPlaceholder')"
                :maxlength="100"
                show-count
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="6">
            <a-form-item :label="$t('competitors.form.regionLabel')">
              <a-select
                v-model:value="searchForm.region"
                :placeholder="$t('competitors.selectRegion')"
                show-search
                option-filter-prop="label"
              >
                <a-select-opt-group :label="$t('competitors.form.regionGroups.americas')">
                  <a-select-option value="US" label="United States">{{ $t('competitors.region.US') }}</a-select-option>
                  <a-select-option value="CA" label="Canada">{{ $t('competitors.region.CA') }}</a-select-option>
                  <a-select-option value="MX" label="Mexico">{{ $t('competitors.region.MX') }}</a-select-option>
                  <a-select-option value="BR" label="Brazil">{{ $t('competitors.region.BR') }}</a-select-option>
                </a-select-opt-group>
                <a-select-opt-group :label="$t('competitors.form.regionGroups.europe')">
                  <a-select-option value="GB" label="United Kingdom">{{ $t('competitors.region.GB') }}</a-select-option>
                  <a-select-option value="DE" label="Germany">{{ $t('competitors.region.DE') }}</a-select-option>
                  <a-select-option value="FR" label="France">{{ $t('competitors.region.FR') }}</a-select-option>
                  <a-select-option value="IT" label="Italy">{{ $t('competitors.region.IT') }}</a-select-option>
                </a-select-opt-group>
                <a-select-opt-group :label="$t('competitors.form.regionGroups.oceania')">
                  <a-select-option value="AU" label="Australia">{{ $t('competitors.region.AU') }}</a-select-option>
                  <a-select-option value="NZ" label="New Zealand">{{ $t('competitors.region.NZ') }}</a-select-option>
                </a-select-opt-group>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="6">
            <a-form-item :label="$t('competitors.maxResults')">
              <a-input-number
                v-model:value="searchForm.limit"
                :min="1"
                :max="50"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="isSearching"
            :disabled="!searchForm.brandName"
          >
            <template #icon><search-outlined /></template>
            {{ $t('competitors.form.searchButton') }}
          </a-button>
        </a-form-item>
      </a-form>
    </section>

    <section class="surface-card section-card">
      <div class="section-heading">
        <div>
          <h2>{{ $t('competitors.watchlist.title') }}</h2>
          <p class="section-subtitle">{{ $t('competitors.watchlist.subtitle') }}</p>
        </div>
        <a-button type="default" @click="handleRefreshAllWatchlist" :disabled="!watchlist.length">
          {{ $t('competitors.watchlist.actions.refreshAll') }}
        </a-button>
      </div>
      <a-form layout="vertical" class="watchlist-form">
        <a-row :gutter="[16, 16]" align="bottom">
          <a-col :xs="24" :md="10">
            <a-form-item :label="$t('competitors.watchlist.brandLabel')">
              <a-input v-model:value="watchlistForm.brandName" :placeholder="$t('competitors.watchlist.brandPlaceholder')" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="4">
            <a-form-item :label="$t('competitors.watchlist.platformLabel')">
              <a-select v-model:value="watchlistForm.platform">
                <a-select-option value="facebook">{{ $t('competitors.platformLabels.facebook') }}</a-select-option>
                <a-select-option value="google">{{ $t('competitors.platformLabels.google') }}</a-select-option>
                <a-select-option value="tiktok">{{ $t('competitors.platformLabels.tiktok') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="4">
            <a-form-item :label="$t('competitors.watchlist.regionLabel')">
              <a-input v-model:value="watchlistForm.region" :placeholder="$t('competitors.watchlist.regionPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="3">
            <a-form-item :label="$t('competitors.watchlist.limitLabel')">
              <a-input-number v-model:value="watchlistForm.limit" :min="1" :max="50" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="3">
            <a-form-item label=" ">
              <a-button type="primary" block @click="handleAddWatchlist">
                {{ $t('competitors.watchlist.actions.add') }}
              </a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div v-if="watchlist.length" class="watchlist-grid">
        <article v-for="item in watchlist" :key="item.id" class="watchlist-card surface-card">
          <div class="watchlist-card-header">
            <div>
              <h4>{{ item.brandName }}</h4>
              <p class="watchlist-meta">
                {{ getRegionLabel(item.region) }} · {{ item.lastChecked ? formatWatchlistTimestamp(item.lastChecked) : $t('competitors.watchlist.neverChecked') }}
              </p>
            </div>
            <div>
              <a-tag color="blue">{{ getPlatformLabel(item.platform) }}</a-tag>
              <a-tag v-if="item.hasNew" color="green">{{ $t('competitors.watchlist.newLabel') }}</a-tag>
            </div>
          </div>
          <p class="watchlist-count">
            {{ $t('competitors.watchlist.adsTracked', { count: item.lastResultCount ?? 0 }) }}
          </p>
          <p class="watchlist-hint" v-if="item.lastMessage">{{ item.lastMessage }}</p>
          <div class="watchlist-card-actions">
            <a-button
              size="small"
              type="link"
              @click="handleRefreshWatchlist(item)"
              :loading="isEntryRefreshing(item.id)"
            >
              {{ $t('competitors.watchlist.actions.refresh') }}
            </a-button>
            <a-button size="small" type="link" danger @click="handleRemoveWatchlist(item)">
              {{ $t('competitors.watchlist.actions.remove') }}
            </a-button>
          </div>
        </article>
      </div>
      <a-empty v-else :description="$t('competitors.watchlist.empty')" />

      <div v-if="watchlistActivity.length" class="watchlist-activity">
        <p class="eyebrow">{{ $t('competitors.watchlist.activityTitle') }}</p>
        <ul>
          <li v-for="activity in watchlistActivity" :key="activity.id">
            <strong>{{ activity.brandName }}</strong> · {{ activity.message }} ·
            <span class="watchlist-time">{{ formatWatchlistTimestamp(activity.timestamp) }}</span>
          </li>
        </ul>
      </div>
    </section>

    <section v-if="recentPlatformStatuses.length" class="status-feed surface-card">
      <a-alert
        v-for="status in recentPlatformStatuses"
        :key="status.id"
        :type="status.success ? 'success' : status.mode === 'iframe' ? 'info' : status.mode === 'error' ? 'error' : 'warning'"
        :message="getStatusMessage(status)"
        :description="getStatusDescription(status)"
        show-icon
      />
    </section>

    <section v-if="hasSearchResults || displayMode !== 'empty'" class="surface-card section-card">
        <template v-if="displayMode === 'data' && searchResults.length > 0" #title>
          <div class="section-heading">
            <div>
              <h2>{{ $t('competitors.results.title', { count: searchResults.length, platform: platformName }) }}</h2>
              <p class="section-subtitle">{{ $t('competitors.results.subtitle') }}</p>
            </div>
            <div class="section-controls">
              <a-button
                v-if="hasSelectedAds"
                type="primary"
                @click="showComparisonModal = true"
              >
                <template #icon><comparison-outlined /></template>
                {{ $t('competitors.compareSelected', { count: selectedCount }) }}
              </a-button>
              <a-button @click="exportResults">
                <template #icon><export-outlined /></template>
                {{ $t('competitors.results.export') }}
              </a-button>
              <a-button @click="clearSelection">{{ $t('competitors.clearSelection') }}</a-button>
            </div>
          </div>
        </template>

        <!-- Data Mode - Structured Results -->
        <div v-if="displayMode === 'data' && searchResults.length > 0">

          <!-- Competitor Ads Grid -->
          <a-row :gutter="[16, 16]">
            <a-col
              v-for="ad in searchResults"
              :key="ad.adId || ad.adLibraryId"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
            >
              <a-card
                hoverable
                class="competitor-ad-card surface-card"
                :class="{ 'selected': isAdSelected(ad) }"
                @click="toggleAdSelection(ad)"
              >
                <template #extra>
                  <a-tag :color="getPlatformColor(ad.dataSource || selectedPlatform)">
                    {{ getPlatformLabel(ad.dataSource || selectedPlatform) }}
                  </a-tag>
                </template>

                <!-- Ad Image -->
                <div class="ad-image-container">
                  <img
                    v-if="getAdImage(ad)"
                    :src="getAdImage(ad)"
                    :alt="ad.headline || 'Competitor Ad'"
                    class="ad-image"
                  />
                  <div v-else class="no-image-placeholder">
                    <file-image-outlined style="font-size: 48px; color: #d9d9d9" />
                  </div>

                  <!-- Selection Indicator -->
                  <div v-if="isAdSelected(ad)" class="selection-badge">
                    <check-circle-filled style="color: #52c41a; font-size: 24px" />
                  </div>
                </div>

                <!-- Ad Content -->
                <div class="ad-content">
                  <h4 class="ad-headline">{{ ad.headline || 'No Headline' }}</h4>
                  <p class="ad-description">{{ truncateText(ad.primaryText || ad.description, 100) }}</p>

                  <div class="ad-metadata">
                    <a-tag v-if="ad.advertiserName" color="purple">{{ ad.advertiserName }}</a-tag>
                    <a-tag v-if="ad.callToAction" color="green">{{ ad.callToAction }}</a-tag>
                  </div>

                  <!-- Ad Actions -->
                  <div class="ad-actions">
                    <a-button size="small" type="link" @click.stop="viewAdDetails(ad)">
                      <eye-outlined /> View Details
                    </a-button>
                    <a-button size="small" type="link" @click.stop="analyzeAd(ad)">
                      <bulb-outlined /> Analyze
                    </a-button>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </div>

        <!-- Iframe Mode - Embedded View -->
        <div v-else-if="displayMode === 'iframe'" class="iframe-container">


          <!-- Quick Search Bar for Iframe Mode -->
          <div class="iframe-search-bar">
            <a-input-group compact>
              <a-input
                v-model:value="iframeSearchQuery"
                :placeholder="$t('competitors.iframe.searchPlaceholder')"
                size="large"
                style="width: calc(100% - 180px)"
                allow-clear
                @pressEnter="updateIframeSearch"
              />
              <a-button
                type="primary"
                size="large"
                @click="updateIframeSearch"
                :disabled="!iframeSearchQuery"
              >
                <template #icon><search-outlined /></template>
                {{ $t('competitors.iframe.searchButton') }}
              </a-button>
            </a-input-group>
          </div>

          <iframe
            v-if="currentIframeUrl"
            :src="currentIframeUrl"
            width="100%"
            height="800px"
            frameborder="0"
            sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
            style="border: 1px solid #d9d9d9; border-radius: 4px;"
          />
          <a-empty v-else :description="$t('competitors.iframe.notAvailable')" />

          <a-space style="margin-top: 16px">
            <a-button @click="openInNewTab" type="primary">
              <link-outlined /> {{ $t('competitors.iframe.openNewTab') }}
            </a-button>
            <a-button v-if="selectedPlatform === 'google'" @click="retryWithDataMode">
              <database-outlined /> {{ $t('competitors.iframe.retryDataMode') }}
            </a-button>
          </a-space>
        </div>

        <!-- Loading State -->
        <div v-else-if="isSearching" style="text-align: center; padding: 40px;">
          <a-spin size="large" />
          <p style="margin-top: 16px;">{{ $t('competitors.results.searching', { platform: platformName }) }}</p>
        </div>

        <!-- Empty State -->
        <a-empty v-else :description="$t('competitors.results.empty')" />
    </section>

    <!-- Empty State -->
    <div v-if="!hasSearchResults && !isSearching && displayMode === 'empty'" class="empty-state surface-card">
      <a-empty :description="$t('competitors.emptyPrompt')">
        <template #image>
          <search-outlined style="font-size: 64px; color: #d9d9d9" />
        </template>
      </a-empty>
    </div>

    <!-- Comparison Modal -->
    <a-modal
      v-model:open="showComparisonModal"
      :title="$t('competitors.modals.comparison.title')"
      width="90%"
      :footer="null"
      :destroy-on-close="true"
      wrap-class-name="surface-modal"
      :body-style="{ padding: 0 }"
    >
      <div class="modal-content">
        <a-tabs v-model:activeKey="comparisonTab">
          <!-- Compare with My Ad -->
          <a-tab-pane key="compare" :tab="$t('competitors.modals.comparison.tabs.compare')">
            <a-form layout="vertical">
              <a-form-item :label="$t('competitors.modals.comparison.fields.myAd')">
                <a-textarea
                  v-model:value="myAdText"
                  :rows="4"
                  :placeholder="$t('competitors.modals.comparison.fields.placeholderMyAd')"
                />
              </a-form-item>

              <a-form-item :label="$t('competitors.modals.comparison.fields.selectAd')">
                <a-select
                  v-model:value="selectedCompetitorForComparison"
                  size="large"
                  :placeholder="$t('competitors.modals.comparison.fields.placeholderSelect')"
                  style="width: 100%"
                >
                  <a-select-option
                    v-for="ad in selectedCompetitorAds"
                    :key="ad.adLibraryId"
                    :value="ad.adLibraryId"
                  >
                    {{ ad.headline || $t('competitors.modals.details.notAvailable') }}
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item>
                <a-button
                  type="primary"
                  :loading="isAnalyzing"
                  @click="generateComparison"
                  :disabled="!myAdText || !selectedCompetitorForComparison"
                >
                  <template #icon><bulb-outlined /></template>
                  {{ $t('competitors.modals.comparison.generate') }}
                </a-button>
              </a-form-item>
            </a-form>

            <!-- AI Suggestion Result -->
            <div v-if="aiSuggestion" class="ai-result-section">
              <a-alert type="success" :message="$t('competitors.modals.comparison.suggestionReady')" show-icon class="mb-4" />
              <div class="suggestion-content" v-html="formatAISuggestion(aiSuggestion)"></div>
            </div>
          </a-tab-pane>

          <!-- Identify Patterns -->
          <a-tab-pane key="patterns" :tab="$t('competitors.modals.comparison.tabs.patterns')">
            <p class="section-subtitle">
              {{ $t('competitors.modals.comparison.patternsHint', { count: selectedCount }) }}
            </p>

            <a-button
              type="primary"
              :loading="isAnalyzing"
              @click="identifyPatterns"
              :disabled="selectedCount < 2"
            >
              <template #icon><fund-outlined /></template>
              {{ $t('competitors.modals.comparison.patternsButton') }}
            </a-button>

            <!-- Patterns Result -->
            <div v-if="patterns" class="ai-result-section mt-4">
              <a-alert type="success" :message="$t('competitors.modals.comparison.patternsReady')" show-icon class="mb-4" />
              <div class="suggestion-content" v-html="formatPatterns(patterns)"></div>
            </div>

            <a-alert v-if="selectedCount < 2" type="info" :message="$t('competitors.modals.comparison.selectTwoNotice')" show-icon class="mt-4" />
          </a-tab-pane>

          <!-- A/B Test Variations -->
          <a-tab-pane key="abtest" :tab="$t('competitors.modals.comparison.tabs.abtest')">
            <a-form layout="vertical">
              <a-form-item :label="$t('competitors.modals.abTest.myAd')">
                <a-textarea
                  v-model:value="myAdForABTest"
                  :rows="4"
                  :placeholder="$t('competitors.modals.comparison.fields.placeholderMyAd')"
                />
              </a-form-item>

              <a-form-item :label="$t('competitors.modals.abTest.competitorAd')">
                <a-select
                  v-model:value="competitorForABTest"
                  size="large"
                  :placeholder="$t('competitors.modals.comparison.fields.placeholderSelect')"
                  style="width: 100%"
                >
                  <a-select-option
                    v-for="ad in selectedCompetitorAds"
                    :key="ad.adLibraryId"
                    :value="ad.adLibraryId"
                  >
                    {{ ad.headline || $t('competitors.modals.details.notAvailable') }}
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item :label="$t('competitors.modals.abTest.variationCount')">
                <a-input-number
                  v-model:value="abTestVariationCount"
                  :min="1"
                  :max="5"
                  size="large"
                />
              </a-form-item>

              <a-button
                type="primary"
                :loading="isAnalyzing"
                @click="generateABTest"
                :disabled="!myAdForABTest || !competitorForABTest"
              >
                <template #icon><experiment-outlined /></template>
                {{ $t('competitors.modals.abTest.button') }}
              </a-button>
            </a-form>

            <!-- A/B Test Results -->
            <div v-if="abTestVariations.length > 0" class="ai-result-section mt-4">
              <a-alert type="success" :message="$t('competitors.modals.abTest.ready')" show-icon class="mb-4" />
              <a-row :gutter="[16, 16]">
                <a-col
                  v-for="(variation, index) in abTestVariations"
                  :key="index"
                  :xs="24"
                  :sm="12"
                >
                  <a-card :title="$t('competitors.modals.abTest.variation', { index: index + 1 })" size="small">
                    <p>{{ variation }}</p>
                  </a-card>
                </a-col>
              </a-row>
            </div>
          </a-tab-pane>
        </a-tabs>

        <!-- Analysis Error -->
        <a-alert
          v-if="analysisError"
          type="error"
          :message="analysisError"
          show-icon
          closable
          @close="clearErrors"
          class="mt-4"
        />
      </div>
    </a-modal>

    <!-- Ad Details Modal -->
    <a-modal
      v-model:open="showDetailsModal"
      :title="selectedAdForDetails?.headline || $t('competitors.modals.details.title')"
      width="800px"
      :footer="null"
      wrap-class-name="surface-modal"
      :body-style="{ padding: 0 }"
    >
      <div v-if="selectedAdForDetails" class="modal-content ad-details">
        <div v-if="selectedAdForDetails.imageUrl" class="mb-4">
          <img :src="selectedAdForDetails.imageUrl" alt="Ad" class="w-full rounded" />
        </div>

        <a-descriptions bordered :column="1">
          <a-descriptions-item :label="$t('competitors.modals.details.labels.headline')">
            {{ selectedAdForDetails.headline || $t('competitors.modals.details.notAvailable') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('competitors.modals.details.labels.primaryText')">
            {{ selectedAdForDetails.primaryText || $t('competitors.modals.details.notAvailable') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('competitors.modals.details.labels.description')">
            {{ selectedAdForDetails.description || $t('competitors.modals.details.notAvailable') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('competitors.modals.details.labels.callToAction')">
            {{ selectedAdForDetails.callToAction || $t('competitors.modals.details.notAvailable') }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('competitors.modals.details.labels.platform')">
            {{ getPlatformLabel(selectedAdForDetails.platform || selectedPlatform) }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('competitors.modals.details.labels.landingPage')">
            <a
              v-if="selectedAdForDetails.landingPageUrl"
              :href="selectedAdForDetails.landingPageUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ selectedAdForDetails.landingPageUrl }}
            </a>
            <span v-else>{{ $t('competitors.modals.details.notAvailable') }}</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex'
import {
  SearchOutlined,
  FileImageOutlined,
  EyeOutlined,
  BulbOutlined,
  CheckCircleFilled,
  ComparisonOutlined,
  FundOutlined,
  ExperimentOutlined,
  FacebookOutlined,
  GoogleOutlined,
  PlayCircleOutlined,
  ExportOutlined,
  LinkOutlined,
  DatabaseOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
export default {
  name: 'CompetitorsView',

  components: {
    SearchOutlined,
    FileImageOutlined,
    EyeOutlined,
    BulbOutlined,
    CheckCircleFilled,
    ComparisonOutlined,
    FundOutlined,
    ExperimentOutlined,
    FacebookOutlined,
    GoogleOutlined,
    PlayCircleOutlined,
    ExportOutlined,
    LinkOutlined,
    DatabaseOutlined
  },

  created() {
    this.initWatchlist()
  },

  data() {
    return {
      // Platform selection
      selectedPlatform: 'facebook',
      iframeSearchQuery: '',

      // Search form
      searchForm: {
        brandName: '',
        region: 'VN',
        limit: 5
      },

      // Modals
      showComparisonModal: false,
      showDetailsModal: false,

      // Comparison
      comparisonTab: 'compare',
      myAdText: '',
      selectedCompetitorForComparison: null,

      // Patterns
      patternsAiProvider: 'openai',

      // A/B Test
      myAdForABTest: '',
      competitorForABTest: null,
      abTestVariationCount: 3,

      // Ad Details
      selectedAdForDetails: null,

      // Watchlist
      watchlistForm: {
        brandName: '',
        platform: 'facebook',
        region: 'US',
        limit: 5
      }
    }
  },

  computed: {
    ...mapState('competitor', [
      'searchResults',
      'selectedCompetitorAds',
      'aiSuggestion',
      'patterns',
      'abTestVariations',
      'isSearching',
      'isAnalyzing',
      'searchError',
      'analysisError',
      'platformResponses',
      'recentPlatformStatuses',
      'watchlist',
      'watchlistActivity',
      'watchlistRefreshing'
    ]),

    ...mapGetters('competitor', [
      'hasSearchResults',
      'hasSelectedAds',
      'selectedCount',
      'isWatchlistRefreshing'
    ]),

    platformName() {
      return this.getPlatformLabel(this.selectedPlatform)
    },

    currentResponse() {
      return this.platformResponses?.[this.selectedPlatform] || {}
    },

    displayMode() {
      return this.currentResponse.mode || 'empty'
    },

    currentIframeUrl() {
      return this.currentResponse.iframeUrl || null
    },

    platformMessage() {
      return this.currentResponse.userMessage || this.currentResponse.message || ''
    },

    platformAlertType() {
      if (this.displayMode === 'data') return 'success'
      if (this.displayMode === 'iframe') return 'info'
      if (this.displayMode === 'error') return 'error'
      return 'warning'
    }
  },

  methods: {
    ...mapActions('competitor', {
      searchPlatformAds: 'searchPlatformAds',
      toggleAdSelection: 'toggleAdSelection',
      clearSelection: 'clearSelection',
      generateSuggestion: 'generateSuggestion',
      identifyPatternsAction: 'identifyPatterns',
      generateABTestAction: 'generateABTest',
      analyzeCompetitorAd: 'analyzeCompetitorAd',
      clearErrors: 'clearErrors',
      initWatchlist: 'initWatchlist',
      addWatchlistItemAction: 'addWatchlistItem',
      removeWatchlistItemAction: 'removeWatchlistItem',
      refreshWatchlistItemAction: 'refreshWatchlistItem',
      refreshAllWatchlistAction: 'refreshAllWatchlist'
    }),

    getStatusMessage(status) {
      if (!status) return ''
      const base = status.userMessage || status.message || ''
      const label = status.platformLabel || ''
      if (!label) return base
      return base ? `${label}: ${base}` : label
    },

    getStatusDescription(status) {
      if (!status) return undefined
      return status.friendlySuggestion || undefined
    },

    async handleSearch() {
      if (!this.searchForm.brandName.trim()) {
        message.warning(this.$t('competitors.messages.enterBrand'))
        return
      }

      try {
        const result = await this.searchPlatformAds({
          platform: this.selectedPlatform,
          brandName: this.searchForm.brandName.trim(),
          region: this.searchForm.region || 'VN',
          limit: this.searchForm.limit || 20
        })

        this.iframeSearchQuery = result.brandName || this.searchForm.brandName
        const displayMessage = result.userMessage || result.message

        if (result.mode === 'data' && (result.ads || []).length) {
          message.success(displayMessage || this.$t('competitors.messages.searchSuccess', { count: result.ads.length, platform: this.platformName }))
        } else if (result.mode === 'iframe') {
          message.info(displayMessage || this.$t('competitors.messages.iframeMode', { platform: this.platformName }))
        } else if (result.mode === 'empty') {
          message.warning(displayMessage || this.$t('competitors.messages.searchEmpty'))
        } else if (result.mode === 'error') {
          message.error(displayMessage || this.$t('competitors.messages.searchFailed'))
        }
      } catch (error) {
        console.error('Search error:', error)
      }
    },

    handlePlatformChange() {
      const response = this.currentResponse
      if (response.mode === 'data') {
        this.$store.commit('competitor/SET_SEARCH_RESULTS', response.ads || [])
      } else {
        this.$store.commit('competitor/CLEAR_SEARCH_RESULTS')
      }

      this.iframeSearchQuery = response.brandName || this.searchForm.brandName
      localStorage.setItem('preferredAdPlatform', this.selectedPlatform)
    },

    isAdSelected(ad) {
      return this.selectedCompetitorAds.some(a => a.adLibraryId === ad.adLibraryId)
    },

    viewAdDetails(ad) {
      this.selectedAdForDetails = ad
      this.showDetailsModal = true
    },

    async analyzeAd(ad) {
      try {
        await this.analyzeCompetitorAd({
          competitorAd: ad,
          aiProvider: 'openai'
        })

        message.success(this.$t('competitors.messages.analysisComplete'))
        this.showComparisonModal = true
        this.comparisonTab = 'compare'
      } catch (error) {
        message.error(this.$t('competitors.messages.analysisFailed'))
      }
    },

    async generateComparison() {
      const competitorAd = this.selectedCompetitorAds.find(
        ad => ad.adLibraryId === this.selectedCompetitorForComparison
      )

      if (!competitorAd) {
        message.error(this.$t('competitors.messages.selectCompetitor'))
        return
      }

      try {
        await this.generateSuggestion({
          competitorAd,
          myAd: this.myAdText,
          aiProvider: 'openai'
        })

        message.success(this.$t('competitors.messages.suggestionSuccess'))
      } catch (error) {
        message.error(this.$t('competitors.messages.suggestionFailed'))
      }
    },

    async identifyPatterns() {
      if (this.selectedCount < 2) {
        message.warning(this.$t('competitors.messages.patternsNeedSelection'))
        return
      }

      try {
        await this.identifyPatternsAction({
          competitorAds: this.selectedCompetitorAds,
          aiProvider: 'openai'
        })

        message.success(this.$t('competitors.messages.patternsSuccess'))
      } catch (error) {
        message.error(this.$t('competitors.messages.patternsError'))
      }
    },

    async generateABTest() {
      const competitorAd = this.selectedCompetitorAds.find(
        ad => ad.adLibraryId === this.competitorForABTest
      )

      if (!competitorAd) {
        message.error(this.$t('competitors.messages.selectCompetitor'))
        return
      }

      try {
        await this.generateABTestAction({
          competitorAd,
          myAd: this.myAdForABTest,
          variationCount: this.abTestVariationCount,
          aiProvider: 'openai'
        })

        message.success(this.$t('competitors.messages.abTestSuccess'))
      } catch (error) {
        message.error(this.$t('competitors.messages.abTestError'))
      }
    },

    truncateText(text, maxLength) {
      if (!text) return ''
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    },

    getPlatformColor(platform) {
      const colors = {
        facebook: 'blue',
        meta: 'blue',
        google: 'red',
        tiktok: 'magenta',
        SCRAPE_CREATORS_API: 'blue',
        GOOGLE_ADS_TRANSPARENCY: 'red',
        TIKTOK_CREATIVE_CENTER: 'magenta'
      }
      return colors[platform] || 'default'
    },

    getPlatformLabel(platform) {
      if (!platform) return ''
      const map = {
        facebook: 'facebook',
        meta: 'facebook',
        google: 'google',
        tiktok: 'tiktok',
        SCRAPE_CREATORS_API: 'facebook',
        GOOGLE_ADS_TRANSPARENCY: 'google',
        TIKTOK_CREATIVE_CENTER: 'tiktok'
      }
      const key = map[platform] || platform
      const normalized = key.toString().toLowerCase()
      const translationKey = `competitors.platformLabels.${normalized}`
      const translated = this.$t(translationKey)
      return translated === translationKey ? key : translated
    },

    getRegionLabel(region) {
      if (!region) return ''
      const normalized = region.toString().toUpperCase()
      const translationKey = `competitors.region.${normalized}`
      const translated = this.$t(translationKey)
      return translated === translationKey ? region : translated
    },

    getAdImage(ad) {
      // Handle different image URL formats
      if (ad.imageUrl) return ad.imageUrl
      if (ad.imageUrls && ad.imageUrls.length > 0) return ad.imageUrls[0]
      return null
    },

    updateIframeSearch() {
      if (!this.iframeSearchQuery || !this.iframeSearchQuery.trim()) {
        message.warning(this.$t('competitors.messages.enterBrand'))
        return
      }

      const brandName = encodeURIComponent(this.iframeSearchQuery.trim())
      const region = this.searchForm.region || 'VN'

      // Update iframe URL based on platform
      let iframeUrl = this.currentIframeUrl
      if (this.selectedPlatform === 'google') {
        iframeUrl = `https://adstransparency.google.com/?region=${region}&q=${brandName}`
      } else if (this.selectedPlatform === 'tiktok') {
        iframeUrl = `https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=${brandName}`
      }

      this.$store.commit('competitor/UPDATE_PLATFORM_IFRAME_URL', {
        platform: this.selectedPlatform,
        iframeUrl
      })
      message.success(this.$t('competitors.messages.updatedSearch', { query: this.iframeSearchQuery }))
    },

    openInNewTab() {
      if (!this.currentIframeUrl) {
        message.warning(this.$t('competitors.messages.iframeUnavailable'))
        return
      }
      window.open(this.currentIframeUrl, '_blank')
    },

    retryWithDataMode() {
      message.info(this.$t('competitors.messages.retryDataMode'))
      this.handleSearch()
    },

    async handleAddWatchlist() {
      if (!this.watchlistForm.brandName.trim()) {
        message.warning(this.$t('competitors.messages.enterBrand'))
        return
      }
      try {
        await this.addWatchlistItemAction({ ...this.watchlistForm })
        this.watchlistForm.brandName = ''
        message.success(this.$t('competitors.messages.watchlistAdded'))
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.watchlistAddError'))
      }
    },

    async handleRefreshWatchlist(item) {
      try {
        await this.refreshWatchlistItemAction(item)
        message.success(this.$t('competitors.messages.watchlistRefreshItem', { brand: item.brandName }))
      } catch (error) {
        message.error(error?.message || this.$t('competitors.messages.watchlistRefreshItemError'))
      }
    },

    async handleRefreshAllWatchlist() {
      if (!this.watchlist.length) return
      try {
        await this.refreshAllWatchlistAction()
        message.success(this.$t('competitors.messages.watchlistRefreshed'))
      } catch (error) {
        message.error(this.$t('competitors.messages.watchlistRefreshAllError'))
      }
    },

    handleRemoveWatchlist(item) {
      this.removeWatchlistItemAction(item.id)
      message.info(this.$t('competitors.messages.watchlistRemoved', { brand: item.brandName }))
    },

    formatWatchlistTimestamp(value) {
      if (!value) return this.$t('competitors.watchlist.neverChecked')
      return new Intl.DateTimeFormat(undefined, {
        day: '2-digit',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit'
      }).format(new Date(value))
    },

    isEntryRefreshing(id) {
      return this.watchlistRefreshing.includes(id) || this.isWatchlistRefreshing(id)
    },

    exportResults() {
      if (!this.searchResults || this.searchResults.length === 0) {
        message.warning(this.$t('competitors.messages.exportNoResults'))
        return
      }

      try {
        const headers = [
          this.$t('competitors.export.headers.platform'),
          this.$t('competitors.export.headers.adId'),
          this.$t('competitors.export.headers.headline'),
          this.$t('competitors.export.headers.description'),
          this.$t('competitors.export.headers.cta'),
          this.$t('competitors.export.headers.advertiser'),
          this.$t('competitors.export.headers.url')
        ]
        const rows = this.searchResults.map(ad => [
          ad.dataSource || this.selectedPlatform,
          ad.adId || ad.adLibraryId || '',
          ad.headline || '',
          (ad.primaryText || ad.description || '').replace(/\n/g, ' '),
          ad.callToAction || '',
          ad.advertiserName || '',
          ad.adLibraryUrl || ''
        ])

        const csv = [headers, ...rows]
          .map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(','))
          .join('\n')

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${this.platformName}_ads_${new Date().getTime()}.csv`
        link.click()
        URL.revokeObjectURL(url)

        message.success(this.$t('competitors.messages.exportSuccess'))
      } catch (error) {
        console.error('Export error:', error)
        message.error(this.$t('competitors.messages.exportError'))
      }
    },

    formatAISuggestion(suggestion) {
      if (!suggestion) return ''

      // Handle string suggestions
      if (typeof suggestion === 'string') {
        // Split by lines and format with proper spacing
        const lines = suggestion.split('\n').filter(line => line.trim())
        let formatted = '<div class="ai-suggestion-content">'

        lines.forEach(line => {
          const trimmed = line.trim()
          // Detect headers (ALL CAPS or ends with :)
          if (trimmed === trimmed.toUpperCase() && trimmed.length < 50 && !trimmed.includes('.')) {
            formatted += `<h4 class="suggestion-header">${trimmed}</h4>`
          }
          // Detect list items (starts with - or number)
          else if (/^[-•*]\s/.test(trimmed) || /^\d+[.)]\s/.test(trimmed)) {
            formatted += `<li class="suggestion-list-item">${trimmed.replace(/^[-•*]\s|^\d+[.)]\s/, '')}</li>`
          }
          // Regular paragraph
          else {
            formatted += `<p class="suggestion-paragraph">${trimmed}</p>`
          }
        })

        formatted += '</div>'
        return formatted
      }

      // Handle structured object
      if (typeof suggestion === 'object') {
        return this.formatStructuredAnalysis(suggestion)
      }

      return `<div class="whitespace-pre-wrap">${JSON.stringify(suggestion, null, 2)}</div>`
    },

    formatStructuredAnalysis(analysis) {
      if (!analysis || typeof analysis !== 'object') return ''

      let html = '<div class="structured-analysis">'
      const errorFallback = this.$t('competitors.modals.analysis.error')
      const sectionLabels = {
        strengths: this.$t('competitors.modals.analysis.sections.strengths'),
        weaknesses: this.$t('competitors.modals.analysis.sections.weaknesses'),
        recommendations: this.$t('competitors.modals.analysis.sections.recommendations'),
        patterns: this.$t('competitors.modals.analysis.sections.patterns'),
        raw: this.$t('competitors.modals.analysis.sections.raw')
      }

      // Display error message if present
      if (analysis.error) {
        html += `<div class="analysis-error">
          <p class="error-message">${analysis.error_message || errorFallback}</p>
        </div>`
      }

      // Format strengths
      if (analysis.strengths && Array.isArray(analysis.strengths)) {
        html += '<div class="analysis-section strengths-section">'
        html += `<h4 class="section-title">✅ ${sectionLabels.strengths}</h4>`
        html += '<ul class="analysis-list">'
        analysis.strengths.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format weaknesses
      if (analysis.weaknesses && Array.isArray(analysis.weaknesses)) {
        html += '<div class="analysis-section weaknesses-section">'
        html += `<h4 class="section-title">⚠️ ${sectionLabels.weaknesses}</h4>`
        html += '<ul class="analysis-list">'
        analysis.weaknesses.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format recommendations
      if (analysis.recommendations && Array.isArray(analysis.recommendations)) {
        html += '<div class="analysis-section recommendations-section">'
        html += `<h4 class="section-title">💡 ${sectionLabels.recommendations}</h4>`
        html += '<ul class="analysis-list">'
        analysis.recommendations.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format patterns
      if (analysis.patterns && Array.isArray(analysis.patterns)) {
        html += '<div class="analysis-section patterns-section">'
        html += `<h4 class="section-title">🔍 ${sectionLabels.patterns}</h4>`
        html += '<ul class="analysis-list">'
        analysis.patterns.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Show raw analysis as fallback
      if (analysis.raw_analysis) {
        html += '<div class="analysis-section raw-section">'
        html += '<details class="raw-details">'
        html += `<summary class="raw-summary">📄 ${sectionLabels.raw}</summary>`
        html += `<pre class="raw-content">${analysis.raw_analysis}</pre>`
        html += '</details></div>'
      }

      html += '</div>'
      return html
    },

    formatABTestVariations(variations) {
      if (!variations || !Array.isArray(variations)) return ''

      let html = '<div class="ab-variations-container">'
      const labels = {
        headline: this.$t('competitors.modals.analysis.fields.headline'),
        primaryText: this.$t('competitors.modals.analysis.fields.primaryText'),
        callToAction: this.$t('competitors.modals.analysis.fields.callToAction'),
        viewRaw: this.$t('competitors.modals.analysis.fields.viewRaw')
      }

      variations.forEach((variation, index) => {
        html += `<div class="variation-card variation-${index + 1}">`
        const title = this.$t('competitors.modals.abTest.variation', { index: variation.variationNumber || index + 1 })
        html += `<div class="variation-header">
          <span class="variation-number">${title}</span>
          ${variation.testingFocus ? `<span class="testing-focus">${variation.testingFocus}</span>` : ''}
        </div>`

        if (variation.headline) {
          html += `<div class="variation-field">
            <label class="field-label">${labels.headline}:</label>
            <p class="field-content headline-content">${variation.headline}</p>
          </div>`
        }

        if (variation.primaryText) {
          html += `<div class="variation-field">
            <label class="field-label">${labels.primaryText}:</label>
            <p class="field-content text-content">${variation.primaryText}</p>
          </div>`
        }

        if (variation.callToAction) {
          html += `<div class="variation-field">
            <label class="field-label">${labels.callToAction}:</label>
            <span class="cta-badge">${variation.callToAction}</span>
          </div>`
        }

        // Show raw variation in details
        if (variation.rawVariation) {
          html += '<details class="raw-variation-details">'
          html += `<summary class="raw-variation-summary">${labels.viewRaw}</summary>`
          html += `<pre class="raw-variation-content">${variation.rawVariation}</pre>`
          html += '</details>'
        }

        html += '</div>'
      })

      html += '</div>'
      return html
    },

    formatPatterns(patterns) {
      if (!patterns) return ''

      // Handle string patterns
      if (typeof patterns === 'string') {
        return `<div class="whitespace-pre-wrap">${patterns}</div>`
      }

      // Handle structured patterns
      if (typeof patterns === 'object') {
        return this.formatStructuredAnalysis(patterns)
      }

      return `<div class="whitespace-pre-wrap">${JSON.stringify(patterns, null, 2)}</div>`
    }
  }
}
</script>

<style scoped>
.competitors-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.surface-card {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  padding: 32px;
  margin-bottom: 24px;
}

.hero-text h1 {
  margin: 8px 0 12px;
  font-size: 28px;
  color: #0f172a;
}

.eyebrow {
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.hero-description {
  margin: 0 0 20px;
  color: #475569;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-meta {
  display: grid;
  gap: 16px;
}

.hero-stat {
  padding: 16px 20px;
  border-radius: 16px;
  background: #f8fafc;
}

.hero-stat-label {
  margin: 0;
  font-size: 13px;
  color: #475569;
}

.hero-stat-value {
  margin: 4px 0 0;
  font-size: 28px;
  font-weight: 600;
  color: #0f172a;
}

.section-card {
  padding: 24px;
  margin-bottom: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-heading h2 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.section-subtitle {
  margin: 4px 0 0;
  color: #64748b;
}

.section-controls {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.status-feed {
  padding: 16px;
  margin-bottom: 24px;
}

.watchlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.watchlist-card {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

.watchlist-card-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.watchlist-card-header h4 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.watchlist-meta,
.watchlist-hint,
.watchlist-time {
  color: #94a3b8;
  font-size: 13px;
}

.watchlist-count {
  font-weight: 600;
  margin: 8px 0;
  color: #0f172a;
}

.watchlist-card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.watchlist-activity ul {
  padding-left: 16px;
  margin: 8px 0 0;
  color: #475569;
}

.competitor-ad-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  position: relative;
}

.competitor-ad-card.selected {
  border-color: #2563eb;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.2);
}

.ad-image-container {
  position: relative;
  width: 100%;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  background: #f1f5f9;
  margin-bottom: 12px;
}

.ad-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #94a3b8;
}

.selection-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #fff;
  border-radius: 50%;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.ad-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ad-headline {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.ad-description {
  margin: 0;
  color: #475569;
  min-height: 48px;
}

.ad-actions {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.iframe-container {
  position: relative;
}

.iframe-search-bar {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.empty-state {
  margin-top: 24px;
  padding: 24px;
  text-align: center;
  border: 1px dashed #e2e8f0;
  border-radius: 16px;
}

:deep(.surface-modal .ant-modal-content) {
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.2);
}

:deep(.surface-modal .ant-modal-header) {
  border-bottom: 1px solid #e2e8f0;
  border-radius: 24px 24px 0 0;
  padding: 20px 24px;
}

:deep(.surface-modal .ant-modal-body) {
  padding: 0;
}

.modal-content {
  padding: 24px;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    padding: 24px;
  }

  .hero-actions,
  .section-controls {
    flex-direction: column;
    width: 100%;
  }
}
</style>
