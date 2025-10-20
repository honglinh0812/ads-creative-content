<template>
  <div class="competitors-page">
    <div class="page-header">
      <div>
        <h1 class="text-3xl font-bold text-secondary-900">Competitor Insights</h1>
        <p class="text-secondary-600">Analyze competitor ads and get AI-powered suggestions</p>
      </div>
    </div>

    <!-- Search Section -->
    <div class="search-section">
      <a-card title="Search Competitor Ads" :bordered="false">
        <a-form layout="vertical" @submit.prevent="handleSearch">
          <a-row :gutter="16">
            <a-col :xs="24" :sm="24" :md="12">
              <a-form-item label="Brand Name">
                <a-auto-complete
                  v-model:value="searchForm.brandName"
                  :options="brandSuggestionOptions"
                  placeholder="Enter competitor brand name (e.g., Nike, Coca-Cola)"
                  size="large"
                  :filter-option="false"
                  @search="handleBrandSearch"
                  @select="handleBrandSelect"
                >
                  <template #option="{ value }">
                    <div class="suggestion-item">
                      <span>{{ value }}</span>
                    </div>
                  </template>
                </a-auto-complete>
              </a-form-item>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-form-item label="Region">
                <a-select
                  v-model:value="searchForm.region"
                  size="large"
                  placeholder="Select region"
                >
                  <a-select-option value="US">United States</a-select-option>
                  <a-select-option value="GB">United Kingdom</a-select-option>
                  <a-select-option value="CA">Canada</a-select-option>
                  <a-select-option value="AU">Australia</a-select-option>
                  <a-select-option value="DE">Germany</a-select-option>
                  <a-select-option value="FR">France</a-select-option>
                  <a-select-option value="JP">Japan</a-select-option>
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
    <div v-if="hasSearchResults" class="results-section">
      <a-card :bordered="false">
        <template #title>
          <div class="flex justify-between items-center">
            <span>Found {{ searchResults.length }} competitor ads</span>
            <a-space>
              <a-button
                v-if="hasSelectedAds"
                type="primary"
                @click="showComparisonModal = true"
              >
                <template #icon><comparison-outlined /></template>
                Compare Selected ({{ selectedCount }})
              </a-button>
              <a-button @click="clearSelection">Clear Selection</a-button>
            </a-space>
          </div>
        </template>

        <!-- Competitor Ads Grid -->
        <a-row :gutter="[16, 16]">
          <a-col
            v-for="ad in searchResults"
            :key="ad.adLibraryId"
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
              <!-- Ad Image -->
              <div class="ad-image-container">
                <img
                  v-if="ad.imageUrl"
                  :src="ad.imageUrl"
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
                  <a-tag color="blue">{{ ad.platform || 'Facebook' }}</a-tag>
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
      </a-card>
    </div>

    <!-- Empty State -->
    <div v-if="!hasSearchResults && !isSearching" class="empty-state">
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
  ExperimentOutlined
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
    ExperimentOutlined
  },

  data() {
    return {
      // Search form
      searchForm: {
        brandName: '',
        region: 'US',
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
      'brandSuggestions',
      'searchError',
      'analysisError'
    ]),

    ...mapGetters('competitor', [
      'hasSearchResults',
      'hasSelectedAds',
      'selectedCount'
    ]),

    brandSuggestionOptions() {
      return this.brandSuggestions.map(suggestion => ({
        value: suggestion
      }))
    }
  },

  methods: {
    ...mapActions('competitor', [
      'searchCompetitorAds',
      'loadBrandSuggestions',
      'toggleAdSelection',
      'clearSelection',
      'generateSuggestion',
      'identifyPatterns as identifyPatternsAction',
      'generateABTest as generateABTestAction',
      'analyzeCompetitorAd',
      'clearErrors'
    ]),

    async handleSearch() {
      try {
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
      } catch (error) {
        message.error('Failed to search competitor ads')
      }
    },

    async handleBrandSearch(value) {
      if (value && value.length >= 2) {
        await this.loadBrandSuggestions(value)
      }
    },

    handleBrandSelect(value) {
      this.searchForm.brandName = value
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

    formatAISuggestion(suggestion) {
      if (!suggestion) return ''
      if (typeof suggestion === 'string') {
        return `<div class="whitespace-pre-wrap">${suggestion}</div>`
      }
      return `<div class="whitespace-pre-wrap">${JSON.stringify(suggestion, null, 2)}</div>`
    },

    formatPatterns(patterns) {
      if (!patterns) return ''
      if (typeof patterns === 'string') {
        return `<div class="whitespace-pre-wrap">${patterns}</div>`
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

@media (max-width: 768px) {
  .competitors-page {
    padding: 12px;
  }

  .ad-image-container {
    height: 150px;
  }
}
</style>
