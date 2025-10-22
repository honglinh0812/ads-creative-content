<template>
  <div class="ad-create-page">
    <div class="ad-create-content">

      <!-- Page Header -->
      <a-page-header title="Create New Ad" sub-title="Generate compelling ads with AI assistance">
        <template #extra>
          <a-button @click="$router.push('/dashboard')">
            <template #icon><arrow-left-outlined /></template>
            Back to Dashboard
          </a-button>
        </template>
      </a-page-header>

      <!-- Creative Progress Steps -->
      <div class="creative-progress-container">
        <div class="progress-header">
          <h2 class="progress-title">Let's create your ad</h2>
          <div class="progress-subtitle">Step {{ currentStep }} of 3</div>
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

        <!-- Fun Progress Indicator -->
        <div class="fun-progress">
          <div class="progress-character">
            <span v-if="currentStep === 1">🎯</span>
            <span v-else-if="currentStep === 2">🤖</span>
            <span v-else>🎉</span>
          </div>
          <div class="progress-message">
            <span v-if="currentStep === 1">Tell us about your ad</span>
            <span v-else-if="currentStep === 2">Pick your AI superpowers</span>
            <span v-else>Almost there!</span>
          </div>
        </div>
      </div>

      <!-- Step 1: Basic Information -->
      <div v-if="currentStep === 1" class="step-content">
        <a-card title="Basic Information" class="enhanced-card">
          <a-form layout="vertical">
            <!-- Campaign Selection -->
            <a-form-item label="Campaign" required>
              <a-select 
                v-model:value="formData.campaignId" 
                placeholder="Select a campaign"
                :loading="loadingCampaigns"
                show-search
                :filter-option="false"
                @search="loadCampaigns"
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

            <!-- Ad Type Selection -->
            <a-form-item>
              <template #label>
                <span>Ad Type</span>
                <a-button type="text" size="small" @click="showAdTypeHelp = true">
                  <template #icon><question-circle-outlined /></template>
                </a-button>
              </template>
              <a-radio-group v-model:value="formData.adType" @change="onAdTypeChange">
                <a-radio 
                  v-for="type in adTypes" 
                  :key="type.value" 
                  :value="type.value"
                  class="ad-type-radio"
                >
                  {{ type.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>

            <!-- Ad Name -->
            <a-form-item label="Ad Name" required>
              <a-input 
                v-model:value="formData.name" 
                placeholder="Enter ad name"
                :maxlength="100"
                show-count
              />
            </a-form-item>

            <!-- Number of Variations -->
            <a-form-item label="Number of Variations">
              <a-input-number 
                v-model:value="formData.numberOfVariations" 
                :min="1" 
                :max="5" 
                style="width: 100%"
              />
            </a-form-item>

            <!-- Language -->
            <a-form-item label="Language">
              <a-select v-model:value="formData.language" placeholder="Select language">
                <a-select-option value="vi">Vietnamese</a-select-option>
                <a-select-option value="en">English</a-select-option>
              </a-select>
            </a-form-item>

            <!-- Call to Action -->
            <a-form-item label="Call to Action">
              <a-select 
                v-model:value="formData.callToAction" 
                placeholder="Select call to action"
                :loading="loadingCTAs"
              >
                <a-select-option 
                  v-for="cta in standardCTAs" 
                  :key="cta.value" 
                  :value="cta.value"
                >
                  {{ cta.label }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <!-- Website URL (for website conversion ads) -->
            <a-form-item 
              v-if="formData.adType === 'website_conversion'" 
              label="Website URL" 
              required
            >
              <a-input 
                v-model:value="formData.websiteUrl" 
                placeholder="https://example.com"
                type="url"
              />
            </a-form-item>

            <!-- Lead Form Questions (for lead generation ads) -->
            <a-form-item 
              v-if="formData.adType === 'lead_generation'" 
              label="Lead Form Questions"
            >
              <div class="lead-form-questions">
                <div 
                  v-for="(question, index) in formData.leadFormQuestions" 
                  :key="index" 
                  class="question-item"
                >
                  <a-input 
                    v-model:value="formData.leadFormQuestions[index]" 
                    placeholder="Enter question"
                  >
                    <template #suffix>
                      <a-button 
                        type="text" 
                        size="small" 
                        @click="removeLeadFormQuestion(index)"
                        danger
                      >
                        <template #icon><delete-outlined /></template>
                      </a-button>
                    </template>
                  </a-input>
                </div>
                <a-button 
                  type="dashed" 
                  @click="addLeadFormQuestion" 
                  block
                  style="margin-top: 8px"
                >
                  <template #icon><plus-outlined /></template>
                  Add Question
                </a-button>
              </div>
            </a-form-item>

            <!-- Prompt -->
            <a-form-item>
              <template #label>
                <span>Prompt</span>
                <a-button type="text" size="small" @click="showHelpDialog = true">
                  <template #icon><question-circle-outlined /></template>
                </a-button>
              </template>
              <a-textarea 
                v-model:value="formData.prompt" 
                placeholder="Describe your product/service or enter specific instructions for the AI..."
                :rows="4"
                :maxlength="2000"
                show-count
              />
              
              <!-- Prompt Validator Component -->
              <PromptValidator
                :prompt="formData.prompt"
                :ad-type="formData.adType"
                :language="formData.language"
                :target-audience="formData.targetAudience || ''"
                :auto-validate="true"
                @prompt-updated="onPromptUpdated"
              />
            </a-form-item>

            <!-- Ad Links -->
            <a-form-item label="Ad Links (Optional)">
              <div class="ad-links-container">
                <div 
                  v-for="(link, index) in adLinks" 
                  :key="index" 
                  class="ad-link-item"
                >
                  <a-input 
                    v-model:value="adLinks[index]" 
                    placeholder="https://example.com/ad"
                    ref="adLinkInput"
                  >
                    <template #suffix>
                      <a-button 
                        type="text" 
                        size="small" 
                        @click="removeAdLink(index)"
                        danger
                      >
                        <template #icon><delete-outlined /></template>
                      </a-button>
                    </template>
                  </a-input>
                </div>
                <a-button 
                  type="dashed" 
                  @click="addAdLink" 
                  block
                  style="margin-top: 8px"
                >
                  <template #icon><plus-outlined /></template>
                  Add Link
                </a-button>
                <a-button 
                  type="primary" 
                  @click="extractFromLibrary" 
                  :loading="extracting"
                  style="margin-top: 8px"
                  block
                >
                  <template #icon><download-outlined /></template>
                  Extract Content from Links
                </a-button>
              </div>
            </a-form-item>

            <!-- Audience Segment Targeting -->
            <AudienceSegmentForm v-model="formData.audienceSegment" />

            <!-- Persona Selector -->
            <PersonaSelector
              v-model="formData.personaId"
              @persona-selected="handlePersonaSelected"
            />

            <!-- Trending Keywords -->
            <TrendingKeywords @keywords-selected="handleKeywordsSelected" />
          </a-form>

          <!-- Error Display -->
          <FieldError :error="generateError" />

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button
              type="primary"
              @click="nextStep"
              :disabled="!validateStep1()"
              size="large"
            >
              Next: AI Configuration
              <template #icon><arrow-right-outlined /></template>
            </a-button>
          </div>
        </a-card>
      </div>

      <!-- Step 2: AI Configuration -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="ai-config-container">
          <div class="ai-section-header">
            <h2 class="section-title-magic">Choose your AI superpowers! 🤖</h2>
            <p class="section-subtitle-magic">Select the best AI providers for your creative needs</p>
          </div>

          <!-- Text Provider Selection - Creative Layout -->
          <div class="provider-section text-providers">
            <div class="provider-section-header">
              <div class="section-icon">✍️</div>
              <div>
                <h3 class="provider-title">Text Generation</h3>
                <p class="provider-subtitle">Pick your copywriting assistant</p>
              </div>
            </div>

            <div class="creative-provider-grid">
              <div
                v-for="provider in textProviders"
                :key="provider.value"
                class="creative-provider-card text-provider"
                :class="{
                  selected: formData.textProvider === provider.value,
                  disabled: !provider.enabled
                }"
                @click="formData.textProvider = provider.value"
              >
                <input
                  type="radio"
                  :value="provider.value"
                  v-model="formData.textProvider"
                  style="display: none"
                />

                <div class="provider-visual">
                  <div class="provider-icon">{{ getProviderIcon(provider.value, 'text') }}</div>
                  <div class="provider-status" v-if="!provider.enabled">⚠️</div>
                </div>

                <div class="provider-info">
                  <h4 class="provider-name">{{ provider.name }}</h4>
                  <p class="provider-desc">{{ provider.description }}</p>

                  <div class="provider-badges">
                    <span
                      v-for="feature in provider.features"
                      :key="feature"
                      class="feature-badge"
                    >
                      {{ feature }}
                    </span>
                  </div>

                  <div class="provider-rating">
                    <div class="rating-stars">
                      <span v-for="n in getProviderRating(provider.value)" :key="n">⭐</span>
                    </div>
                    <span class="rating-text">{{ getProviderRatingText(provider.value) }}</span>
                  </div>
                </div>

                <div class="provider-selection">
                  <div class="selection-indicator">
                    <div class="selection-dot"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Image Provider Selection - Creative Layout -->
          <div class="provider-section image-providers">
            <div class="provider-section-header">
              <div class="section-icon">🎨</div>
              <div>
                <h3 class="provider-title">Image Generation</h3>
                <p class="provider-subtitle">Choose your visual artist</p>
              </div>
            </div>

            <!-- Image Upload Info Message -->
            <a-alert
              v-if="hasUploadedImage"
              message="Image already uploaded"
              description="You've uploaded an image, so AI image generation is disabled. Remove the uploaded image to enable AI image providers."
              type="info"
              show-icon
              closable
              style="margin-bottom: 16px;"
            />

            <div class="creative-provider-grid">
              <div
                v-for="provider in imageProviders"
                :key="provider.value"
                class="creative-provider-card image-provider"
                :class="{
                  selected: formData.imageProvider === provider.value,
                  disabled: !provider.enabled || hasUploadedImage
                }"
                @click="!hasUploadedImage && provider.enabled ? (formData.imageProvider = provider.value) : null"
              >
                <input
                  type="radio"
                  :value="provider.value"
                  v-model="formData.imageProvider"
                  style="display: none"
                />

                <div class="provider-visual">
                  <div class="provider-icon">{{ getProviderIcon(provider.value, 'image') }}</div>
                  <div class="provider-status" v-if="!provider.enabled">⚠️</div>
                </div>

                <div class="provider-info">
                  <h4 class="provider-name">{{ provider.name }}</h4>
                  <p class="provider-desc">{{ provider.description }}</p>

                  <div class="provider-badges">
                    <span
                      v-for="feature in provider.features"
                      :key="feature"
                      class="feature-badge"
                    >
                      {{ feature }}
                    </span>
                  </div>

                  <div class="provider-rating">
                    <div class="rating-stars">
                      <span v-for="n in getProviderRating(provider.value)" :key="n">⭐</span>
                    </div>
                    <span class="rating-text">{{ getProviderRatingText(provider.value) }}</span>
                  </div>
                </div>

                <div class="provider-selection">
                  <div class="selection-indicator">
                    <div class="selection-dot"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- AI Power Level Indicator -->
          <div class="ai-power-indicator">
            <div class="power-header">
              <span class="power-icon">⚡</span>
              <span class="power-title">AI Power Level</span>
            </div>
            <div class="power-bar">
              <div class="power-fill" :style="{ width: calculateAIPowerLevel() + '%' }"></div>
            </div>
            <div class="power-message">{{ getAIPowerMessage() }}</div>
          </div>
        </div>

          <!-- Optional Media Upload -->
          <a-form-item label="Upload Media (Optional)">
            <a-upload
              :file-list="uploadedFiles"
              :before-upload="handleFileUpload"
              @remove="removeFile"
              accept="image/*,video/*"
              list-type="picture-card"
            >
              <div v-if="uploadedFiles.length < 1">
                <plus-outlined />
                <div style="margin-top: 8px">Upload</div>
              </div>
            </a-upload>
          </a-form-item>

          <!-- Enhancement Options -->
          <a-form-item label="Image Enhancement Options" v-if="uploadedFiles.length > 0">
            <a-checkbox-group v-model:value="formData.enhancementOptions">
              <a-checkbox value="upscale">Upscale Image</a-checkbox>
              <a-checkbox value="remove_background">Remove Background</a-checkbox>
              <a-checkbox value="style_transfer">Style Transfer</a-checkbox>
            </a-checkbox-group>
            <a-button @click="enhanceImages" :loading="isEnhancing" style="margin-top: 10px;">Apply Enhancements & Preview</a-button>
          </a-form-item>

          <!-- Enhancement Previews -->
          <a-form-item label="Enhanced Images Preview" v-if="enhancedImages.length > 0">
            <div class="enhanced-previews">
              <div v-for="(img, index) in enhancedImages" :key="index" class="preview-item">
                <img :src="img.url" alt="Enhanced Preview" style="max-width: 300px;" />
                <p>Original: {{ img.originalName }}</p>
              </div>
            </div>
          </a-form-item>

          <!-- Error Display -->
          <FieldError :error="generateError" />

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              Previous
            </a-button>
            <a-button
              type="primary"
              @click="generateAd"
              :loading="isGenerating"
              :disabled="!validateStep2()"
              size="large"
            >
              <template #icon><thunderbolt-outlined /></template>
              Generate Ad
            </a-button>
          </div>
        </div>
      </div>

      <!-- Step 3: Preview & Save -->
      <div v-if="currentStep === 3" class="step-content">
        <a-card title="Preview & Save" class="enhanced-card">
          <div v-if="adVariations.length > 0" class="ad-preview-container">
            <p class="preview-instruction">
              Select your preferred ad variation. Quality scores help you choose the best one!
            </p>

            <!-- Quality Score Summary Card -->
            <div v-if="bestQualityScore" class="quality-score-summary">
              <div class="summary-card">
                <span class="summary-icon">⭐</span>
                <div class="summary-content">
                  <div class="summary-label">Best Quality Score</div>
                  <div class="summary-value">{{ bestQualityScore }}</div>
                </div>
              </div>
              <div v-if="loadingQualityScores" class="summary-loading">
                <a-spin size="small" /> Analyzing quality...
              </div>
            </div>

            <div class="ad-preview-grid">
              <div
                v-for="variation in adVariations"
                :key="variation.id"
                class="ad-preview-card"
                :class="{
                  selected: selectedVariation?.id === variation.id,
                  'best-quality': variation.qualityScore?.totalScore === bestQualityScoreValue
                }"
                @click="selectVariation(variation)"
              >
                <!-- Best Quality Badge -->
                <div
                  v-if="variation.qualityScore?.totalScore === bestQualityScoreValue && bestQualityScoreValue >= 80"
                  class="best-quality-badge"
                >
                  🏆 Best Quality
                </div>

                <div class="ad-preview-content">
                  <div
                    v-if="variation.imageUrl"
                    class="ad-preview-image"
                    @click.stop="openMediaModal(variation.imageUrl)"
                  >
                    <img
                      :src="variation.imageUrl"
                      :alt="variation.headline"
                      @error="handleImageError($event, variation)"
                    />
                  </div>
                  <div class="ad-preview-text">
                    <h3 class="ad-preview-headline">{{ variation.headline }}</h3>
                    <p class="ad-preview-primary-text">{{ variation.primaryText }}</p>
                    <p class="ad-preview-description">{{ variation.description }}</p>
                    <div class="ad-preview-cta">{{ variation.callToAction }}</div>
                  </div>
                </div>

                <!-- Quality Score Component -->
                <div v-if="variation.qualityScore" class="quality-score-wrapper">
                  <QualityScore :score="variation.qualityScore" :compact="true" />
                </div>
                <div v-else-if="loadingQualityScores" class="quality-score-loading">
                  <a-spin size="small" /> Loading quality score...
                </div>

                <div class="ad-preview-actions">
                  <a-button
                    type="text"
                    @click.stop="editVariation(variation)"
                    size="small"
                  >
                    <template #icon><edit-outlined /></template>
                    Edit
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <a-empty v-else description="No ad variations generated yet" />

          <!-- Error Display -->
          <FieldError :error="saveError" />

          <!-- Navigation -->
          <div class="step-navigation">
            <a-button @click="prevStep" size="large">
              <template #icon><arrow-left-outlined /></template>
              Previous
            </a-button>
            <a-button
              type="primary"
              @click="saveAd"
              :disabled="!selectedVariation"
              :loading="isSaving"
              size="large"
            >
              <template #icon><save-outlined /></template>
              Save Ad
            </a-button>
          </div>
        </a-card>
      </div>

      <!-- Edit Modal -->
    <a-modal 
      v-model:open="showEditModal" 
      title="Edit Ad Variation" 
      width="600px"
      @ok="saveEdit"
      @cancel="cancelEdit"
    >
      <a-form v-if="editingVariation" layout="vertical">
        <a-form-item label="Headline">
          <a-input v-model:value="editingVariation.headline" />
        </a-form-item>
        <a-form-item label="Primary Text">
          <a-textarea v-model:value="editingVariation.primaryText" :rows="3" />
        </a-form-item>
        <a-form-item label="Description">
          <a-textarea v-model:value="editingVariation.description" :rows="2" />
        </a-form-item>
        <a-form-item label="Call to Action">
          <a-input v-model:value="editingVariation.callToAction" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Confirm Save Modal -->
    <a-modal 
      v-model:open="showConfirmModal" 
      title="Confirm Save" 
      @ok="confirmSave"
      @cancel="showConfirmModal = false"
    >
      <p>Are you sure you want to save this ad variation?</p>
      <div v-if="selectedVariation" class="confirm-preview">
        <h4>{{ selectedVariation.headline }}</h4>
        <p>{{ selectedVariation.primaryText }}</p>
      </div>
    </a-modal>

    <!-- Extract Error Dialog -->
    <a-modal 
      v-model:open="showExtractErrorDialog" 
      title="Content Extraction Failed" 
      @ok="onExtractErrorYes"
      @cancel="onExtractErrorNo"
      ok-text="Continue Anyway"
      cancel-text="Go Back"
    >
      <p>We couldn't extract content from the provided ad links. Would you like to continue generating the ad with just your prompt, or go back to fix the links?</p>
    </a-modal>

    <!-- Media Modal -->
    <a-modal 
      v-model:open="showMediaModal" 
      title="Media Preview" 
      :footer="null"
      width="80%"
      centered
    >
      <div class="media-modal-content">
        <img 
          v-if="selectedMediaUrl" 
          :src="selectedMediaUrl" 
          alt="Media preview" 
          style="max-width: 100%; height: auto;"
        />
      </div>
    </a-modal>

    <!-- Ad Type Help Dialog -->
    <a-modal 
      v-model:open="showAdTypeHelp" 
      title="Ad Types Guide" 
      :footer="null"
      width="700px"
    >
      <div class="ad-types-help">
        <div v-for="type in adTypes" :key="type.value" class="ad-type-help-item">
          <h3 class="ad-type-title">{{ type.label }}</h3>
          <p class="ad-type-desc">{{ type.description }}</p>
          <ul class="ad-type-features">
            <li v-for="feature in type.features" :key="feature">{{ feature }}</li>
          </ul>
        </div>
      </div>
    </a-modal>

    <!-- Help Dialog -->
    <a-modal 
      v-model:open="showHelpDialog" 
      title="How to Write Effective Prompts" 
      :footer="null"
      width="700px"
    >
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">What is a Prompt?</h3>
          <p class="help-text">
            A prompt is a description or instruction that tells the AI what kind of ad content you want to create. 
            The more specific and detailed your prompt, the better the AI can understand your needs.
          </p>
        </div>
        
        <div class="help-section">
          <h3 class="help-title">Tips for Writing Great Prompts</h3>
          <div class="help-steps">
            <div class="help-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>Describe Your Product/Service</h4>
                <p>Clearly explain what you're selling, its main benefits, and target audience.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>Specify the Tone</h4>
                <p>Mention if you want the ad to be professional, casual, exciting, trustworthy, etc.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>Include Key Features</h4>
                <p>List the most important features or benefits you want to highlight.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h4>Mention Your Audience</h4>
                <p>Describe who your ideal customers are (age, interests, problems they face).</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { 
  ArrowLeftOutlined,
  ArrowRightOutlined,
  QuestionCircleOutlined,
  DeleteOutlined,
  PlusOutlined,
  DownloadOutlined,
  ThunderboltOutlined,
  EditOutlined,
  SaveOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'
import PromptValidator from '@/components/PromptValidator.vue'
import AudienceSegmentForm from '@/components/AudienceSegmentForm.vue'
import PersonaSelector from '@/components/PersonaSelector.vue'
import TrendingKeywords from '@/components/TrendingKeywords.vue'
import FieldError from '@/components/FieldError.vue'
import QualityScore from '@/components/QualityScore.vue'
import qualityApi from '@/services/qualityApi'

export default {
  name: 'AdCreate',
  components: {
    ArrowLeftOutlined,
    ArrowRightOutlined,
    QuestionCircleOutlined,
    DeleteOutlined,
    PlusOutlined,
    DownloadOutlined,
    ThunderboltOutlined,
    EditOutlined,
    SaveOutlined,
    PromptValidator,
    AudienceSegmentForm,
    PersonaSelector,
    TrendingKeywords,
    FieldError,
    QualityScore
  },
  data() {
    return {
      currentStep: 1,
      formData: {
        campaignId: null,
        adType: 'website_conversion',
        name: '',
        numberOfVariations: 3,
        language: 'vi',
        callToAction: '',
        websiteUrl: '',
        leadFormQuestions: [''],
        prompt: '',
        textProvider: 'openai',
        imageProvider: 'openai',
        enhancementOptions: [],
        audienceSegment: {
          gender: 'ALL',
          minAge: 18,
          maxAge: 65,
          location: '',
          interests: ''
        }
      },
      steps: [
        { title: 'Basic Information', description: 'Campaign details and ad type' },
        { title: 'AI Configuration', description: 'Choose AI providers and settings' },
        { title: 'Preview & Save', description: 'Review and save your ad' }
      ],
      adTypes: [
        {
          value: 'website_conversion',
          label: 'Website Conversion',
          description: 'Drive traffic to your website and increase conversions',
          features: ['Website URL required', 'Conversion tracking', 'Traffic optimization']
        },
        {
          value: 'lead_generation',
          label: 'Lead Generation',
          description: 'Collect leads directly through Facebook forms',
          features: ['Custom form questions', 'Lead collection', 'Contact information']
        },
        {
          value: 'page_post',
          label: 'Page Post',
          description: 'Promote your page posts to increase engagement',
          features: ['Post promotion', 'Engagement boost', 'Social interaction']
        }
      ],
      campaigns: [],
      standardCTAs: [],
      textProviders: [
        {
          value: 'openai',
          name: 'OpenAI GPT',
          description: 'Advanced language model for high-quality ad copy',
          features: ['Creative writing', 'Multiple languages', 'Context awareness'],
          enabled: true
        },
        {
          value: 'claude',
          name: 'Anthropic Claude',
          description: 'Reliable and safe AI for professional ad content',
          features: ['Professional tone', 'Safety focused', 'Detailed responses'],
          enabled: true
        }
      ],
      imageProviders: [
        {
          value: 'openai',
          name: 'OpenAI DALL-E',
          description: 'Create stunning, original images for your ads',
          features: ['High quality', 'Creative styles', 'Custom prompts'],
          enabled: true
        },
        {
          value: 'fal-ai',
          name: 'FAL AI',
          description: 'Advanced AI image generation',
          features: ['High resolution', 'Fast generation', 'Multiple models'],
          enabled: false
        },
        {
          value: 'stable-diffusion',
          name: 'Stable Diffusion',
          description: 'Open-source image generation',
          features: ['Open source', 'Customizable', 'Fast generation'],
          enabled: false
        }
      ],
      adVariations: [],
      selectedVariation: null,
      editingVariation: null,
      adLinks: [''],
      uploadedFiles: [],
      uploadedFileUrl: '',
      selectedMediaUrl: '',
      
      // Loading states
      loadingCampaigns: false,
      loadingCTAs: false,
      isGenerating: false,
      isSaving: false,
      extracting: false,
      
      // Modal states
      showEditModal: false,
      showConfirmModal: false,
      showExtractErrorDialog: false,
      showMediaModal: false,
      showAdTypeHelp: false,
      showHelpDialog: false,
      
      // Other states
      showValidation: false,
      adId: null,
      selectedPromptTemplate: '',
      customPromptAddition: '',
      savedPrompts: [],
      enhancedImages: [],
      isEnhancing: false,
      generateError: null,
      saveError: null,

      // Quality scoring states
      loadingQualityScores: false,
      bestQualityScore: null,
      bestQualityScoreValue: null
    }
  },
  computed: {
    progressSteps() {
      return [
        {
          title: 'Basic Info',
          description: 'Tell us about your ad',
          emoji: '📝'
        },
        {
          title: 'AI Magic',
          description: 'Choose your AI tools',
          emoji: '🤖'
        },
        {
          title: 'Preview',
          description: 'See your creation',
          emoji: '✨'
        }
      ]
    },
    hasUploadedImage() {
      // Check if user has uploaded an image
      return this.uploadedFileUrl && this.uploadedFileUrl.trim() !== ''
    }
  },
  async mounted() {
    await this.loadData()
  },
  methods: {
    handleKeywordsSelected(keywords) {
      // Format trending keywords for prompt injection
      const keywordText = keywords.map(k => `• ${k}`).join('\n')
      const trendingSection = `\n\n📈 TRENDING KEYWORDS:\n${keywordText}\n\n💡 Incorporate these trending topics naturally into the ad content.`

      // Append to prompt (or custom prompt if it exists)
      if (!this.formData.prompt.includes('TRENDING KEYWORDS')) {
        this.formData.prompt += trendingSection
      }

      this.$message.success(`Added ${keywords.length} trending keyword(s) to prompt`)
    },

    handlePersonaSelected(persona) {
      // Store the persona information for later use
      if (persona) {
        // The personaId is already bound via v-model
        // We can optionally show a success message or update UI
        this.$message.success(`Targeting persona: ${persona.name}`)

        // Note: The backend PersonaService.enrichPromptWithPersona() will automatically
        // enhance the prompt with persona details when personaId is included in the request
      } else {
        this.$message.info('Persona selection cleared')
      }
    },

    async enhanceImages() {
      if (!this.formData.enhancementOptions.length) {
        this.$message.warning('Please select at least one enhancement option');
        return;
      }
      if (!this.uploadedFiles.length) {
        this.$message.warning('Please upload images first');
        return;
      }
      this.isEnhancing = true;
      this.enhancedImages = [];
      try {
        const enhancementPromises = this.uploadedFiles.map(async (file) => {
          const formData = new FormData();
          formData.append('file', file.originFileObj || file);
          const uploadResponse = await api.post('/upload/media', formData);
          const imageUrl = uploadResponse.data.fileUrl;
          const enhanceResponse = await api.post('/ads/enhance-image', {
            imageUrl,
            provider: this.formData.imageProvider,
            enhancementTypes: this.formData.enhancementOptions
          });
          return {
            url: enhanceResponse.data.enhancedUrl,
            originalName: file.name
          };
        });
        this.enhancedImages = await Promise.all(enhancementPromises);
        this.$message.success('Images enhanced successfully with real-time preview');
      } catch (error) {
        this.$message.error('Failed to enhance images');
      } finally {
        this.isEnhancing = false;
      }
    },
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
        this.$message.error('Failed to load campaigns')
      } finally {
        this.loadingCampaigns = false
      }
    },
    
    async loadCallToActions() {
      this.loadingCTAs = true
      try {
        const response = await api.providers.getCallToActions(this.formData.language)
        this.standardCTAs = response.data.ctas || response.data || []
      } catch (error) {
        console.error('Error loading CTAs:', error)
        this.$message.error('Failed to load call to actions')
      } finally {
        this.loadingCTAs = false
      }
    },
    
    async loadProviders() {
      try {
        // Load text providers
        const textResponse = await api.providers.getTextProviders()
        this.textProviders = textResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name,
          description: provider.description,
          features: ['AI-powered content', 'Multiple languages', 'High quality'],
          enabled: true
        }))
        
        // Load image providers
        const imageResponse = await api.providers.getImageProviders()
        this.imageProviders = imageResponse.data.map(provider => ({
          value: provider.id,
          name: provider.name,
          description: provider.description,
          features: ['AI-generated images', 'High resolution', 'Creative styles'],
          enabled: true
        }))
        
        // Set default providers if not already set
        if (this.textProviders.length > 0 && !this.formData.textProvider) {
          this.formData.textProvider = this.textProviders[0].value
        }
        if (this.imageProviders.length > 0 && !this.formData.imageProvider) {
          this.formData.imageProvider = this.imageProviders[0].value
        }
        
      } catch (error) {
        console.error('Error loading providers:', error)
        this.$message.warning('Failed to load AI providers, using default options')
        // Keep the hardcoded providers as fallback
      }
    },
    
    nextStep() {
      if (this.currentStep < 3) {
        this.currentStep++
      }
    },
    
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    
    onAdTypeChange() {
      // Reset type-specific fields when ad type changes
      this.formData.websiteUrl = ''
      this.formData.leadFormQuestions = ['']
    },
    
    addLeadFormQuestion() {
      this.formData.leadFormQuestions.push('')
    },
    
    removeLeadFormQuestion(index) {
      this.formData.leadFormQuestions.splice(index, 1)
    },
    
    validateStep1() {
      return this.formData.campaignId && 
             this.formData.name && 
             this.formData.adType &&
             (this.formData.prompt || this.adLinks.some(link => link.trim()))
    },
    
    validateStep2() {
      // Valid if text provider selected AND (image provider selected OR image uploaded)
      return this.formData.textProvider && (this.formData.imageProvider || this.hasUploadedImage)
    },
    
    async handleFileUpload(file) {
      this.uploadedFiles = [file];
      try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post('/upload/media', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        if (response.data.success) {
          this.uploadedFileUrl = response.data.fileUrl;
          this.$message.success('File uploaded successfully');
        }
      } catch (error) {
        this.$message.error('Failed to upload file');
        this.uploadedFiles = [];
      }
      return false;
    },
    
    removeFile() {
      this.uploadedFiles = []
      this.uploadedFileUrl = ''
    },

    handleImageError(event, variation) {
      // Log the error for debugging
      console.error(`Failed to load image for variation: ${variation.headline}`, variation.imageUrl)

      // Set fallback placeholder image
      event.target.src = '/img/placeholder.png'

      // Show a warning toast to user (only once per session to avoid spam)
      if (!this.$root.imageErrorShown) {
        this.$message.warning('Some images failed to load. Using placeholder images.')
        this.$root.imageErrorShown = true
      }
    },

    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    async generateAd() {
      this.isGenerating = true
      this.generateError = null

      try {
        // Validate required fields
        if (!this.formData.prompt && !this.adLinks.some(link => link.trim())) {
          this.$message.error('Please enter prompt content or provide ad links')
          this.currentStep = 1
          this.showValidation = true
          return
        }
        
        // Extract content from ad links if provided
        const validLinks = this.adLinks.filter(link => link.trim())
        let extractedContent = ''
        let extractFailed = false
        
        if (validLinks.length > 0) {
          try {
            const extractionResponse = await api.ads.extractFromLibrary({
              adLinks: validLinks,
              promptStyle: this.selectedPromptTemplate || 'Dynamic',
              customPrompt: this.customPromptAddition
            })
            
            let success = false
            if (Array.isArray(extractionResponse.data)) {
              success = extractionResponse.data.some(this.hasAdBody)
              if (success) {
                const found = extractionResponse.data.find(this.hasAdBody)
                extractedContent = found.body || (found.snapshot && found.snapshot.body) || found.text || ''
              }
            } else {
              success = this.hasAdBody(extractionResponse.data)
              if (success) {
                extractedContent = extractionResponse.data.body || (extractionResponse.data.snapshot && extractionResponse.data.snapshot.body) || extractionResponse.data.text || ''
              }
            }
            if (!success) extractFailed = true
          } catch (extractionError) {
            extractFailed = true
          }
        }
        
        // If extraction failed, show dialog
        if (validLinks.length > 0 && extractFailed) {
          this.showExtractErrorDialog = true
          this.isGenerating = false
          return
        }
        
        // Generate ad preview
        let promptWithCTA = this.formData.prompt || ''
        if (promptWithCTA.indexOf('Call to Action') === -1) {
          const ctaList = this.standardCTAs.map(cta => cta.label.split(' - ')[0]).join(', ')
          promptWithCTA += `\n\nLưu ý: Chỉ sử dụng một trong các Call to Action sau cho quảng cáo: ${ctaList}. Không tạo CTA khác.`
        }
        
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: promptWithCTA,
          language: this.formData.language,
          adLinks: validLinks,
          promptStyle: this.selectedPromptTemplate || 'Dynamic',
          customPrompt: this.customPromptAddition,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl,
          callToAction: this.formData.callToAction,
          extractedContent: extractedContent,
          personaId: this.formData.personaId || null,
          isPreview: true
        }
        
        const response = await api.post('/ads/generate', requestData)
        
        if (response.data.status === 'success') {
          this.adVariations = response.data.variations.map(v => ({
            ...v,
            imageUrl: v.imageUrl || this.uploadedFileUrl || v.mediaFileUrl || ''
          }))

          this.adId = response.data.adId
          this.currentStep = 3
          this.$message.success('Ad created successfully! Please preview and save.')

          // Load quality scores for variations
          await this.loadQualityScoresForVariations()
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error generating ad:', error)

        // Store entire error object for FieldError component
        this.generateError = error

        const errorMessage = error.message || 'Could not create ad. Please try again.'

        this.$message.error(errorMessage)

        if (errorMessage.includes('Please enter prompt content') || errorMessage.includes('valid ad link')) {
          this.currentStep = 1
          this.showValidation = true
        }
      } finally {
        this.isGenerating = false
      }
    },
    
    selectVariation(variation) {
      this.selectedVariation = variation
    },
    
    editVariation(variation) {
      this.editingVariation = { ...variation }
      this.showEditModal = true
    },
    
    cancelEdit() {
      this.editingVariation = null
      this.showEditModal = false
    },
    
    saveEdit() {
      if (this.editingVariation) {
        const index = this.adVariations.findIndex(v => v.id === this.editingVariation.id)
        if (index !== -1) {
          this.adVariations[index] = { ...this.editingVariation }
          if (this.selectedVariation?.id === this.editingVariation.id) {
            this.selectedVariation = { ...this.editingVariation }
          }
        }
      }
      this.showEditModal = false
      this.editingVariation = null
    },
    
    saveAd() {
      if (!this.selectedVariation) {
        this.$message.warning('Vui lòng chọn một quảng cáo để lưu')
        return
      }
      this.showConfirmModal = true
    },
    
    addAdLink() {
      this.adLinks.push('')
    },
    
    removeAdLink(index) {
      this.adLinks.splice(index, 1)
    },
    
    async confirmSave() {
      this.showConfirmModal = false
      this.isSaving = true
      this.saveError = null

      try{
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: this.formData.prompt,
          language: this.formData.language,
          adLinks: this.adLinks.filter(link => link.trim()),
          promptStyle: this.selectedPromptTemplate || 'Dynamic',
          customPrompt: this.customPromptAddition,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl,
          selectedVariation: this.selectedVariation,
          isPreview: false,
          saveExistingContent: true
        }
        
        const response = await api.ads.saveExisting(requestData)
        
        if (response.data.status === 'success') {
          this.$message.success('Ad saved successfully!')
          await this.$store.dispatch('dashboard/fetchDashboardData', null, { root: true })
          this.$router.push('/dashboard')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error saving ad:', error)

        // Store entire error object for FieldError component
        this.saveError = error

        this.$message.error(error.message || 'Could not save ad. Please try again.')
      } finally {
        this.isSaving = false
      }
    },
    
    async extractFromLibrary() {
      if (!this.adLinks.length || !this.adLinks[0].trim()) {
        this.$message.warning('Vui lòng nhập ít nhất một link quảng cáo')
        return
      }
      
      this.extracting = true
      
      try {
        const validLinks = this.adLinks.filter(link => link.trim())
        const response = await api.ads.extractFromLibrary({
          adLinks: validLinks,
          promptStyle: this.selectedPromptTemplate || 'Dynamic',
          customPrompt: this.customPromptAddition
        })
        
        let success = false
        let content = ''
        if (Array.isArray(response.data)) {
          success = response.data.some(this.hasAdBody)
          if (success) {
            const found = response.data.find(this.hasAdBody)
            content = found.body || (found.snapshot && found.snapshot.body) || found.text || ''
          }
        } else {
          success = this.hasAdBody(response.data)
          if (success) {
            content = response.data.body || (response.data.snapshot && response.data.snapshot.body) || response.data.text || ''
          }
        }
        
        if (success) {
          this.formData.prompt = content
          this.$message.success('Đã trích xuất nội dung thành công!')
        } else {
          this.$message.warning(response.data.message || 'Không thể trích xuất nội dung')
        }
      } catch (error) {
        console.error('Error extracting from library:', error)
        this.$message.error('Lỗi khi trích xuất nội dung: ' + (error.response?.data?.message || error.message))
      } finally {
        this.extracting = false
      }
    },
    
    onExtractErrorYes() {
      this.showExtractErrorDialog = false
      this.isGenerating = true
      this.generateAdContinueWithoutExtract()
    },
    
    onExtractErrorNo() {
      this.showExtractErrorDialog = false
      this.currentStep = 1
      this.$nextTick(() => {
        if (this.$refs.adLinkInput && this.$refs.adLinkInput[0]) {
          this.$refs.adLinkInput[0].focus()
        }
      })
    },
    
    async generateAdContinueWithoutExtract() {
      try {
        const validLinks = this.adLinks.filter(link => link.trim())
        const requestData = {
          campaignId: this.formData.campaignId,
          adType: this.formData.adType,
          name: this.formData.name,
          prompt: this.formData.prompt,
          language: this.formData.language,
          adLinks: validLinks,
          promptStyle: this.selectedPromptTemplate || 'Dynamic',
          customPrompt: this.customPromptAddition,
          textProvider: this.formData.textProvider,
          imageProvider: this.formData.imageProvider,
          numberOfVariations: this.formData.numberOfVariations,
          mediaFileUrl: this.uploadedFileUrl,
          extractedContent: null,
          personaId: this.formData.personaId || null,
          isPreview: true
        }
        
        const response = await api.post('/ads/generate', requestData)
        
        if (response.data.status === 'success') {
          this.adVariations = response.data.variations.map(v => ({
            ...v,
            imageUrl: v.imageUrl || this.uploadedFileUrl || v.mediaFileUrl || ''
          }))
          this.adId = response.data.adId
          this.currentStep = 3
          this.$message.success('Ad created successfully! Please preview and save.')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        // Store entire error object for FieldError component
        this.generateError = error

        const errorMessage = error.message || 'Could not create ad. Please try again.'
        this.$message.error(errorMessage)

        if (errorMessage.includes('Please enter prompt content') || errorMessage.includes('valid ad link')) {
          this.currentStep = 1
          this.showValidation = true
        }
      } finally {
        this.isGenerating = false
      }
    },
    
    hasAdBody(ad) {
      return (
        (ad.body && ad.body.trim() !== '') ||
        (ad.snapshot && ad.snapshot.body && ad.snapshot.body.trim() !== '') ||
        (ad.text && ad.text.trim() !== '')
      )
    },
    
    openMediaModal(url) {
      this.selectedMediaUrl = url
      this.showMediaModal = true
    },
    
    closeMediaModal() {
      this.showMediaModal = false
      this.selectedMediaUrl = ''
    },

    onPromptUpdated(improvedPrompt) {
      this.formData.prompt = improvedPrompt
      this.$message.success('Prompt updated with improved version!')
    },

    // New Creative Methods for Phase 3 Implementation
    getProviderIcon(provider, type) {
      const icons = {
        text: {
          openai: '🧠',
          claude: '🎭',
          gemini: '💎',
          huggingface: '🤗'
        },
        image: {
          openai: '🎨',
          stability: '🌟',
          fal: '⚡',
          midjourney: '🎪'
        }
      }
      return icons[type]?.[provider] || (type === 'text' ? '✏️' : '🖼️')
    },

    getProviderRating(provider) {
      const ratings = {
        openai: 5,
        claude: 4,
        gemini: 4,
        stability: 4,
        fal: 3,
        huggingface: 3
      }
      return ratings[provider] || 3
    },

    getProviderRatingText(provider) {
      const texts = {
        openai: 'Most Popular',
        claude: 'Reliable',
        gemini: 'Fast',
        stability: 'High Quality',
        fal: 'Experimental',
        huggingface: 'Open Source'
      }
      return texts[provider] || 'Good Choice'
    },

    calculateAIPowerLevel() {
      let power = 0

      // Text provider power
      const textPower = {
        openai: 35,
        claude: 30,
        gemini: 25,
        huggingface: 20
      }
      power += textPower[this.formData.textProvider] || 20

      // Image provider power
      const imagePower = {
        openai: 35,
        stability: 30,
        fal: 25,
        midjourney: 40
      }
      power += imagePower[this.formData.imageProvider] || 20

      return Math.min(power, 100)
    },

    getAIPowerMessage() {
      const level = this.calculateAIPowerLevel()
      if (level >= 80) return "🔥 Ultra powerful combo! Your ads will be amazing!"
      if (level >= 60) return "⚡ Great combination! Ready for high-quality results!"
      if (level >= 40) return "✨ Solid choice! Your ads will look professional!"
      return "🌱 Good start! Your ads will be decent!"
    },

    async loadQualityScoresForVariations() {
      if (!this.adVariations || this.adVariations.length === 0) {
        return
      }

      this.loadingQualityScores = true
      try {
        const adContentIds = this.adVariations.map(v => v.id).filter(id => id)

        if (adContentIds.length === 0) {
          console.warn('No valid ad content IDs found')
          return
        }

        const response = await qualityApi.getQualityScoreBatch(adContentIds)

        // Attach quality scores to variations
        this.adVariations = this.adVariations.map(variation => {
          const scoreData = response.data.find(s => s.adContentId === variation.id)
          return {
            ...variation,
            qualityScore: scoreData || null
          }
        })

        // Find best quality score
        const scores = this.adVariations
          .map(v => v.qualityScore?.totalScore)
          .filter(s => s !== null && s !== undefined)

        if (scores.length > 0) {
          this.bestQualityScoreValue = Math.max(...scores)
          const bestVariation = this.adVariations.find(
            v => v.qualityScore?.totalScore === this.bestQualityScoreValue
          )

          if (bestVariation && bestVariation.qualityScore) {
            this.bestQualityScore = `${bestVariation.qualityScore.totalScore.toFixed(1)}/100 (${bestVariation.qualityScore.grade})`
          }
        }
      } catch (error) {
        console.error('Error loading quality scores:', error)
        this.$message.warning('Could not load quality scores')
      } finally {
        this.loadingQualityScores = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
/* Creative Ad Create Styles - Phase 3 Implementation */
.ad-create-page {
  padding: 20px;
  background: linear-gradient(135deg, #fafafa 0%, #f0f2f5 100%);
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
}

/* Creative Progress Container */
.creative-progress-container {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.progress-header {
  text-align: center;
  margin-bottom: 28px;
}

.progress-title {
  font-size: 28px;
  font-weight: 700;
  color: #2d5aa0;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.progress-subtitle {
  font-size: 16px;
  color: #8c8c8c;
  font-weight: 500;
}

.creative-progress-tracker {
  position: relative;
  margin-bottom: 24px;
}

.progress-line {
  position: absolute;
  top: 28px;
  left: 32px;
  right: 32px;
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  z-index: 1;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2d5aa0 0%, #1890ff 100%);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 3px solid #f0f2f5;
  margin-bottom: 12px;
  transition: all 0.4s ease;
  position: relative;
  z-index: 3;
}

.progress-step.completed .step-circle {
  background: #2d5aa0;
  border-color: #2d5aa0;
  color: white;
}

.progress-step.current .step-circle {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
  animation: pulse-step 2s ease-in-out infinite;
}

@keyframes pulse-step {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.step-check,
.step-number {
  font-size: 18px;
  font-weight: 700;
}

.step-content {
  text-align: center;
  max-width: 120px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
  margin-bottom: 8px;
}

.step-emoji {
  font-size: 20px;
  animation: bounce-emoji 1.5s ease-in-out infinite;
}

@keyframes bounce-emoji {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.fun-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border-radius: 12px;
  border: 1px solid #ffd6cc;
}

.progress-character {
  font-size: 24px;
  animation: character-float 2s ease-in-out infinite;
}

@keyframes character-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.progress-message {
  font-size: 16px;
  font-weight: 600;
  color: #d46b08;
}

/* AI Configuration Creative Styling */
.ai-config-container {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.ai-section-header {
  text-align: center;
  margin-bottom: 40px;
}

.section-title-magic {
  font-size: 32px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 12px 0;
  line-height: 1.2;
}

.section-subtitle-magic {
  font-size: 16px;
  color: #8c8c8c;
  margin: 0;
}

.provider-section {
  margin-bottom: 40px;
}

.provider-section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f2f5;
}

.section-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-radius: 12px;
  border: 2px solid #91d5ff;
}

.provider-title {
  font-size: 22px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 4px 0;
}

.provider-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.creative-provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.creative-provider-card {
  background: white;
  border: 2px solid #f0f2f5;
  border-radius: 16px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.creative-provider-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, transparent 0%, rgba(24, 144, 255, 0.02) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.creative-provider-card:hover::before {
  opacity: 1;
}

.creative-provider-card:hover {
  border-color: #91d5ff;
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
  transform: translateY(-2px);
}

.creative-provider-card.selected {
  border-color: #1890ff;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 8px 32px rgba(24, 144, 255, 0.2);
}

.creative-provider-card.selected::after {
  content: '✓';
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.creative-provider-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.provider-visual {
  position: relative;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-icon {
  font-size: 32px;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border-radius: 12px;
  border: 2px solid #ffadd2;
}

.provider-status {
  font-size: 18px;
}

.provider-info {
  flex: 1;
}

.provider-name {
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 8px 0;
}

.provider-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.4;
  margin: 0 0 16px 0;
}

.provider-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.feature-badge {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  color: #0958d9;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  border: 1px solid #91d5ff;
}

.provider-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.rating-text {
  font-size: 12px;
  font-weight: 600;
  color: #f4a261;
}

.provider-selection {
  position: absolute;
  top: 20px;
  right: 20px;
}

.selection-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.creative-provider-card.selected .selection-indicator {
  background: #1890ff;
  border-color: #1890ff;
}

.selection-dot {
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
}

/* AI Power Level Indicator */
.ai-power-indicator {
  background: linear-gradient(135deg, #fff8e1 0%, #fff1b0 100%);
  border: 2px solid #ffd666;
  border-radius: 16px;
  padding: 20px;
  margin-top: 32px;
}

.power-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.power-icon {
  font-size: 20px;
}

.power-title {
  font-size: 16px;
  font-weight: 700;
  color: #d46b08;
}

.power-bar {
  background: rgba(255, 255, 255, 0.8);
  height: 12px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.power-fill {
  height: 100%;
  background: linear-gradient(90deg, #ffd666 0%, #f4a261 100%);
  border-radius: 6px;
  transition: width 0.6s ease;
}

.power-message {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
  text-align: center;
}

/* Step Content Styling */
.step-content {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.enhanced-card {
  border: none;
  box-shadow: none;
  background: transparent;
}

.enhanced-card :deep(.ant-card-head) {
  border: none;
  padding: 0 0 24px 0;
}

.enhanced-card :deep(.ant-card-head-title) {
  font-size: 24px;
  font-weight: 700;
  color: #2d5aa0;
}

.enhanced-card :deep(.ant-card-body) {
  padding: 0;
}

/* Step Navigation */
.step-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f2f5;
}

.step-navigation .ant-btn {
  height: 48px;
  border-radius: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-navigation .ant-btn-primary {
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
}

.step-navigation .ant-btn-primary:hover {
  background: linear-gradient(135deg, #1e3a6f 0%, #0d7cc0 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
}

/* Mobile Responsiveness - Phase 2 Enhanced */
@media (max-width: 768px) {
  .ad-create-page {
    padding: 16px;
  }

  .creative-progress-container {
    padding: 20px;
  }

  .progress-title {
    font-size: 24px;
  }

  .progress-steps {
    gap: 8px;
  }

  .step-circle {
    width: 44px;
    height: 44px;
  }

  .step-content {
    max-width: 90px;
  }

  .step-title {
    font-size: 12px;
  }

  .step-desc {
    font-size: 10px;
  }

  .ai-config-container {
    padding: 24px;
  }

  .section-title-magic {
    font-size: 26px;
  }

  .creative-provider-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .creative-provider-card {
    padding: 20px;
  }

  .provider-icon {
    width: 44px;
    height: 44px;
    font-size: 24px;
  }

  .step-navigation {
    flex-direction: column;
    gap: 12px;
  }

  .step-navigation .ant-btn {
    width: 100%;
    height: 44px;
  }
}

@media (max-width: 480px) {
  .progress-line {
    left: 22px;
    right: 22px;
  }

  .step-circle {
    width: 36px;
    height: 36px;
  }

  .step-check,
  .step-number {
    font-size: 14px;
  }

  .section-title-magic {
    font-size: 22px;
  }

  .provider-title {
    font-size: 18px;
  }
}

  .step-navigation .ant-btn:first-child {
    order: 1;
  }

@media (max-width: 480px) {
  .ad-create-page {
    padding: 8px;
  }

  .step-content {
    padding: 0.75rem;
  }

  .wizard-progress {
    padding: 0.75rem;
  }

  .ai-provider-card {
    min-height: 120px;
    padding: 0.75rem;
  }
}

.nav-item:hover,
.nav-item.active {
  background: #e6f7ff;
  color: #1890ff;
  border-right: 3px solid #1890ff;
}

.main-content {
  flex: 1;
  margin-left: 0;
  transition: margin-left 0.3s ease;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.mobile-header h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.step-content {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.wizard-progress {
  padding: 2rem;
  background: white;
  margin-bottom: 2rem;
}

.step-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #f0f0f0;
}

.ai-provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 1rem;
}

.ai-provider-card {
  border: 2px solid #e5e7eb;
  border-radius: 1rem;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.ai-provider-card:hover {
  border-color: #1890ff;
  box-shadow: 0 10px 25px rgba(24, 144, 255, 0.1);
  border-color: #6b8499;
}

.ai-provider-card.selected {
  border-color: #2d5aa0;
  background: #f0f4f7;
  box-shadow: 0 3px 12px rgba(45, 90, 160, 0.12);
}

.ai-provider-card.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.provider-header h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.provider-features {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1rem;
}

.ad-preview-container {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
}

.preview-instruction {
  font-size: 1.125rem;
  color: #666;
  margin-bottom: 1.5rem;
  text-align: center;
}

.ad-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 2rem;
  width: 100%;
}

.ad-preview-card {
  border: 2px solid #e5e7eb;
  border-radius: 1.5rem;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.ad-preview-card:hover {
  border-color: #52c41a;
  box-shadow: 0 20px 40px rgba(82, 196, 26, 0.1);
  border-color: #4a8c4a;
}

.ad-preview-card.selected {
  border-color: #4a8c4a;
  background: #f2f8f2;
  box-shadow: 0 3px 12px rgba(74, 140, 74, 0.12);
}

.ad-preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ad-preview-image {
  width: 192px;
  height: 128px;
  border-radius: 1rem;
  overflow: hidden;
  margin-bottom: 1rem;
  background: #f4f4f2;
  border: 2px solid #e8e8e4;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ad-preview-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.ad-preview-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ad-preview-headline {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
  margin-bottom: 0.5rem;
}

.ad-preview-primary-text {
  font-size: 1rem;
  color: #4b5563;
  line-height: 1.5;
  margin-bottom: 0.5rem;
  flex: 1;
}

.ad-preview-description {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

.ad-preview-cta {
  background: #2d5aa0;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  border: 1px solid #274d89;
  font-weight: 600;
  text-align: center;
  transition: all 0.2s ease;
}

.ad-preview-cta:hover {
  background: #274d89;
  border-color: #1f3d6b;
}

.ad-preview-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f0f0f0;
}

.lead-form-questions {
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 1rem;
  background: #f9fafb;
}

.question-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 0.375rem;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
}

.ad-links-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ad-link-item {
  margin-bottom: 0.5rem;
}

.confirm-preview {
  background: #f9fafb;
  padding: 1rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
}

.confirm-preview h4 {
  margin: 0 0 0.5rem 0;
  font-weight: 600;
}

.confirm-preview p {
  margin: 0;
  color: #666;
}

.media-modal-content {
  text-align: center;
}

.ad-types-help {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.ad-type-help-item {
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.75rem;
  background: #f9fafb;
}

.ad-type-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.ad-type-desc {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.75rem;
  line-height: 1.5;
}

.ad-type-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ad-type-features li {
  font-size: 0.875rem;
  color: #4b5563;
  padding: 0.25rem 0;
  position: relative;
  padding-left: 1.25rem;
}

.ad-type-features li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #52c41a;
  font-weight: bold;
}

.help-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.help-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.help-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.5rem;
}

.help-text {
  font-size: 1rem;
  color: #6b7280;
  line-height: 1.6;
}

.help-steps {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.help-step {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.help-step .step-number {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
}

.help-step .step-content h4 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.25rem;
}

.help-step .step-content p {
  font-size: 0.875rem;
  color: #6b7280;
  line-height: 1.5;
}

/* Responsive Design */
@media (max-width: 768px) {
  .ai-provider-grid {
    grid-template-columns: 1fr;
  }
  
  .ad-preview-grid {
    grid-template-columns: 1fr;
  }
  
  .ad-preview-card {
    min-height: 350px;
  }
  
  .step-navigation {
    flex-direction: column;
    gap: 1rem;
  }
  
  .step-content {
    padding: 1rem;
  }
}

@media (min-width: 1024px) {
  .main-content {
    margin-left: 250px;
  }
  
  .sidebar {
    position: relative;
    left: 0;
  }
  
  .mobile-header {
    display: none;
  }
  
  .ai-provider-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .ad-preview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1280px) {
  .ad-preview-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Quality Score Integration Styles */
.quality-score-summary {
  background: linear-gradient(135deg, #fff8e1 0%, #fff1b0 100%);
  border: 2px solid #ffd666;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-icon {
  font-size: 32px;
  animation: pulse-icon 2s ease-in-out infinite;
}

@keyframes pulse-icon {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.summary-content {
  display: flex;
  flex-direction: column;
}

.summary-label {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #d46b08;
}

.summary-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #d46b08;
  font-weight: 600;
}

.best-quality-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #ffd666 0%, #f4a261 100%);
  color: #8c4a00;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 214, 102, 0.4);
  z-index: 10;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.ad-preview-card.best-quality {
  border-color: #ffd666;
  box-shadow: 0 8px 32px rgba(255, 214, 102, 0.3);
}

.ad-preview-card.best-quality:hover {
  border-color: #f4a261;
  box-shadow: 0 12px 40px rgba(255, 214, 102, 0.4);
}

.quality-score-wrapper {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

.quality-score-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #8c8c8c;
  font-size: 14px;
}

/* Adjust ad preview card to accommodate quality score */
.ad-preview-card {
  position: relative;
  padding-bottom: 1rem;
}

/* Mobile responsive adjustments for quality score */
@media (max-width: 768px) {
  .quality-score-summary {
    flex-direction: column;
    padding: 16px;
  }

  .summary-icon {
    font-size: 24px;
  }

  .summary-value {
    font-size: 20px;
  }

  .best-quality-badge {
    font-size: 11px;
    padding: 4px 8px;
  }
}
</style>