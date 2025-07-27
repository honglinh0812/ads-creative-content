<template>
  <div :class="['page-wrapper', { 'sidebar-closed': !sidebarOpen }]">
    <AppSidebar :sidebarOpen="sidebarOpen" @toggle="toggleSidebar" @logout="handleLogout" />
    <div class="app-layout">
      <div class="main-content-wrapper" :style="mainContentStyle">
        <!-- Mobile Header -->
        <div class="mobile-header lg:hidden">
          <button 
            @click="toggleSidebar" 
            class="btn btn-ghost"
            aria-label="Toggle menu"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <h1 class="text-lg font-semibold text-secondary-900">Campaigns</h1>
        </div>

        <!-- Page Header -->
        <div class="page-header flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 gap-4">
          <div>
            <h1 class="text-3xl font-bold text-secondary-900">Campaigns</h1>
            <p class="text-secondary-600">Manage your advertising campaigns</p>
          </div>
          <router-link to="/campaign/create" class="btn btn-primary btn-lg flex items-center gap-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
            </svg>
            New Campaign
          </router-link>
        </div>

        <!-- Search Bar -->
        <div class="mb-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
          <div class="relative w-full sm:w-80">
            <input 
              v-model="searchQuery"
              type="text" 
              placeholder="Search campaign..."
              class="form-input pl-10 w-full h-12 text-lg rounded-lg border border-gray-200 focus:border-primary focus:ring-2 focus:ring-primary"
              @input="handleSearch"
            />
            <svg class="w-5 h-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <LoadingSkeleton v-for="i in 8" :key="i" type="card" :width="'100%'" :height="'180px'" />
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="alert alert-error mb-6">
          <div class="alert-title">Error loading campaign list</div>
          <div class="alert-message">{{ error }}</div>
          <button @click="loadCampaigns" class="btn btn-sm btn-secondary mt-3">
            Try again
          </button>
        </div>

        <!-- Empty State -->
        <div v-else-if="campaigns.length === 0" class="card text-center py-12">
          <div class="card-body">
            <h3 class="text-xl font-semibold text-secondary-900 mb-2">No campaign found</h3>
            <p class="text-secondary-600 mb-6">Create your first campaign to start advertising</p>
            <router-link to="/campaign/create" class="btn btn-primary btn-lg">
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
              </svg>
              Create First Campaign
            </router-link>
          </div>
        </div>

        <!-- Campaigns List -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          <div v-for="campaign in displayedCampaigns" :key="campaign.id" class="group">
            <div class="card hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2 border-0 bg-white shadow-lg group-hover:shadow-3xl overflow-hidden rounded-2xl">
              <div class="card-body p-0">
                <div class="p-4">
                  <div class="flex items-start justify-between mb-3">
                    <div class="flex-1">
                      <h3 class="text-base font-semibold text-gray-900 line-clamp-2 mb-1">{{ campaign.name }}</h3>
                      <p class="text-xs text-gray-500">Budget: {{ campaign.budget || 'N/A' }}</p>
                    </div>
                  </div>
                  <div class="mb-3">
                    <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {{ campaign.status || 'Unknown' }}
                    </span>
                  </div>
                  <div class="grid grid-cols-2 gap-3 mb-4">
                    <div>
                      <p class="text-xs text-gray-500 mb-1 font-medium">Created Date</p>
                      <p class="text-sm font-semibold text-gray-900">{{ formatDate(campaign.createdDate) }}</p>
                    </div>
                    <div>
                      <p class="text-xs text-gray-500 mb-1 font-medium">Total Ads</p>
                      <p class="text-sm font-semibold text-gray-900">{{ campaign.totalAds || 0 }}</p>
                    </div>
                  </div>
                  <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                    <div class="flex gap-2">
                      <button @click="showEditCampaignModal(campaign)" class="btn btn-xs btn-outline-secondary hover:bg-gray-100 transition-colors">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                        </svg>
                        Edit
                      </button>
                      <button @click="confirmDeleteCampaign(campaign.id)" class="btn btn-xs btn-outline-error hover:bg-red-50 transition-colors">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                        Delete
                      </button>
                    </div>
                    <router-link :to="`/campaigns/${campaign.id}`" class="btn btn-xs btn-primary group-hover:scale-105 transition-transform">
                      View Details
                    </router-link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex justify-center mt-8">
          <Paginator :rows="size" :totalRecords="totalItems" :rowsPerPageOptions="[5]" @page="onPageChange"></Paginator>
        </div>
      </div>

      <!-- Campaign Detail Modal -->
      <Dialog v-model:visible="showDetailModal" modal header="Campaign Details" :style="{ width: '90vw', maxWidth: '1200px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">
        <div v-if="selectedCampaign">
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
            <p class="text-secondary-900">{{ selectedCampaign.name }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Budget:</label>
            <p class="text-secondary-900">{{ selectedCampaign.budget }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Status:</label>
            <p class="text-secondary-900">{{ selectedCampaign.status }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Created Date:</label>
            <p class="text-secondary-900">{{ formatDate(selectedCampaign.createdDate) }}</p>
          </div>
          <div class="field mb-4">
            <label class="block text-sm font-medium text-secondary-700 mb-1">Total Ads:</label>
            <p class="text-secondary-900">{{ selectedCampaign.totalAds || 0 }}</p>
          </div>
        </div>
        <template #footer>
          <button @click="showDetailModal = false" class="btn btn-primary">OK</button>
        </template>
      </Dialog>

      <!-- Edit Campaign Modal -->
      <Dialog v-model:visible="showEditModal" modal header="Edit Campaign" :style="{ width: '90vw', maxWidth: '1200px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">
        <div v-if="editingCampaign">
          <div class="field mb-4">
            <label for="editCampaignName" class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
            <InputText id="editCampaignName" v-model="editingCampaign.name" class="w-full" :class="{ 'border-red-500': errors.name }" @blur="validateEditCampaign()" />
            <p v-if="errors.name" class="form-error flex items-center gap-2 mt-1">
              <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {{ errors.name }}
            </p>
          </div>
          <div class="field mb-4">
            <label for="editCampaignBudget" class="block text-sm font-medium text-secondary-700 mb-1">Budget:</label>
            <InputText id="editCampaignBudget" v-model="editingCampaign.budget" class="w-full" />
          </div>
          <div class="field mb-4">
            <label for="editCampaignStatus" class="block text-sm font-medium text-secondary-700 mb-1">Status:</label>
            <Dropdown id="editCampaignStatus" v-model="editingCampaign.status" :options="['DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED']" placeholder="Select status" class="w-full" />
          </div>
        </div>
        <template #footer>
          <button @click="confirmCancelEdit" class="btn btn-secondary">Cancel</button>
          <button @click="saveEditedCampaign" class="btn btn-primary">Save</button>
        </template>
      </Dialog>

      <ConfirmDialog></ConfirmDialog>
      <Toast />
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import Paginator from 'primevue/paginator';
import Dialog from 'primevue/dialog';
import ConfirmDialog from 'primevue/confirmdialog';
import Toast from 'primevue/toast';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import AppSidebar from '@/components/AppSidebar.vue'

export default {
  name: "CampaignPage",
  components: {
    Paginator,
    Dialog,
    ConfirmDialog,
    Toast,
    InputText,
    Dropdown,
    LoadingSkeleton,
    AppSidebar
  },
  data() {
    return {
      sidebarOpen: true,
      page: 0,
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
    mainContentStyle() {
      return this.sidebarOpen ? { marginLeft: '240px' } : { marginLeft: '0' }
    }
  },
  async mounted() {
    await this.loadCampaigns()
  },
  created() {
    this.$confirm = this.$root.$confirm;
  },
  methods: {
    ...mapActions("campaign", ["fetchCampaigns", "deleteCampaign", "updateCampaign"]),
    async loadCampaigns() {
      try {
        await this.fetchCampaigns({ page: this.page, size: this.size })
      } catch (error) {
        console.error("Failed to load campaigns:", error)
      }
    },
    onPageChange(event) {
      this.page = event.page;
      this.size = event.rows;
      this.loadCampaigns();
    },
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
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
        this.$store.dispatch('toast/showToast', {
          type: 'error',
          message: 'Please fill in all required fields.'
        });
        return;
      }
      try {
        const { id, name, budget, status } = this.editingCampaign;
        await this.updateCampaign({
          campaignId: id,
          campaignData: { name, budget, status }
        });
        this.showEditModal = false;
        this.$store.dispatch('toast/showToast', {
          type: 'success',
          message: 'Update campaign successfully'
        });
        await this.loadCampaigns();
      } catch (error) {
        console.error('Failed to save campaign:', error);
        this.$store.dispatch('toast/showToast', {
          type: 'error',
          message: error.message || 'Update campaign failed'
        });
      }
    },
    confirmDeleteCampaign(campaignId) {
      this.$confirm.require({
        message: 'Are you sure you want to delete this campaign? This action cannot be undone.',
        header: 'Confirm delete',
        icon: 'pi pi-info-circle',
        acceptClass: 'p-button-danger',
        accept: async () => {
          try {
            await this.deleteCampaign(campaignId);
            this.$store.dispatch('toast/showToast', {
              type: 'success',
              message: 'Delete campaign successfully'
            });
            await this.loadCampaigns();
          } catch (error) {
            console.error('Failed to delete campaign:', error);
            this.$store.dispatch('toast/showToast', {
              type: 'error',
              message: error.message || 'Delete campaign failed'
            });
          }
        },
        reject: () => {
          this.$store.dispatch('toast/showToast', {
            type: 'info',
            message: 'Cancel delete campaign'
          });
        }
      });
    },
    confirmCancelEdit() {
      this.$confirm.require({
        message: 'Are you sure you want to cancel? The changes you made will be lost.',
        header: 'Confirm',
        icon: 'pi pi-exclamation-triangle',
        accept: () => {
          this.showEditModal = false;
          this.$store.dispatch('toast/showToast', {
            type: 'info',
            message: 'Cancel changes'
          });
        },
        reject: () => {
        // Do nothing, keep the modal
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
    }
  }
}
</script> 

<style scoped>
.page-wrapper.sidebar-closed .main-content-wrapper {
  margin-left: 0 !important;
}
</style> 