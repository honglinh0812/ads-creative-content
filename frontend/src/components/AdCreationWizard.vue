<template>
  <div class="ad-creation-wizard">
    <!-- Modern Progress Indicator -->
    <div class="wizard-progress">
      <div class="progress-container">
        <div 
          v-for="(step, index) in steps" 
          :key="step.id"
          class="progress-step"
          :class="{
            'active': currentStep === index + 1,
            'completed': currentStep > index + 1,
            'upcoming': currentStep < index + 1
          }"
        >
          <div class="step-indicator">
            <div class="step-number" v-if="currentStep <= index + 1">
              {{ index + 1 }}
            </div>
            <svg v-else class="step-check" viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
            </svg>
          </div>
          <div class="step-content">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-description">{{ step.description }}</div>
          </div>
          <div v-if="index < steps.length - 1" class="step-connector"></div>
        </div>
      </div>
    </div>

    <!-- Wizard Content -->
    <div class="wizard-content">
      <transition name="slide-fade" mode="out-in">
        <!-- Step 1: Basic Information -->
        <div v-if="currentStep === 1" key="step1" class="wizard-step">
          <div class="step-header">
            <h2 class="step-title">Basic Information</h2>
            <p class="step-subtitle">Let's start with the basics for your ad</p>
          </div>
          
          <div class="form-grid">
            <!-- Campaign Selection -->
            <div class="form-field">
              <label class="field-label">
                <span class="label-text">Campaign</span>
                <span class="label-required">*</span>
              </label>
              <Dropdown 
                v-model="formData.campaignId" 
                :options="campaigns" 
                optionLabel="name" 
                optionValue="id" 
                placeholder="Select a campaign"
                class="field-input"
                :class="{ 'field-error': errors.campaignId }"
              />
              <div v-if="errors.campaignId" class="field-error-message">
                {{ errors.campaignId }}
              </div>
            </div>

            <!-- Ad Name -->
            <div class="form-field">
              <label class="field-label">
                <span class="label-text">Ad Name</span>
                <span class="label-required">*</span>
              </label>
              <InputText 
                v-model="formData.name" 
                placeholder="Enter a descriptive name for your ad"
                class="field-input"
                :class="{ 'field-error': errors.name }"
              />
              <div v-if="errors.name" class="field-error-message">
                {{ errors.name }}
              </div>
            </div>

            <!-- Ad Type -->
            <div class="form-field">
              <label class="field-label">
                <span class="label-text">Ad Type</span>
                <span class="label-required">*</span>
              </label>
              <div class="ad-type-grid">
                <div 
                  v-for="type in adTypes" 
                  :key="type.value"
                  class="ad-type-option"
                  :class="{ 'selected': formData.adType === type.value }"
                  @click="formData.adType = type.value"
                >
                  <div class="type-icon">
                    <component :is="type.icon" />
                  </div>
                  <div class="type-content">
                    <div class="type-title">{{ type.label }}</div>
                    <div class="type-description">{{ type.description }}</div>
                  </div>
                </div>
              </div>
              <div v-if="errors.adType" class="field-error-message">
                {{ errors.adType }}
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Content Creation -->
        <div v-else-if="currentStep === 2" key="step2" class="wizard-step">
          <div class="step-header">
            <h2 class="step-title">Content Creation</h2>
            <p class="step-subtitle">Describe your ad or provide examples</p>
          </div>

          <div class="content-creation-tabs">
            <div class="tab-buttons">
              <button 
                class="tab-button"
                :class="{ 'active': contentMode === 'prompt' }"
                @click="contentMode = 'prompt'"
              >
                <svg class="tab-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd" />
                </svg>
                Describe Your Ad
              </button>
              <button 
                class="tab-button"
                :class="{ 'active': contentMode === 'examples' }"
                @click="contentMode = 'examples'"
              >
                <svg class="tab-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M12.586 4.586a2 2 0 112.828 2.828l-3 3a2 2 0 01-2.828 0 1 1 0 00-1.414 1.414 4 4 0 005.656 0l3-3a4 4 0 00-5.656-5.656l-1.5 1.5a1 1 0 101.414 1.414l1.5-1.5zm-5 5a2 2 0 012.828 0 1 1 0 101.414-1.414 4 4 0 00-5.656 0l-3 3a4 4 0 105.656 5.656l1.5-1.5a1 1 0 10-1.414-1.414l-1.5 1.5a2 2 0 11-2.828-2.828l3-3z" clip-rule="evenodd" />
                </svg>
                Use Examples
              </button>
            </div>

            <div class="tab-content">
              <!-- Prompt Mode -->
              <div v-if="contentMode === 'prompt'" class="content-mode">
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Ad Description</span>
                    <span class="label-required">*</span>
                  </label>
                  <Textarea 
                    v-model="formData.prompt" 
                    rows="6"
                    placeholder="Describe your product, target audience, and key message. Be specific about what you want to promote..."
                    class="field-input"
                    :class="{ 'field-error': errors.prompt }"
                  />
                  <div class="field-hint">
                    💡 Tip: Include details about your product, target audience, tone, and key benefits
                  </div>
                  <div v-if="errors.prompt" class="field-error-message">
                    {{ errors.prompt }}
                  </div>
                </div>
              </div>

              <!-- Examples Mode -->
              <div v-else class="content-mode">
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Example Ad Links</span>
                  </label>
                  <div class="ad-links-container">
                    <div v-for="(link, index) in adLinks" :key="index" class="ad-link-input">
                      <InputText 
                        v-model="adLinks[index]" 
                        placeholder="Paste Facebook Ad Library URL..."
                        class="field-input"
                      />
                      <button 
                        v-if="adLinks.length > 1"
                        @click="removeAdLink(index)"
                        class="remove-link-btn"
                      >
                        <svg viewBox="0 0 20 20" fill="currentColor">
                          <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                        </svg>
                      </button>
                    </div>
                    <button @click="addAdLink" class="add-link-btn">
                      <svg viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
                      </svg>
                      Add Another Link
                    </button>
                  </div>
                  <div class="field-hint">
                    💡 Find inspiration at <a href="https://www.facebook.com/ads/library" target="_blank" class="link">Facebook Ads Library</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3: AI Configuration -->
        <div v-else-if="currentStep === 3" key="step3" class="wizard-step">
          <div class="step-header">
            <h2 class="step-title">AI Configuration</h2>
            <p class="step-subtitle">Choose your AI providers and settings</p>
          </div>

          <div class="ai-config-grid">
            <!-- Text Provider -->
            <div class="provider-section">
              <h3 class="provider-title">Text Generation</h3>
              <div class="provider-grid">
                <div 
                  v-for="provider in textProviders" 
                  :key="provider.id"
                  class="provider-card"
                  :class="{ 'selected': formData.textProvider === provider.id }"
                  @click="formData.textProvider = provider.id"
                >
                  <div class="provider-header">
                    <div class="provider-icon">
                      <component :is="provider.icon" />
                    </div>
                    <div class="provider-info">
                      <div class="provider-name">{{ provider.name }}</div>
                      <div class="provider-description">{{ provider.description }}</div>
                    </div>
                  </div>
                  <div class="provider-features">
                    <div v-for="feature in provider.features" :key="feature" class="feature-tag">
                      {{ feature }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- File Upload Section -->
            <div class="provider-section">
              <h3 class="provider-title">Upload Your Own Media</h3>
              <div class="upload-section">
                <div class="upload-area" :class="{ 'has-file': formData.uploadedFile }">
                  <input 
                    type="file" 
                    @change="handleFileUpload"
                    accept="image/*,video/*"
                    class="file-input"
                    id="file-upload"
                  />
                  <label for="file-upload" class="upload-label">
                    <svg class="upload-icon" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd" />
                    </svg>
                    <div class="upload-text">
                      <div class="upload-title">Upload Image or Video</div>
                      <div class="upload-description">Drag and drop or click to browse</div>
                    </div>
                  </label>
                </div>
                
                <div v-if="formData.uploadedFile" class="uploaded-file">
                  <div class="file-info">
                    <svg class="file-icon" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd" />
                    </svg>
                    <div class="file-details">
                      <div class="file-name">{{ formData.uploadedFile.name }}</div>
                      <div class="file-size">{{ (formData.uploadedFile.size / 1024 / 1024).toFixed(2) }} MB</div>
                    </div>
                  </div>
                  <button @click="clearUploadedFile" class="remove-file-btn">
                    <svg viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>

            <!-- Image Provider -->
            <div class="provider-section" :class="{ 'disabled': formData.uploadedFile }">
              <h3 class="provider-title">Or Generate with AI</h3>
              <div class="provider-grid">
                <div 
                  v-for="provider in imageProviders" 
                  :key="provider.id"
                  class="provider-card"
                  :class="{ 
                    'selected': formData.imageProvider === provider.id,
                    'disabled': formData.uploadedFile
                  }"
                  @click="!formData.uploadedFile && (formData.imageProvider = provider.id)"
                >
                  <div class="provider-header">
                    <div class="provider-icon">
                      <component :is="provider.icon" />
                    </div>
                    <div class="provider-info">
                      <div class="provider-name">{{ provider.name }}</div>
                      <div class="provider-description">{{ provider.description }}</div>
                    </div>
                  </div>
                  <div class="provider-features">
                    <div v-for="feature in provider.features" :key="feature" class="feature-tag">
                      {{ feature }}
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="formData.uploadedFile" class="disabled-message">
                Image provider selection is disabled when you upload your own file
              </div>
            </div>
          </div>

          <!-- Advanced Settings -->
          <div class="advanced-settings">
            <button 
              @click="showAdvanced = !showAdvanced"
              class="advanced-toggle"
            >
              <svg class="toggle-icon" :class="{ 'rotated': showAdvanced }" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
              </svg>
              Advanced Settings
            </button>
            
            <transition name="expand">
              <div v-if="showAdvanced" class="advanced-content">
                <div class="settings-grid">
                  <div class="form-field">
                    <label class="field-label">
                      <span class="label-text">Number of Variations</span>
                    </label>
                    <InputNumber 
                      v-model="formData.numberOfVariations" 
                      :min="1" 
                      :max="5"
                      class="field-input"
                    />
                  </div>
                  
                  <div class="form-field">
                    <label class="field-label">
                      <span class="label-text">Language</span>
                    </label>
                    <Dropdown 
                      v-model="formData.language" 
                      :options="languages" 
                      optionLabel="label" 
                      optionValue="value" 
                      placeholder="Select language"
                      class="field-input"
                    />
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>

        <!-- Step 4: Preview & Generate -->
        <div v-else-if="currentStep === 4" key="step4" class="wizard-step">
          <div class="step-header">
            <h2 class="step-title">Preview & Generate</h2>
            <p class="step-subtitle">Review your settings and generate your ad</p>
          </div>

          <div class="preview-container">
            <!-- Settings Summary -->
            <div class="settings-summary">
              <h3 class="summary-title">Configuration Summary</h3>
              <div class="summary-grid">
                <div class="summary-item">
                  <div class="summary-label">Campaign</div>
                  <div class="summary-value">{{ selectedCampaign?.name }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">Ad Type</div>
                  <div class="summary-value">{{ selectedAdType?.label }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">Text Provider</div>
                  <div class="summary-value">{{ selectedTextProvider?.name }}</div>
                </div>
                <div class="summary-item">
                  <div class="summary-label">Image Provider</div>
                  <div class="summary-value">{{ selectedImageProvider?.name }}</div>
                </div>
              </div>
            </div>

            <!-- Generate Button -->
            <div class="generate-section">
              <button 
                @click="generatePreview"
                :disabled="isGenerating"
                class="generate-btn"
              >
                <svg v-if="isGenerating" class="btn-icon animate-spin" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
                </svg>
                <svg v-else class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
                </svg>
                {{ isGenerating ? 'Generating...' : 'Generate Ad Variations' }}
              </button>
            </div>

            <!-- Preview Results -->
            <div v-if="adVariations.length > 0" class="preview-results">
              <h3 class="results-title">Generated Variations</h3>
              <div class="variations-grid">
                <div 
                  v-for="(variation, index) in adVariations" 
                  :key="index"
                  class="variation-card"
                  :class="{ 'selected': selectedVariation === index }"
                  @click="selectedVariation = index"
                >
                  <div class="variation-header">
                    <div class="variation-number">Variation {{ index + 1 }}</div>
                    <div class="variation-select">
                      <input 
                        type="radio" 
                        :value="index" 
                        v-model="selectedVariation"
                        class="variation-radio"
                      />
                    </div>
                  </div>
                  <div class="variation-content">
                    <div v-if="variation.imageUrl" class="variation-image">
                      <img :src="variation.imageUrl" :alt="`Variation ${index + 1}`" />
                    </div>
                    <div class="variation-text">
                      <div class="text-field">
                        <div class="text-label">Headline</div>
                        <div class="text-content">{{ variation.headline }}</div>
                      </div>
                      <div class="text-field">
                        <div class="text-label">Description</div>
                        <div class="text-content">{{ variation.description }}</div>
                      </div>
                      <div class="text-field">
                        <div class="text-label">Primary Text</div>
                        <div class="text-content">{{ variation.primaryText }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Wizard Navigation -->
    <div class="wizard-navigation">
      <button 
        @click="cancelWizard"
        class="nav-btn nav-btn-ghost"
      >
        <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
        </svg>
        Cancel
      </button>
      
      <button 
        v-if="currentStep > 1"
        @click="previousStep"
        class="nav-btn nav-btn-secondary"
      >
        <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" />
        </svg>
        Previous
      </button>
      
      <div class="nav-spacer"></div>
      
      <button 
        v-if="currentStep < steps.length"
        @click="nextStep"
        :disabled="!canProceedToNext"
        class="nav-btn nav-btn-primary"
      >
        Next
        <svg class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" />
        </svg>
      </button>
      
      <button 
        v-else-if="currentStep === steps.length"
        @click="generatePreview"
        :disabled="isGenerating"
        class="nav-btn nav-btn-primary"
      >
        <svg v-if="isGenerating" class="btn-icon animate-spin" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
        </svg>
        <svg v-else class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
        </svg>
        {{ isGenerating ? 'Generating...' : 'Generate Preview' }}
      </button>
      
      <button 
        v-if="adVariations.length > 0"
        @click="saveAd"
        :disabled="loading"
        class="nav-btn nav-btn-success"
      >
        <svg v-if="loading" class="btn-icon animate-spin" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
        </svg>
        <svg v-else class="btn-icon" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
        </svg>
        {{ loading ? 'Saving...' : 'Save Ad' }}
      </button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'

export default {
  name: 'AdCreationWizard',
  
  emits: ['ad-created', 'wizard-cancelled'],
  
  setup(props, { emit }) {
    const toast = useToast()
    
    // Reactive data
    const currentStep = ref(1)
    const loading = ref(false)
    const isGenerating = ref(false)
    const showAdvanced = ref(false)
    const contentMode = ref('prompt')
    const selectedVariation = ref(0)
    const adLinks = ref([''])
    
    // Form data
    const formData = reactive({
      campaignId: null,
      name: '',
      adType: '',
      prompt: '',
      textProvider: '',
      imageProvider: '',
      numberOfVariations: 3,
      language: 'en',
      uploadedFile: null
    })
    
    // Validation errors
    const errors = reactive({
      campaignId: '',
      name: '',
      adType: '',
      prompt: '',
      textProvider: '',
      imageProvider: ''
    })
    
    // Data arrays
    const campaigns = ref([])
    const adVariations = ref([])
    
    // Steps configuration
    const steps = [
      {
        id: 1,
        title: 'Basic Info',
        description: 'Thông tin cơ bản cho quảng cáo'
      },
      {
        id: 2,
        title: 'AI Setup',
        description: 'Cấu hình AI tạo nội dung'
      },
      {
        id: 3,
        title: 'Preview',
        description: 'Xem trước và lưu quảng cáo'
      }
    ]
    
    // Ad types
    const adTypes = [
      {
        value: 'image',
        label: 'Image Ad',
        description: 'Single image with text overlay',
        icon: 'ImageIcon'
      },
      {
        value: 'video',
        label: 'Video Ad',
        description: 'Video content with captions',
        icon: 'VideoIcon'
      },
      {
        value: 'carousel',
        label: 'Carousel Ad',
        description: 'Multiple images in a carousel',
        icon: 'CarouselIcon'
      }
    ]
    
    // Text providers
    const textProviders = ref([
      {
        id: 'openai',
        name: 'OpenAI GPT-4',
        description: 'Advanced language model for creative text generation',
        icon: 'OpenAIIcon',
        features: ['Creative', 'Contextual', 'Multi-language']
      },
      {
        id: 'claude',
        name: 'Claude AI',
        description: 'Anthropic\'s conversational AI for natural content',
        icon: 'ClaudeIcon',
        features: ['Conversational', 'Safe', 'Detailed']
      },
      {
        id: 'gemini',
        name: 'Google Gemini',
        description: 'Google\'s multimodal AI for comprehensive content',
        icon: 'GeminiIcon',
        features: ['Multimodal', 'Fast', 'Integrated']
      }
    ])
    
    // Image providers
    const imageProviders = ref([
      {
        id: 'dalle',
        name: 'DALL-E 3',
        description: 'OpenAI\'s advanced image generation model',
        icon: 'DalleIcon',
        features: ['High Quality', 'Creative', 'Detailed']
      },
      {
        id: 'midjourney',
        name: 'Midjourney',
        description: 'Artistic and creative image generation',
        icon: 'MidjourneyIcon',
        features: ['Artistic', 'Creative', 'Stylized']
      },
      {
        id: 'stable-diffusion',
        name: 'Stable Diffusion',
        description: 'Fast and customizable image generation',
        icon: 'StableDiffusionIcon',
        features: ['Fast', 'Customizable', 'Open Source']
      }
    ])
    
    // Languages
    const languages = [
      { label: 'English', value: 'en' },
      { label: 'Vietnamese', value: 'vi' },
      { label: 'Spanish', value: 'es' },
      { label: 'French', value: 'fr' },
      { label: 'German', value: 'de' }
    ]
    
    // Computed properties
    const selectedCampaign = computed(() => {
      return campaigns.value.find(c => c.id === formData.campaignId)
    })
    
    const selectedAdType = computed(() => {
      return adTypes.find(t => t.value === formData.adType)
    })
    
    const selectedTextProvider = computed(() => {
      return textProviders.value.find(p => p.id === formData.textProvider)
    })
    
    const selectedImageProvider = computed(() => {
      return imageProviders.value.find(p => p.id === formData.imageProvider)
    })
    
    const canProceedToNext = computed(() => {
      switch (currentStep.value) {
        case 1:
          return formData.campaignId && formData.name && formData.adType
        case 2:
          return formData.prompt && formData.textProvider && 
                 (formData.uploadedFile || formData.imageProvider)
        case 3:
          return true
        default:
          return false
      }
    })
    
    const canGoBack = computed(() => {
      return currentStep.value > 1
    })
    
    // Methods
    const loadData = async () => {
      loading.value = true
      try {
        // Load data from backend APIs
        const [campaignsRes, textProvidersRes, imageProvidersRes] = await Promise.all([
          fetch('/api/campaigns'),
          fetch('/api/ai-providers/text'),
          fetch('/api/ai-providers/image')
        ])
        
        // Parse responses
        const campaignsData = await campaignsRes.json()
        const textProvidersData = await textProvidersRes.json()
        const imageProvidersData = await imageProvidersRes.json()
        
        campaigns.value = campaignsData.content || campaignsData || []
        textProviders.value = textProvidersData
        imageProviders.value = imageProvidersData
      } catch (error) {
        console.error('Error loading data:', error)
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load data. Please try again.',
          life: 3000
        })
      } finally {
        loading.value = false
      }
    }
    
    const validateStep = () => {
      // Clear previous errors
      Object.keys(errors).forEach(key => errors[key] = '')
      
      let isValid = true
      
      switch (currentStep.value) {
        case 1:
          if (!formData.campaignId) {
            errors.campaignId = 'Please select a campaign'
            isValid = false
          }
          if (!formData.name.trim()) {
            errors.name = 'Please enter an ad name'
            isValid = false
          }
          if (!formData.adType) {
            errors.adType = 'Please select an ad type'
            isValid = false
          }
          break
          
        case 2:
          if (!formData.prompt.trim()) {
            errors.prompt = 'Please provide a description or examples'
            isValid = false
          }
          if (!formData.textProvider) {
            errors.textProvider = 'Please select a text provider'
            isValid = false
          }
          if (!formData.uploadedFile && !formData.imageProvider) {
            errors.imageProvider = 'Please either upload a file or select an image provider'
            isValid = false
          }
          break
      }
      
      return isValid
    }
    
    const nextStep = () => {
      if (!validateStep()) return
      
      if (currentStep.value < steps.length) {
        currentStep.value++
      }
    }
    
    const previousStep = () => {
      if (currentStep.value > 1) {
        currentStep.value--
      }
    }
    
    const addAdLink = () => {
      adLinks.value.push('')
    }
    
    const removeAdLink = (index) => {
      if (adLinks.value.length > 1) {
        adLinks.value.splice(index, 1)
      }
    }
    
    const handleFileUpload = (event) => {
      const file = event.target.files[0]
      if (file) {
        formData.uploadedFile = file
        // Clear image provider when file is uploaded
        formData.imageProvider = ''
      }
    }
    
    const clearUploadedFile = () => {
      formData.uploadedFile = null
    }
    
    const generatePreview = async () => {
      isGenerating.value = true
      try {
        // Call real API to generate ad variations
        const response = await fetch('/api/ads/generate', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            campaignId: formData.campaignId,
            name: formData.name,
            adType: formData.adType,
            prompt: formData.prompt,
            textProvider: formData.textProvider,
            imageProvider: formData.imageProvider,
            numberOfVariations: formData.numberOfVariations,
            language: formData.language,
            uploadedFile: formData.uploadedFile
          })
        })
        
        if (!response.ok) {
          throw new Error('Failed to generate ad variations')
        }
        
        const result = await response.json()
        adVariations.value = result.data.variations || []
        
        toast.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Ad variations generated successfully!',
          life: 3000
        })
      } catch (error) {
        console.error('Error generating preview:', error)
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to generate preview. Please try again.',
          life: 3000
        })
      } finally {
        isGenerating.value = false
      }
    }
    
    const saveAd = async () => {
      if (selectedVariation.value === null) {
        toast.add({
          severity: 'warn',
          summary: 'Warning',
          detail: 'Please select a variation to save',
          life: 3000
        })
        return
      }
      
      loading.value = true
      try {
        // Call real API to save ad
        const response = await fetch('/api/ads', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            campaignId: formData.campaignId,
            name: formData.name,
            adType: formData.adType,
            prompt: formData.prompt,
            textProvider: formData.textProvider,
            imageProvider: formData.imageProvider,
            numberOfVariations: formData.numberOfVariations,
            language: formData.language,
            uploadedFile: formData.uploadedFile,
            selectedVariation: adVariations.value[selectedVariation.value]
          })
        })
        
        if (!response.ok) {
          throw new Error('Failed to save ad')
        }
        
        const result = await response.json()
        const adData = result.data
        
        emit('ad-created', adData)
        
        toast.add({
          severity: 'success',
          summary: 'Success',
          detail: 'Ad created successfully!',
          life: 3000
        })
      } catch (error) {
        console.error('Error saving ad:', error)
        toast.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to save ad. Please try again.',
          life: 3000
        })
      } finally {
        loading.value = false
      }
    }
    
    const cancelWizard = () => {
      emit('wizard-cancelled')
    }
    
    // Lifecycle
    onMounted(() => {
      loadData()
    })
    
    return {
      // Reactive data
      currentStep,
      loading,
      isGenerating,
      showAdvanced,
      contentMode,
      selectedVariation,
      adLinks,
      formData,
      errors,
      campaigns,
      adVariations,
      
      // Static data
      steps,
      adTypes,
      textProviders,
      imageProviders,
      languages,
      
      // Computed
      selectedCampaign,
      selectedAdType,
      selectedTextProvider,
      selectedImageProvider,
      canProceedToNext,
      canGoBack,
      
      // Methods
      nextStep,
      previousStep,
      addAdLink,
      removeAdLink,
      handleFileUpload,
      clearUploadedFile,
      generatePreview,
      saveAd,
      cancelWizard
    }
  }
}
</script>

<style scoped>
/* Modern Ad Creation Wizard Styles */
.ad-creation-wizard {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-6);
}

/* Progress Indicator */
.wizard-progress {
  margin-bottom: var(--space-8);
}

.progress-container {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.progress-step {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

.step-indicator {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-semibold);
  transition: var(--transition-all);
  border: 2px solid var(--neutral-300);
  background: var(--color-bg-secondary);
  color: var(--color-text-muted);
}

.progress-step.active .step-indicator {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: white;
  box-shadow: 0 0 0 4px rgba(24, 119, 242, 0.1);
}

.progress-step.completed .step-indicator {
  background: var(--success-500);
  border-color: var(--success-500);
  color: white;
}

.step-number {
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
}

.step-check {
  width: 1.25rem;
  height: 1.25rem;
}

.step-content {
  margin-left: var(--space-3);
  text-align: left;
}

.step-title {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.step-description {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.step-connector {
  width: 4rem;
  height: 2px;
  background: var(--neutral-200);
  margin: 0 var(--space-4);
}

.progress-step.completed + .progress-step .step-connector {
  background: var(--success-500);
}

/* Wizard Content */
.wizard-content {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: var(--space-8);
  margin-bottom: var(--space-6);
  min-height: 600px;
}

.wizard-step {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.step-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.step-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  margin-bottom: var(--space-2);
}

.step-subtitle {
  font-size: var(--text-lg);
  color: var(--color-text-secondary);
}

/* Form Styles */
.form-grid {
  display: grid;
  gap: var(--space-6);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.field-label {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.label-required {
  color: var(--error-500);
  font-weight: var(--font-bold);
}

.field-input {
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  transition: var(--transition-all);
  background: var(--color-bg-secondary);
}

.field-input:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(24, 119, 242, 0.1);
}

.field-input.field-error {
  border-color: var(--error-500);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
}

.field-error-message {
  font-size: var(--text-sm);
  color: var(--error-600);
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.field-hint {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* Ad Type Selection */
.ad-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.ad-type-option {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: var(--transition-all);
  background: var(--color-bg-secondary);
}

.ad-type-option:hover {
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-md);
}

.ad-type-option.selected {
  border-color: var(--brand-primary);
  background: var(--primary-50);
  box-shadow: 0 0 0 3px rgba(24, 119, 242, 0.1);
}

.type-icon {
  width: 3rem;
  height: 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-100);
  border-radius: var(--radius-lg);
  color: var(--brand-primary);
}

.type-content {
  flex: 1;
}

.type-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.type-description {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

/* Content Creation Tabs */
.content-creation-tabs {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.tab-buttons {
  display: flex;
  gap: var(--space-2);
  background: var(--color-bg-tertiary);
  padding: var(--space-1);
  border-radius: var(--radius-lg);
}

.tab-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  background: transparent;
  cursor: pointer;
  transition: var(--transition-all);
}

.tab-button:hover {
  color: var(--color-text);
  background: var(--color-hover);
}

.tab-button.active {
  color: var(--brand-primary);
  background: var(--color-bg-secondary);
  box-shadow: var(--shadow-sm);
}

.tab-icon {
  width: 1.25rem;
  height: 1.25rem;
}

.tab-content {
  min-height: 300px;
}

/* Ad Links */
.ad-links-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.ad-link-input {
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.remove-link-btn {
  padding: var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  background: var(--error-100);
  color: var(--error-600);
  cursor: pointer;
  transition: var(--transition-colors);
}

.remove-link-btn:hover {
  background: var(--error-200);
}

.remove-link-btn svg {
  width: 1.25rem;
  height: 1.25rem;
}

.add-link-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: var(--transition-all);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.add-link-btn:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  background: var(--primary-50);
}

.add-link-btn svg {
  width: 1.25rem;
  height: 1.25rem;
}

/* AI Configuration */
.ai-config-grid {
  display: grid;
  gap: var(--space-8);
}

.provider-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.provider-section.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.provider-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-4);
}

.provider-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: var(--transition-all);
  background: var(--color-bg-secondary);
}

.provider-card:hover:not(.disabled) {
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-md);
}

.provider-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.provider-card.selected {
  border-color: var(--brand-primary);
  background: var(--primary-50);
  box-shadow: 0 0 0 3px rgba(24, 119, 242, 0.1);
}

.provider-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.provider-icon {
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-100);
  border-radius: var(--radius-md);
  color: var(--brand-primary);
}

.provider-info {
  flex: 1;
}

.provider-name {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.provider-description {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.provider-features {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.feature-tag {
  padding: var(--space-1) var(--space-2);
  background: var(--neutral-100);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

.disabled-message {
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--warning-50);
  color: var(--warning-700);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  text-align: center;
}

/* File Upload Section */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.upload-area {
  position: relative;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-8);
  text-align: center;
  transition: var(--transition-all);
  background: var(--color-bg-secondary);
}

.upload-area:hover {
  border-color: var(--brand-primary);
  background: var(--primary-50);
}

.upload-area.has-file {
  border-color: var(--success-500);
  background: var(--success-50);
}

.file-input {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.upload-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  cursor: pointer;
}

.upload-icon {
  width: 3rem;
  height: 3rem;
  color: var(--color-text-secondary);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.upload-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.upload-description {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.uploaded-file {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  background: var(--success-100);
  border: 1px solid var(--success-200);
  border-radius: var(--radius-lg);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.file-icon {
  width: 2rem;
  height: 2rem;
  color: var(--success-600);
}

.file-details {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.file-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text);
}

.file-size {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.remove-file-btn {
  padding: var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  background: var(--error-100);
  color: var(--error-600);
  cursor: pointer;
  transition: var(--transition-colors);
}

.remove-file-btn:hover {
  background: var(--error-200);
}

.remove-file-btn svg {
  width: 1.25rem;
  height: 1.25rem;
}

/* Advanced Settings */
.advanced-settings {
  margin-top: var(--space-6);
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-6);
}

.advanced-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-bg-tertiary);
  color: var(--color-text);
  cursor: pointer;
  transition: var(--transition-colors);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.advanced-toggle:hover {
  background: var(--color-hover);
}

.toggle-icon {
  width: 1.25rem;
  height: 1.25rem;
  transition: var(--transition-transform);
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.advanced-content {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

/* Preview Section */
.preview-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.settings-summary {
  background: var(--color-bg-tertiary);
  padding: var(--space-6);
  border-radius: var(--radius-lg);
}

.summary-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-4);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.summary-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

.summary-value {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.generate-section {
  display: flex;
  justify-content: center;
}

.generate-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-8);
  background: var(--brand-primary);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: var(--transition-all);
  box-shadow: var(--shadow-md);
}

.generate-btn:hover:not(:disabled) {
  background: var(--brand-primary-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-icon {
  width: 1.5rem;
  height: 1.5rem;
}

/* Variations Grid */
.preview-results {
  margin-top: var(--space-6);
}

.results-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-4);
}

.variations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: var(--space-4);
}

.variation-card {
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition-all);
  background: var(--color-bg-secondary);
}

.variation-card:hover {
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-md);
}

.variation-card.selected {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgba(24, 119, 242, 0.1);
}

.variation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border);
}

.variation-number {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.variation-radio {
  width: 1.25rem;
  height: 1.25rem;
  accent-color: var(--brand-primary);
}

.variation-content {
  padding: var(--space-4);
}

.variation-image {
  margin-bottom: var(--space-4);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.variation-image img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: var(--radius-md);
}

.variation-text {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.text-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.text-label {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.text-content {
  font-size: var(--text-sm);
  color: var(--color-text);
  line-height: var(--leading-relaxed);
}

/* Navigation */
.wizard-navigation {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.nav-spacer {
  flex: 1;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: var(--transition-all);
}

.nav-btn-secondary {
  background: var(--color-bg-tertiary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.nav-btn-secondary:hover:not(:disabled) {
  background: var(--color-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.nav-btn-primary {
  background: var(--brand-primary);
  color: white;
}

.nav-btn-primary:hover:not(:disabled) {
  background: var(--brand-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.nav-btn-success {
  background: var(--success-500);
  color: white;
}

.nav-btn-success:hover:not(:disabled) {
  background: var(--success-600);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.nav-btn-ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.nav-btn-ghost:hover:not(:disabled) {
  background: var(--color-hover);
  color: var(--color-text);
  border-color: var(--color-border-hover);
}

.nav-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Animations */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 500px;
  opacity: 1;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Enhanced Mobile Responsiveness */

/* Large tablets and small desktops */
@media (max-width: 1024px) {
  .ad-creation-wizard {
    padding: var(--space-4);
  }

  .wizard-content {
    padding: var(--space-6);
  }

  .progress-container {
    flex-wrap: wrap;
    justify-content: center;
    gap: var(--space-3);
  }

  .step-content {
    text-align: center;
    margin-left: 0;
    margin-top: var(--space-2);
  }

  .ad-type-grid,
  .provider-grid {
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  }

  .variations-grid {
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  }
}

/* Tablets */
@media (max-width: 768px) {
  .ad-creation-wizard {
    padding: var(--space-3);
  }

  .wizard-content {
    padding: var(--space-4);
    margin-bottom: var(--space-4);
    min-height: auto;
  }

  .step-header {
    margin-bottom: var(--space-6);
  }

  .step-title {
    font-size: var(--text-2xl);
  }

  .step-subtitle {
    font-size: var(--text-base);
  }

  /* Progress indicator - horizontal scroll on mobile */
  .progress-container {
    flex-direction: row;
    overflow-x: auto;
    padding: var(--space-2) 0;
    gap: var(--space-6);
    justify-content: flex-start;
    scroll-snap-type: x mandatory;
  }

  .progress-step {
    flex-shrink: 0;
    scroll-snap-align: center;
    min-width: 120px;
  }

  .step-connector {
    display: block;
    width: 2rem;
    margin: 0 var(--space-2);
  }

  .step-content {
    text-align: center;
    margin-left: 0;
    margin-top: var(--space-2);
  }

  .step-title {
    font-size: var(--text-xs);
  }

  .step-description {
    font-size: var(--text-xs);
  }

  /* Form improvements */
  .form-grid {
    gap: var(--space-4);
  }

  .field-input {
    padding: var(--space-4);
    font-size: var(--text-base);
    border-radius: var(--radius-lg);
  }

  /* Ad type selection - stack vertically */
  .ad-type-grid {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .ad-type-option {
    padding: var(--space-4);
    gap: var(--space-3);
  }

  .type-icon {
    width: 2.5rem;
    height: 2.5rem;
  }

  /* Content creation tabs */
  .tab-buttons {
    flex-direction: column;
    gap: var(--space-1);
  }

  .tab-button {
    padding: var(--space-4);
    font-size: var(--text-base);
  }

  /* Ad links management */
  .ad-link-input {
    flex-direction: column;
    gap: var(--space-2);
  }

  .remove-link-btn {
    align-self: flex-end;
    padding: var(--space-3);
  }

  /* Provider selection */
  .provider-grid {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .provider-card {
    padding: var(--space-4);
  }

  .provider-header {
    gap: var(--space-3);
  }

  .provider-icon {
    width: 2rem;
    height: 2rem;
  }

  /* Advanced settings */
  .settings-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }

  /* Preview and variations */
  .summary-grid {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .variations-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }

  .variation-card {
    margin-bottom: var(--space-4);
  }

  /* Navigation */
  .wizard-navigation {
    flex-direction: column;
    gap: var(--space-3);
    padding: var(--space-4);
    position: sticky;
    bottom: 0;
    background: var(--color-bg-secondary);
    border-top: 1px solid var(--color-border);
    box-shadow: var(--shadow-lg);
  }

  .nav-btn {
    width: 100%;
    justify-content: center;
    padding: var(--space-4) var(--space-6);
    font-size: var(--text-base);
  }
}

/* Mobile phones */
@media (max-width: 640px) {
  .ad-creation-wizard {
    padding: var(--space-2);
  }

  .wizard-content {
    padding: var(--space-3);
    border-radius: var(--radius-lg);
  }

  .step-header {
    margin-bottom: var(--space-4);
    text-align: center;
  }

  .step-title {
    font-size: var(--text-xl);
    line-height: var(--leading-tight);
  }

  .step-subtitle {
    font-size: var(--text-sm);
  }

  /* Progress indicator - compact version */
  .progress-container {
    gap: var(--space-4);
    padding: var(--space-1) 0;
  }

  .progress-step {
    min-width: 100px;
  }

  .step-indicator {
    width: 2rem;
    height: 2rem;
  }

  .step-number {
    font-size: var(--text-xs);
  }

  .step-connector {
    width: 1.5rem;
  }

  /* Form optimizations */
  .field-input {
    padding: var(--space-3);
    font-size: var(--text-base);
    min-height: 44px; /* iOS touch target minimum */
  }

  .field-input:focus {
    transform: none; /* Prevent zoom on iOS */
  }

  /* Dropdown improvements */
  .p-dropdown {
    min-height: 44px;
  }

  .p-dropdown .p-dropdown-label {
    padding: var(--space-3);
    font-size: var(--text-base);
  }

  /* Textarea improvements */
  .p-inputtextarea {
    min-height: 120px;
    resize: vertical;
  }

  /* Ad type selection - more compact */
  .ad-type-option {
    padding: var(--space-3);
    flex-direction: column;
    text-align: center;
    gap: var(--space-2);
  }

  .type-icon {
    width: 2rem;
    height: 2rem;
    margin: 0 auto;
  }

  .type-title {
    font-size: var(--text-base);
  }

  .type-description {
    font-size: var(--text-xs);
  }

  /* Provider cards - more compact */
  .provider-card {
    padding: var(--space-3);
  }

  .provider-header {
    flex-direction: column;
    text-align: center;
    gap: var(--space-2);
  }

  .provider-icon {
    width: 1.5rem;
    height: 1.5rem;
    margin: 0 auto;
  }

  .provider-name {
    font-size: var(--text-base);
  }

  .provider-description {
    font-size: var(--text-xs);
  }

  .provider-features {
    justify-content: center;
  }

  .feature-tag {
    font-size: var(--text-xs);
    padding: var(--space-1) var(--space-2);
  }

  /* Variation cards - optimized for mobile */
  .variation-card {
    border-radius: var(--radius-lg);
  }

  .variation-content {
    padding: var(--space-3);
  }

  .variation-image img {
    height: 100px;
    width: 100px;
  }

  .text-content {
    font-size: var(--text-sm);
  }

  /* Navigation - fixed bottom */
  .wizard-navigation {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: var(--space-3);
    background: var(--color-bg-secondary);
    border-top: 1px solid var(--color-border);
    box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.1);
    z-index: 10;
  }

  .nav-btn {
    padding: var(--space-3) var(--space-4);
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    min-height: 44px;
  }

  /* Add bottom padding to wizard content to account for fixed navigation */
  .wizard-content {
    margin-bottom: 80px;
  }
}

/* Extra small devices */
@media (max-width: 480px) {
  .progress-container {
    gap: var(--space-2);
  }

  .progress-step {
    min-width: 80px;
  }

  .step-title {
    font-size: var(--text-lg);
  }

  .wizard-navigation {
    padding: var(--space-2);
  }

  .nav-btn {
    padding: var(--space-3);
    font-size: var(--text-sm);
  }

  .btn-icon {
    width: 1rem;
    height: 1rem;
  }
}

.link {
  color: var(--brand-primary);
  text-decoration: none;
  font-weight: var(--font-medium);
}

.link:hover {
  text-decoration: underline;
}
</style>
