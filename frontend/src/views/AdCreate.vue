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

      <!-- Progress Steps -->
      <div class="wizard-progress">
        <a-steps :current="currentStep - 1" size="small">
          <a-step title="Basic Information" description="Campaign details and ad type" />
          <a-step title="AI Configuration" description="Choose AI providers and settings" />
          <a-step title="Preview & Save" description="Review and save your ad" />
        </a-steps>
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
          </a-form>

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
        <a-card title="AI Configuration" class="enhanced-card">
          <!-- Text Provider Selection -->
          <a-form-item label="Text Generation Provider">
            <a-radio-group v-model:value="formData.textProvider" class="ai-provider-grid">
              <div 
                v-for="provider in textProviders" 
                :key="provider.value" 
                class="ai-provider-card" 
                :class="{ 
                  selected: formData.textProvider === provider.value,
                  disabled: !provider.enabled 
                }"
                @click="formData.textProvider = provider.value"
              >
                <a-radio :value="provider.value" style="display: none" />
                <div class="provider-header">
                  <h3>{{ provider.name }}</h3>
                  <a-tag v-if="!provider.enabled" color="red">Disabled</a-tag>
                </div>
                <p>{{ provider.description }}</p>
                <div class="provider-features">
                  <a-tag v-for="feature in provider.features" :key="feature" color="blue">
                    {{ feature }}
                  </a-tag>
                </div>
              </div>
            </a-radio-group>
          </a-form-item>

          <!-- Image Provider Selection -->
          <a-form-item label="Image Generation Provider">
            <a-radio-group v-model:value="formData.imageProvider" class="ai-provider-grid">
              <div 
                v-for="provider in imageProviders" 
                :key="provider.value" 
                class="ai-provider-card" 
                :class="{ 
                  selected: formData.imageProvider === provider.value,
                  disabled: !provider.enabled 
                }"
                @click="formData.imageProvider = provider.value"
              >
                <a-radio :value="provider.value" style="display: none" />
                <div class="provider-header">
                  <h3>{{ provider.name }}</h3>
                  <a-tag v-if="!provider.enabled" color="red">Disabled</a-tag>
                </div>
                <p>{{ provider.description }}</p>
                <div class="provider-features">
                  <a-tag v-for="feature in provider.features" :key="feature" color="blue">
                    {{ feature }}
                  </a-tag>
                </div>
              </div>
            </a-radio-group>
          </a-form-item>

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
        </a-card>
      </div>

      <!-- Step 3: Preview & Save -->
      <div v-if="currentStep === 3" class="step-content">
        <a-card title="Preview & Save" class="enhanced-card">
          <div v-if="adVariations.length > 0" class="ad-preview-container">
            <p class="preview-instruction">Select your preferred ad variation:</p>
            <div class="ad-preview-grid">
              <div 
                v-for="variation in adVariations" 
                :key="variation.id" 
                class="ad-preview-card" 
                :class="{ selected: selectedVariation?.id === variation.id }"
                @click="selectVariation(variation)"
              >
                <div class="ad-preview-content">
                  <div 
                    v-if="variation.imageUrl" 
                    class="ad-preview-image"
                    @click.stop="openMediaModal(variation.imageUrl)"
                  >
                    <img :src="variation.imageUrl" :alt="variation.headline" />
                  </div>
                  <div class="ad-preview-text">
                    <h3 class="ad-preview-headline">{{ variation.headline }}</h3>
                    <p class="ad-preview-primary-text">{{ variation.primaryText }}</p>
                    <p class="ad-preview-description">{{ variation.description }}</p>
                    <div class="ad-preview-cta">{{ variation.callToAction }}</div>
                  </div>
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
    SaveOutlined
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
        imageProvider: 'dalle'
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
          value: 'brand_awareness',
          label: 'Brand Awareness',
          description: 'Increase brand visibility and recognition',
          features: ['Reach optimization', 'Brand exposure', 'Awareness metrics']
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
          value: 'dalle',
          name: 'DALL-E',
          description: 'Create stunning, original images for your ads',
          features: ['High quality', 'Creative styles', 'Custom prompts'],
          enabled: true
        },
        {
          value: 'midjourney',
          name: 'Midjourney',
          description: 'Artistic and creative image generation',
          features: ['Artistic style', 'High resolution', 'Creative freedom'],
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
      savedPrompts: []
    }
  },
  computed: {
    // Add any computed properties here
  },
  async mounted() {
    await this.loadData()
  },
  methods: {
    async loadData() {
      await Promise.all([
        this.loadCampaigns(),
        this.loadCallToActions()
      ])
    },
    
    async loadCampaigns() {
      this.loadingCampaigns = true
      try {
        const response = await api.campaigns.getAll()
        this.campaigns = response.data.campaigns || []
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
      return this.formData.textProvider && this.formData.imageProvider
    },
    
    handleFileUpload(file) {
      // Handle file upload logic
      this.uploadedFiles = [file]
      return false // Prevent automatic upload
    },
    
    removeFile() {
      this.uploadedFiles = []
      this.uploadedFileUrl = ''
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
        console.error('Error generating ad:', error)
        const errorMessage = error.response?.data?.message || 'Could not create ad. Please try again.'
        
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
      
      try {
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
        this.$message.error(error.response?.data?.message || 'Could not save ad. Please try again.')
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
        const errorMessage = error.response?.data?.message || 'Could not create ad. Please try again.'
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
    }
  }
}
</script>

<style lang="scss" scoped>
/* Enhanced AdCreate Styles */
.ad-create-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
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
  transform: translateY(-2px);
}

.ai-provider-card.selected {
  border-color: #1890ff;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 10px 25px rgba(24, 144, 255, 0.2);
  transform: scale(1.02);
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
  transform: translateY(-4px);
}

.ad-preview-card.selected {
  border-color: #52c41a;
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  box-shadow: 0 20px 40px rgba(82, 196, 26, 0.2);
  transform: scale(1.02);
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
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
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
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 600;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(24, 144, 255, 0.2);
}

.ad-preview-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 12px rgba(24, 144, 255, 0.3);
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
</style>