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

    <!-- Creative Error State -->
    <CreativeEmptyState
      v-if="error"
      variant="loading-failed"
      :custom-message="error"
      action-text="Try Again"
      :action-handler="loadCampaigns"
    />

    <!-- Creative Empty State -->
    <CreativeEmptyState
      v-else-if="campaigns.length === 0 && !loading"
      variant="no-campaigns"
      action-text="Create First Campaign"
      :action-handler="() => $router.push('/campaign/create')"
    />

    <!-- Campaign Table with Advanced Filtering -->
    <div v-else>
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
        :width="800"
        :footer="null"
      >
        <div v-if="selectedCampaign">
          <a-descriptions :column="1" bordered>
            <a-descriptions-item :label="$t('campaign.modal.details.label.name')">
              {{ selectedCampaign.name }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.budget')">
              {{ selectedCampaign.budget }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.status')">
              <a-tag :color="getStatusColor(selectedCampaign.status)">
                {{ selectedCampaign.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.createdDate')">
              {{ formatDate(selectedCampaign.createdDate) }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('campaign.modal.details.label.totalAds')">
              {{ selectedCampaign.totalAds || 0 }}
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
        :width="800"
        @ok="saveEditedCampaign"
        @cancel="confirmCancelEdit"
      >
        <div v-if="editingCampaign">
          <a-form layout="vertical">
            <a-form-item
              :label="$t('campaign.modal.details.label.name')"
              :validate-status="errors.name ? 'error' : ''"
              :help="errors.name"
            >
              <a-input
                v-model:value="editingCampaign.name"
                :placeholder="$t('campaign.modal.edit.placeholder.name')"
                @blur="validateEditCampaign"
              />
            </a-form-item>
            <a-form-item :label="$t('campaign.modal.details.label.budget')">
              <a-input
                v-model:value="editingCampaign.budget"
                :placeholder="$t('campaign.modal.edit.placeholder.budget')"
              />
            </a-form-item>
            <a-form-item :label="$t('campaign.modal.details.label.status')">
              <a-select
                v-model:value="editingCampaign.status"
                :placeholder="$t('campaign.modal.edit.placeholder.status')"
                style="width: 100%;"
              >
                <a-select-option value="DRAFT">{{ $t('campaign.status.draft') }}</a-select-option>
                <a-select-option value="ACTIVE">{{ $t('campaign.status.active') }}</a-select-option>
                <a-select-option value="PAUSED">{{ $t('campaign.status.paused') }}</a-select-option>
                <a-select-option value="COMPLETED">{{ $t('campaign.status.completed') }}</a-select-option>
                <a-select-option value="FAILED">{{ $t('campaign.status.failed') }}</a-select-option>
              </a-select>
            </a-form-item>
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

import CampaignTable from '@/components/CampaignTable.vue'
import CreativeEmptyState from '@/components/ui/CreativeEmptyState.vue'

export default {
  name: "CampaignPage",
  components: {
    PlusOutlined,
    CampaignTable,
    CreativeEmptyState
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
      this.editingCampaign = { ...campaign };
      this.showEditModal = true;
      this.errors = {};
    },
    async saveEditedCampaign() {
      this.errors = {};
      let isValid = true;
      if (!this.editingCampaign.name) {
        this.errors.name = this.$t('campaign.modal.edit.validation.nameRequired');
        isValid = false;
      }
      if (!isValid) {
        this.$message.error(this.$t('campaign.modal.edit.validation.fillRequired'));
        return;
      }
      try {
        const { id, name, budget, status } = this.editingCampaign;
        await this.updateCampaign({
          campaignId: id,
          campaignData: { name, budget, status }
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
    formatDate(dateString) {
      if (!dateString) return "Not available"

      const date = new Date(dateString)
      if (isNaN(date.getTime())) {
        console.warn('Invalid date:', dateString)
        return "Invalid date"
      }

      return date.toLocaleDateString("vi-VN", {
        month: "short",
        day: "numeric",
        year: "numeric"
      })
    },
    handleLogout() {
      this.$store.dispatch('auth/logout')
    },
    validateEditCampaign() {
      if (!this.editingCampaign.name) {
        this.errors.name = this.$t('campaign.modal.edit.validation.nameRequired');
      } else {
        this.errors.name = '';
      }
    },
    getStatusColor(status) {
      const statusColors = {
        'DRAFT': 'default',
        'ACTIVE': 'success',
        'PAUSED': 'warning',
        'COMPLETED': 'blue',
        'FAILED': 'error'
      };
      return statusColors[status] || 'default';
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
