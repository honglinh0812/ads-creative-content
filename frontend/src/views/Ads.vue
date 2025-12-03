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
      <a-modal
        v-model:open="showDetailModal"
        :title="$t('ads.detailModal.title')"
        width="90vw"
        style="max-width: 1200px"
        :footer="null"
      >
        <div v-if="selectedAd" class="ad-detail-modal">
          <a-descriptions :column="2" bordered size="small" class="mb-6">
            <a-descriptions-item :label="$t('ads.detailModal.fields.name')">
              {{ selectedAd.name || $t('ads.detailModal.notAvailable') }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.campaign')">
              {{ selectedAd.campaignName || $t('ads.detailModal.notAvailable') }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.adType')">
              <a-tag v-if="selectedAd.adType" color="blue">
                {{ getAdTypeLabel(selectedAd.adType) }}
              </a-tag>
              <span v-else>{{ $t('ads.detailModal.notAvailable') }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.status')">
              <a-tag :color="getStatusColor(selectedAd.status)">
                {{ getStatusLabel(selectedAd.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.callToAction')">
              {{ getCTALabel(selectedAd.callToAction) || $t('ads.detailModal.notAvailable') }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.createdDate')">
              {{ formatDate(selectedAd.createdDate) || $t('ads.detailModal.notAvailable') }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.websiteUrl')">
              <template v-if="selectedAd.websiteUrl">
                <a :href="selectedAd.websiteUrl" target="_blank" class="break-all text-blue-600 hover:text-blue-800">
                  {{ selectedAd.websiteUrl }}
                </a>
              </template>
              <span v-else>{{ $t('ads.detailModal.notAvailable') }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.imageUrl')">
              <template v-if="selectedAd.imageUrl">
                <a :href="selectedAd.imageUrl" target="_blank" class="break-all text-blue-600 hover:text-blue-800">
                  {{ selectedAd.imageUrl }}
                </a>
              </template>
              <span v-else>{{ $t('ads.detailModal.notAvailable') }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ads.detailModal.fields.videoUrl')" :span="2">
              <template v-if="selectedAd.videoUrl">
                <a :href="selectedAd.videoUrl" target="_blank" class="break-all text-blue-600 hover:text-blue-800">
                  {{ selectedAd.videoUrl }}
                </a>
              </template>
              <span v-else>{{ $t('ads.detailModal.notAvailable') }}</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="grid md:grid-cols-2 gap-4 mb-6">
            <div class="field">
              <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.headline') }}:</label>
              <p class="text-secondary-900 whitespace-pre-wrap">
                {{ selectedAd.headline || $t('ads.detailModal.notAvailable') }}
              </p>
            </div>
            <div class="field">
              <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.description') }}:</label>
              <p class="text-secondary-900 whitespace-pre-wrap">
                {{ selectedAd.description || $t('ads.detailModal.notAvailable') }}
              </p>
            </div>
            <div class="field md:col-span-2">
              <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('ads.detailModal.fields.primaryText') }}:</label>
              <p class="text-secondary-900 whitespace-pre-wrap">
                {{ selectedAd.primaryText || $t('ads.detailModal.notAvailable') }}
              </p>
            </div>
          </div>

          <div class="field mb-6">
            <label class="block text-sm font-medium text-secondary-700 mb-3">{{ $t('ads.detailModal.fields.media') }}:</label>
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl" class="space-y-3">
              <div v-if="selectedAd.imageUrl" class="flex items-center space-x-3">
                <img :src="selectedAd.imageUrl" alt="Ad Image" class="w-20 h-20 object-cover rounded" />
                <div>
                  <p class="text-sm font-medium text-secondary-700">{{ $t('ads.detailModal.fields.imageUrl') }}:</p>
                  <a :href="selectedAd.imageUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.imageUrl }}</a>
                </div>
              </div>
              <div v-if="selectedAd.videoUrl" class="flex items-center space-x-3">
                <video :src="selectedAd.videoUrl" class="w-20 h-20 object-cover rounded" controls></video>
                <div>
                  <p class="text-sm font-medium text-secondary-700">{{ $t('ads.detailModal.fields.videoUrl') }}:</p>
                  <a :href="selectedAd.videoUrl" target="_blank" class="text-blue-600 hover:text-blue-800 text-sm break-all">{{ selectedAd.videoUrl }}</a>
                </div>
              </div>
            </div>
            <div v-else class="py-8 text-center">
              <svg class="w-10 h-10 sm:w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
              </svg>
              <p class="text-gray-600">{{ $t('ads.detailModal.noMedia') }}</p>
            </div>
          </div>

          <div class="text-right">
            <a-button type="primary" @click="showDetailModal = false">
              {{ $t('common.action.close') }}
            </a-button>
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
          <a-form layout="vertical">
            <a-row :gutter="[16, 16]">
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.name')" :validate-status="errors.name ? 'error' : ''" :help="errors.name">
                  <a-input v-model:value="editingAd.name" @blur="validateEditAd" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.adType')">
                  <a-select
                    v-model:value="editingAd.adType"
                    :options="adTypeSelectOptions"
                    allow-clear
                    :placeholder="$t('ads.editModal.fields.adType')"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.status')" :validate-status="errors.status ? 'error' : ''" :help="errors.status">
                  <a-select
                    v-model:value="editingAd.status"
                    :options="editStatusOptions"
                    :placeholder="$t('ads.editModal.fields.status')"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.callToAction')" :validate-status="errors.callToAction ? 'error' : ''" :help="errors.callToAction">
                  <a-select
                    v-model:value="editingAd.callToAction"
                    :placeholder="$t('ads.editModal.fields.callToAction')"
                    show-search
                  >
                    <a-select-option v-for="cta in standardCTAs" :key="cta.value" :value="cta.value">
                      {{ cta.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.headline')" :validate-status="errors.headline ? 'error' : ''" :help="errors.headline">
                  <a-input v-model:value="editingAd.headline" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.websiteUrl')">
                  <a-input v-model:value="editingAd.websiteUrl" type="url" />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item :label="$t('ads.editModal.fields.description')">
                  <a-textarea v-model:value="editingAd.description" :rows="3" />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item :label="$t('ads.editModal.fields.primaryText')">
                  <a-textarea v-model:value="editingAd.primaryText" :rows="4" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.imageUrl')">
                  <a-input v-model:value="editingAd.imageUrl" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item :label="$t('ads.editModal.fields.videoUrl')">
                  <a-input v-model:value="editingAd.videoUrl" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
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
        { label: this.$t('adTable.table.statusFilters.ready'), value: 'READY' },
        { label: this.$t('adTable.table.statusFilters.active'), value: 'ACTIVE' },
        { label: this.$t('adTable.table.statusFilters.paused'), value: 'PAUSED' },
        { label: this.$t('adTable.table.statusFilters.completed'), value: 'COMPLETED' },
        { label: this.$t('adTable.table.statusFilters.failed'), value: 'FAILED' },
        { label: this.$t('adTable.table.statusFilters.draft'), value: 'DRAFT' }
      ]
    },

    adTypeOptions() {
      return [
        { label: this.$t('ads.adTypeLabel.websiteConversion'), value: 'WEBSITE_CONVERSION_AD' },
        { label: this.$t('ads.adTypeLabel.pagePost'), value: 'PAGE_POST_AD' },
        { label: this.$t('ads.adTypeLabel.leadForm'), value: 'LEAD_FORM_AD' }
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
    },

    statusLabelMap() {
      return {
        DRAFT: this.$t('ads.statusLabel.draft'),
        READY: this.$t('ads.statusLabel.ready'),
        ACTIVE: this.$t('ads.statusLabel.active'),
        PAUSED: this.$t('ads.statusLabel.paused'),
        COMPLETED: this.$t('ads.statusLabel.completed'),
        FAILED: this.$t('ads.statusLabel.failed')
      }
    },

    adTypeLabelMap() {
      return {
        WEBSITE_CONVERSION_AD: this.$t('ads.adTypeLabel.websiteConversion'),
        PAGE_POST_AD: this.$t('ads.adTypeLabel.pagePost'),
        LEAD_FORM_AD: this.$t('ads.adTypeLabel.leadForm')
      }
    },

    editStatusOptions() {
      return [
        { value: 'DRAFT', label: this.statusLabelMap.DRAFT },
        { value: 'ACTIVE', label: this.statusLabelMap.ACTIVE },
        { value: 'PAUSED', label: this.statusLabelMap.PAUSED },
        { value: 'COMPLETED', label: this.statusLabelMap.COMPLETED },
        { value: 'FAILED', label: this.statusLabelMap.FAILED }
      ]
    },

    adTypeSelectOptions() {
      return Object.entries(this.adTypeLabelMap).map(([value, label]) => ({
        value,
        label
      }))
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
      this.editingAd = this.prepareEditingAd(ad);
      this.showEditModal = true;
      this.errors = {};
    },

    async saveEditedAd() {
      if (!this.validateEditAd()) {
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
          status,
          adType,
          websiteUrl
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
            status,
            adType,
            websiteUrl
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

    getStatusLabel(status) {
      if (!status) {
        return this.$t('ads.detailModal.notAvailable');
      }
      return this.statusLabelMap[status] || this.formatEnumValue(status);
    },

    getAdTypeLabel(adType) {
      if (!adType) {
        return this.$t('ads.detailModal.notAvailable');
      }
      return this.adTypeLabelMap[adType] || this.formatEnumValue(adType);
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

    prepareEditingAd(ad) {
      return {
        ...ad,
        adType: ad.adType || null,
        websiteUrl: ad.websiteUrl || '',
        imageUrl: ad.imageUrl || '',
        videoUrl: ad.videoUrl || ''
      }
    },

    formatEnumValue(value) {
      if (!value) {
        return this.$t('ads.detailModal.notAvailable');
      }
      return value.replace(/_/g, ' ');
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
        const redirectUrl = result?.redirectUrl || result?.autoUpload?.adsManagerUrl
        message.success(this.$t('ads.messages.success.exportSuccess', { count }))
        if (redirectUrl) {
          window.open(redirectUrl, '_blank', 'noopener,noreferrer')
        }
      } else if (count) {
        message.success(this.$t('ads.messages.success.exportSuccess', { count }))
        const format = this.$store.state.fbExport.format
        this.exportFormat = format
        this.exportTimestamp = Date.now()
        setTimeout(() => {
          this.showInstructionsModal = true
        }, 500)
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
