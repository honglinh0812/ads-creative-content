<template>
  <div class="competitors-page">
    <div class="page-header">
      <div>
        <h1 class="text-3xl font-bold text-secondary-900">Competitor Insights</h1>
        <p class="text-secondary-600">Analyze competitor ads and get AI-powered suggestions</p>
      </div>
    </div>

    <!-- Platform Selector -->
    <div class="platform-selector-section">
      <a-card :bordered="false" class="platform-selector-card">
        <a-radio-group
          v-model:value="selectedPlatform"
          button-style="solid"
          size="large"
          @change="handlePlatformChange"
          class="platform-radio-group"
        >
          <a-radio-button value="facebook">
            <facebook-outlined /> Facebook & Instagram
          </a-radio-button>
          <a-radio-button value="google">
            <google-outlined /> Google Ads & YouTube
          </a-radio-button>
          <a-radio-button value="tiktok">
            <play-circle-outlined /> TikTok Ads
          </a-radio-button>
        </a-radio-group>
      </a-card>
    </div>

    <!-- Search Section -->
    <div class="search-section">
      <a-card title="Search Competitor Ads" :bordered="false">
        <a-form layout="vertical" @submit.prevent="handleSearch">
          <a-row :gutter="16">
            <a-col :xs="24" :sm="24" :md="12">
              <a-form-item label="Brand Name">
                <a-input
                  v-model:value="searchForm.brandName"
                  placeholder="Enter competitor brand name (e.g., Nike, Coca-Cola)"
                  size="large"
                  :maxlength="100"
                  show-count
                  allow-clear
                />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-form-item label="Region">
                <a-select
                  v-model:value="searchForm.region"
                  size="large"
                  placeholder="Select region"
                  show-search
                  option-filter-prop="label"
                >
                  <a-select-opt-group label="Americas">
                    <a-select-option value="US" label="United States">🇺🇸 United States</a-select-option>
                    <a-select-option value="CA" label="Canada">🇨🇦 Canada</a-select-option>
                    <a-select-option value="MX" label="Mexico">🇲🇽 Mexico</a-select-option>
                    <a-select-option value="BR" label="Brazil">🇧🇷 Brazil</a-select-option>
                  </a-select-opt-group>
                  <a-select-opt-group label="Europe">
                    <a-select-option value="GB" label="United Kingdom">🇬🇧 United Kingdom</a-select-option>
                    <a-select-option value="DE" label="Germany">🇩🇪 Germany</a-select-option>
                    <a-select-option value="FR" label="France">🇫🇷 France</a-select-option>
                    <a-select-option value="IT" label="Italy">🇮🇹 Italy</a-select-option>
                    <a-select-option value="ES" label="Spain">🇪🇸 Spain</a-select-option>
                    <a-select-option value="NL" label="Netherlands">🇳🇱 Netherlands</a-select-option>
                  </a-select-opt-group>
                  <a-select-opt-group label="Oceania">
                    <a-select-option value="AU" label="Australia">🇦🇺 Australia</a-select-option>
                    <a-select-option value="NZ" label="New Zealand">🇳🇿 New Zealand</a-select-option>
                  </a-select-opt-group>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-form-item label="Max Results">
                <a-input-number
                  v-model:value="searchForm.limit"
                  :min="1"
                  :max="50"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="isSearching"
              :disabled="!searchForm.brandName"
            >
              <template #icon><search-outlined /></template>
              Search Competitor Ads
            </a-button>
          </a-form-item>
        </a-form>

        <!-- Search Error -->
        <a-alert
          v-if="searchError"
          type="error"
          :message="searchError"
          show-icon
          closable
          @close="clearErrors"
          class="mt-4"
        />
      </a-card>
    </div>

    <!-- Search Results -->
    <div v-if="hasSearchResults || displayMode === 'iframe'" class="results-section">
      <a-card :bordered="false">
        <template v-if="displayMode === 'data' && searchResults.length > 0" #title>
          <div class="flex justify-between items-center">
            <span>Found {{ searchResults.length }} ads from {{ platformName }}</span>
            <a-space>
              <a-button
                v-if="hasSelectedAds"
                type="primary"
                @click="showComparisonModal = true"
              >
                <template #icon><comparison-outlined /></template>
                Compare Selected ({{ selectedCount }})
              </a-button>
              <a-button @click="exportResults">
                <template #icon><export-outlined /></template>
                Export
              </a-button>
              <a-button @click="clearSelection">Clear Selection</a-button>
            </a-space>
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
                class="competitor-ad-card"
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
          <a-alert
            type="info"
            :message="`Viewing ${platformName} in embedded mode`"
            description="For structured data export and AI analysis, ensure SERPAPI_KEY is configured."
            show-icon
            style="margin-bottom: 16px"
            closable
          />

          <!-- Quick Search Bar for Iframe Mode -->
          <div class="iframe-search-bar">
            <a-input-group compact>
              <a-input
                v-model:value="iframeSearchQuery"
                placeholder="Search another brand..."
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
                Search
              </a-button>
            </a-input-group>
          </div>

          <iframe
            :src="iframeUrl"
            width="100%"
            height="800px"
            frameborder="0"
            sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
            style="border: 1px solid #d9d9d9; border-radius: 4px;"
          />

          <a-space style="margin-top: 16px">
            <a-button @click="openInNewTab" type="primary">
              <link-outlined /> Open in New Tab
            </a-button>
            <a-button v-if="selectedPlatform === 'google'" @click="retryWithDataMode">
              <database-outlined /> Try Structured Data Mode
            </a-button>
          </a-space>
        </div>

        <!-- Loading State -->
        <div v-else-if="isSearching" style="text-align: center; padding: 40px;">
          <a-spin size="large" />
          <p style="margin-top: 16px;">Searching {{ platformName }}...</p>
        </div>

        <!-- Empty State -->
        <a-empty v-else description="No results found" />
      </a-card>
    </div>

    <!-- Empty State -->
    <div v-if="!hasSearchResults && !isSearching && displayMode !== 'iframe'" class="empty-state">
      <a-empty description="Search for competitor brands to view their ads">
        <template #image>
          <search-outlined style="font-size: 64px; color: #d9d9d9" />
        </template>
      </a-empty>
    </div>

    <!-- Comparison Modal -->
    <a-modal
      v-model:open="showComparisonModal"
      title="AI-Powered Ad Comparison"
      width="90%"
      :footer="null"
      :destroy-on-close="true"
    >
      <div class="comparison-content">
        <a-tabs v-model:activeKey="comparisonTab">
          <!-- Compare with My Ad -->
          <a-tab-pane key="compare" tab="Compare with My Ad">
            <a-form layout="vertical">
              <a-form-item label="Your Current Ad Text">
                <a-textarea
                  v-model:value="myAdText"
                  :rows="4"
                  placeholder="Enter your current ad text here..."
                />
              </a-form-item>

              <a-form-item label="Select Competitor Ad to Compare">
                <a-select
                  v-model:value="selectedCompetitorForComparison"
                  size="large"
                  placeholder="Select an ad"
                  style="width: 100%"
                >
                  <a-select-option
                    v-for="ad in selectedCompetitorAds"
                    :key="ad.adLibraryId"
                    :value="ad.adLibraryId"
                  >
                    {{ ad.headline || 'Unnamed Ad' }}
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
                  Generate AI Suggestion
                </a-button>
              </a-form-item>
            </a-form>

            <!-- AI Suggestion Result -->
            <div v-if="aiSuggestion" class="ai-result-section">
              <a-alert type="success" message="AI Suggestion Generated" show-icon class="mb-4" />
              <div class="suggestion-content" v-html="formatAISuggestion(aiSuggestion)"></div>
            </div>
          </a-tab-pane>

          <!-- Identify Patterns -->
          <a-tab-pane key="patterns" tab="Identify Patterns">
            <p class="text-gray-600 mb-4">
              Analyze {{ selectedCount }} selected ads to identify common patterns and strategies.
            </p>

            <a-button
              type="primary"
              :loading="isAnalyzing"
              @click="identifyPatterns"
              :disabled="selectedCount < 2"
            >
              <template #icon><fund-outlined /></template>
              Analyze Patterns
            </a-button>

            <!-- Patterns Result -->
            <div v-if="patterns" class="ai-result-section mt-4">
              <a-alert type="success" message="Patterns Identified" show-icon class="mb-4" />
              <div class="suggestion-content" v-html="formatPatterns(patterns)"></div>
            </div>

            <a-alert v-if="selectedCount < 2" type="info" message="Select at least 2 ads to identify patterns" show-icon class="mt-4" />
          </a-tab-pane>

          <!-- A/B Test Variations -->
          <a-tab-pane key="abtest" tab="A/B Test Variations">
            <a-form layout="vertical">
              <a-form-item label="Your Current Ad">
                <a-textarea
                  v-model:value="myAdForABTest"
                  :rows="4"
                  placeholder="Enter your current ad text..."
                />
              </a-form-item>

              <a-form-item label="Competitor Ad to Learn From">
                <a-select
                  v-model:value="competitorForABTest"
                  size="large"
                  placeholder="Select an ad"
                  style="width: 100%"
                >
                  <a-select-option
                    v-for="ad in selectedCompetitorAds"
                    :key="ad.adLibraryId"
                    :value="ad.adLibraryId"
                  >
                    {{ ad.headline || 'Unnamed Ad' }}
                  </a-select-option>
                </a-select>
              </a-form-item>

              <a-form-item label="Number of Variations">
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
                Generate Variations
              </a-button>
            </a-form>

            <!-- A/B Test Results -->
            <div v-if="abTestVariations.length > 0" class="ai-result-section mt-4">
              <a-alert type="success" message="A/B Test Variations Generated" show-icon class="mb-4" />
              <a-row :gutter="[16, 16]">
                <a-col
                  v-for="(variation, index) in abTestVariations"
                  :key="index"
                  :xs="24"
                  :sm="12"
                >
                  <a-card :title="`Variation ${index + 1}`" size="small">
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
      :title="selectedAdForDetails?.headline || 'Ad Details'"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedAdForDetails" class="ad-details">
        <div v-if="selectedAdForDetails.imageUrl" class="mb-4">
          <img :src="selectedAdForDetails.imageUrl" alt="Ad" class="w-full rounded" />
        </div>

        <a-descriptions bordered :column="1">
          <a-descriptions-item label="Headline">
            {{ selectedAdForDetails.headline || 'N/A' }}
          </a-descriptions-item>
          <a-descriptions-item label="Primary Text">
            {{ selectedAdForDetails.primaryText || 'N/A' }}
          </a-descriptions-item>
          <a-descriptions-item label="Description">
            {{ selectedAdForDetails.description || 'N/A' }}
          </a-descriptions-item>
          <a-descriptions-item label="Call to Action">
            {{ selectedAdForDetails.callToAction || 'N/A' }}
          </a-descriptions-item>
          <a-descriptions-item label="Platform">
            {{ selectedAdForDetails.platform || 'Facebook' }}
          </a-descriptions-item>
          <a-descriptions-item label="Landing Page">
            <a
              v-if="selectedAdForDetails.landingPageUrl"
              :href="selectedAdForDetails.landingPageUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ selectedAdForDetails.landingPageUrl }}
            </a>
            <span v-else>N/A</span>
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
import apiClient from '@/services/api'

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

  data() {
    return {
      // Platform selection
      selectedPlatform: 'facebook',
      displayMode: 'data', // 'data' or 'iframe'
      iframeUrl: '',
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
      selectedAdForDetails: null
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
      'analysisError'
    ]),

    ...mapGetters('competitor', [
      'hasSearchResults',
      'hasSelectedAds',
      'selectedCount'
    ]),

    platformName() {
      const names = {
        facebook: 'Facebook & Instagram',
        google: 'Google Ads & YouTube',
        tiktok: 'TikTok'
      }
      return names[this.selectedPlatform] || this.selectedPlatform
    }
  },

  methods: {
    ...mapActions('competitor', [
      'searchCompetitorAds',
      'toggleAdSelection',
      'clearSelection',
      'generateSuggestion',
      'identifyPatterns as identifyPatternsAction',
      'generateABTest as generateABTestAction',
      'analyzeCompetitorAd',
      'clearErrors'
    ]),

    async handleSearch() {
      if (!this.searchForm.brandName.trim()) {
        message.warning('Please enter a brand name')
        return
      }

      try {
        let endpoint = '/competitors/search'

        // Route to appropriate endpoint based on platform
        if (this.selectedPlatform === 'google') {
          endpoint = '/competitors/search/google'
        } else if (this.selectedPlatform === 'tiktok') {
          endpoint = '/competitors/search/tiktok'
        }

        this.$store.commit('competitor/SET_SEARCHING', true)

        const response = await apiClient.post(endpoint, {
          brandName: this.searchForm.brandName,
          region: this.searchForm.region || 'VN',
          limit: this.searchForm.limit || 20
        })

        // Check response mode
        if (response.data.mode === 'iframe') {
          // Display in iframe mode
          this.displayMode = 'iframe'
          this.iframeUrl = response.data.url
          this.iframeSearchQuery = this.searchForm.brandName
          message.info(`Viewing ${this.platformName} ads in embedded mode`)
        } else if (response.data.ads) {
          // Structured data mode
          this.displayMode = 'data'
          this.$store.commit('competitor/SET_SEARCH_RESULTS', response.data.ads)
          message.success(`Found ${response.data.ads.length} ads from ${this.platformName}`)
        } else {
          // Legacy response format (Facebook)
          this.displayMode = 'data'
          await this.searchCompetitorAds({
            brandName: this.searchForm.brandName,
            region: this.searchForm.region,
            limit: this.searchForm.limit
          })

          if (this.searchResults.length === 0) {
            message.info('No competitor ads found for this brand')
          } else {
            message.success(`Found ${this.searchResults.length} competitor ads`)
          }
        }
      } catch (error) {
        console.error('Search error:', error)

        // Fallback: display in iframe for non-Facebook platforms
        if (this.selectedPlatform !== 'facebook') {
          const brandName = encodeURIComponent(this.searchForm.brandName)
          const region = this.searchForm.region || 'VN'

          this.displayMode = 'iframe'
          this.iframeSearchQuery = this.searchForm.brandName

          if (this.selectedPlatform === 'google') {
            this.iframeUrl = `https://adstransparency.google.com/?region=${region}&q=${brandName}`
          } else if (this.selectedPlatform === 'tiktok') {
            this.iframeUrl = `https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=${brandName}`
          }

          message.info(`Displaying ${this.platformName} in embedded mode`)
        } else {
          message.error('Failed to search competitor ads')
        }
      } finally {
        this.$store.commit('competitor/SET_SEARCHING', false)
      }
    },

    handlePlatformChange() {
      // Reset results when switching platform
      this.$store.commit('competitor/SET_SEARCH_RESULTS', [])
      this.displayMode = 'data'
      this.iframeUrl = ''

      // Save preference
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

        message.success('Ad analysis complete')
        this.showComparisonModal = true
        this.comparisonTab = 'compare'
      } catch (error) {
        message.error('Failed to analyze ad')
      }
    },

    async generateComparison() {
      const competitorAd = this.selectedCompetitorAds.find(
        ad => ad.adLibraryId === this.selectedCompetitorForComparison
      )

      if (!competitorAd) {
        message.error('Please select a competitor ad')
        return
      }

      try {
        await this.generateSuggestion({
          competitorAd,
          myAd: this.myAdText,
          aiProvider: 'openai'
        })

        message.success('AI suggestion generated successfully')
      } catch (error) {
        message.error('Failed to generate suggestion')
      }
    },

    async identifyPatterns() {
      if (this.selectedCount < 2) {
        message.warning('Please select at least 2 ads to identify patterns')
        return
      }

      try {
        await this.identifyPatternsAction({
          competitorAds: this.selectedCompetitorAds,
          aiProvider: 'openai'
        })

        message.success('Patterns identified successfully')
      } catch (error) {
        message.error('Failed to identify patterns')
      }
    },

    async generateABTest() {
      const competitorAd = this.selectedCompetitorAds.find(
        ad => ad.adLibraryId === this.competitorForABTest
      )

      if (!competitorAd) {
        message.error('Please select a competitor ad')
        return
      }

      try {
        await this.generateABTestAction({
          competitorAd,
          myAd: this.myAdForABTest,
          variationCount: this.abTestVariationCount,
          aiProvider: 'openai'
        })

        message.success('A/B test variations generated successfully')
      } catch (error) {
        message.error('Failed to generate A/B test variations')
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
      const labels = {
        facebook: 'Facebook',
        meta: 'Facebook',
        google: 'Google',
        tiktok: 'TikTok',
        SCRAPE_CREATORS_API: 'Facebook',
        GOOGLE_ADS_TRANSPARENCY: 'Google',
        TIKTOK_CREATIVE_CENTER: 'TikTok'
      }
      return labels[platform] || platform
    },

    getAdImage(ad) {
      // Handle different image URL formats
      if (ad.imageUrl) return ad.imageUrl
      if (ad.imageUrls && ad.imageUrls.length > 0) return ad.imageUrls[0]
      return null
    },

    updateIframeSearch() {
      if (!this.iframeSearchQuery || !this.iframeSearchQuery.trim()) {
        message.warning('Please enter a brand name')
        return
      }

      const brandName = encodeURIComponent(this.iframeSearchQuery.trim())
      const region = this.searchForm.region || 'VN'

      // Update iframe URL based on platform
      if (this.selectedPlatform === 'google') {
        this.iframeUrl = `https://adstransparency.google.com/?region=${region}&q=${brandName}`
      } else if (this.selectedPlatform === 'tiktok') {
        this.iframeUrl = `https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=${brandName}`
      }

      message.success(`Updated search to: ${this.iframeSearchQuery}`)
    },

    openInNewTab() {
      window.open(this.iframeUrl, '_blank')
    },

    retryWithDataMode() {
      message.info('Retrying search with structured data mode...')
      this.handleSearch()
    },

    exportResults() {
      if (!this.searchResults || this.searchResults.length === 0) {
        message.warning('No results to export')
        return
      }

      try {
        const headers = ['Platform', 'Ad ID', 'Headline', 'Description', 'CTA', 'Advertiser', 'URL']
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

        message.success('Export completed')
      } catch (error) {
        console.error('Export error:', error)
        message.error('Failed to export results')
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

      // Display error message if present
      if (analysis.error) {
        html += `<div class="analysis-error">
          <p class="error-message">${analysis.error_message || 'Analysis failed'}</p>
        </div>`
      }

      // Format strengths
      if (analysis.strengths && Array.isArray(analysis.strengths)) {
        html += '<div class="analysis-section strengths-section">'
        html += '<h4 class="section-title">✅ Strengths</h4>'
        html += '<ul class="analysis-list">'
        analysis.strengths.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format weaknesses
      if (analysis.weaknesses && Array.isArray(analysis.weaknesses)) {
        html += '<div class="analysis-section weaknesses-section">'
        html += '<h4 class="section-title">⚠️ Areas for Improvement</h4>'
        html += '<ul class="analysis-list">'
        analysis.weaknesses.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format recommendations
      if (analysis.recommendations && Array.isArray(analysis.recommendations)) {
        html += '<div class="analysis-section recommendations-section">'
        html += '<h4 class="section-title">💡 Recommendations</h4>'
        html += '<ul class="analysis-list">'
        analysis.recommendations.forEach(item => {
          html += `<li class="analysis-list-item">${item}</li>`
        })
        html += '</ul></div>'
      }

      // Format patterns
      if (analysis.patterns && Array.isArray(analysis.patterns)) {
        html += '<div class="analysis-section patterns-section">'
        html += '<h4 class="section-title">🔍 Patterns Identified</h4>'
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
        html += '<summary class="raw-summary">📄 View Raw Analysis</summary>'
        html += `<pre class="raw-content">${analysis.raw_analysis}</pre>`
        html += '</details></div>'
      }

      html += '</div>'
      return html
    },

    formatABTestVariations(variations) {
      if (!variations || !Array.isArray(variations)) return ''

      let html = '<div class="ab-variations-container">'

      variations.forEach((variation, index) => {
        html += `<div class="variation-card variation-${index + 1}">`
        html += `<div class="variation-header">
          <span class="variation-number">Variation ${variation.variationNumber || index + 1}</span>
          ${variation.testingFocus ? `<span class="testing-focus">${variation.testingFocus}</span>` : ''}
        </div>`

        if (variation.headline) {
          html += `<div class="variation-field">
            <label class="field-label">Headline:</label>
            <p class="field-content headline-content">${variation.headline}</p>
          </div>`
        }

        if (variation.primaryText) {
          html += `<div class="variation-field">
            <label class="field-label">Primary Text:</label>
            <p class="field-content text-content">${variation.primaryText}</p>
          </div>`
        }

        if (variation.callToAction) {
          html += `<div class="variation-field">
            <label class="field-label">Call to Action:</label>
            <span class="cta-badge">${variation.callToAction}</span>
          </div>`
        }

        // Show raw variation in details
        if (variation.rawVariation) {
          html += '<details class="raw-variation-details">'
          html += '<summary class="raw-variation-summary">View Raw</summary>'
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
.competitors-page {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.platform-selector-section {
  margin-bottom: 24px;
}

.platform-selector-card {
  background: #fff;
  border: 1px solid #e8e8e8;
}

.platform-selector-card :deep(.ant-card-body) {
  padding: 16px;
}

.platform-radio-group {
  width: 100%;
  display: flex;
  justify-content: center;
}

.platform-radio-group :deep(.ant-radio-button-wrapper) {
  flex: 1;
  text-align: center;
  background: #fafafa;
  border-color: #d9d9d9;
  font-weight: 500;
  transition: all 0.3s;
}

.platform-radio-group :deep(.ant-radio-button-wrapper-checked) {
  background: #1890ff;
  border-color: #1890ff;
  color: #fff;
}

.platform-radio-group :deep(.ant-radio-button-wrapper:hover) {
  border-color: #40a9ff;
  color: #40a9ff;
}

.platform-radio-group :deep(.ant-radio-button-wrapper-checked:hover) {
  background: #40a9ff;
  border-color: #40a9ff;
  color: #fff;
}

.iframe-container {
  position: relative;
}

.iframe-search-bar {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.iframe-search-bar :deep(.ant-input-group) {
  display: flex;
}

.iframe-search-bar :deep(.ant-input-group .ant-input) {
  border-right: none;
}

.iframe-search-bar :deep(.ant-input-group .ant-btn) {
  width: 180px;
}

.search-section {
  margin-bottom: 24px;
}

.results-section {
  margin-bottom: 24px;
}

.competitor-ad-card {
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.competitor-ad-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.competitor-ad-card.selected {
  border-color: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
}

.ad-image-container {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background: #f5f5f5;
  border-radius: 4px;
  margin-bottom: 12px;
}

.ad-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.selection-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: white;
  border-radius: 50%;
  padding: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.ad-content {
  padding: 8px 0;
}

.ad-headline {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1.4;
}

.ad-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
}

.ad-metadata {
  margin-bottom: 12px;
}

.ad-actions {
  display: flex;
  justify-content: space-between;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.comparison-content {
  padding: 16px 0;
}

.ai-result-section {
  background: #f9f9f9;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 16px;
  margin-top: 16px;
}

.suggestion-content {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
}

.suggestion-item {
  padding: 4px 0;
}

/* AI Suggestion Formatting Styles */
.ai-suggestion-content {
  line-height: 1.6;
}

.suggestion-header {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
  margin: 16px 0 8px 0;
  padding-bottom: 4px;
  border-bottom: 2px solid #e8e8e8;
}

.suggestion-list-item {
  margin: 8px 0;
  padding-left: 20px;
  position: relative;
  list-style: none;
}

.suggestion-list-item::before {
  content: "•";
  position: absolute;
  left: 0;
  color: #1890ff;
  font-weight: bold;
}

.suggestion-paragraph {
  margin: 12px 0;
  color: #595959;
}

/* Structured Analysis Styles */
.structured-analysis {
  padding: 16px;
}

.analysis-section {
  margin-bottom: 24px;
  padding: 16px;
  border-radius: 8px;
  background: #fafafa;
}

.strengths-section {
  background: #f6ffed;
  border-left: 4px solid #52c41a;
}

.weaknesses-section {
  background: #fff7e6;
  border-left: 4px solid #faad14;
}

.recommendations-section {
  background: #e6f7ff;
  border-left: 4px solid #1890ff;
}

.patterns-section {
  background: #f9f0ff;
  border-left: 4px solid #722ed1;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #262626;
}

.analysis-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.analysis-list-item {
  padding: 8px 0 8px 24px;
  position: relative;
  color: #595959;
  line-height: 1.6;
}

.analysis-list-item::before {
  content: "▸";
  position: absolute;
  left: 8px;
  color: #8c8c8c;
}

.analysis-error {
  padding: 16px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  margin-bottom: 16px;
}

.error-message {
  margin: 0;
  color: #cf1322;
  font-weight: 500;
}

.raw-section {
  background: #f5f5f5;
  border: 1px dashed #d9d9d9;
}

.raw-details {
  cursor: pointer;
}

.raw-summary {
  font-weight: 500;
  color: #595959;
  padding: 8px;
  user-select: none;
}

.raw-summary:hover {
  color: #1890ff;
}

.raw-content {
  margin: 8px 0 0 0;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #595959;
  overflow-x: auto;
}

/* A/B Test Variations Styles */
.ab-variations-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.variation-card {
  background: #fff;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.variation-card:hover {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
  transform: translateY(-2px);
}

.variation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.variation-number {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.testing-focus {
  font-size: 12px;
  color: #8c8c8c;
  font-style: italic;
}

.variation-field {
  margin-bottom: 12px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #8c8c8c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.field-content {
  margin: 0;
  color: #262626;
  line-height: 1.6;
}

.headline-content {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.text-content {
  font-size: 14px;
}

.cta-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #1890ff;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.raw-variation-details {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #d9d9d9;
}

.raw-variation-summary {
  font-size: 12px;
  color: #8c8c8c;
  cursor: pointer;
  user-select: none;
}

.raw-variation-summary:hover {
  color: #1890ff;
}

.raw-variation-content {
  margin: 8px 0 0 0;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 11px;
  line-height: 1.4;
  color: #595959;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .competitors-page {
    padding: 12px;
  }

  .ad-image-container {
    height: 150px;
  }

  .ab-variations-container {
    grid-template-columns: 1fr;
  }

  .variation-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .testing-focus {
    margin-top: 4px;
  }
}
</style>
