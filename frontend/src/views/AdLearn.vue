<template>
  <div class="ad-create-page">
    <div class="ad-create-content">
      <a-page-header :title="$t('adLearn.pageHeader.title')" :sub-title="$t('adLearn.pageHeader.subtitle')">
        <template #extra>
          <a-button @click="$router.push('/ads')">
            <template #icon><arrow-left-outlined /></template>
            {{ $t('adLearn.pageHeader.backToAds') }}
          </a-button>
        </template>
      </a-page-header>

      <div class="creative-progress-container">
        <div class="progress-header">
          <h2 class="progress-title">{{ $t('adLearn.progress.headerTitle') }}</h2>
          <div class="progress-subtitle">{{ $t('adLearn.progress.stepOf', { step: currentStep, total: 3 }) }}</div>
        </div>

        <div class="creative-progress-tracker">
          <div class="progress-line">
            <div class="progress-fill" :style="{ width: (currentStep / 3 * 100) + '%' }"></div>
          </div>
          <div class="progress-steps">
            <div
              v-for="(step, index) in progressSteps"
              :key="index"
              class="progress-step"
              :class="{
                active: currentStep === index + 1,
                completed: currentStep > index + 1,
                current: currentStep === index + 1
              }"
            >
              <div class="step-circle">
                <div v-if="currentStep > index + 1" class="step-check">✓</div>
                <div v-else class="step-number">{{ index + 1 }}</div>
              </div>
              <div class="step-content">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-desc">{{ step.description }}</div>
                <div v-if="currentStep === index + 1" class="step-emoji">{{ step.emoji }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="currentStep === 1" class="step-content">
        <a-card :title="$t('adLearn.step1.cardTitle')" class="enhanced-card">
          <a-form layout="vertical">
            <a-form-item :label="$t('adLearn.step1.campaign.label')" required>
              <a-select
                v-model:value="formData.campaignId"
                :placeholder="$t('adLearn.step1.campaign.placeholder')"
                :loading="loadingCampaigns"
                show-search
                :filter-option="false"
                @search="loadCampaigns"
                @change="handleCampaignChange"
              >
                <a-select-option
                  v-for="campaign in campaigns"
                  :key="campaign.id"
                  :value="campaign.id"
                >
                  {{ campaign.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item :label="$t('adLearn.step1.adName.label')" required>
              <a-input
                v-model:value="formData.name"
                :placeholder="$t('adLearn.step1.adName.placeholder')"
                :maxlength="100"
                show-count
              />
            </a-form-item>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="$t('adLearn.step1.variations.label')">
                  <a-input-number
                    v-model:value="formData.numberOfVariations"
                    :min="1"
                    :max="5"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="$t('adLearn.step1.language.label')">
                  <a-select v-model:value="formData.language">
                    <a-select-option value="vi">{{ $t('adLearn.step1.language.vietnamese') }}</a-select-option>
                    <a-select-option value="en">{{ $t('adLearn.step1.language.english') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="$t('adLearn.step1.baseContent.label')" required>
              <a-textarea
                v-model:value="formData.baseContent"
                :rows="5"
                :maxlength="2000"
                show-count
                :placeholder="$t('adLearn.step1.baseContent.placeholder')"
              />
            </a-form-item>

            <a-form-item :label="$t('adLearn.step1.referenceLink.label')" required>
              <a-input
                v-model:value="formData.referenceLink"
                :placeholder="$t('adLearn.step1.referenceLink.placeholder')"
              />
              <a-alert
                v-if="referenceSummaryText"
                type="success"
                :message="$t('adLearn.step1.referenceLink.previewReady')"
                :description="referenceSummaryText || $t('adLearn.step1.referenceLink.fallbackSummary')"
                show-icon
                style="margin-top: 8px"
              />
            </a-form-item>

            <a-card
              v-if="hasReferenceInsights"
              class="reference-intel-card"
              size="small"
            >
              <a-descriptions
                :column="1"
                size="small"
                :title="$t('adLearn.referenceSummary.title')"
              >
                <a-descriptions-item :label="$t('adLearn.referenceSummary.summary')">
                  {{ referenceSummaryText || $t('adLearn.referenceSummary.fallback') }}
                </a-descriptions-item>
                <a-descriptions-item :label="$t('adLearn.referenceSummary.detectedStyle')">
                  {{ detectedStyleLabel }}
                </a-descriptions-item>
                <a-descriptions-item :label="$t('adLearn.referenceSummary.detectedCTA')">
                  {{ detectedCTALabel }}
                </a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-form>

          <div class="step-navigation">
            <a-button
              type="primary"
              size="large"
              :loading="extracting"
              :disabled="!validateStep1()"
              @click="handleNextFromStep1"
            >
              {{ $t('adLearn.step1.next') }}
              <template #icon><arrow-right-outlined /></template>
            </a-button>
          </div>
        </a-card>
      </div>

      <div v-if="currentStep === 2" class="step-content">
        <a-card :title="$t('adLearn.step2.cardTitle')" class="enhanced-card">
          <div class="ai-config-container">
            <div class="ai-section-header">
              <h2 class="section-title-magic">{{ $t('adLearn.step2.sectionTitle') }}</h2>
              <p class="section-subtitle-magic">{{ $t('adLearn.step2.sectionSubtitle') }}</p>
            </div>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-card size="small" class="provider-card">
                  <div class="provider-header">
                    <robot-outlined />
                    <span class="provider-title">{{ $t('adLearn.step2.textProvider') }}</span>
                  </div>
                  <a-select v-model:value="formData.textProvider" style="width: 100%">
                    <a-select-option
                      v-for="provider in textProviders"
                      :key="provider.value"
                      :value="provider.value"
                    >
                      {{ provider.name }}
                    </a-select-option>
                  </a-select>
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card size="small" class="provider-card">
                  <div class="provider-header">
                    <file-image-outlined />
                    <span class="provider-title">{{ $t('adLearn.step2.imageProvider') }}</span>
                  </div>
                  <a-select v-model:value="formData.imageProvider" style="width: 100%">
                    <a-select-option
                      v-for="provider in imageProviders"
                      :key="provider.value"
                      :value="provider.value"
                    >
                      {{ provider.name }}
                    </a-select-option>
                  </a-select>
                </a-card>
              </a-col>
            </a-row>
          </div>

          <div class="step-navigation spaced">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              {{ $t('adLearn.navigation.back') }}
            </a-button>
            <a-button
              type="primary"
              size="large"
              @click="generateAd"
              :loading="isGenerating"
              :disabled="!validateStep2()"
            >
              <robot-outlined />
              {{ $t('adLearn.step2.generate') }}
            </a-button>
          </div>
        </a-card>
      </div>

      <div v-if="currentStep === 3" class="step-content">
        <a-card :title="$t('adLearn.step3.cardTitle')" class="enhanced-card">
          <FieldError :error="generateError" />
          <a-empty v-if="!adVariations.length" :description="$t('adLearn.step3.empty')" />
          <div v-else class="variation-grid">
            <a-card
              v-for="variation in adVariations"
              :key="getVariationIdentifier(variation)"
              class="variation-card"
              :class="{ selected: isVariationSelected(variation) }"
              @click="selectVariation(variation)"
              hoverable
            >
              <div class="variation-header">
                <span class="variation-title">{{ variation.headline || $t('adLearn.step3.defaultHeadline') }}</span>
                <a-tag color="blue">{{ getProviderName(formData.textProvider) }}</a-tag>
              </div>
              <p class="variation-body">{{ variation.body || variation.text }}</p>
              <div v-if="variation.callToAction" class="ad-preview-cta">
                {{ getCTALabel(variation.callToAction) }}
              </div>
              <div v-if="variation.imageUrl" class="variation-image">
                <img :src="variation.imageUrl" alt="Ad" />
              </div>
            </a-card>
          </div>

          <div class="step-navigation spaced">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              {{ $t('adLearn.navigation.back') }}
            </a-button>
            <a-button
              type="primary"
              size="large"
              :disabled="!selectedVariations.length"
              :loading="isSaving"
              @click="saveAd"
            >
              <save-outlined />
              {{ $t('adLearn.step3.save') }}
            </a-button>
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import {
  ArrowLeftOutlined,
  ArrowRightOutlined,
  RobotOutlined,
  FileImageOutlined,
  SaveOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'
import FieldError from '@/components/FieldError.vue'
import { detectLanguage, i18nTemplates } from '@/utils/languageDetector'

export default {
  name: 'AdLearn',
  components: {
    ArrowLeftOutlined,
    ArrowRightOutlined,
    RobotOutlined,
    FileImageOutlined,
    SaveOutlined,
    FieldError
  },
  data() {
    return {
      currentStep: 1,
      campaigns: [],
      selectedCampaign: null,
      formData: {
        campaignId: null,
        name: '',
        numberOfVariations: 3,
        language: 'vi',
        baseContent: '',
        referenceLink: '',
        textProvider: 'openai',
        imageProvider: 'gemini',
        personaId: null
      },
      textProviders: [],
      imageProviders: [],
      adVariations: [],
      selectedVariations: [],
      extractedContent: '',
      referenceContent: '',
      referenceSummaryText: '',
      referenceInsights: null,
      referenceAdData: null,
      detectedStyle: null,
      detectedCallToAction: null,
      adId: null,
      hasReferenceInsights: false,
      loadingCampaigns: false,
      loadingCTAs: false,
      extracting: false,
      isGenerating: false,
      isSaving: false,
      generateError: null
    }
  },
  computed: {
    ...mapGetters('cta', {
      standardCTAs: 'allCTAs',
      getCTALabel: 'getCTALabel'
    }),
    progressSteps() {
      return [
        {
          title: this.$t('adLearn.progressSteps.step1.title'),
          description: this.$t('adLearn.progressSteps.step1.description'),
          emoji: this.$t('adLearn.progressSteps.step1.emoji')
        },
        {
          title: this.$t('adLearn.progressSteps.step2.title'),
          description: this.$t('adLearn.progressSteps.step2.description'),
          emoji: this.$t('adLearn.progressSteps.step2.emoji')
        },
        {
          title: this.$t('adLearn.progressSteps.step3.title'),
          description: this.$t('adLearn.progressSteps.step3.description'),
          emoji: this.$t('adLearn.progressSteps.step3.emoji')
        }
      ]
    },
    adStyleOptions() {
      return [
        {
          value: 'PROFESSIONAL',
          label: this.$t('adCreate.adStyles.professional.label'),
          description: this.$t('adCreate.adStyles.professional.description')
        },
        {
          value: 'CASUAL',
          label: this.$t('adCreate.adStyles.casual.label'),
          description: this.$t('adCreate.adStyles.casual.description')
        },
        {
          value: 'HUMOROUS',
          label: this.$t('adCreate.adStyles.humorous.label'),
          description: this.$t('adCreate.adStyles.humorous.description')
        },
        {
          value: 'URGENT',
          label: this.$t('adCreate.adStyles.urgent.label'),
          description: this.$t('adCreate.adStyles.urgent.description')
        },
        {
          value: 'LUXURY',
          label: this.$t('adCreate.adStyles.luxury.label'),
          description: this.$t('adCreate.adStyles.luxury.description')
        },
        {
          value: 'EDUCATIONAL',
          label: this.$t('adCreate.adStyles.educational.label'),
          description: this.$t('adCreate.adStyles.educational.description')
        },
        {
          value: 'INSPIRATIONAL',
          label: this.$t('adCreate.adStyles.inspirational.label'),
          description: this.$t('adCreate.adStyles.inspirational.description')
        },
        {
          value: 'MINIMALIST',
          label: this.$t('adCreate.adStyles.minimalist.label'),
          description: this.$t('adCreate.adStyles.minimalist.description')
        }
      ]
    },
    detectedStyleLabel() {
      if (!this.detectedStyle) {
        return this.$t('adLearn.referenceSummary.notDetected')
      }
      const match = this.adStyleOptions.find(style => style.value === this.detectedStyle)
      return match ? match.label : this.detectedStyle
    },
    detectedCTALabel() {
      if (!this.detectedCallToAction) {
        return this.$t('adLearn.referenceSummary.notDetected')
      }
      return this.getCTALabel(this.detectedCallToAction) || this.detectedCallToAction
    }
  },
  created() {
    this.loadData()
  },
  watch: {
    'formData.language'() {
      this.loadCallToActions()
    }
  },
  methods: {
    ...mapActions('cta', ['loadCTAs']),
    async loadData() {
      await Promise.all([
        this.loadCampaigns(),
        this.loadCallToActions(),
        this.loadProviders()
      ])
    },
    async loadCampaigns() {
      this.loadingCampaigns = true
      try {
        const response = await api.campaigns.getAll()
        this.campaigns = response.data.content || []
      } catch (error) {
        console.error('Error loading campaigns:', error)
        this.$message.error(this.$t('adLearn.messages.error.loadCampaignsFailed'))
      } finally {
        this.loadingCampaigns = false
      }
    },
    async loadCallToActions() {
      this.loadingCTAs = true
      try {
        await this.loadCTAs({ language: this.formData.language })
      } catch (error) {
        console.error('Error loading CTAs:', error)
        this.$message.error(this.$t('adLearn.messages.error.loadCTAsFailed'))
      } finally {
        this.loadingCTAs = false
      }
    },
    async loadProviders() {
      try {
        const textResponse = await api.providers.getTextProviders()
        this.textProviders = textResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name
        }))
        const imageResponse = await api.providers.getImageProviders()
        this.imageProviders = imageResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name
        }))
        if (this.textProviders.length > 0 && !this.formData.textProvider) {
          this.formData.textProvider = this.textProviders[0].value
        }
        if (this.imageProviders.length > 0 && !this.formData.imageProvider) {
          this.formData.imageProvider = this.imageProviders[0].value
        }
      } catch (error) {
        console.error('Error loading providers:', error)
        this.$message.warning(this.$t('adLearn.messages.warning.loadProvidersFailed'))
      }
    },
    handleCampaignChange(id) {
      this.selectedCampaign = this.campaigns.find(c => c.id === id) || null
    },
    validateStep1() {
      return this.formData.campaignId &&
        this.formData.name &&
        this.formData.baseContent &&
        this.formData.referenceLink
    },
    validateStep2() {
      return this.formData.textProvider && this.formData.imageProvider
    },
    async handleNextFromStep1() {
      if (!this.validateStep1()) return
      await this.analyzeReference()
      this.currentStep = 2
    },
    async analyzeReference() {
      if (!this.formData.referenceLink) return false
      this.extracting = true
      try {
        const payload = {
          referenceLink: this.formData.referenceLink.trim(),
          fallbackContent: this.formData.baseContent
        }
        const response = await api.post('/ads/learn/reference', payload)
        this.referenceContent = response.data.referenceContent || ''
        this.extractedContent = this.referenceContent
        this.referenceInsights = response.data.insights || null
        this.referenceAdData = response.data.referenceAdData || null
        this.detectedStyle = response.data.detectedStyle || null
        this.detectedCallToAction = response.data.suggestedCallToAction || null
        this.referenceSummaryText = this.formatInsights(this.referenceInsights, response.data.message)
        this.hasReferenceInsights = true
        this.$message.success(this.$t('adLearn.messages.success.extracted'))
        return true
      } catch (error) {
        console.error('Reference analysis error:', error)
        this.$message.warning(this.$t('adLearn.messages.error.extractContentFailed'))
        this.referenceSummaryText = ''
        this.hasReferenceInsights = false
        return false
      } finally {
        this.extracting = false
      }
    },
    formatInsights(insights, fallbackMessage) {
      if (!insights) {
        return fallbackMessage || this.$t('adLearn.referenceSummary.fallback')
      }
      const { wordCount = 0, sentenceCount = 0, containsCallToAction, containsPrice } = insights
      return this.$t('adLearn.referenceSummary.details', {
        words: wordCount,
        sentences: sentenceCount,
        cta: containsCallToAction ? this.$t('common.action.yes') : this.$t('common.action.no'),
        price: containsPrice ? this.$t('common.action.yes') : this.$t('common.action.no')
      })
    },
    determineAdType() {
      if (this.selectedCampaign && this.selectedCampaign.websiteUrl) {
        return 'website_conversion'
      }
      return 'page_post'
    },
    determineWebsiteUrl() {
      return this.selectedCampaign && this.selectedCampaign.websiteUrl
        ? this.selectedCampaign.websiteUrl
        : null
    },
    determineCallToAction() {
      return this.detectedCallToAction || null
    },
    determineCallToActionLabel() {
      const cta = this.determineCallToAction()
      return cta ? (this.getCTALabel(cta) || cta) : ''
    },
    buildPrompt() {
      const language = detectLanguage(this.formData.baseContent || '') || this.formData.language
      const templates = i18nTemplates.adReference[language] || i18nTemplates.adReference.en
      const base = this.formData.baseContent || ''
      let prompt = `${base}\n\n${templates.instruction}`

      if (this.extractedContent) {
        prompt += `\n\n${templates.title}:\n${this.extractedContent}\n\n${this.$t('adLearn.prompt.referenceStyle')}`
      } else if (this.formData.referenceLink) {
        prompt += `\n\n${this.$t('adLearn.prompt.referenceLinkOnly', { link: this.formData.referenceLink })}`
      }

      if (this.detectedStyle) {
        const style = this.adStyleOptions.find(s => s.value === this.detectedStyle)
        prompt += `\n${this.$t('adLearn.prompt.styleCue', { style: style ? style.label : this.detectedStyle })}`
      }

      const ctaLabel = this.determineCallToActionLabel()
      if (ctaLabel) {
        prompt += `\n${this.$t('adLearn.prompt.ctaCue', { cta: ctaLabel })}`
      }

      prompt += `\n${this.$t('adLearn.prompt.guardrail')}`
      return prompt
    },
    async generateAd() {
      this.isGenerating = true
      this.generateError = null
      try {
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.determineAdType(),
          name: this.formData.name,
          productDescription: this.formData.baseContent,
          referenceLink: this.formData.referenceLink,
          referenceContent: this.referenceContent || this.formData.baseContent,
          referenceAdData: this.referenceAdData,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          language: this.formData.language,
          callToAction: this.determineCallToAction(),
          creativeStyle: this.detectedStyle,
          websiteUrl: this.determineWebsiteUrl(),
          personaId: this.formData.personaId || null
        }
        const response = await api.post('/ads/learn/generate', requestData)
        if (response.data.status === 'success') {
          this.adVariations = this.normalizeVariations(response.data.variations)
          this.selectedVariations = [...this.adVariations]
          this.adId = response.data.adId
          this.currentStep = 3
          this.$message.success(this.$t('adLearn.messages.success.generated'))
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Generate ad error:', error)
        this.generateError = error
        this.$message.error(error.message || this.$t('adLearn.messages.error.generateAdFailed'))
      } finally {
        this.isGenerating = false
      }
    },
    async saveAd() {
      if (!this.selectedVariations.length) {
        this.$message.warning(this.$t('adLearn.messages.warning.selectVariations'))
        return
      }
      this.isSaving = true
      try {
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.determineAdType(),
          name: this.formData.name,
          prompt: this.buildPrompt(),
          language: this.formData.language,
          adLinks: this.formData.referenceLink ? [this.formData.referenceLink.trim()] : [],
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          selectedVariations: this.selectedVariations,
          isPreview: false,
          saveExistingContent: true,
          adStyle: this.detectedStyle || null,
          callToAction: this.determineCallToAction(),
          extractedContent: this.extractedContent,
          websiteUrl: this.determineWebsiteUrl()
        }
        const response = await api.post('/ads/learn/save', requestData)
        if (response.data.status === 'success') {
          this.$message.success(this.$t('adLearn.messages.success.saved'))
          this.$router.push('/ads')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Save ad error:', error)
        this.$message.error(error.message || this.$t('adLearn.messages.error.saveAdFailed'))
      } finally {
        this.isSaving = false
      }
    },
    normalizeVariations(variations = []) {
      const timestamp = Date.now()
      return variations.map((variation, index) => {
        const clientId = variation.id || variation.clientId || `variation-${timestamp}-${index}`
        return {
          ...variation,
          clientId,
          imageUrl: variation.imageUrl || variation.uploadedFileUrl || variation.mediaFileUrl || ''
        }
      })
    },
    getVariationIdentifier(variation) {
      if (!variation) return null
      return variation.id || variation.clientId || `variation-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
    },
    isVariationSelected(variation) {
      const identifier = this.getVariationIdentifier(variation)
      return this.selectedVariations.some(v => this.getVariationIdentifier(v) === identifier)
    },
    selectVariation(variation) {
      const identifier = this.getVariationIdentifier(variation)
      const index = this.selectedVariations.findIndex(v => this.getVariationIdentifier(v) === identifier)
      if (index > -1) {
        this.selectedVariations = this.selectedVariations.filter(v => this.getVariationIdentifier(v) !== identifier)
      } else {
        this.selectedVariations = [...this.selectedVariations, { ...variation, clientId: identifier }]
      }
    },
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    getProviderName(providerValue) {
      const allProviders = [...this.textProviders, ...this.imageProviders]
      const provider = allProviders.find(p => p.value === providerValue)
      return provider ? provider.name : providerValue
    }
  }
}
</script>

<style scoped>
.ad-create-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.ad-create-content {
  max-width: 1100px;
  margin: 0 auto;
}

.creative-progress-container {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.05);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.progress-subtitle {
  color: #8c8c8c;
}

.creative-progress-tracker {
  margin-top: 10px;
}

.progress-line {
  height: 6px;
  background: #f0f0f0;
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 16px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  transition: width 0.3s ease;
}

.progress-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.progress-step {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.progress-step.current {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.progress-step.completed {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #d9d9d9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-weight: 700;
}

.progress-step.current .step-circle {
  border-color: #1890ff;
  color: #1890ff;
}

.progress-step.completed .step-circle {
  background: #52c41a;
  border-color: #52c41a;
  color: #fff;
}

.step-title {
  font-weight: 600;
}

.step-desc {
  color: #8c8c8c;
  font-size: 12px;
}

.step-emoji {
  margin-top: 4px;
}

.step-content {
  margin-bottom: 24px;
}

.enhanced-card {
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}

.step-navigation {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.step-navigation.spaced {
  justify-content: space-between;
}

.style-option {
  display: flex;
  flex-direction: column;
}

.style-label {
  font-weight: 600;
}

.style-description {
  font-size: 12px;
  color: #8c8c8c;
}

.ai-config-container {
  display: flex;
  flex-direction: column;
}

.ai-section-header {
  text-align: center;
  margin-bottom: 16px;
}

.section-title-magic {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
}

.section-subtitle-magic {
  margin: 0;
  color: #8c8c8c;
}

.provider-card {
  height: 100%;
}

.provider-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 600;
}

.provider-title {
  font-size: 14px;
}

.variation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.variation-card {
  cursor: pointer;
  transition: all 0.2s ease;
}

.variation-card.selected {
  border-color: #1890ff;
  box-shadow: 0 10px 30px rgba(24, 144, 255, 0.15);
}

.variation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.variation-title {
  font-weight: 700;
}

.variation-body {
  white-space: pre-wrap;
  min-height: 60px;
}

.variation-image img {
  width: 100%;
  border-radius: 8px;
  margin-top: 8px;
}

.ad-preview-cta {
  display: inline-block;
  background: #e6f7ff;
  color: #1890ff;
  padding: 6px 10px;
  border-radius: 6px;
  font-weight: 600;
  margin-top: 8px;
}

.reference-intel-card {
  margin-top: 12px;
  border: 1px solid #dbeafe;
  background: #f7fbff;
}
</style>
