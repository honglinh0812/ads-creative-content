<template>
  <div class="page-container">
    <!-- Standardized Page Header -->
    <div class="page-header-standard">
      <div class="page-header-content">
        <h1 class="page-title-standard">Campaigns</h1>
        <p class="page-subtitle-standard">
          Manage your advertising campaigns and monitor their performance
        </p>
      </div>

      <div class="page-actions-standard">
        <router-link to="/campaign/create">
          <button class="btn-primary-standard">
            <PlusOutlined />
            New Campaign
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
        @view-details="showCampaignDetails"
        @edit-campaign="showEditCampaignModal"
        @delete-campaign="confirmDeleteCampaign"
        @view-ads="viewCampaignAds"
      />
    </div>

      <!-- Campaign Detail Modal -->
      <a-modal
        v-model:open="showDetailModal"
        title="Campaign Details"
        :width="800"
        :footer="null"
      >
        <div v-if="selectedCampaign">
          <a-descriptions :column="1" bordered>
            <a-descriptions-item label="Name">
              {{ selectedCampaign.name }}
            </a-descriptions-item>
            <a-descriptions-item label="Budget">
              {{ selectedCampaign.budget }}
            </a-descriptions-item>
            <a-descriptions-item label="Status">
              <a-tag :color="getStatusColor(selectedCampaign.status)">
                {{ selectedCampaign.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="Created Date">
              {{ formatDate(selectedCampaign.createdDate) }}
            </a-descriptions-item>
            <a-descriptions-item label="Total Ads">
              {{ selectedCampaign.totalAds || 0 }}
            </a-descriptions-item>
          </a-descriptions>
        </div>
        <template #footer>
          <a-button type="primary" @click="showDetailModal = false">OK</a-button>
        </template>
      </a-modal>

      <!-- Edit Campaign Modal -->
      <a-modal
        v-model:open="showEditModal"
        title="Edit Campaign"
        :width="800"
        @ok="saveEditedCampaign"
        @cancel="confirmCancelEdit"
      >
        <div v-if="editingCampaign">
          <a-form layout="vertical">
            <a-form-item
              label="Name"
              :validate-status="errors.name ? 'error' : ''"
              :help="errors.name"
            >
              <a-input
                v-model:value="editingCampaign.name"
                placeholder="Enter campaign name"
                @blur="validateEditCampaign"
              />
            </a-form-item>
            <a-form-item label="Budget">
              <a-input
                v-model:value="editingCampaign.budget"
                placeholder="Enter budget"
              />
            </a-form-item>
            <a-form-item label="Status">
              <a-select
                v-model:value="editingCampaign.status"
                placeholder="Select status"
                style="width: 100%;"
              >
                <a-select-option value="DRAFT">DRAFT</a-select-option>
                <a-select-option value="ACTIVE">ACTIVE</a-select-option>
                <a-select-option value="PAUSED">PAUSED</a-select-option>
                <a-select-option value="COMPLETED">COMPLETED</a-select-option>
                <a-select-option value="FAILED">FAILED</a-select-option>
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
    ...mapState("campaign", [
      "campaigns",
      "totalItems",
      "totalPages",
      "loading",
      "error"
    ]),
    displayedCampaigns() {
      if (!this.searchQuery.trim()) {
        return this.campaigns
      }
      return this.filteredCampaigns
    },

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
        this.errors.name = 'Name is required';
        isValid = false;
      }
      if (!isValid) {
        this.$message.error('Please fill in all required fields.');
        return;
      }
      try {
        const { id, name, budget, status } = this.editingCampaign;
        await this.updateCampaign({
          campaignId: id,
          campaignData: { name, budget, status }
        });
        this.showEditModal = false;
        this.$message.success('Update campaign successfully');
        await this.loadCampaigns();
      } catch (error) {
        console.error('Failed to save campaign:', error);
        this.$message.error(error.message || 'Update campaign failed');
      }
    },
    confirmDeleteCampaign(campaignId) {
      Modal.confirm({
        title: 'Confirm delete',
        content: 'Are you sure you want to delete this campaign? This action cannot be undone.',
        okText: 'Delete',
        okType: 'danger',
        cancelText: 'Cancel',
        onOk: async () => {
          try {
            await this.deleteCampaign(campaignId);
            this.$message.success('Delete campaign successfully');
            await this.loadCampaigns();
          } catch (error) {
            console.error('Failed to delete campaign:', error);
            this.$message.error(error.message || 'Delete campaign failed');
          }
        },
        onCancel: () => {
          this.$message.info('Cancel delete campaign');
        }
      });
    },
    confirmCancelEdit() {
      Modal.confirm({
        title: 'Confirm',
        content: 'Are you sure you want to cancel? The changes you made will be lost.',
        okText: 'Yes',
        cancelText: 'No',
        onOk: () => {
          this.showEditModal = false;
          this.$message.info('Cancel changes');
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
      if (!dateString) return ""
      const date = new Date(dateString)
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
        this.errors.name = 'Name is required';
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