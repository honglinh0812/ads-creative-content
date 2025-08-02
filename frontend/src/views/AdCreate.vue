<template>
  <div :class="['page-wrapper', { 'sidebar-closed': !sidebarOpen }]">
    <AppSidebar :sidebarOpen="sidebarOpen" @toggle="toggleSidebar" @logout="handleLogout" />
    <div class="main-content-wrapper" :style="mainContentStyle">
      <!-- Mobile Header -->
      <div class="mobile-header lg:hidden">
        <button @click="toggleSidebar" class="btn btn-sm btn-ghost hover:bg-gray-100 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
        <h1 class="text-lg font-semibold text-secondary-900">Create Ad</h1>
      </div>

      <div class="content-wrapper">
        <div class="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- Header -->
          <div class="mb-8">
            <div class="flex items-center justify-between">
              <div>
                <h1 class="text-3xl font-bold text-secondary-900">Create New Ad</h1>
                <p class="text-secondary-600 mt-2">Create compelling Facebook ads with AI assistance</p>
              </div>
              <div class="flex gap-2">
                <router-link to="/ads" class="btn btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"></path>
                  </svg>
                  Back to Ads
                </router-link>
                <button @click="showHelp = true" class="btn btn-ghost" aria-label="Help">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Progress Steps -->
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
          <!-- End Progress Steps -->

          <!-- Step 1: Basic Information -->
          <div v-if="currentStep === 1" class="card">
            <div class="card-header">
              <h2 class="text-xl font-semibold text-secondary-900">Step 1: Basic Information</h2>
              <p class="text-secondary-600">Fill in the basic details for your ad</p>
            </div>
            <div class="card-body">
              <div class="form-grid">
                <!-- Campaign Selection -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Campaign</span>
                    <span class="label-required">*</span>
                  </label>
                  <select v-model="formData.campaignId" class="form-input" :class="{ 'error': showValidation && !formData.campaignId }">
                    <option value="">Select a campaign</option>
                    <option v-for="campaign in campaigns" :key="campaign.id" :value="campaign.id">
                      {{ campaign.name }}
                    </option>
                  </select>
                  <div v-if="showValidation && !formData.campaignId" class="error-message">Please select a campaign</div>
                </div>

                <!-- Ad Type -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Ad Type</span>
                    <span class="label-required">*</span>
                    <button 
                      type="button" 
                      @click="showAdTypeHelp = true" 
                      class="help-btn ml-2"
                      title="Learn more about ad types"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                    </button>
                  </label>
                  <select v-model="formData.adType" class="form-input" @change="onAdTypeChange">
                    <option v-for="type in adTypes" :key="type.value" :value="type.value">
                      {{ type.label }}
                    </option>
                  </select>
                  <div v-if="selectedAdType" class="ad-type-description mt-2 p-3 bg-blue-50 rounded-lg">
                    <div class="flex items-start">
                      <svg class="w-5 h-5 text-blue-600 mt-0.5 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                      <div>
                        <p class="text-sm font-medium text-blue-900">{{ selectedAdType.label }}</p>
                        <p class="text-sm text-blue-700">{{ selectedAdType.description }}</p>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Ad Name -->
                <div class="form-field md:col-span-2">
                  <label class="field-label">
                    <span class="label-text">Ad Name</span>
                    <span class="label-required">*</span>
                  </label>
                  <input v-model="formData.name" type="text" class="form-input" :class="{ 'error': showValidation && !formData.name }" placeholder="Enter ad name">
                  <div v-if="showValidation && !formData.name" class="error-message">Please enter an ad name</div>
                </div>

                <!-- Number of Variations -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Number of Variations</span>
                    <span class="label-required">*</span>
                  </label>
                  <input v-model.number="formData.numberOfVariations" type="number" min="1" max="5" class="form-input" :class="{ 'error': showValidation && !formData.numberOfVariations }">
                  <div v-if="showValidation && !formData.numberOfVariations" class="error-message">Please enter number of variations</div>
                </div>

                <!-- Language -->
                <div class="form-field">
                  <label class="field-label">Language</label>
                  <select v-model="formData.language" class="form-input">
                    <option value="vi">Vietnamese</option>
                    <option value="en">English</option>
                  </select>
                </div>

                <!-- Call to Action -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Call to Action</span>
                    <span class="label-required">*</span>
                  </label>
                  <select v-model="formData.callToAction" class="form-input" :class="{ 'error': showValidation && !formData.callToAction }">
                    <option value="">Select Call to Action</option>
                    <option v-for="cta in availableCTAs" :key="cta.value" :value="cta.value">
                      {{ cta.label }}
                    </option>
                  </select>
                  <div v-if="showValidation && !formData.callToAction" class="error-message">Please select a Call to Action</div>
                </div>

                <!-- Website URL (for Website Conversion Ad) -->
                <div v-if="formData.adType === 'WEBSITE_CONVERSION_AD'" class="form-field md:col-span-2">
                  <label class="field-label">
                    <span class="label-text">Website URL</span>
                    <span class="label-required">*</span>
                  </label>
                  <input 
                    v-model="formData.websiteUrl" 
                    type="url" 
                    class="form-input" 
                    :class="{ 'error': showValidation && formData.adType === 'WEBSITE_CONVERSION_AD' && !formData.websiteUrl }" 
                    placeholder="https://your-website.com"
                  >
                  <div v-if="showValidation && formData.adType === 'WEBSITE_CONVERSION_AD' && !formData.websiteUrl" class="error-message">
                    Please enter your website URL
                  </div>
                </div>

                <!-- Lead Form Questions (for Lead Form Ad) -->
                <div v-if="formData.adType === 'LEAD_FORM_AD'" class="form-field md:col-span-2">
                  <label class="field-label">
                    <span class="label-text">Lead Form Questions</span>
                    <span class="label-required">*</span>
                  </label>
                  <div class="lead-form-questions">
                    <div v-for="(question, index) in formData.leadFormQuestions" :key="index" class="question-item mb-3">
                      <div class="flex gap-2">
                        <select v-model="question.type" class="form-input flex-1">
                          <option value="FULL_NAME">Full Name</option>
                          <option value="EMAIL">Email</option>
                          <option value="PHONE">Phone Number</option>
                          <option value="COMPANY">Company</option>
                          <option value="JOB_TITLE">Job Title</option>
                          <option value="CUSTOM">Custom Question</option>
                        </select>
                        <input 
                          v-if="question.type === 'CUSTOM'"
                          v-model="question.customText" 
                          type="text" 
                          class="form-input flex-1" 
                          placeholder="Enter your question"
                        >
                        <button 
                          v-if="formData.leadFormQuestions.length > 1"
                          @click="removeLeadFormQuestion(index)" 
                          type="button" 
                          class="btn btn-sm btn-danger"
                        >
                          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <button @click="addLeadFormQuestion" type="button" class="btn btn-sm btn-secondary">
                      <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                      </svg>
                      Add Question
                    </button>
                  </div>
                  <div v-if="showValidation && formData.adType === 'LEAD_FORM_AD' && formData.leadFormQuestions.length === 0" class="error-message">
                    Please add at least one question to your lead form
                  </div>
                </div>

                <!-- Prompt or Ad Links -->
                <div class="form-field md:col-span-2">
                  <label class="field-label">Ad Content</label>
                  <textarea v-model="formData.prompt" rows="4" class="form-input" placeholder="Describe your ad content or provide a prompt for AI generation"></textarea>
                  <div class="mb-2"></div>
                  <label class="field-label">Or provide Facebook Ad Library links:</label>
                  <div v-for="(link, index) in adLinks" :key="index" class="flex gap-2 mb-2">
                    <input v-model="adLinks[index]" type="url" class="form-input flex-1" placeholder="https://www.facebook.com/ads/library/...">
                    <button v-if="adLinks.length > 1" @click="removeAdLink(index)" type="button" class="btn btn-sm btn-danger">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                      </svg>
                    </button>
                  </div>
                  <button @click="addAdLink" type="button" class="btn btn-sm btn-secondary">
                    <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                    </svg>
                    Add Link
                  </button>
                  <div v-if="promptOrAdLinksError" class="error-message">
                    Please provide either a prompt or at least one ad link
                  </div>
                </div>
              </div>

              <!-- Navigation -->
              <div class="flex justify-end mt-8">
                <button @click="nextStep" class="btn btn-primary">
                  Next Step
                  <svg class="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Step 2: AI Configuration -->
          <div v-if="currentStep === 2" class="card">
            <div class="card-header">
              <h2 class="text-xl font-semibold text-secondary-900">Step 2: AI Configuration</h2>
              <p class="text-secondary-600">Choose your AI providers for content generation</p>
            </div>
            <div class="card-body">
              <div class="ai-provider-grid">
                <!-- Text Provider -->
                <div class="ai-provider-card" :class="{ 'selected': formData.textProvider }">
                  <div>
                    <h3 class="text-lg font-semibold mb-2">Text Generation</h3>
                    <p class="text-secondary-600 mb-4">Choose an AI provider for generating ad text</p>
                  </div>
                  <select v-model="formData.textProvider" class="form-input" :class="{ 'error': showValidation && !formData.textProvider }">
                    <option value="">Select text provider</option>
                    <option v-for="provider in textProviders" :key="provider.id" :value="provider.id">
                      {{ provider.name }} - {{ provider.description }}
                    </option>
                  </select>
                  <div v-if="showValidation && !formData.textProvider" class="error-message">Please select a text provider</div>
                  <div v-if="formData.textProvider" class="mt-2">
                    <div class="text-sm text-secondary-600">
                      <strong>Capabilities:</strong> 
                      <span v-for="capability in getSelectedTextProvider?.capabilities" :key="capability" 
                            class="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded mr-1 mb-1">
                        {{ capability }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Image Provider -->
                <div class="ai-provider-card" :class="{ 'selected': formData.imageProvider, 'disabled': uploadedFile }">
                  <div>
                    <h3 class="text-lg font-semibold mb-2">Image Generation</h3>
                    <p class="text-secondary-600 mb-4">Choose an AI provider for generating images</p>
                  </div>
                  <select 
                    v-model="formData.imageProvider" 
                    class="form-input" 
                    :class="{ 'error': showValidation && !formData.imageProvider && !uploadedFile }"
                    :disabled="uploadedFile"
                  >
                    <option value="">Select image provider</option>
                    <option v-for="provider in imageProviders" :key="provider.id" :value="provider.id">
                      {{ provider.name }} - {{ provider.description }}
                    </option>
                  </select>
                  <div v-if="showValidation && !formData.imageProvider && !uploadedFile" class="error-message">
                    Please either upload a file or select an image provider
                  </div>
                  <div v-if="uploadedFile" class="text-sm text-amber-600 mt-2">
                    Image provider selection is disabled when you upload your own file
                  </div>
                </div>
              </div>

              <!-- File Upload -->
              <div class="mt-8">
                <h3 class="text-lg font-semibold mb-4">Upload Media (Optional)</h3>
                <div class="border-2 border-dashed border-secondary-300 rounded-lg p-6 text-center">
                  <input ref="fileInput" type="file" @change="handleFileUpload" accept="image/*" class="hidden">
                  <div v-if="!uploadedFile" @click="$refs.fileInput.click()" class="cursor-pointer">
                    <svg class="w-12 h-12 text-secondary-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
                    </svg>
                    <p class="text-secondary-600">Click to upload an image or drag and drop</p>
                    <p class="text-sm text-secondary-500 mt-2">PNG, JPG up to 10MB</p>
                  </div>
                  <div v-else class="text-center">
                    <img :src="uploadedFileUrl" alt="Uploaded file" class="w-32 h-32 object-cover mx-auto mb-4 rounded">
                    <p class="text-secondary-600">{{ uploadedFile.name }}</p>
                    <p class="text-sm text-secondary-500">{{ formatFileSize(uploadedFile.size) }}</p>
                    <button @click="removeFile" class="btn btn-sm btn-danger mt-2">Remove</button>
                  </div>
                </div>
              </div>

              <!-- Navigation -->
              <div class="flex justify-between mt-8">
                <button @click="prevStep" class="btn btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  Previous
                </button>
                <button @click="generateAd" :disabled="isGenerating" class="btn btn-primary">
                  <svg v-if="isGenerating" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isGenerating ? 'Generating...' : 'Generate Ad' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Step 3: Preview -->
          <div v-if="currentStep === 3" class="card">
            <div class="card-header">
              <h2 class="text-xl font-semibold text-secondary-900">Step 3: Preview & Save</h2>
              <p class="text-secondary-600">Review your generated ads and save the best one</p>
            </div>
            <div class="card-body">
              <div v-if="adVariations.length === 0" class="text-center py-12">
                <div class="text-secondary-400 mb-4">
                  <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                  </svg>
                </div>
                <h3 class="text-lg font-semibold text-secondary-900 mb-2">No ads generated yet</h3>
                <p class="text-secondary-600">Go back to step 2 and generate your ads</p>
              </div>

              <div v-else class="ad-preview-grid">
                <div v-for="(variation, index) in adVariations" :key="index" 
                     class="ad-preview-card" 
                     :class="{ 'selected': selectedVariation === variation }"
                     @click="selectVariation(variation)">
                  <div class="ad-preview-header">
                    <h3 class="text-lg font-semibold">Variation {{ index + 1 }}</h3>
                    <div class="flex gap-2">
                      <button @click.stop="editVariation(variation)" class="btn btn-sm btn-secondary">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                        </svg>
                      </button>
                    </div>
                  </div>
                  
                  <div class="ad-preview-content">
                    <div v-if="variation.imageUrl || uploadedFileUrl" class="ad-image mb-4">
                      <img :src="variation.imageUrl || uploadedFileUrl" :alt="variation.headline" class="w-full h-48 object-cover rounded">
                    </div>
                    
                    <div class="ad-text">
                      <h4 class="font-semibold text-lg mb-2">{{ variation.headline }}</h4>
                      <p class="text-secondary-600 mb-3">{{ variation.body }}</p>
                      <div class="flex items-center justify-between">
                        <span class="text-sm text-secondary-500">{{ variation.callToAction }}</span>
                        <span v-if="selectedVariation === variation" class="text-primary">
                          <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path>
                          </svg>
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Navigation -->
              <div class="flex justify-between mt-8">
                <button @click="prevStep" class="btn btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  Previous
                </button>
                <button @click="saveAd" :disabled="!selectedVariation || isSaving" class="btn btn-primary">
                  <svg v-if="isSaving" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {{ isSaving ? 'Saving...' : 'Save Ad' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Modal -->
    <Dialog v-model:visible="showEditModal" header="Edit Ad Variation" :modal="true" :closable="true" class="w-full max-w-2xl">
      <div v-if="editingVariation" class="space-y-4">
        <div class="form-group">
          <label class="form-label">Headline</label>
          <input v-model="editingVariation.headline" type="text" class="form-input">
        </div>
        <div class="form-group">
          <label class="form-label">Body</label>
          <textarea v-model="editingVariation.body" rows="4" class="form-textarea"></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">Call to Action</label>
          <select v-model="editingVariation.callToAction" class="form-select">
            <option v-for="cta in standardCTAs" :key="cta.value" :value="cta.value">
              {{ cta.label }}
            </option>
          </select>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button @click="cancelEdit" class="btn btn-secondary">Cancel</button>
          <button @click="saveEdit" class="btn btn-primary">Save Changes</button>
        </div>
      </template>
    </Dialog>

    <!-- Confirm Save Modal -->
    <Dialog v-model:visible="showConfirmModal" header="Confirm Save" :modal="true" :closable="true">
      <p>Are you sure you want to save this ad?</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button @click="showConfirmModal = false" class="btn btn-secondary">Cancel</button>
          <button @click="confirmSave" :disabled="isSaving" class="btn btn-primary">
            {{ isSaving ? 'Saving...' : 'Save Ad' }}
          </button>
        </div>
      </template>
    </Dialog>

    <!-- Extract Error Dialog -->
    <Dialog v-model:visible="showExtractErrorDialog" header="Extraction Failed" :modal="true" :closable="true">
      <p>Failed to extract content from the provided ad links. Would you like to continue without extracted content?</p>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button @click="showExtractErrorDialog = false" class="btn btn-secondary">Go Back</button>
          <button @click="generateAdContinueWithoutExtract" class="btn btn-primary">Continue</button>
        </div>
      </template>
    </Dialog>

    <!-- Media Modal -->
    <Dialog v-model:visible="showMediaModal" header="Media Preview" :modal="true" :closable="true" class="w-full max-w-4xl">
      <div class="text-center">
        <img :src="selectedMediaUrl" alt="Media preview" class="max-w-full max-h-96 mx-auto">
      </div>
    </Dialog>

    <!-- Ad Type Help Dialog -->
    <Dialog v-model:visible="showAdTypeHelp" header="Loại quảng cáo Facebook" :modal="true" :closable="true" class="help-dialog">
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">3 Loại quảng cáo chính</h3>
          <div class="ad-types-help">
            <div class="ad-type-help-item">
              <h4 class="ad-type-title">📱 Page Post Ad</h4>
              <p class="ad-type-desc">Quảng cáo hiển thị như bài đăng trên trang Facebook. Phù hợp cho:</p>
              <ul class="ad-type-features">
                <li>Brand awareness và engagement</li>
                <li>Tương tác với cộng đồng</li>
                <li>Chia sẻ thông tin, tin tức</li>
                <li>Giới thiệu sản phẩm/dịch vụ</li>
              </ul>
            </div>
            
            <div class="ad-type-help-item">
              <h4 class="ad-type-title">🌐 Website Conversion Ad</h4>
              <p class="ad-type-desc">Quảng cáo dẫn người dùng đến website để thực hiện hành động. Phù hợp cho:</p>
              <ul class="ad-type-features">
                <li>Bán hàng online</li>
                <li>Đăng ký dịch vụ</li>
                <li>Tải ứng dụng</li>
                <li>Thu thập leads qua website</li>
              </ul>
            </div>
            
            <div class="ad-type-help-item">
              <h4 class="ad-type-title">📋 Lead Form Ad</h4>
              <p class="ad-type-desc">Quảng cáo thu thập thông tin khách hàng trực tiếp trên Facebook. Phù hợp cho:</p>
              <ul class="ad-type-features">
                <li>Thu thập thông tin liên hệ</li>
                <li>Đăng ký nhận báo giá</li>
                <li>Đăng ký webinar/event</li>
                <li>Khảo sát, nghiên cứu thị trường</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </Dialog>

    <!-- Help Dialog -->
    <Dialog v-model:visible="showHelp" header="Hướng dẫn tạo quảng cáo" :modal="true" :closable="true" class="help-dialog">
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">Bắt đầu</h3>
          <p class="help-text">
            Trình hướng dẫn này sẽ giúp bạn tạo quảng cáo Facebook chuyên nghiệp với AI qua 3 bước đơn giản.
          </p>
        </div>
        <div class="help-section">
          <h3 class="help-title">Các bước thực hiện</h3>
          <div class="help-steps">
            <div class="help-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>Thông tin cơ bản</h4>
                <p>Chọn chiến dịch, loại quảng cáo, đặt tên cho quảng cáo.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>Tạo nội dung</h4>
                <p>Nhập mô tả quảng cáo hoặc cung cấp link mẫu từ Facebook Ad Library.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>Cấu hình AI</h4>
                <p>Chọn AI tạo text và hình ảnh cho quảng cáo.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">4</div>
              <div class="step-content">
                <h4>Xem trước & Lưu</h4>
                <p>Xem trước các phiên bản quảng cáo và lưu lại phiên bản tốt nhất.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/services/api'
import '@/assets/styles/adcreate.css'
import Dialog from 'primevue/dialog'

import AppSidebar from '@/components/AppSidebar.vue'

export default {
  name: 'AdCreate',
  components: { 
    AppSidebar,
    Dialog
  },
  data() {
    return {
      sidebarOpen: true,
      currentStep: 1,
      showValidation: false,
      isGenerating: false,
      isSaving: false,
      showEditModal: false,
      showConfirmModal: false,
      showHelp: false,
      
      formData: {
        campaignId: null,
        adType: 'PAGE_POST_AD',
        name: '',
        prompt: '',
        language: 'vi',
        textProvider: '',
        imageProvider: '',
        numberOfVariations: 1,
        callToAction: '', // Thêm callToAction vào formData
        websiteUrl: '', // For Website Conversion Ad
        leadFormQuestions: [{ type: 'FULL_NAME' }] // For Lead Form Ad
      },
      
      uploadedFile: null,
      uploadedFileUrl: null,
      
      steps: [
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
      ],
      
      adTypes: [
        {
          value: 'PAGE_POST_AD',
          label: 'Page Post Ad',
          description: 'Quảng cáo hiển thị như bài đăng trên trang Facebook. Phù hợp cho brand awareness, engagement và tương tác với cộng đồng.',
          fields: ['headline', 'description', 'primaryText', 'image'],
          ctaOptions: ['LEARN_MORE', 'SHOP_NOW', 'SIGN_UP', 'CONTACT_US']
        },
        {
          value: 'WEBSITE_CONVERSION_AD',
          label: 'Website Conversion Ad',
          description: 'Quảng cáo dẫn người dùng đến website để thực hiện hành động cụ thể như mua hàng, đăng ký, tải app.',
          fields: ['headline', 'description', 'primaryText', 'image', 'websiteUrl'],
          ctaOptions: ['SHOP_NOW', 'LEARN_MORE', 'SIGN_UP', 'DOWNLOAD', 'GET_QUOTE']
        },
        {
          value: 'LEAD_FORM_AD',
          label: 'Lead Form Ad',
          description: 'Quảng cáo thu thập thông tin khách hàng tiềm năng trực tiếp trên Facebook, không cần chuyển đến website.',
          fields: ['headline', 'description', 'primaryText', 'image', 'leadFormQuestions'],
          ctaOptions: ['SIGN_UP', 'GET_QUOTE', 'CONTACT_US', 'APPLY_NOW']
        }
      ],
      
      campaigns: [],
      textProviders: [],
      imageProviders: [],
      adVariations: [],
      selectedVariation: null,
      editingVariation: null,
      adId: null,
      promptTemplates: [],
      customPromptAddition: '',
      saveCustomPrompt: false,
      savedPrompts: [],
      promptOrAdLinksError: false,
      adLinks: [''],
      selectedPromptTemplate: '',
      showExtractErrorDialog: false, // Thêm biến trạng thái cho dialog lỗi extract
      failedExtractLinkIndex: 0, // Lưu index ad link lỗi để focus
      promptTemplateOptions: [
        { label: 'Dynamic & Energetic', value: 'dynamic' },
        { label: 'Creative & Innovative', value: 'creative' },
        { label: 'Professional & Serious', value: 'professional' },
        { label: 'Friendly & Approachable', value: 'friendly' },
        { label: 'Urgent & Action-oriented', value: 'urgent' },
        { label: 'Luxury & Premium', value: 'luxury' },
        { label: 'Educational & Informative', value: 'educational' },
      ],
      showMediaModal: false,
      selectedMediaUrl: '',
      placeholderImageUrl: '/img/placeholder.png',
      showAdTypeHelp: false,
      // Thêm danh sách CTA chuẩn của Facebook
      standardCTAs: [],

    }
  },
  
  computed: {
    ...mapState('auth', ['user']),
    
    userInitials() {
      if (!this.user?.name) return 'U'
      return this.user.name.split(' ').map(n => n[0]).join('').toUpperCase()
    },
    
    userName() {
      return this.user?.name || 'User'
    },
    
    userEmail() {
      return this.user?.email || ''
    },
    mainContentStyle() {
      return this.sidebarOpen ? { marginLeft: '240px' } : { marginLeft: '0' }
    },
    
    getSelectedTextProvider() {
      return this.textProviders.find(p => p.id === this.formData.textProvider)
    },
    
    selectedAdType() {
      return this.adTypes.find(type => type.value === this.formData.adType)
    },
    
    availableCTAs() {
      if (!this.selectedAdType) return this.standardCTAs
      return this.standardCTAs.filter(cta => 
        this.selectedAdType.ctaOptions.includes(cta.value)
      )
    },
    
    getSelectedImageProvider() {
      return this.imageProviders.find(p => p.id === this.formData.imageProvider)
    }
  },
  
  async mounted() {
    await this.loadData()
    await this.loadCallToActions()
    this.loadSavedPrompts()
    // Initialize adLinks with at least one empty item
    if (this.adLinks.length === 0) {
      this.adLinks = ['']
    }
  },
  
  watch: {
    'formData.language': {
      handler(newLanguage) {
        if (newLanguage) {
          this.loadCallToActions()
        }
      },
      immediate: false
    }
  },
  
  methods: {
    ...mapActions('auth', ['logout']),
    ...mapActions('toast', ['showToast']),
    
    async loadData() {
      try {
        // Load campaigns
        const campaignsResponse = await api.campaigns.getAll(0, 100); // Get more campaigns to ensure we find them
        this.campaigns = campaignsResponse.data.content || campaignsResponse.data || []
        
        // Load AI providers
        const [textResponse, imageResponse] = await Promise.all([
          api.providers.getTextProviders(),
          api.providers.getImageProviders()
        ])
        
        this.textProviders = textResponse.data
        this.imageProviders = imageResponse.data
        
      } catch (error) {
        console.error('Error loading data:', error)
        this.showToast({
          type: 'error',
          message: 'Unable to load data. Please try again.'
        })
      }
    },
    
    async loadCallToActions() {
      try {
        const language = this.formData.language || 'en'
        const response = await api.providers.getCallToActions(language)
        this.standardCTAs = response.data
      } catch (error) {
        console.error('Failed to load call to actions:', error)
        // Fallback to default CTAs if API fails
        this.standardCTAs = [
          { value: 'SHOP_NOW', label: 'Shop Now' },
          { value: 'LEARN_MORE', label: 'Learn More' },
          { value: 'SIGN_UP', label: 'Sign Up' },
          { value: 'DOWNLOAD', label: 'Download' },
          { value: 'CONTACT_US', label: 'Contact Us' },
          { value: 'APPLY_NOW', label: 'Apply Now' },
          { value: 'BOOK_NOW', label: 'Book Now' },
          { value: 'GET_OFFER', label: 'Get Offer' },
          { value: 'MESSAGE_PAGE', label: 'Message Page' },
          { value: 'SUBSCRIBE', label: 'Subscribe' }
        ]
      }
    },
    
    nextStep() {
      console.log('[DEBUG] nextStep called, currentStep:', this.currentStep);
      if (this.currentStep === 1) {
        const valid = this.validateStep1();
        console.log('[DEBUG] validateStep1 result:', valid);
        if (!valid) {
          this.showValidation = true
          return
        }
      }
      if (this.currentStep < 3) {
        this.currentStep++
        this.showValidation = false
      }
    },
    
    prevStep() {
      if (this.currentStep > 1) {
        this.currentStep--
        this.showValidation = false
      }
    },
    
    onAdTypeChange() {
      // Reset ad-specific fields when ad type changes
      if (this.formData.adType !== 'WEBSITE_CONVERSION_AD') {
        this.formData.websiteUrl = ''
      }
      if (this.formData.adType !== 'LEAD_FORM_AD') {
        this.formData.leadFormQuestions = [{ type: 'FULL_NAME' }]
      }
      // Reset CTA to empty when ad type changes
      this.formData.callToAction = ''
    },
    
    addLeadFormQuestion() {
      this.formData.leadFormQuestions.push({ type: 'EMAIL' })
    },
    
    removeLeadFormQuestion(index) {
      if (this.formData.leadFormQuestions.length > 1) {
        this.formData.leadFormQuestions.splice(index, 1)
      }
    },
    
    validateStep1() {
      const hasPrompt = !!this.formData.prompt && this.formData.prompt.trim() !== ''
      const hasAdLinks = Array.isArray(this.adLinks) && this.adLinks.some(link => link && link.trim() !== '')
      const basicValid = this.formData.campaignId && 
                         this.formData.adType && 
                         this.formData.name && 
                         this.formData.numberOfVariations &&
                         this.formData.callToAction
      
      // Validate ad-specific fields
      let adSpecificValid = true
      if (this.formData.adType === 'WEBSITE_CONVERSION_AD' && !this.formData.websiteUrl) {
        adSpecificValid = false
      }
      if (this.formData.adType === 'LEAD_FORM_AD' && this.formData.leadFormQuestions.length === 0) {
        adSpecificValid = false
      }
      
      this.promptOrAdLinksError = false
      if (!hasPrompt && !hasAdLinks) {
        this.promptOrAdLinksError = true
      }
      
      console.log('[DEBUG] validateStep1 fields:', {
        campaignId: this.formData.campaignId,
        adType: this.formData.adType,
        name: this.formData.name,
        numberOfVariations: this.formData.numberOfVariations,
        callToAction: this.formData.callToAction,
        websiteUrl: this.formData.websiteUrl,
        leadFormQuestions: this.formData.leadFormQuestions,
        prompt: this.formData.prompt,
        adLinks: this.adLinks,
        hasPrompt,
        hasAdLinks,
        basicValid,
        adSpecificValid,
        promptOrAdLinksError: this.promptOrAdLinksError
      });
      
      return basicValid && (hasPrompt || hasAdLinks)
    },
    
    validateStep2() {
      if (this.uploadedFile) {
        return this.formData.textProvider
      }
      return this.formData.textProvider && this.formData.imageProvider
    },
    
    async handleFileUpload(event) {
      const file = event.target.files[0]
      if (!file) return
      
      // Validate file size (10MB)
      if (file.size > 10 * 1024 * 1024) {
        this.showToast({
          type: 'error',
          message: 'File too large. Please select a file smaller than 10MB.'
        })
        return
      }
      
      try {
        const formData = new FormData()
        formData.append('file', file)
        
        const response = await api.post('/upload/media', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data.success) {
          this.uploadedFile = file
          this.uploadedFileUrl = response.data.fileUrl
          // Clear image provider when file is uploaded
          this.formData.imageProvider = ''
          this.showToast({
            type: 'success',
            message: 'File uploaded successfully!'
          })
        }
      } catch (error) {
        console.error('Error uploading file:', error)
        this.showToast({
          type: 'error',
          message: 'Unable to upload file. Please try again.'
        })
      }
    },
    
    removeFile() {
      this.uploadedFile = null
      this.uploadedFileUrl = ''
      this.$refs.fileInput.value = ''
      // Re-enable image provider selection when file is removed
      this.formData.imageProvider = ''
    },
    
    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes'
      const k = 1024
      const sizes = ['Bytes', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    
    async generateAd() {
      if (!this.validateStep2()) {
        this.showValidation = true
        return
      }
      this.isGenerating = true
      try {
        const validLinks = this.adLinks.filter(link => link.trim())
        let extractedContent = null
        let extractFailed = false
        if (validLinks.length > 0) {
          try {
            const extractionResponse = await api.ads.extractFromLibrary({
              adLinks: validLinks,
              promptStyle: this.selectedPromptTemplate || 'Dynamic',
              customPrompt: this.customPromptAddition
            })
            // Thêm log debug dữ liệu trả về
            console.log('extractionResponse.data', extractionResponse.data)
            if (Array.isArray(extractionResponse.data)) {
              extractionResponse.data.forEach((item, idx) => {
                console.log(`item[${idx}] body:`, item.body, 'snapshot.body:', item.snapshot?.body)
              })
            } else {
              console.log('body:', extractionResponse.data.body, 'snapshot.body:', extractionResponse.data.snapshot?.body)
            }
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
        // Nếu có ad link và extract thất bại hoặc không có nội dung, hiện dialog xác nhận
        if (validLinks.length > 0 && extractFailed) {
          this.showExtractErrorDialog = true
          this.isGenerating = false
          return
        }

        // Tạo preview quảng cáo (không lưu vào database)
        // Bổ sung hướng dẫn CTA vào prompt
        let promptWithCTA = this.formData.prompt || '';
        if (promptWithCTA.indexOf('Call to Action') === -1) {
          const ctaList = this.standardCTAs.map(cta => cta.label.split(' - ')[0]).join(', ');
          promptWithCTA += `\n\nLưu ý: Chỉ sử dụng một trong các Call to Action sau cho quảng cáo: ${ctaList}. Không tạo CTA khác.`;
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
          extractedContent: extractedContent, // Add extracted content if available
          isPreview: true // Flag để backend biết đây là preview
        }
        
        // Gọi API để tạo preview (không lưu database)
        const response = await api.post('/ads/generate', requestData)
        
        if (response.data.status === 'success') {
          console.log('Response variations:', response.data.variations)
          console.log('Uploaded file URL:', this.uploadedFileUrl)
          
          this.adVariations = response.data.variations.map(v => {
            const imageUrl = v.imageUrl || this.uploadedFileUrl || v.mediaFileUrl || ''
            console.log('Variation imageUrl:', imageUrl, 'for variation:', v.headline)
            return {
              ...v,
              imageUrl: imageUrl
            }
          })
          
          console.log('Final adVariations:', this.adVariations)
          this.adId = response.data.adId // Lưu adId tạm thời cho việc save sau
          this.currentStep = 3
          this.showToast({
            type: 'success',
            message: 'Ad created successfully! Please preview and save.'
          })
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error generating ad:', error)
        const errorMessage = error.response?.data?.message || 'Could not create ad. Please try again.'
        
        this.showToast({
          type: 'error',
          message: errorMessage
        })
        
        // If it's a validation error (no prompt and no ad link content), go back to step 1
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
        this.showToast({
          type: 'warning',
          message: 'Vui lòng chọn một quảng cáo để lưu'
        })
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
    
    getPromptTemplate(style) {
      const templates = {
        dynamic: "Create an energetic and dynamic ad that captures attention with bold language, exciting benefits, and a strong call-to-action. Use power words and create urgency.",
        creative: "Develop a creative and innovative ad that stands out with unique angles, storytelling elements, and imaginative approaches. Think outside the box.",
        professional: "Write a professional and serious ad that builds trust through credibility, expertise, and clear value propositions. Use formal tone and industry authority.",
        friendly: "Create a friendly and approachable ad that connects personally with the audience using conversational tone, relatability, and warm messaging.",
        urgent: "Generate an urgent and action-oriented ad that drives immediate response with time-sensitive offers, scarcity, and compelling reasons to act now.",
        luxury: "Craft a luxury and premium ad that emphasizes exclusivity, quality, sophistication, and prestige. Use elegant language and highlight premium benefits.",
        educational: "Develop an educational and informative ad that teaches while selling, providing valuable insights, tips, or knowledge that positions your brand as an expert."
      }
      return templates[style] || ''
    },
    
    useCustomPrompt(prompt) {
      this.formData.prompt = prompt.content
      this.customPromptAddition = ''
      this.selectedPromptTemplate = ''
    },
    
    deleteCustomPrompt(index) {
      this.savedPrompts.splice(index, 1)
      this.saveSavedPrompts()
    },
    
    saveSavedPrompts() {
      localStorage.setItem('savedPrompts', JSON.stringify(this.savedPrompts))
    },
    
    loadSavedPrompts() {
      const saved = localStorage.getItem('savedPrompts')
      if (saved) {
        this.savedPrompts = JSON.parse(saved)
      }
    },
    
    async confirmSave() {
      this.showConfirmModal = false
      this.isSaving = true
      
      try {
        // Lưu quảng cáo với nội dung đã được chọn (không tạo mới)
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
          selectedVariation: this.selectedVariation, // Gửi variation được chọn
          isPreview: false, // Flag để backend biết đây là save thực sự
          saveExistingContent: true // Flag để backend biết chỉ lưu nội dung hiện tại
        }
        
        // Gọi API để lưu quảng cáo với nội dung đã có
        const response = await api.ads.saveExisting(requestData)
        
        if (response.data.status === 'success') {
          this.showToast({
            type: 'success',
            message: 'Ad saved successfully!'
          })
          await this.$store.dispatch('dashboard/fetchDashboardData', null, { root: true })
          this.$router.push('/dashboard')
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        console.error('Error saving ad:', error)
        this.showToast({
          type: 'error',
          message: error.response?.data?.message || 'Could not save ad. Please try again.'
        })
      } finally {
        this.isSaving = false
      }
    },
    

    
    async extractFromLibrary() {
      if (!this.adLinks.length || !this.adLinks[0].trim()) {
        this.showToast({
          type: 'warning',
          message: 'Vui lòng nhập ít nhất một link quảng cáo'
        })
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
          this.showToast({
            type: 'success',
            message: 'Đã trích xuất nội dung thành công!'
          })
        } else {
          this.showToast({
            type: 'warning',
            message: response.data.message || 'Không thể trích xuất nội dung'
          })
        }
      } catch (error) {
        console.error('Error extracting from library:', error)
        this.showToast({
          type: 'error',
          message: 'Lỗi khi trích xuất nội dung: ' + (error.response?.data?.message || error.message)
        })
      } finally {
        this.extracting = false
      }
    },
    

    // Thêm 2 method xử lý dialog xác nhận
    onExtractErrorYes() {
      this.showExtractErrorDialog = false
      // Tiếp tục sang bước 3 (preview)
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
    // Hàm tiếp tục generateAd bỏ qua extract
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
          extractedContent: null, // Không có nội dung extract
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
          this.showToast({
            type: 'success',
            message: 'Ad created successfully! Please preview and save.'
          })
        } else {
          throw new Error(response.data.message)
        }
      } catch (error) {
        const errorMessage = error.response?.data?.message || 'Could not create ad. Please try again.'
        this.showToast({
          type: 'error',
          message: errorMessage
        })
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
      );
    },
    openMediaModal(url) {
      this.selectedMediaUrl = url;
      this.showMediaModal = true;
    },
    closeMediaModal() {
      this.showMediaModal = false;
      this.selectedMediaUrl = '';
    },
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    handleLogout() {
      this.$store.dispatch('auth/logout')
    }
  }
}
</script>

<style lang="scss" scoped>
/* Enhanced AdCreate Styles */

/* Step 2 - AI Provider Selection */
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
  border-color: #8b5cf6;
  box-shadow: 0 10px 25px rgba(139, 92, 246, 0.1);
  transform: translateY(-2px);
}

.ai-provider-card.selected {
  border-color: #8b5cf6;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  box-shadow: 0 10px 25px rgba(139, 92, 246, 0.2);
  transform: scale(1.02);
}

/* Step 3 - Ad Preview */
.ad-preview-container {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
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
  border-color: #10b981;
  box-shadow: 0 20px 40px rgba(16, 185, 129, 0.1);
  transform: translateY(-4px);
}

.ad-preview-card.selected {
  border-color: #10b981;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  box-shadow: 0 20px 40px rgba(16, 185, 129, 0.2);
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
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 600;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.2);
}

.ad-preview-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 12px rgba(59, 130, 246, 0.3);
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
}

@media (min-width: 1024px) {
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

/* Enhanced Card Styles */
.enhanced-card {
  border: none;
  border-radius: 1.5rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  overflow: hidden;
}

.enhanced-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.enhanced-card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 1.5rem 1.5rem 0 0;
}

.enhanced-card-body {
  padding: 2rem;
  background: white;
}

/* Form Enhancements */
.enhanced-form-group {
  margin-bottom: 2rem;
}

.enhanced-form-label {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.enhanced-form-input {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.75rem;
  font-size: 1rem;
  transition: all 0.3s ease;
  background: white;
}

.enhanced-form-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  outline: none;
}

/* Help Dialog Styles */
.help-dialog {
  max-width: 700px !important;
}

.help-dialog .p-dialog-header {
  font-size: 1.5rem !important;
  font-weight: 600 !important;
  padding: 1.5rem !important;
}

.help-dialog .p-dialog-content {
  font-size: 1rem !important;
  line-height: 1.6 !important;
  padding: 1.5rem !important;
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
  background: #3b82f6;
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

/* Ad Type Help Styles */
.help-btn {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.375rem;
  transition: all 0.2s;
}

.help-btn:hover {
  background: #f3f4f6;
  color: #374151;
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

.ad-type-features li:before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #10b981;
  font-weight: bold;
}

/* Lead Form Questions Styles */
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
}

/* Disabled state for AI provider cards */
.ai-provider-card.disabled {
  opacity: 0.6;
  pointer-events: none;
}

.ai-provider-card.disabled .form-input {
  background-color: #f3f4f6;
  cursor: not-allowed;
}

.enhanced-form-textarea {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.75rem;
  font-size: 1rem;
  transition: all 0.3s ease;
  background: white;
  resize: vertical;
  min-height: 120px;
}

.enhanced-form-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  outline: none;
}

/* Button Enhancements */
.enhanced-btn {
  padding: 1rem 2rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 1rem;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.enhanced-btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.2);
}

.enhanced-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 12px rgba(59, 130, 246, 0.3);
}

.enhanced-btn-secondary {
  background: white;
  color: #374151;
  border: 2px solid #e5e7eb;
}

.enhanced-btn-secondary:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.enhanced-btn-success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 6px rgba(16, 185, 129, 0.2);
}

.enhanced-btn-success:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 12px rgba(16, 185, 129, 0.3);
}

/* Progress Indicator Styles */
.wizard-progress {
  margin-bottom: 2rem;
}

.progress-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 800px;
  margin: 0 auto;
}

.progress-step {
  display: flex;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-indicator {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.3s ease;
  z-index: 2;
}

.step-number {
  color: #6b7280;
  background: #f3f4f6;
  border: 2px solid #e5e7eb;
}

.step-check {
  width: 20px;
  height: 20px;
  color: white;
}

.progress-step.active .step-indicator {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
}

.progress-step.completed .step-indicator {
  background: #10b981;
  border-color: #10b981;
  color: white;
}

.progress-step.upcoming .step-indicator {
  background: #f3f4f6;
  border-color: #e5e7eb;
  color: #6b7280;
}

.step-content {
  margin-left: 1rem;
  flex: 1;
}

.step-title {
  font-weight: 600;
  font-size: 0.875rem;
  color: #374151;
  margin-bottom: 0.25rem;
}

.step-description {
  font-size: 0.75rem;
  color: #6b7280;
  line-height: 1.4;
}

.progress-step.active .step-title {
  color: #3b82f6;
}

.progress-step.completed .step-title {
  color: #10b981;
}

.progress-step.upcoming .step-title {
  color: #6b7280;
}

.step-connector {
  flex: 1;
  height: 2px;
  background: #e5e7eb;
  margin: 0 1rem;
  position: relative;
  z-index: 1;
}

.progress-step.completed .step-connector {
  background: #10b981;
}

.progress-step.active .step-connector {
  background: linear-gradient(to right, #10b981 50%, #e5e7eb 50%);
}
</style>