<template>
  <div class="ad-detail-container max-w-4xl mx-auto py-8 px-4">
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 gap-4">
      <div>
        <h1 class="text-3xl font-bold text-secondary-900 mb-2">{{ ad.headline }}</h1>
        <span :class="getStatusClass(ad.status)">{{ ad.status }}</span>
      </div>
      <div class="flex gap-2">
        <Button 
          label="Publish to Facebook" 
          icon="pi pi-send" 
          @click="publishAd" 
          :disabled="!canPublish || publishing"
          :loading="publishing"
          v-if="ad.status !== 'ACTIVE'"
          class="btn btn-primary btn-lg flex items-center gap-2"
        />
      </div>
    </div>
    <Card class="rounded-2xl shadow-lg border-0 mb-8">
      <template #content>
        <div v-if="loading" class="flex flex-col items-center justify-center py-12">
          <ProgressSpinner />
          <p class="mt-4 text-secondary-600">Loading ad details...</p>
        </div>
        <div v-else-if="error" class="alert alert-error mb-6">
          <p class="error-message">{{ error }}</p>
          <Button label="Retry" icon="pi pi-refresh" @click="fetchAd" class="btn btn-secondary mt-3" />
        </div>
        <div v-else>
          <div class="ad-preview-container flex flex-col md:flex-row gap-8">
            <div class="ad-preview flex-1 bg-gradient-to-br from-gray-50 to-gray-100 rounded-xl p-6 shadow">
              <div class="ad-preview-header flex items-center justify-between mb-4">
                <div class="page-info flex items-center gap-3">
                  <div class="page-avatar w-10 h-10 bg-gray-200 rounded-full"></div>
                  <div class="page-name font-semibold">Your Page Name</div>
                </div>
                <div class="post-time text-xs text-secondary-500">Just now · <i class="pi pi-globe"></i></div>
              </div>
              <div class="ad-preview-content">
                <p class="primary-text text-lg text-secondary-900 mb-4">{{ ad.primaryText }}</p>
                <div class="ad-image mb-4" v-if="ad.imageUrl">
                  <div class="w-[192px] h-[128px] bg-neutral-100 rounded-lg overflow-hidden border border-gray-200 shadow-sm cursor-pointer flex items-center justify-center" @click="showMediaModal = true">
                    <img :src="ad.imageUrl" alt="Ad Image" class="max-w-full max-h-full object-contain" />
                  </div>
                </div>
                <div class="ad-headline-description mb-4">
                  <h3 class="font-semibold text-xl mb-1">{{ ad.headline }}</h3>
                  <p class="text-secondary-700">{{ ad.description }}</p>
                </div>
                <div class="ad-cta">
                  <Button :label="formatCTA(ad.cta)" class="btn btn-success btn-sm" />
                </div>
              </div>
            </div>
            <div class="ad-details flex-1">
              <h3 class="font-semibold text-lg mb-4">Ad Details</h3>
              <div class="details-grid grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div class="detail-item">
                  <span class="label text-secondary-600">Type:</span>
                  <span class="value text-secondary-900 font-semibold">{{ ad.adType }}</span>
                </div>
                <div class="detail-item">
                  <span class="label text-secondary-600">Status:</span>
                  <span class="value font-semibold" :class="getStatusClass(ad.status)">{{ ad.status }}</span>
                </div>
                <div class="detail-item">
                  <span class="label text-secondary-600">Call to Action:</span>
                  <span class="value text-secondary-900 font-semibold">{{ formatCTA(ad.cta) }}</span>
                </div>
                <div class="detail-item">
                  <span class="label text-secondary-600">Created:</span>
                  <span class="value text-secondary-900 font-semibold">{{ formatDate(ad.createdDate) }}</span>
                </div>
                <div class="detail-item" v-if="ad.fbAdId">
                  <span class="label text-secondary-600">Facebook Ad ID:</span>
                  <span class="value text-secondary-900 font-semibold">{{ ad.fbAdId }}</span>
                </div>
              </div>
            </div>
          </div>
          <Divider class="my-8" />
          <div class="ad-content-section">
            <h3 class="font-semibold text-lg mb-4">Generated Content</h3>
            <div v-if="adContents.length === 0" class="empty-state text-center py-8">
              <p class="text-secondary-600 mb-4">No AI-generated content yet</p>
              <div class="ai-prompt-container flex flex-col sm:flex-row gap-4 items-center justify-center">
                <Textarea 
                  v-model="aiPrompt" 
                  rows="3" 
                  placeholder="Describe what you want to advertise..."
                  class="form-input w-full max-w-md"
                />
                <div class="ai-provider-selection flex gap-4">
                  <div v-for="provider in textProviders" :key="provider.id" class="p-field-radiobutton flex items-center gap-2">
                    <RadioButton :id="provider.id" :value="provider.id" v-model="textProvider" />
                    <label :for="provider.id" class="text-secondary-700">{{ provider.name }}</label>
                  </div>
                </div>
                <Button label="Generate" icon="pi pi-magic" @click="generateContent" class="btn btn-primary btn-lg" :disabled="!aiPrompt || !textProvider" />
              </div>
            </div>
            <div v-else class="generated-content-list grid grid-cols-1 md:grid-cols-2 gap-6 mt-6">
              <div v-for="content in adContents" :key="content.id" class="generated-content-card card p-6 rounded-xl shadow border-0 bg-gradient-to-br from-white to-gray-50">
                <h4 class="font-semibold text-lg mb-2">{{ content.title }}</h4>
                <p class="text-secondary-700 mb-2">{{ content.body }}</p>
                <div class="flex gap-2 mt-4">
                  <Button label="Use This" class="btn btn-success btn-sm" @click="useGeneratedContent(content)" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import moment from 'moment'
import api from '@/services/api'

export default {
  name: 'AdDetail',
  props: {
    campaignId: {
      type: String,
      required: true
    },
    adId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      aiPrompt: '',
      textProvider: 'openai',
      publishing: false,
      textProviders: [],
      showMediaModal: false,
      selectedMediaUrl: '',
      standardCTAs: []
    }
  },
  async mounted() {
    await this.loadCallToActions()
  },
  computed: {
    ...mapGetters('ad', ['currentAd', 'adContents', 'loading', 'error', 'generatingContent']),
    ad() {
      const a = this.currentAd || {}
      return { ...a, imageUrl: a.imageUrl || a.mediaFileUrl || '' }
    },
    canPublish() {
      return this.ad.status === 'DRAFT' && this.adContents.some(content => content.isSelected)
    }
  },
  methods: {
    ...mapActions('ad', ['fetchAd', 'generateContent', 'selectContent', 'publishAd']),
    ...mapActions('toast', ['showSuccess', 'showError']),
    
    async loadTextProviders() {
      try {
        const response = await api.providers.getTextProviders()
        this.textProviders = response.data
        if (this.textProviders.length > 0 && !this.textProvider) {
          this.textProvider = this.textProviders[0].id
        }
      } catch (error) {
        console.error('Error loading text providers:', error)
      }
    },
    
    async loadCallToActions() {
      try {
        const response = await api.providers.getCallToActions('en') // Default to English
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
    
    fetchAd() {
      this.fetchAd({
        campaignId: this.campaignId,
        adId: this.adId
      })
    },
    
    async generateContent() {
      if (!this.aiPrompt) return
      
      try {
        await this.generateContent({
          campaignId: this.campaignId,
          adId: this.adId,
          prompt: this.aiPrompt,
          provider: this.textProvider
        })
        this.aiPrompt = ''
      } catch (error) {
        this.showError({
          title: 'Error',
          message: `Failed to generate content: ${error.message}`
        })
      }
    },
    
    async selectContent(content) {
      try {
        await this.selectContent({
          campaignId: this.campaignId,
          adId: this.adId,
          contentId: content.id
        })
        this.showSuccess({
          title: 'Success',
          message: 'Content selected successfully'
        })
      } catch (error) {
        this.showError({
          title: 'Error',
          message: `Failed to select content: ${error.message}`
        })
      }
    },
    
    async publishAd() {
      this.publishing = true
      
      try {
        await this.publishAd({
          campaignId: this.campaignId,
          adId: this.adId
        })
        this.showSuccess({
          title: 'Success',
          message: 'Ad published successfully to Facebook'
        })
      } catch (error) {
        this.showError({
          title: 'Error',
          message: `Failed to publish ad: ${error.message}`
        })
      } finally {
        this.publishing = false
      }
    },
    
    formatDate(dateString) {
      return moment(dateString).format('MMM D, YYYY')
    },
    
    formatCTA(cta) {
      if (!cta) return '';
      // Try to find in standardCTAs first
      const found = this.standardCTAs.find(item => item.value === cta || item.value === (cta || '').toUpperCase());
      if (found) {
        return found.label;
      }
      // If not found, return the CTA value as is
      return cta;
    },
    
    getStatusClass(status) {
      const statusClasses = {
        'DRAFT': 'status-draft',
        'PENDING': 'status-pending',
        'ACTIVE': 'status-active',
        'PAUSED': 'status-paused',
        'COMPLETED': 'status-completed',
        'FAILED': 'status-failed',
        'REJECTED': 'status-rejected'
      }
      return `status-badge ${statusClasses[status] || ''}`
    },

    openMediaModal(url) {
      this.selectedMediaUrl = url;
      this.showMediaModal = true;
    },

    closeMediaModal() {
      this.showMediaModal = false;
      this.selectedMediaUrl = '';
    },

    async useGeneratedContent(content) {
      try {
        await this.selectContent({
          campaignId: this.campaignId,
          adId: this.adId,
          contentId: content.id
        });
        this.showSuccess({
          title: 'Success',
          message: 'Content selected successfully'
        });
      } catch (error) {
        this.showError({
          title: 'Error',
          message: `Failed to select content: ${error.message}`
        });
      }
    }
  },
  created() {
    this.fetchAd({
      campaignId: this.campaignId,
      adId: this.adId
    })
    this.loadTextProviders()
  }
}
</script>

<style lang="scss" scoped>
.ad-detail-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
    
    .title-section {
      display: flex;
      align-items: center;
      
      h2 {
        margin: 0;
        margin-right: 1rem;
      }
    }
  }
  
  .loading-container, .error-container, .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 0;
    text-align: center;
  }
  
  .error-message {
    color: #f44336;
    margin-bottom: 1rem;
  }
  
  .ad-preview-container {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 2rem;
    
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
    
    .ad-preview {
      border: 1px solid #ddd;
      border-radius: 8px;
      overflow: hidden;
      background-color: #fff;
      
      .ad-preview-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px;
        border-bottom: 1px solid #f0f0f0;
        
        .page-info {
          display: flex;
          align-items: center;
          
          .page-avatar {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            background-color: #1877f2;
            margin-right: 8px;
          }
          
          .page-name {
            font-weight: 600;
          }
        }
        
        .post-time {
          color: #65676b;
          font-size: 0.9rem;
        }
      }
      
      .ad-preview-content {
        padding: 12px;
        
        .primary-text {
          margin-bottom: 12px;
        }
        
        .ad-image {
          margin: 0 -12px;
          
          img {
            width: 100%;
            max-height: 300px;
            object-fit: cover;
          }
        }
        
        .ad-headline-description {
          padding: 12px;
          border: 1px solid #f0f0f0;
          margin: 12px 0;
          
          h3 {
            margin: 0 0 8px 0;
            font-size: 1.1rem;
          }
          
          p {
            margin: 0;
            color: #65676b;
            font-size: 0.9rem;
          }
        }
        
        .ad-cta {
          display: flex;
          justify-content: center;
        }
      }
    }
    
    .ad-details {
      h3 {
        margin-top: 0;
      }
      
      .details-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 1rem;
        
        .detail-item {
          .label {
            display: block;
            font-weight: 600;
            margin-bottom: 0.25rem;
          }
        }
      }
    }
  }
  
  .ad-content-section {
    margin-top: 2rem;
    
    .content-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 1rem;
      
      .content-card {
        .primary-text {
          font-size: 0.9rem;
          margin-bottom: 1rem;
        }
        
        .description {
          font-size: 0.85rem;
          color: #666;
        }
        
        .provider {
          font-size: 0.8rem;
          color: #999;
          font-style: italic;
          margin-top: 1rem;
        }
      }
    }
    
    .generate-more {
      margin-top: 2rem;
      
      h4 {
        margin-top: 0;
      }
    }
  }
  
  .ai-prompt-container {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-width: 100%;
    margin: 0 auto;
    
    .ai-provider-selection {
      display: flex;
      gap: 2rem;
    }
  }
  
  .status-badge {
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.875rem;
    font-weight: 500;
    
    &.status-draft {
      background-color: #e0e0e0;
      color: #616161;
    }
    
    &.status-pending {
      background-color: #fff8e1;
      color: #ff8f00;
    }
    
    &.status-active {
      background-color: #e8f5e9;
      color: #2e7d32;
    }
    
    &.status-paused {
      background-color: #e3f2fd;
      color: #1565c0;
    }
    
    &.status-completed {
      background-color: #e8eaf6;
      color: #3949ab;
    }
    
    &.status-failed {
      background-color: #ffebee;
      color: #c62828;
    }
    
    &.status-rejected {
      background-color: #fce4ec;
      color: #c2185b;
    }
  }
}
</style>
