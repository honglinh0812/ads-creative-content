<template>
  <div class="ad-learn-layout">
    <div class="ad-learn-inner">
      <a-page-header
        class="page-header"
        :title="$t('adLearn.pageHeader.title')"
        :sub-title="$t('adLearn.pageHeader.subtitle')"
      >
        <template #extra>
          <a-button type="default" @click="$router.push('/ads')">
            {{ $t('adLearn.pageHeader.backToAds') }}
          </a-button>
        </template>
      </a-page-header>

      <a-card class="progress-card surface-card" :bordered="false">
        <div class="progress-heading">
          <div>
            <div class="progress-title">{{ $t('adLearn.progress.headerTitle') }}</div>
            <div class="progress-subtitle">{{ $t('adLearn.progress.stepOf', { step: currentStep, total: 3 }) }}</div>
          </div>
          <span class="progress-tag">Step {{ currentStep }} / 3</span>
        </div>
        <a-steps :current="currentStep - 1" size="small" responsive>
          <a-step
            v-for="(step, index) in progressSteps"
            :key="index"
            :title="step.title"
            :description="step.description"
          />
        </a-steps>
      </a-card>

      <div v-if="currentStep === 1" class="step-wrapper">
        <a-card class="surface-card" :bordered="false">
          <div class="section-heading">
            <a-typography-title :level="4">{{ $t('adLearn.step1.cardTitle') }}</a-typography-title>
            <p class="section-description">
              {{ $t('adLearn.progressSteps.step1.description') }}
            </p>
          </div>
          <a-form layout="vertical" class="clean-form">
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

            <a-row :gutter="[16, 16]">
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('adLearn.step1.variations.label')">
                  <a-input-number
                    v-model:value="formData.numberOfVariations"
                    :min="1"
                    :max="5"
                    class="full-width-input"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
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
                class="inline-alert"
              />
            </a-form-item>

            <a-form-item :label="$t('adLearn.step1.websiteUrl.label')">
              <a-input
                v-model:value="formData.websiteUrl"
                :placeholder="$t('adLearn.step1.websiteUrl.placeholder')"
              />
              <div class="field-hint">
                {{ $t('adLearn.step1.websiteUrl.helper') }}
              </div>
            </a-form-item>

            <a-card
              v-if="hasReferenceInsights"
              class="reference-intel-card"
              size="small"
              :bordered="false"
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

          <div class="form-actions">
            <a-button
              type="primary"
              size="large"
              :loading="extracting"
              :disabled="!validateStep1()"
              @click="handleNextFromStep1"
            >
              {{ $t('adLearn.step1.next') }}
            </a-button>
          </div>
        </a-card>
      </div>

      <div v-if="currentStep === 2" class="step-wrapper">
        <a-card class="surface-card" :bordered="false">
          <div class="section-heading">
            <a-typography-title :level="4">{{ $t('adLearn.step2.cardTitle') }}</a-typography-title>
            <p class="section-description">{{ $t('adLearn.step2.sectionSubtitle') }}</p>
          </div>
          <div class="provider-grid">
            <div class="provider-panel">
              <div class="provider-label">{{ $t('adLearn.step2.textProvider') }}</div>
              <a-select v-model:value="formData.textProvider" class="full-width-input">
                <a-select-option
                  v-for="provider in textProviders"
                  :key="provider.value"
                  :value="provider.value"
                >
                  {{ provider.name }}
                </a-select-option>
              </a-select>
            </div>
            <div class="provider-panel">
              <div class="provider-label">{{ $t('adLearn.step2.imageProvider') }}</div>
              <a-select v-model:value="formData.imageProvider" class="full-width-input">
                <a-select-option
                  v-for="provider in imageProviders"
                  :key="provider.value"
                  :value="provider.value"
                >
                  {{ provider.name }}
                </a-select-option>
              </a-select>
            </div>
          </div>

          <div class="character-limit-toggle">
            <a-checkbox v-model:checked="formData.allowUnlimitedLength">
              {{ $t('adLearn.step2.allowUnlimitedLength.label') }}
            </a-checkbox>
            <div class="toggle-hint">
              {{ $t('adLearn.step2.allowUnlimitedLength.description') }}
            </div>
          </div>

          <div class="form-actions dual">
            <a-button size="large" @click="prevStep">
              {{ $t('adLearn.navigation.back') }}
            </a-button>
            <a-button
              type="primary"
              size="large"
              @click="generateAd"
              :loading="isGenerating"
              :disabled="!validateStep2()"
            >
              {{ $t('adLearn.step2.generate') }}
            </a-button>
          </div>
        </a-card>
      </div>

      <div v-if="currentStep === 3" class="step-wrapper">
        <a-card class="surface-card" :bordered="false">
          <div class="section-heading">
            <a-typography-title :level="4">{{ $t('adLearn.step3.cardTitle') }}</a-typography-title>
            <p class="section-description">{{ $t('adLearn.progressSteps.step3.description') }}</p>
          </div>
          <FieldError :error="generateError" />
          <a-empty v-if="!adVariations.length" :description="$t('adLearn.step3.empty')" />
          <div v-else class="variation-grid">
            <a-card
              v-for="variation in adVariations"
              :key="getVariationIdentifier(variation)"
              class="variation-card surface-card"
              :bordered="false"
              :class="{ selected: isVariationSelected(variation) }"
              @click="selectVariation(variation)"
              hoverable
            >
              <div class="variation-header">
                <span class="variation-title">{{ variation.headline || $t('adLearn.step3.defaultHeadline') }}</span>
                <a-tag color="blue">{{ getProviderName(formData.textProvider) }}</a-tag>
              </div>
              <p class="variation-body">{{ variation.primaryText || variation.body || variation.text }}</p>
              <div v-if="variation.callToAction" class="ad-preview-cta">
                {{ getCTALabel(variation.callToAction) }}
              </div>
              <div v-if="variation.imageUrl" class="variation-image">
                <img :src="variation.imageUrl" alt="Ad" />
              </div>
            </a-card>
          </div>

          <div class="form-actions dual">
            <a-button size="large" @click="prevStep">
              {{ $t('adLearn.navigation.back') }}
            </a-button>
            <a-button
              type="primary"
              size="large"
              :disabled="!selectedVariations.length"
              :loading="isSaving"
              @click="saveAd"
            >
              {{ $t('adLearn.step3.save') }}
            </a-button>
          </div>
        </a-card>
      </div>
    </div>

    <a-modal
      v-model:visible="showAsyncProgressModal"
      :title="$t('adLearn.async.modalTitle')"
      :closable="false"
      :footer="null"
      :maskClosable="false"
      :width="480"
    >
      <div class="async-progress-container">
        <div class="progress-icon">
          <a-spin size="large" />
        </div>
        <div class="progress-info">
          <h3>{{ asyncJobCurrentStep || $t('adLearn.async.initializing') }}</h3>
          <a-progress
            :percent="asyncJobProgress"
            :status="asyncJobStatus === 'FAILED' ? 'exception' : 'active'"
            :show-info="true"
          />
          <p class="progress-details">
            {{ $t('adLearn.async.hint') }}
          </p>
          <p class="progress-jobid">
            {{ $t('adLearn.async.jobId') }}: {{ asyncJobId }}
          </p>
        </div>
        <div class="progress-actions">
          <a-button @click="cancelAsyncJob" danger>
            {{ $t('adLearn.async.cancel') }}
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import api from '@/services/api'
import FieldError from '@/components/FieldError.vue'
import { detectLanguage, i18nTemplates } from '@/utils/languageDetector'

export default {
  name: 'AdLearn',
  components: {
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
        personaId: null,
        websiteUrl: '',
        allowUnlimitedLength: false
      },
      textProviders: [],
      imageProviders: [],
      adVariations: [],
      selectedVariations: [],
      extractedContent: '',
      referenceContent: '',
      referenceSummaryText: '',
      referenceInsights: null,
      detectedStyle: null,
      detectedCallToAction: null,
      adId: null,
      hasReferenceInsights: false,
      loadingCampaigns: false,
      loadingCTAs: false,
      extracting: false,
      isGenerating: false,
      isSaving: false,
      generateError: null,
      showAsyncProgressModal: false,
      asyncJobId: null,
      asyncJobStatus: null,
      asyncJobProgress: 0,
      asyncJobCurrentStep: '',
      pollingInterval: null,
      pollingStartTime: null,
      pollingRetryCount: 0
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
  beforeUnmount() {
    this.stopJobPolling()
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
      const manualUrl = this.formData.websiteUrl ? this.formData.websiteUrl.trim() : ''
      if (manualUrl) {
        return manualUrl
      }
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
          referenceInsights: this.referenceInsights || null,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          language: this.formData.language,
          callToAction: this.determineCallToAction(),
          creativeStyle: this.detectedStyle,
          websiteUrl: this.determineWebsiteUrl(),
          personaId: this.formData.personaId || null,
          allowUnlimitedLength: this.formData.allowUnlimitedLength
        }

        const startedAsync = await this.startAsyncGeneration(requestData)
        if (startedAsync) {
          return
        }

        await this.generateAdSync(requestData)
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
          websiteUrl: this.determineWebsiteUrl(),
          allowUnlimitedLength: this.formData.allowUnlimitedLength
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
    async startAsyncGeneration(requestData) {
      try {
        const response = await api.post('/ads/learn/async/generate', requestData)
        if (!response.data || !response.data.jobId) {
          return false
        }
        this.asyncJobId = response.data.jobId
        this.asyncJobStatus = 'PENDING'
        this.asyncJobProgress = 0
        this.asyncJobCurrentStep = ''
        this.showAsyncProgressModal = true
        this.startJobPolling()
        this.$message.info(this.$t('adLearn.async.started'))
        return true
      } catch (error) {
        const status = error.response?.status
        if (status === 429) {
          this.$message.warning(error.response?.data?.error || this.$t('adLearn.messages.warning.asyncJobLimit'))
        } else {
          console.warn('Async generation unavailable, falling back to sync.', error)
          this.$message.warning(this.$t('adLearn.messages.warning.asyncUnavailable'))
        }
        return false
      }
    },
    async generateAdSync(requestData) {
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
    },
    startJobPolling() {
      this.stopJobPolling()
      this.pollingRetryCount = 0
      this.pollingStartTime = Date.now()
      this.pollingInterval = setInterval(() => {
        this.checkJobStatus()
      }, 2000)
      this.checkJobStatus()
    },
    async checkJobStatus() {
      if (!this.asyncJobId) return

      const maxDuration = 10 * 60 * 1000
      if (this.pollingStartTime && Date.now() - this.pollingStartTime > maxDuration) {
        this.handleJobFailed(this.$t('adLearn.async.timeout'))
        return
      }

      try {
        const response = await api.ads.getJobStatus(this.asyncJobId)
        const job = response.data
        this.asyncJobStatus = job.status
        this.asyncJobProgress = job.progress || 0
        this.asyncJobCurrentStep = job.currentStep || ''

        if (job.status === 'COMPLETED') {
          await this.handleJobCompleted()
        } else if (job.status === 'FAILED') {
          this.handleJobFailed(job.errorMessage)
        } else if (job.status === 'CANCELLED') {
          this.handleJobCancelled()
        }

        this.pollingRetryCount = 0
      } catch (error) {
        console.error('Error checking async job status:', error)
        this.pollingRetryCount++
        if (this.pollingRetryCount > 3) {
          this.handleJobFailed(this.$t('adLearn.async.statusFailed'))
        }
      }
    },
    async handleJobCompleted() {
      this.stopJobPolling()
      try {
        const response = await api.ads.getJobResult(this.asyncJobId)
        const result = response.data?.result || []
        this.adVariations = this.normalizeVariations(result)
        this.selectedVariations = [...this.adVariations]
        this.currentStep = 3
        this.showAsyncProgressModal = false
        this.asyncJobId = null
        this.$message.success(this.$t('adLearn.messages.success.generated'))
      } catch (error) {
        console.error('Failed to fetch async job result:', error)
        this.handleJobFailed(error.message || this.$t('adLearn.async.statusFailed'))
      }
    },
    handleJobFailed(message) {
      this.stopJobPolling()
      this.showAsyncProgressModal = false
      this.asyncJobStatus = 'FAILED'
      this.asyncJobId = null
      this.$message.error(message || this.$t('adLearn.messages.error.generateAdFailed'))
    },
    handleJobCancelled() {
      this.stopJobPolling()
      this.showAsyncProgressModal = false
      this.asyncJobStatus = 'CANCELLED'
      this.asyncJobId = null
      this.$message.info(this.$t('adLearn.async.cancelled'))
    },
    stopJobPolling() {
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval)
        this.pollingInterval = null
      }
    },
    async cancelAsyncJob() {
      if (!this.asyncJobId) return
      try {
        this.asyncJobStatus = 'CANCELLING'
        await api.ads.cancelJob(this.asyncJobId)
        this.handleJobCancelled()
      } catch (error) {
        console.error('Failed to cancel async job', error)
        this.$message.error(this.$t('adLearn.async.cancelFailed'))
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
.ad-learn-layout {
  min-height: 100vh;
  background: #f4f7fb;
  padding: 24px 16px 48px;
}

.ad-learn-inner {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  padding: 0;
  margin-bottom: 16px;
}

.surface-card {
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.progress-card {
  margin-bottom: 20px;
  padding: 20px;
}

.progress-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.progress-subtitle {
  color: #64748b;
  font-size: 14px;
}

.progress-tag {
  color: #0f172a;
  font-weight: 600;
}

.step-wrapper {
  margin-bottom: 24px;
}

.section-heading {
  margin-bottom: 24px;
}

.section-description {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}

.clean-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #0f172a;
}

.full-width-input {
  width: 100%;
}

.inline-alert {
  margin-top: 12px;
}

.field-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.reference-intel-card {
  margin-top: 12px;
  background: #f8fbff;
  border: 1px solid #e0edff;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.form-actions.dual {
  justify-content: space-between;
}

.character-limit-toggle {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.toggle-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.provider-panel {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fdfefe;
}

.provider-label {
  font-weight: 600;
  margin-bottom: 8px;
  color: #0f172a;
}

.variation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.variation-card {
  cursor: pointer;
  border: 1px solid #e2e8f0;
  box-shadow: none;
}

.variation-card:hover {
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
}

.variation-card.selected {
  border-color: #2563eb;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.18);
}

.variation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.variation-title {
  font-weight: 600;
  color: #0f172a;
}

.variation-body {
  white-space: pre-wrap;
  color: #1f2937;
  min-height: 72px;
}

.variation-image img {
  width: 100%;
  border-radius: 10px;
  margin-top: 8px;
}

.ad-preview-cta {
  display: inline-flex;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 600;
  margin-top: 8px;
}

.async-progress-container {
  text-align: center;
}

.async-progress-container .progress-icon {
  margin-bottom: 20px;
}

.async-progress-container .progress-info h3 {
  margin-bottom: 12px;
  font-weight: 600;
  color: #1d4ed8;
}

.async-progress-container .progress-details,
.async-progress-container .progress-jobid {
  margin-top: 12px;
  font-size: 13px;
  color: #64748b;
}

.async-progress-container .progress-actions {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .ad-learn-layout {
    padding: 16px 12px 32px;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions.dual {
    gap: 12px;
  }
}
</style>
