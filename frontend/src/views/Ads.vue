<template>
  <div class="ads-page">
    <div class="ads-content">

      <!-- Page Header -->
      <div class="page-header flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 gap-4">
        <div>
          <h1 class="text-3xl font-bold text-secondary-900">Ads</h1>
          <p class="text-secondary-600">Manage your advertising content</p>
        </div>
        <a-button type="primary" size="large" @click="$router.push('/ad/create')">
          <template #icon><plus-outlined /></template>
          New Ad
        </a-button>
      </div>

      <!-- Ad Table Component -->
      <AdTable
        :ads="ads"
        :campaigns="campaigns"
        :loading="loading"
        @view-details="showAdDetails"
        @edit-ad="showEditAdModal"
        @delete-ad="confirmDeleteAd"
        @duplicate-ad="duplicateAd"
        @export-ad="exportAdToFacebook"
      />

          <!-- Error State -->
          <div v-if="error" class="mb-6">
            <a-empty description="Error loading ads">
              <template #image>
                <div class="text-red-500">
                  <svg class="w-12 h-12 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
              </template>
              <p class="text-gray-600 mb-4">{{ error }}</p>
              <a-button type="primary" @click="loadAds">
                Try Again
              </a-button>
            </a-empty>
          </div>

          <!-- Empty State -->
          <div v-if="!loading && ads.length === 0" class="text-center py-12">
            <a-empty description="No ads yet">
              <p class="text-secondary-600 mb-6">Create your first ad to start advertising</p>
              <a-button type="primary" size="large" @click="$router.push('/ad/create')">
                <template #icon><plus-outlined /></template>
                Create Your First Ad
              </a-button>
            </a-empty>
          </div>
      </div>

      <!-- Ad Detail Modal -->
      <a-modal v-model:open="showDetailModal" title="Ad Details" width="90vw" style="max-width: 1200px">
        <div v-if="selectedAd">
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
            <p class="text-secondary-900">{{ selectedAd.name }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Campaign:</label>
            <p class="text-secondary-900">{{ selectedAd.campaignName || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Ad Type:</label>
            <p class="text-secondary-900">{{ selectedAd.adType?.replace('_', ' ') || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Status:</label>
            <p class="text-secondary-900">{{ selectedAd.status || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Headline:</label>
            <p class="text-secondary-900">{{ selectedAd.headline || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Description:</label>
            <p class="text-secondary-900">{{ selectedAd.description || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Primary Text:</label>
            <p class="text-secondary-900">{{ selectedAd.primaryText || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Call to Action:</label>
            <p class="text-secondary-900">{{ selectedAd.callToAction || 'N/A' }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Media:</label>
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl" class="space-y-3">
              <!-- Image Preview -->
              <div v-if="selectedAd.imageUrl" class="flex items-center space-x-3">
                <img :src="selectedAd.imageUrl" alt="Ad Image" class="w-20 h-20 object-cover rounded" />
                <div>
                  <p class="text-sm font-medium text-secondary-700">Image URL:</p>
                  <a :href="selectedAd.imageUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.imageUrl }}</a>
                </div>
              </div>
              <!-- Video Preview -->
              <div v-if="selectedAd.videoUrl" class="flex items-center space-x-3">
                <video :src="selectedAd.videoUrl" class="w-20 h-20 object-cover rounded" controls></video>
                <div>
                  <p class="text-sm font-medium text-secondary-700">Video URL:</p>
                  <a :href="selectedAd.videoUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.videoUrl }}</a>
                </div>
              </div>
            </div>
            <!-- No Media -->
            <div v-else class="py-8">
              <svg class="w-10 h-10 sm:w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
              </svg>
              <p class="text-gray-600">No image/video for this ad</p>
            </div>
          </div>
        </div>
      </a-modal>

      <!-- Edit Ad Modal -->
      <a-modal v-model:open="showEditModal" title="Edit Ad" width="90vw" style="max-width: 1200px">
        <div v-if="editingAd">
          <div class="field mb-4">
            <label for="editAdName" class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
            <a-input id="editAdName" v-model:value="editingAd.name" :status="errors.name ? 'error' : ''" @blur="validateEditAd()" />
            <p v-if="errors.name" class="form-error flex items-center gap-2 mt-1">
              <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {{ errors.name }}
            </p>
          </div>
          <div class="field mb-4">
            <label for="editAdHeadline" class="block text-sm font-medium text-secondary-700 mb-1">Headline:</label>
            <a-input id="editAdHeadline" v-model:value="editingAd.headline" />
          </div>
          <div class="field mb-4">
            <label for="editAdDescription" class="block text-sm font-medium text-secondary-700 mb-1">Description:</label>
            <a-textarea id="editAdDescription" v-model:value="editingAd.description" :rows="3" />
          </div>
          <div class="field mb-4">
            <label for="editAdPrimaryText" class="block text-sm font-medium text-secondary-700 mb-1">Primary Text:</label>
            <a-textarea id="editAdPrimaryText" v-model:value="editingAd.primaryText" :rows="3" />
          </div>
          <div class="field mb-4">
            <label for="editAdCallToAction" class="block text-sm font-medium text-secondary-700 mb-1">Call to Action:</label>
            <a-select id="editAdCallToAction" v-model:value="editingAd.callToAction" placeholder="Select Call to Action" style="width: 100%">
              <a-select-option v-for="cta in standardCTAs" :key="cta.value" :value="cta.value">
                {{ cta.label }}
              </a-select-option>
            </a-select>
          </div>
          <div class="field mb-4">
            <label for="editAdStatus" class="block text-sm font-medium text-secondary-700 mb-1">Status:</label>
            <a-select id="editAdStatus" v-model:value="editingAd.status" placeholder="Select Status" style="width: 100%">
              <a-select-option value="DRAFT">DRAFT</a-select-option>
              <a-select-option value="ACTIVE">ACTIVE</a-select-option>
              <a-select-option value="PAUSED">PAUSED</a-select-option>
              <a-select-option value="COMPLETED">COMPLETED</a-select-option>
              <a-select-option value="FAILED">FAILED</a-select-option>
            </a-select>
          </div>
        </div>
        <template #footer>
          <a-button @click="confirmCancelEdit">Cancel</a-button>
          <a-button type="primary" @click="saveEditedAd">Save</a-button>
        </template>
      </a-modal>
    </div>
</template>

<script>
import { mapState, mapActions } from "vuex"
import { Modal, Input, Select, Button, message, Empty } from "ant-design-vue"
import { PlusOutlined } from "@ant-design/icons-vue"

import AdTable from '@/components/AdTable.vue'
import api from '@/services/api'

export default {
  name: "Ads",
  components: {
    AModal: Modal,
    AInput: Input,
    ATextarea: Input.TextArea,
    ASelect: Select,
    AButton: Button,
    AEmpty: Empty,
    PlusOutlined,

    AdTable
  },
  data() {
    return {
      page: 0,
      size: 5,
      showDetailModal: false,
      selectedAd: null,
      showEditModal: false,
      editingAd: null,
      showMediaModal: false,
      searchQuery: '',
      filteredAds: [],
      errors: {},
      standardCTAs: [],
      selectedAds: [],
      isExporting: false
    }
  },
  computed: {
    ...mapState("ad", [
      "ads",
      "totalAds",
      "loading",
      "error"
    ]),
    ...mapState("campaign", {
      campaigns: "campaigns"
    }),
    
    displayedAds() {
      if (!this.searchQuery.trim()) {
        return this.ads
      }
      return this.filteredAds
    },
    

    
    totalPages() {
      return Math.ceil(this.totalAds / this.size)
    },
    
    totalItems() {
      return this.totalAds
    }
  },
  async mounted() {
    await this.loadAds()
    await this.loadCallToActions()
    await this.$store.dispatch('campaign/fetchCampaigns')
  },
  created() {
    this.$confirm = this.$confirm || Modal.confirm;
  },
  methods: {
    ...mapActions("ad", ["fetchAds", "deleteAd", "updateAd"]),
    
    async loadAds() {
      try {
        await this.fetchAds({ page: this.page, size: this.size })
        await this.$store.dispatch('campaign/fetchCampaigns')
      } catch (error) {
        console.error("Failed to load ads:", error)
      }
    },
    
    async loadCallToActions() {
      try {
        const response = await api.providers.getCallToActions('en')
        this.standardCTAs = response.data
      } catch (error) {
        console.error("Failed to load call to actions:", error)
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
    
    onPageChange(page, pageSize) {
      this.page = page - 1;
      this.size = pageSize;
      this.loadAds();
    },
    
    onPageSizeChange(current, size) {
      this.page = 0;
      this.size = size;
      this.loadAds();
    },
    
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    
    showAdDetails(ad) {
      this.selectedAd = ad;
      this.showDetailModal = true;
    },

    showEditAdModal(ad) {
      this.editingAd = { ...ad };
      this.showEditModal = true;
      this.errors = {};
    },

    async saveEditedAd() {
      this.errors = {};
      let isValid = true;

      if (!this.editingAd.name) {
        this.errors.name = 'Name is required';
        isValid = false;
      }
      if (!this.editingAd.headline) {
        this.errors.headline = 'Headline is required';
        isValid = false;
      }
      if (!this.editingAd.callToAction) {
        this.errors.callToAction = 'Call to Action is required';
        isValid = false;
      }
      if (!this.editingAd.status) {
        this.errors.status = 'Status is required';
        isValid = false;
      }

      if (!isValid) {
        message.error('Please fill in all required fields.');
        return;
      }

      try {
        const { id, headline, primaryText, description, callToAction, imageUrl, name } = this.editingAd;
        await this.updateAd({
          adId: id,
          adData: { headline, primaryText, description, callToAction, imageUrl, name }
        });
        this.showEditModal = false;
        message.success('Ad updated successfully');
        await this.loadAds();
      } catch (error) {
        console.error('Failed to save ad:', error);
        message.error(error.message || 'Failed to update ad');
      }
    },
    
    confirmDeleteAd(adId) {
      this.$confirm({
        title: 'Delete Confirmation',
        content: 'Do you really want to delete this ad? This action cannot be undone.',
        okText: 'Delete',
        okType: 'danger',
        cancelText: 'Cancel',
        onOk: async () => {
          try {
            await this.deleteAd(adId);
            message.success('Ad deleted successfully');
            await this.loadAds();
          } catch (error) {
            console.error('Failed to delete ad:', error);
            message.error(error.message || 'Failed to delete ad');
          }
        },
        onCancel: () => {
          message.info('Delete operation cancelled');
        }
      });
    },
    
    confirmCancelEdit() {
      this.$confirm({
        title: 'Confirmation',
        content: 'Are you sure you want to cancel? Your unsaved changes will be lost.',
        okText: 'Yes, Cancel',
        cancelText: 'No, Stay',
        onOk: () => {
          this.showEditModal = false;
          message.info('Changes discarded');
        },
        onCancel: () => {
          // Do nothing, stay in modal
        }
      });
    },

    handleSearch() {
      if (!this.searchQuery.trim()) {
        this.filteredAds = []
        return
      }
      
      const query = this.searchQuery.toLowerCase()
      this.filteredAds = this.ads.filter(ad => {
        return (
          ad.name?.toLowerCase().includes(query) ||
          ad.headline?.toLowerCase().includes(query) ||
          ad.description?.toLowerCase().includes(query) ||
          ad.primaryText?.toLowerCase().includes(query) ||
          ad.campaignName?.toLowerCase().includes(query) ||
          ad.status?.toLowerCase().includes(query)
        )
      })
    },
    
    getStatusColor(status) {
      switch (status?.toLowerCase()) {
        case "active":
          return "green"
        case "paused":
          return "orange"
        case "draft":
          return "default"
        case "completed":
          return "blue"
        case "failed":
          return "red"
        default:
          return "default"
      }
    },
    
    formatDate(dateString) {
      if (!dateString) return ""
      const date = new Date(dateString)
      return date.toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric"
      })
    },
    
    handleImageError(event) {
      event.target.style.display = 'none'
    },

    validateEditAd() {
      this.errors = {};
      let isValid = true;

      if (!this.editingAd.name) {
        this.errors.name = 'Name is required';
        isValid = false;
      }
      if (!this.editingAd.headline) {
        this.errors.headline = 'Headline is required';
        isValid = false;
      }
      if (!this.editingAd.callToAction) {
        this.errors.callToAction = 'Call to Action is required';
        isValid = false;
      }
      if (!this.editingAd.status) {
        this.errors.status = 'Status is required';
        isValid = false;
      }

      return isValid;
    },
    
    handleLogout() {
      this.$store.dispatch('auth/logout')
    },
    

    
    // Facebook Export Methods
    toggleAdSelection(adId) {
      const index = this.selectedAds.indexOf(adId)
      if (index > -1) {
        this.selectedAds.splice(index, 1)
      } else {
        this.selectedAds.push(adId)
      }
    },
    
    clearSelection() {
      this.selectedAds = []
    },
    
    async exportAdToFacebook(adId) {
      try {
        this.isExporting = true
        const response = await api.facebookExport.exportAd(adId)
        
        // Tạo URL để tải file
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `facebook_ad_${adId}.csv`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        
        message.success('Đã xuất quảng cáo thành công!')
      } catch (error) {
        console.error('Error exporting ad:', error)
        message.error('Lỗi khi xuất quảng cáo: ' + (error.response?.data?.message || error.message))
      } finally {
        this.isExporting = false
      }
    },

    async exportSelectedAdsToFacebook() {
      if (this.selectedAds.length === 0) {
        message.warning('Vui lòng chọn ít nhất một quảng cáo để xuất')
        return
      }
      
      try {
        this.isExporting = true
        const response = await api.facebookExport.exportMultipleAds(this.selectedAds)
        
        // Tạo URL để tải file
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `facebook_ads_bulk_${Date.now()}.csv`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        
        message.success(`Đã xuất ${this.selectedAds.length} quảng cáo thành công!`)
        this.selectedAds = []
      } catch (error) {
        console.error('Error exporting ads:', error)
        message.error('Lỗi khi xuất quảng cáo: ' + (error.response?.data?.message || error.message))
      } finally {
        this.isExporting = false
      }
    },

    async duplicateAd(adId) {
      try {
        const adToDuplicate = this.ads.find(ad => ad.id === adId)
        if (!adToDuplicate) {
          message.error('Không tìm thấy quảng cáo để sao chép')
          return
        }

        const duplicatedAdData = {
          name: `${adToDuplicate.name} (Copy)`,
          headline: adToDuplicate.headline,
          description: adToDuplicate.description,
          primaryText: adToDuplicate.primaryText,
          callToAction: adToDuplicate.callToAction,
          imageUrl: adToDuplicate.imageUrl,
          videoUrl: adToDuplicate.videoUrl,
          campaignId: adToDuplicate.campaignId,
          adType: adToDuplicate.adType,
          status: 'DRAFT'
        }

        await this.$store.dispatch('ad/createAd', duplicatedAdData)
        message.success('Đã sao chép quảng cáo thành công!')
        await this.loadAds()
      } catch (error) {
        console.error('Error duplicating ad:', error)
        message.error('Lỗi khi sao chép quảng cáo: ' + (error.response?.data?.message || error.message))
      }
    }
  }
}
</script>

<style scoped>
.page-wrapper.sidebar-closed .main-content-wrapper {
  margin-left: 0 !important;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ads-page {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}
</style>