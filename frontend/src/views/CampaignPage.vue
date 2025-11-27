<template>
  <div class="page-container">
    <!-- Standardized Page Header -->
    <div class="page-header-standard">
      <div class="page-header-content">
        <h1 class="page-title-standard">{{ $t('campaign.page.title') }}</h1>
        <p class="page-subtitle-standard">
          {{ $t('campaign.page.subtitle') }}
        </p>
      </div>

      <div class="page-actions-standard">
        <router-link to="/campaign/create">
          <button class="btn-primary-standard">
            <PlusOutlined />
            {{ $t('campaign.page.action.newCampaign') }}
          </button>
        </router-link>
      </div>
    </div>
    
    <!-- Campaign Table with Advanced Filtering -->
    <div>
      <CampaignTable
        :campaigns="campaigns"
        :loading="loading"
        :total-items="totalItems"
        :current-page="currentPage"
        :page-size="size"
        @view-details="showCampaignDetails"
        @edit-campaign="showEditCampaignModal"
        @delete-campaign="confirmDeleteCampaign"
        @view-ads="viewCampaignAds"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      />
    </div>

      <!-- Campaign Detail Modal -->
      <a-modal
        v-model:open="showDetailModal"
        :title="$t('campaign.modal.details.title')"
        :width="900"
      >
        <div v-if="selectedCampaign">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item :label="$t('campaign.modal.details.label.name')">
              {{ selectedCampaign.name }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.status')">
              <a-tag :color="getStatusColor(selectedCampaign.status)">
                {{ getStatusLabel(selectedCampaign.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.objective')">
              {{ formatObjective(selectedCampaign.objective) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.budgetType')">
              {{ formatBudgetType(selectedCampaign.budgetType) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.dailyBudget')">
              {{ formatCurrency(selectedCampaign.dailyBudget) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.totalBudget')">
              {{ formatCurrency(selectedCampaign.totalBudget) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.bidCap')">
              {{ formatCurrency(selectedCampaign.bidCap) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.totalAds')">
              {{ selectedCampaign.totalAds || 0 }}
            </a-descriptions-item>
            <a-descriptions-item
              :label="$t('campaign.modal.details.label.targetAudience')"
              :span="2"
            >
              {{ selectedCampaign.targetAudience || $t('common.default.notAvailable') }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.startDate')">
              {{ formatDateOnly(selectedCampaign.startDate) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.endDate')">
              {{ formatDateOnly(selectedCampaign.endDate) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.createdDate')">
              {{ formatDateTime(selectedCampaign.createdDate) }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
        <template #footer>
          <a-button type="primary" @click="showDetailModal = false">{{ $t('common.action.ok') }}</a-button>
        </template>
      </a-modal>

      <!-- Edit Campaign Modal -->
      <a-modal
        v-model:open="showEditModal"
        :title="$t('campaign.modal.edit.title')"
        :width="900"
        @ok="saveEditedCampaign"
        @cancel="confirmCancelEdit"
      >
        <div v-if="editingCampaign">
          <a-form layout="vertical" @submit.prevent>
            <a-row :gutter="[16, 8]">
              <a-col :span="24">
                <a-form-item
                  :label="$t('campaign.modal.details.label.name')"
                  :validate-status="errors.name ? 'error' : ''"
                  :help="errors.name"
                >
                  <a-input
                    v-model:value="editingCampaign.name"
                    :placeholder="$t('campaign.modal.edit.placeholder.name')"
                  />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.objective')"
                  :validate-status="errors.objective ? 'error' : ''"
                  :help="errors.objective"
                >
                  <a-select
                    v-model:value="editingCampaign.objective"
                    :placeholder="$t('campaign.modal.edit.placeholder.objective')"
                  >
                    <a-select-option
                      v-for="option in objectiveOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.budgetType')"
                  :validate-status="errors.budgetType ? 'error' : ''"
                  :help="errors.budgetType"
                >
                  <a-select
                    v-model:value="editingCampaign.budgetType"
                    :placeholder="$t('campaign.modal.edit.placeholder.budgetType')"
                  >
                    <a-select-option
                      v-for="option in budgetTypeOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>

              <a-col v-if="editingCampaign.budgetType === 'DAILY'" :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.dailyBudget')"
                  :validate-status="errors.dailyBudget ? 'error' : ''"
                  :help="errors.dailyBudget"
                >
                  <a-input-number
                    v-model:value="editingCampaign.dailyBudget"
                    :min="0"
                    style="width: 100%;"
                    :placeholder="$t('campaign.modal.edit.placeholder.dailyBudget')"
                  />
                </a-form-item>
              </a-col>

              <a-col v-if="editingCampaign.budgetType === 'LIFETIME'" :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.totalBudget')"
                  :validate-status="errors.totalBudget ? 'error' : ''"
                  :help="errors.totalBudget"
                >
                  <a-input-number
                    v-model:value="editingCampaign.totalBudget"
                    :min="0"
                    style="width: 100%;"
                    :placeholder="$t('campaign.modal.edit.placeholder.totalBudget')"
                  />
                </a-form-item>
              </a-col>

              <a-col :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.bidCap')"
                  :validate-status="errors.bidCap ? 'error' : ''"
                  :help="errors.bidCap"
                >
                  <a-input-number
                    v-model:value="editingCampaign.bidCap"
                    :min="0"
                    style="width: 100%;"
                    :placeholder="$t('campaign.modal.edit.placeholder.bidCap')"
                  />
                </a-form-item>
              </a-col>

              <a-col :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.startDate')"
                  :validate-status="errors.startDate ? 'error' : ''"
                  :help="errors.startDate"
                >
                  <a-date-picker
                    v-model:value="editingCampaign.startDate"
                    format="DD/MM/YYYY"
                    style="width: 100%;"
                    :placeholder="$t('campaign.modal.edit.placeholder.startDate')"
                  />
                </a-form-item>
              </a-col>

              <a-col :xs="24" :md="12">
                <a-form-item
                  :label="$t('campaign.modal.details.label.endDate')"
                  :validate-status="errors.endDate ? 'error' : ''"
                  :help="errors.endDate"
                >
                  <a-date-picker
                    v-model:value="editingCampaign.endDate"
                    format="DD/MM/YYYY"
                    style="width: 100%;"
                    :placeholder="$t('campaign.modal.edit.placeholder.endDate')"
                  />
                </a-form-item>
              </a-col>

              <a-col :span="24">
                <a-form-item :label="$t('campaign.modal.details.label.targetAudience')">
                  <a-textarea
                    v-model:value="editingCampaign.targetAudience"
                    :rows="3"
                    :placeholder="$t('campaign.modal.edit.placeholder.targetAudience')"
                    allow-clear
                  />
                </a-form-item>
              </a-col>

              <a-col :span="24">
                <a-form-item :label="$t('campaign.modal.details.label.status')">
                  <a-tag :color="getStatusColor(editingCampaign.status)">
                    {{ getStatusLabel(editingCampaign.status) }}
                  </a-tag>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>
      </a-modal>
    </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import {
  PlusOutlined
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import dayjs from 'dayjs'

import CampaignTable from '@/components/CampaignTable.vue'

export default {
  name: "CampaignPage",
  components: {
    PlusOutlined,
    CampaignTable
  },
  data() {
    return {
      currentPage: 1,
      size: 5,
      showDetailModal: false,
      selectedCampaign: null,
      showEditModal: false,
      editingCampaign: null,
      searchQuery: '',
      filteredCampaigns: [],
      errors: {},
    }
  },
  computed: {
    ...mapState("campaign", {
      campaigns: state => state.campaigns,
      loading: state => state.loading,
      error: state => state.error,
      campaignTotalItems: state => state.totalItems
    }),
    displayedCampaigns() {
      if (!this.searchQuery.trim()) {
        return this.campaigns
      }
      return this.filteredCampaigns
    },

    totalItems() {
      return this.campaignTotalItems ?? this.campaigns.length
    },

    objectiveOptions() {
      return [
        { value: 'BRAND_AWARENESS', label: this.$t('campaign.objective.brandAwareness') },
        { value: 'REACH', label: this.$t('campaign.objective.reach') },
        { value: 'TRAFFIC', label: this.$t('campaign.objective.traffic') },
        { value: 'ENGAGEMENT', label: this.$t('campaign.objective.engagement') },
        { value: 'APP_INSTALLS', label: this.$t('campaign.objective.appInstalls') },
        { value: 'VIDEO_VIEWS', label: this.$t('campaign.objective.videoViews') },
        { value: 'LEAD_GENERATION', label: this.$t('campaign.objective.leadGeneration') },
        { value: 'CONVERSIONS', label: this.$t('campaign.objective.conversions') },
        { value: 'CATALOG_SALES', label: this.$t('campaign.objective.catalogSales') },
        { value: 'STORE_TRAFFIC', label: this.$t('campaign.objective.storeTraffic') }
      ]
    },

    budgetTypeOptions() {
      return [
        { value: 'DAILY', label: this.$t('campaign.create.form.budgetType.daily') },
        { value: 'LIFETIME', label: this.$t('campaign.create.form.budgetType.lifetime') }
      ]
    },

    accountCurrency() {
      return (process.env.VUE_APP_FACEBOOK_ACCOUNT_CURRENCY || 'USD').toUpperCase()
    }
  },
  async mounted() {
    await this.loadCampaigns()
  },
  methods: {
    ...mapActions("campaign", ["fetchCampaigns", "deleteCampaign", "updateCampaign"]),
    async loadCampaigns() {
      try {
        await this.fetchCampaigns({ page: this.currentPage - 1, size: this.size })
      } catch (error) {
        console.error("Failed to load campaigns:", error)
      }
    },
    onPageChange(page, pageSize) {
      this.currentPage = page;
      this.size = pageSize;
      this.loadCampaigns();
    },
    onPageSizeChange(current, size) {
      this.currentPage = 1;
      this.size = size;
      this.loadCampaigns();
    },

    showCampaignDetails(campaign) {
      this.selectedCampaign = campaign;
      this.showDetailModal = true;
    },
    showEditCampaignModal(campaign) {
      this.editingCampaign = this.prepareEditingCampaign(campaign);
      this.showEditModal = true;
      this.errors = {};
    },
    async saveEditedCampaign() {
      if (!this.editingCampaign) {
        return;
      }
      if (!this.validateEditCampaign()) {
        this.$message.error(this.$t('campaign.modal.edit.validation.fillRequired'));
        return;
      }
      try {
        const payload = this.buildCampaignPayload();
        await this.updateCampaign({
          campaignId: this.editingCampaign.id,
          campaignData: payload
        });
        this.showEditModal = false;
        this.$message.success(this.$t('campaign.modal.edit.success.updated'));
        await this.loadCampaigns();
      } catch (error) {
        console.error('Failed to save campaign:', error);
        this.$message.error(error.message || this.$t('campaign.modal.edit.error.updateFailed'));
      }
    },
    confirmDeleteCampaign(campaignId) {
      Modal.confirm({
        title: this.$t('campaign.modal.confirm.deleteTitle'),
        content: this.$t('campaign.modal.confirm.deleteContent'),
        okText: this.$t('common.action.delete'),
        okType: 'danger',
        cancelText: this.$t('common.action.cancel'),
        onOk: async () => {
          try {
            await this.deleteCampaign(campaignId);
            this.$message.success(this.$t('campaign.modal.confirm.success.deleted'));
            await this.loadCampaigns();
          } catch (error) {
            console.error('Failed to delete campaign:', error);
            this.$message.error(error.message || this.$t('campaign.modal.confirm.error.deleteFailed'));
          }
        },
        onCancel: () => {
          this.$message.info(this.$t('campaign.modal.confirm.info.cancelled'));
        }
      });
    },
    confirmCancelEdit() {
      Modal.confirm({
        title: this.$t('common.action.confirm'),
        content: this.$t('campaign.modal.confirmCancel.content'),
        okText: this.$t('common.action.yes'),
        cancelText: this.$t('common.action.no'),
        onOk: () => {
          this.showEditModal = false;
          this.$message.info(this.$t('campaign.modal.confirmCancel.info.cancelled'));
        }
      });
    },
    handleSearch() {
      if (!this.searchQuery.trim()) {
        this.filteredCampaigns = []
        return
      }
      const query = this.searchQuery.toLowerCase()
      this.filteredCampaigns = this.campaigns.filter(campaign => {
        return (
          campaign.name?.toLowerCase().includes(query) ||
          campaign.status?.toLowerCase().includes(query) ||
          (campaign.budget + '').includes(query)
        )
      })
    },
    handleLogout() {
      this.$store.dispatch('auth/logout')
    },
    validateEditCampaign() {
      const errors = {};
      const campaign = this.editingCampaign;

      if (!campaign.name || !campaign.name.trim()) {
        errors.name = this.$t('campaign.modal.edit.validation.nameRequired');
      }
      if (!campaign.objective) {
        errors.objective = this.$t('campaign.modal.edit.validation.objectiveRequired');
      }
      if (!campaign.budgetType) {
        errors.budgetType = this.$t('campaign.modal.edit.validation.budgetTypeRequired');
      }

      if (campaign.budgetType === 'DAILY') {
        if (!campaign.dailyBudget || campaign.dailyBudget <= 0) {
          errors.dailyBudget = this.$t('campaign.modal.edit.validation.dailyBudgetRequired');
        }
        campaign.totalBudget = null;
      } else if (campaign.budgetType === 'LIFETIME') {
        if (!campaign.totalBudget || campaign.totalBudget <= 0) {
          errors.totalBudget = this.$t('campaign.modal.edit.validation.totalBudgetRequired');
        }
        campaign.dailyBudget = null;
      }

      if (campaign.bidCap && campaign.bidCap <= 0) {
        errors.bidCap = this.$t('campaign.modal.edit.validation.bidCapPositive');
      }

      const startDate = campaign.startDate ? dayjs(campaign.startDate) : null;
      const endDate = campaign.endDate ? dayjs(campaign.endDate) : null;
      if (!startDate || !startDate.isValid()) {
        errors.startDate = this.$t('campaign.modal.edit.validation.startDateRequired');
      }
      if (!endDate || !endDate.isValid()) {
        errors.endDate = this.$t('campaign.modal.edit.validation.endDateRequired');
      } else if (startDate && endDate.isBefore(startDate)) {
        errors.endDate = this.$t('campaign.modal.edit.validation.endDateAfterStart');
      }

      this.errors = errors;
      return Object.keys(errors).length === 0;
    },
    getStatusColor(status) {
      const statusColors = {
        'DRAFT': 'default',
        'READY': 'processing',
        'EXPORTED': 'geekblue',
        'PENDING': 'warning',
        'ACTIVE': 'success',
        'PAUSED': 'warning',
        'COMPLETED': 'blue',
        'FAILED': 'error'
      };
      return statusColors[status] || 'default';
    },
    getStatusLabel(status) {
      const labels = {
        'DRAFT': this.$t('campaign.status.draft'),
        'READY': this.$t('campaign.status.ready'),
        'EXPORTED': this.$t('campaign.status.exported'),
        'ACTIVE': this.$t('campaign.status.active'),
        'PAUSED': this.$t('campaign.status.paused'),
        'COMPLETED': this.$t('campaign.status.completed'),
        'FAILED': this.$t('campaign.status.failed'),
        'PENDING': this.$t('campaign.status.pending')
      };
      return labels[status] || this.$t('campaign.status.unknown');
    },
    formatObjective(objective) {
      if (!objective) {
        return this.$t('common.default.notAvailable');
      }
      const labels = this.objectiveOptions.reduce((acc, item) => {
        acc[item.value] = item.label;
        return acc;
      }, {});
      return labels[objective] || objective;
    },
    formatBudgetType(type) {
      if (type === 'DAILY') {
        return this.$t('campaign.create.form.budgetType.daily');
      }
      if (type === 'LIFETIME') {
        return this.$t('campaign.create.form.budgetType.lifetime');
      }
      return this.$t('common.default.notAvailable');
    },
    formatCurrency(value) {
      if (value === null || value === undefined || value === '') {
        return this.$t('common.default.notAvailable');
      }
      try {
        return new Intl.NumberFormat(
          this.$i18n?.locale === 'vi' ? 'vi-VN' : 'en-US',
          { style: 'currency', currency: this.accountCurrency }
        ).format(value);
      } catch (error) {
        console.warn('Failed to format currency:', error);
        return value;
      }
    },
    formatDateOnly(dateString) {
      if (!dateString) {
        return this.$t('campaign.date.notAvailable');
      }
      const parsed = dayjs(dateString);
      if (!parsed.isValid()) {
        console.warn('Invalid date:', dateString);
        return this.$t('campaign.date.invalid');
      }
      return parsed.format('DD/MM/YYYY');
    },
    formatDateTime(dateString) {
      if (!dateString) {
        return this.$t('campaign.date.notAvailable');
      }
      const parsed = dayjs(dateString);
      if (!parsed.isValid()) {
        console.warn('Invalid date:', dateString);
        return this.$t('campaign.date.invalid');
      }
      return parsed.format('DD/MM/YYYY HH:mm');
    },
    prepareEditingCampaign(campaign) {
      return {
        id: campaign.id,
        name: campaign.name || '',
        status: campaign.status,
        objective: campaign.objective || null,
        budgetType: campaign.budgetType || 'DAILY',
        dailyBudget: campaign.dailyBudget ?? null,
        totalBudget: campaign.totalBudget ?? null,
        targetAudience: campaign.targetAudience || '',
        bidCap: campaign.bidCap ?? null,
        startDate: campaign.startDate ? dayjs(campaign.startDate) : null,
        endDate: campaign.endDate ? dayjs(campaign.endDate) : null
      };
    },
    buildCampaignPayload() {
      if (!this.editingCampaign) {
        return null;
      }
      const startDate = this.editingCampaign.startDate ? dayjs(this.editingCampaign.startDate) : null;
      const endDate = this.editingCampaign.endDate ? dayjs(this.editingCampaign.endDate) : null;
      return {
        name: this.editingCampaign.name?.trim(),
        objective: this.editingCampaign.objective,
        budgetType: this.editingCampaign.budgetType,
        dailyBudget: this.editingCampaign.budgetType === 'DAILY'
          ? Number(this.editingCampaign.dailyBudget) || null
          : null,
        totalBudget: this.editingCampaign.budgetType === 'LIFETIME'
          ? Number(this.editingCampaign.totalBudget) || null
          : null,
        targetAudience: this.editingCampaign.targetAudience || '',
        bidCap: this.editingCampaign.bidCap !== null && this.editingCampaign.bidCap !== undefined
          ? Number(this.editingCampaign.bidCap)
          : null,
        startDate: startDate && startDate.isValid() ? startDate.format('YYYY-MM-DD') : null,
        endDate: endDate && endDate.isValid() ? endDate.format('YYYY-MM-DD') : null
      };
    },
    viewCampaignAds(campaign) {
      this.$router.push(`/campaign/${campaign.id}/ads`);
    }
  }
}
</script>

<style scoped>
.campaign-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* Mobile Responsiveness - Phase 2 Implementation */
@media (max-width: 768px) {
  .campaign-page {
    padding: 12px;
  }

  .ant-page-header {
    padding: 12px;
  }

  .ant-page-header-heading-title {
    font-size: 1.5rem;
  }

  .ant-page-header-heading-sub-title {
    font-size: 0.9rem;
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

  .ant-form-item {
    margin-bottom: 16px;
  }
}

@media (max-width: 480px) {
  .campaign-page {
    padding: 8px;
  }

  .ant-page-header {
    padding: 8px;
  }

  .ant-page-header-heading-title {
    font-size: 1.25rem;
  }

  .ant-page-header-heading-sub-title {
    font-size: 0.825rem;
  }

  .ant-form-item {
    margin-bottom: 12px;
  }

  .ant-btn {
    width: 100%;
    margin-bottom: 8px;
  }
}
</style>
