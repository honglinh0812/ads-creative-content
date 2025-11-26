<template>
  <div class="ads-page">
    <div class="ads-content">

      <!-- Page Header -->
      <a-page-header
        class="ads-page-header"
        :title="$t('ads.pageHeader.title')"
        :sub-title="$t('ads.pageHeader.subtitle')"
      >
        <template #extra>
          <a-button type="primary" size="large" @click="$router.push('/ad/create')">
            <template #icon><plus-outlined /></template>
            {{ $t('ads.pageHeader.newAdButton') }}
          </a-button>
        </template>
      </a-page-header>

      <!-- Ad Table Component -->
      <AdTable
        :ads="safeAds"
        :campaigns="safeCampaigns"
        :loading="loading"
        :total-items="totalItems"
        :current-page="page + 1"
        :page-size="size"
        :status-options="statusOptions"
        :ad-type-options="adTypeOptions"
        @view-details="showAdDetails"
        @edit-ad="showEditAdModal"
        @delete-ad="confirmDeleteAd"
        @duplicate-ad="duplicateAd"
        @export-ad="exportAdToFacebook"
        @download-ad="openDownloadModal"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      />

      <!-- Ad Detail Modal -->
      <a-modal v-model:open="showDetailModal" :title="$t('ads.detailModal.title')" width="90vw" style="max-width: 1200px">
        <div v-if="selectedAd">
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.name') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.name }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.campaign') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.campaignName || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.adType') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.adType?.replace('_', ' ') || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.status') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.status || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.headline') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.headline || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.description') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.description || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.primaryText') }}:</label>
            <p class="text-secondary-900">{{ selectedAd.primaryText || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.callToAction') }}:</label>
            <p class="text-secondary-900">{{ getCTALabel(selectedAd.callToAction) || $t('ads.detailModal.notAvailable') }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.media') }}:</label>
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl" class="space-y-3">
              <!-- Image Preview -->
              <div v-if="selectedAd.imageUrl" class="flex items-center space-x-3">
                <img :src="selectedAd.imageUrl" alt="Ad Image" class="w-20 h-20 object-cover rounded" />
                <div>
                  <p class="text-sm font-medium text-secondary-700">{{ $t('ads.detailModal.fields.imageUrl') }}:</p>
                  <a :href="selectedAd.imageUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.imageUrl }}</a>
                </div>
              </div>
              <!-- Video Preview -->
              <div v-if="selectedAd.videoUrl" class="flex items-center space-x-3">
                <video :src="selectedAd.videoUrl" class="w-20 h-20 object-cover rounded" controls></video>
                <div>
                  <p class="text-sm font-medium text-secondary-700">{{ $t('ads.detailModal.fields.videoUrl') }}:</p>
                  <a :href="selectedAd.videoUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.videoUrl }}</a>
                </div>
              </div>
            </div>
            <!-- No Media -->
            <div v-else class="py-8">
              <svg class="w-10 h-10 sm:w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
              </svg>
              <p class="text-gray-600">{{ $t('ads.detailModal.noMedia') }}</p>
            </div>
          </div>
        </div>
      </a-modal>

      <!-- Simple Export Confirmation Modal -->
      <a-modal
        v-model:open="autoUploadConfirmVisible"
        :confirm-loading="autoUploadLoading"
        :title="$t('ads.export.confirm.title', { count: selectedAdIds.length })"
        @ok="confirmAutoUpload"
        @cancel="cancelAutoUpload"
      >
        <p>{{ $t('ads.export.confirm.message', { count: selectedAdIds.length }) }}</p>
        <p class="text-gray-500 text-sm mt-2">
          {{ $t('ads.export.confirm.note') }}
        </p>
      </a-modal>

      <!-- Legacy Export Modal (Download flows) -->
      <ExportToFacebookModal
        :visible="showExportModal"
        :ad-ids="selectedAdIds"
        :show-auto-upload="exportModalShowAutoUpload"
        @update:visible="showExportModal = $event"
        @success="handleExportSuccess"
        @error="handleExportError"
      />

      <!-- Facebook Instructions Modal -->
      <FacebookInstructionsModal
        :visible="showInstructionsModal"
        :format="exportFormat"
        :timestamp="exportTimestamp"
        @update:visible="showInstructionsModal = $event"
      />

      <!-- Edit Ad Modal -->
      <a-modal v-model:open="showEditModal" :title="$t('ads.editModal.title')" width="90vw" style="max-width: 1200px">
        <div v-if="editingAd">
          <div class="field mb-4">
            <label for="editAdName" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.name') }}:</label>
            <a-input id="editAdName" v-model:value="editingAd.name" :status="errors.name ? 'error' : ''" @blur="validateEditAd()" />
            <p v-if="errors.name" class="form-error flex items-center gap-2 mt-1">
              <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {{ errors.name }}
            </p>
          </div>
          <div class="field mb-4">
            <label for="editAdHeadline" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.headline') }}:</label>
            <a-input id="editAdHeadline" v-model:value="editingAd.headline" />
          </div>
          <div class="field mb-4">
            <label for="editAdDescription" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.description') }}:</label>
            <a-textarea id="editAdDescription" v-model:value="editingAd.description" :rows="3" />
          </div>
          <div class="field mb-4">
            <label for="editAdPrimaryText" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.primaryText') }}:</label>
            <a-textarea id="editAdPrimaryText" v-model:value="editingAd.primaryText" :rows="3" />
          </div>
          <div class="field mb-4">
            <label for="editAdCallToAction" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.callToAction') }}:</label>
            <a-select id="editAdCallToAction" v-model:value="editingAd.callToAction" :placeholder="$t('ads.editModal.fields.callToAction')" style="width: 100%">
              <a-select-option v-for="cta in standardCTAs" :key="cta.value" :value="cta.value">
                {{ cta.label }}
              </a-select-option>
            </a-select>
          </div>
          <div class="field mb-4">
            <label for="editAdStatus" class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.editModal.fields.status') }}:</label>
            <a-select id="editAdStatus" v-model:value="editingAd.status" :placeholder="$t('ads.editModal.fields.status')" style="width: 100%">
              <a-select-option value="DRAFT">{{ $t('ads.editModal.statusOptions.draft') }}</a-select-option>
              <a-select-option value="ACTIVE">{{ $t('ads.editModal.statusOptions.active') }}</a-select-option>
              <a-select-option value="PAUSED">{{ $t('ads.editModal.statusOptions.paused') }}</a-select-option>
              <a-select-option value="COMPLETED">{{ $t('ads.editModal.statusOptions.completed') }}</a-select-option>
              <a-select-option value="FAILED">{{ $t('ads.editModal.statusOptions.failed') }}</a-select-option>
            </a-select>
          </div>
        </div>
        <template #footer>
          <a-button @click="confirmCancelEdit">{{ $t('ads.editModal.buttons.cancel') }}</a-button>
          <a-button type="primary" @click="saveEditedAd">{{ $t('ads.editModal.buttons.save') }}</a-button>
        </template>
      </a-modal>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions, mapGetters } from "vuex"
import { Modal, Input, Select, Button, message } from "ant-design-vue"
import { PlusOutlined } from "@ant-design/icons-vue"

import AdTable from '@/components/AdTable.vue'
import ExportToFacebookModal from '@/components/ExportToFacebookModal.vue'
import FacebookInstructionsModal from '@/components/FacebookInstructionsModal.vue'

export default {
  name: "Ads",
  components: {
    AModal: Modal,
    AInput: Input,
    ATextarea: Input.TextArea,
    ASelect: Select,
    ASelectOption: Select.Option,
    AButton: Button,
    PlusOutlined,

    AdTable,
    ExportToFacebookModal,
    FacebookInstructionsModal
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
      selectedAdIds: [],
      previewAutoUpload: true,
      autoUploadConfirmVisible: false,
      autoUploadLoading: false,
      showExportModal: false,
      exportModalShowAutoUpload: true,
      showInstructionsModal: false,
      exportFormat: 'csv',
      exportTimestamp: Date.now()
    }
  },
  computed: {
    ...mapState("ad", {
      ads: state => state.ads,
      loading: state => state.loading,
      error: state => state.error,
      adTotalItems: state => state.totalItems
    }),
    ...mapState("campaign", {
      campaigns: state => state.campaigns
    }),
    ...mapGetters('cta', {
      allCTAs: 'allCTAs',
      ctaLoaded: 'isLoaded'
    }),

    // Use Vuex CTAs instead of local state
    standardCTAs() {
      return this.allCTAs
    },

    statusOptions() {
      return [
        { label: this.$t('ads.filter.status.ready'), value: 'READY' },
        { label: this.$t('ads.filter.status.active'), value: 'ACTIVE' },
        { label: this.$t('ads.filter.status.paused'), value: 'PAUSED' },
        { label: this.$t('ads.filter.status.completed'), value: 'COMPLETED' },
        { label: this.$t('ads.filter.status.failed'), value: 'FAILED' },
        { label: this.$t('ads.filter.status.draft'), value: 'DRAFT' }
      ]
    },

    adTypeOptions() {
      return [
        { label: this.$t('ads.filter.types.website'), value: 'WEBSITE_CONVERSION_AD' },
        { label: this.$t('ads.filter.types.pagePost'), value: 'PAGE_POST_AD' },
        { label: this.$t('ads.filter.types.leadForm'), value: 'LEAD_FORM_AD' }
      ]
    },

    displayedAds() {
      if (!this.searchQuery.trim()) {
        return this.ads
      }
      return this.filteredAds
    },

    safeAds() {
      return Array.isArray(this.ads) ? this.ads : []
    },
    safeCampaigns() {
      return Array.isArray(this.campaigns) ? this.campaigns : []
    },
    totalItems() {
      return typeof this.adTotalItems === 'number' ? this.adTotalItems : this.safeAds.length
    },

    totalPages() {
      if (!this.totalItems) return 0
      return Math.ceil(this.totalItems / this.size)
    }
  },
  async mounted() {
    await this.$store.dispatch('campaign/fetchCampaigns')
    await this.loadAds()
    await this.loadCallToActions()
  },
  created() {
    this.$confirm = this.$confirm || Modal.confirm;
  },
  methods: {
    ...mapActions("ad", ["fetchAds", "deleteAd", "updateAd"]),
    ...mapActions("fbExport", {
      setSelectedAdsForExport: "setSelectedAds"
    }),
    ...mapActions('cta', ['loadCTAs']),

    /**
     * Get CTA display label from enum value
     * @param {string} value - CTA enum value (e.g., 'LEARN_MORE')
     * @returns {string} - Display label (e.g., 'Learn More')
     */
    getCTALabel(value) {
      if (!value) return ''
      const cta = this.standardCTAs.find(c => c.value === value)
      return cta ? cta.label : value
    },

    async loadAds() {
      try {
        await this.fetchAds({ page: this.page, size: this.size })
        await this.$store.dispatch('campaign/fetchCampaigns')
      } catch (error) {
        console.error(this.$t('ads.messages.error.loadAdsFailed'), error)
      }
    },

    async loadCallToActions() {
      try {
        // Load CTAs from Vuex store
        // Use current locale from i18n
        const language = this.$i18n.locale === 'vi' ? 'vi' : 'en'
        await this.loadCTAs({ language })
      } catch (error) {
        console.error(this.$t('ads.messages.error.loadCtaFailed'), error)
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
        this.errors.name = this.$t('ads.editModal.validation.nameRequired');
        isValid = false;
      }
      if (!this.editingAd.headline) {
        this.errors.headline = this.$t('ads.editModal.validation.headlineRequired');
        isValid = false;
      }
      if (!this.editingAd.callToAction) {
        this.errors.callToAction = this.$t('ads.editModal.validation.callToActionRequired');
        isValid = false;
      }
      if (!this.editingAd.status) {
        this.errors.status = this.$t('ads.editModal.validation.statusRequired');
        isValid = false;
      }

      if (!isValid) {
        message.error(this.$t('ads.editModal.validation.fillAllFields'));
        return;
      }

      try {
        const {
          id,
          name,
          headline,
          primaryText,
          description,
          callToAction,
          imageUrl,
          videoUrl,
          status
        } = this.editingAd;

        await this.updateAd({
          adId: id,
          adData: {
            name,
            headline,
            primaryText,
            description,
            callToAction,
            imageUrl,
            videoUrl,
            status
          }
        });
        this.showEditModal = false;
        message.success(this.$t('ads.messages.success.adUpdated'));
        await this.loadAds();
      } catch (error) {
        console.error(this.$t('ads.messages.error.updateAdFailed'), error);
        message.error(error.message || this.$t('ads.messages.error.updateAdFailed'));
      }
    },

    confirmDeleteAd(adId) {
      this.$confirm({
        title: this.$t('ads.deleteConfirm.title'),
        content: this.$t('ads.deleteConfirm.message'),
        okText: this.$t('ads.deleteConfirm.okText'),
        okType: 'danger',
        cancelText: this.$t('ads.deleteConfirm.cancelText'),
        onOk: async () => {
          try {
            await this.deleteAd(adId);
            message.success(this.$t('ads.messages.success.adDeleted'));
            await this.loadAds();
          } catch (error) {
            console.error(this.$t('ads.messages.error.deleteAdFailed'), error);
            message.error(error.message || this.$t('ads.messages.error.deleteAdFailed'));
          }
        },
        onCancel: () => {
          message.info(this.$t('ads.messages.success.deleteCancelled'));
        }
      });
    },

    confirmCancelEdit() {
      this.$confirm({
        title: this.$t('ads.editModal.cancelConfirm.title'),
        content: this.$t('ads.editModal.cancelConfirm.message'),
        okText: this.$t('ads.editModal.cancelConfirm.okText'),
        cancelText: this.$t('ads.editModal.cancelConfirm.cancelText'),
        onOk: () => {
          this.showEditModal = false;
          message.info(this.$t('ads.messages.success.changesDiscarded'));
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
      const locale = this.$i18n.locale === 'vi' ? 'vi-VN' : 'en-US'
      return date.toLocaleDateString(locale, {
        month: "short",
        day: "numeric",
        year: "numeric"
      })
    },

    handleImageError(event) {
      // Set fallback placeholder image using inline SVG data URI
      const errorMessage = this.$t('ads.imageError.cannotLoad')
      event.target.src = 'data:image/svg+xml,' + encodeURIComponent(`
        <svg width="400" height="300" xmlns="http://www.w3.org/2000/svg">
          <rect width="400" height="300" fill="#f0f0f0"/>
          <text x="50%" y="50%" text-anchor="middle" fill="#999" font-size="16" font-family="Arial, sans-serif">
            ${errorMessage}
          </text>
        </svg>
      `)
    },

    validateEditAd() {
      this.errors = {};
      let isValid = true;

      if (!this.editingAd.name) {
        this.errors.name = this.$t('ads.editModal.validation.nameRequired');
        isValid = false;
      }
      if (!this.editingAd.headline) {
        this.errors.headline = this.$t('ads.editModal.validation.headlineRequired');
        isValid = false;
      }
      if (!this.editingAd.callToAction) {
        this.errors.callToAction = this.$t('ads.editModal.validation.callToActionRequired');
        isValid = false;
      }
      if (!this.editingAd.status) {
        this.errors.status = this.$t('ads.editModal.validation.statusRequired');
        isValid = false;
      }

      return isValid;
    },

    handleLogout() {
      this.$store.dispatch('auth/logout')
    },



    // Facebook Export Methods
    exportAdToFacebook(adIdOrRecord) {
      let adIds = []

      if (typeof adIdOrRecord === 'number') {
        adIds = [adIdOrRecord]
      } else if (adIdOrRecord && typeof adIdOrRecord === 'object' && !Array.isArray(adIdOrRecord)) {
        if (adIdOrRecord.id) {
          adIds = [adIdOrRecord.id]
        } else {
          message.error(this.$t('ads.messages.error.invalidAdData'))
          return
        }
      } else if (Array.isArray(adIdOrRecord)) {
        adIds = adIdOrRecord
      } else {
        message.error(this.$t('ads.messages.error.invalidExportData'))
        return
      }

      if (!adIds.length) {
        message.warning(this.$t('ads.messages.warning.selectAdToExport'))
        return
      }

      this.selectedAdIds = adIds
      this.setSelectedAdsForExport(adIds)
      this.previewAutoUpload = true
      this.autoUploadConfirmVisible = true
    },

    handleExportSuccess(result) {
      this.showExportModal = false
      this.autoUploadConfirmVisible = false
      const count = result?.payloads?.length || this.selectedAdIds.length
      const autoUploadCompleted = this.previewAutoUpload || !!(result && result.autoUpload)

      if (autoUploadCompleted) {
        message.success(this.$t('ads.messages.success.exportSuccess', { count }))
        const format = this.$store.state.fbExport.format
        this.exportFormat = format

        setTimeout(() => {
          this.showInstructionsModal = true
        }, 500)

        this.exportTimestamp = Date.now()
      } else if (count) {
        message.success(this.$t('ads.messages.success.exportSuccess', { count }))
      }

      this.selectedAdIds = []
      this.previewAutoUpload = autoUploadCompleted
    },

    handleExportError(error) {
      console.error('Export error:', error)
      message.error(this.$t('ads.messages.error.exportError', { error: error.message || 'Unknown error' }))
      this.autoUploadConfirmVisible = false
      this.autoUploadLoading = false
    },

    toggleAdSelection(adId) {
      const index = this.selectedAdIds.indexOf(adId)
      if (index > -1) {
        this.selectedAdIds.splice(index, 1)
      } else {
        this.selectedAdIds.push(adId)
      }
    },

    clearSelection() {
      this.selectedAdIds = []
    },

    openDownloadModal(ad) {
      const adIds = Array.isArray(ad) ? ad : [ad.id || ad]
      if (!adIds.length) {
        message.warning(this.$t('ads.messages.warning.selectAdToExport'))
        return
      }
      this.selectedAdIds = adIds
      this.setSelectedAdsForExport(adIds)
      this.exportModalShowAutoUpload = true
      this.previewAutoUpload = false
      this.autoUploadConfirmVisible = false
      this.showExportModal = true
    },

    async confirmAutoUpload() {
      if (!this.selectedAdIds.length) {
        message.warning(this.$t('ads.messages.warning.selectAdToExport'))
        return
      }
      this.autoUploadLoading = true
      try {
        const result = await this.$store.dispatch('fbExport/exportToFacebook', {
          autoUpload: true,
          skipDownloadFallback: true
        })
        this.handleExportSuccess(result)
      } catch (error) {
        this.handleExportError(error)
      } finally {
        this.autoUploadLoading = false
      }
    },

    cancelAutoUpload() {
      this.autoUploadConfirmVisible = false
      this.previewAutoUpload = false
    },

    async duplicateAd(adOrRecord) {
      try {
        const adId = typeof adOrRecord === 'object' && adOrRecord !== null ? adOrRecord.id : adOrRecord
        const adToDuplicate = typeof adOrRecord === 'object' && adOrRecord !== null
          ? adOrRecord
          : this.ads.find(ad => ad.id === adId)
        if (!adToDuplicate) {
          message.error(this.$t('ads.messages.error.adNotFound'))
          return
        }

        const campaignId = adToDuplicate.campaignId || adToDuplicate.campaign?.id

        const duplicatedAdData = {
          name: `${adToDuplicate.name} (Copy)`,
          headline: adToDuplicate.headline,
          description: adToDuplicate.description,
          primaryText: adToDuplicate.primaryText,
          callToAction: adToDuplicate.callToAction,
          imageUrl: adToDuplicate.imageUrl,
          videoUrl: adToDuplicate.videoUrl,
          campaignId,
          campaign: campaignId ? { id: campaignId } : undefined,
          adType: adToDuplicate.adType,
          status: 'DRAFT'
        }

        if (!duplicatedAdData.campaignId) {
          message.error(this.$t('ads.messages.error.campaignMissing'))
          return
        }

        await this.$store.dispatch('ad/createAd', duplicatedAdData)
        message.success(this.$t('ads.messages.success.adDuplicated'))
        await this.loadAds()
      } catch (error) {
        console.error('Error duplicating ad:', error)
        message.error(this.$t('ads.messages.error.duplicateError', {
          error: error.response?.data?.message || error.message
        }))
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

.ads-page-header {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.06);
}

/* Mobile Responsiveness - Phase 2 Implementation */
@media (max-width: 768px) {
  .ads-page {
    padding: 12px;
  }

  .ads-page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .ads-page-header .ant-btn {
    width: 100%;
    order: -1;
  }

  .ant-modal {
    margin: 0;
    max-width: 100vw;
    height: 100vh;
  }

  .ant-modal-content {
    height: 100%;
    border-radius: 0;
  }

  .field {
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .ads-page {
    padding: 8px;
  }

  .ads-page-header {
    gap: 8px;
  }

  .ads-page-header h1 {
    font-size: 1.75rem;
  }

  .ads-page-header p {
    font-size: 0.9rem;
  }

  .field label {
    font-size: 0.875rem;
  }
}
</style>
