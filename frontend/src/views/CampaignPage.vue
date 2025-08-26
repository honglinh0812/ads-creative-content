<template>
  <div class="campaign-page">
    <div class="campaign-content">

        <!-- Page Header -->
        <a-page-header
          title="Campaigns"
          sub-title="Manage your advertising campaigns"
          class="mb-8"
        >
          <template #extra>
            <router-link to="/campaign/create">
              <a-button type="primary" size="large">
                <template #icon>
                  <PlusOutlined />
                </template>
                New Campaign
              </a-button>
            </router-link>
          </template>
        </a-page-header>

        <!-- Error State -->
        <div v-if="error" class="mb-6">
          <a-alert
            type="error"
            :message="'Error loading campaign list'"
            :description="error"
            show-icon
            closable
          >
            <template #action>
              <a-button size="small" type="primary" @click="loadCampaigns">
                Try again
              </a-button>
            </template>
          </a-alert>
        </div>

        <!-- Empty State -->
        <div v-else-if="campaigns.length === 0 && !loading">
          <a-empty
            description="No campaign found"
          >
            <template #image>
              <FolderOpenOutlined style="font-size: 64px; color: #d9d9d9;" />
            </template>
            <a-button type="primary" size="large">
              <template #icon>
                <PlusOutlined />
              </template>
              <router-link to="/campaign/create" style="color: inherit; text-decoration: none;">
                Create First Campaign
              </router-link>
            </a-button>
          </a-empty>
        </div>

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
  MenuOutlined, 
  PlusOutlined, 
  FolderOpenOutlined
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'

import CampaignTable from '@/components/CampaignTable.vue'

export default {
  name: "CampaignPage",
  components: {
    MenuOutlined,
    PlusOutlined,
    FolderOpenOutlined,
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
</style>