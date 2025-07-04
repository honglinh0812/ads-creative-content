<template>
  <div class="app-layout">
    <div class="flex flex-1">
      <!-- Sidebar -->
      <aside class="app-sidebar" :class="{ open: sidebarOpen }">
        <div class="sidebar-header">
          <router-link to="/dashboard" class="flex items-center gap-3 px-6 py-4">
            <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
            </div>
            <h1 class="text-lg font-bold text-secondary-900">Ads Quick Content</h1>
          </router-link>
        </div>

        <nav class="nav">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"></path>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5a2 2 0 012-2h4a2 2 0 012 2v6H8V5z"></path>
            </svg>
            <span class="nav-text">Dashboard</span>
          </router-link>
          
          <router-link to="/campaigns" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
            </svg>
            <span class="nav-text">Campaigns</span>
          </router-link>
          
          <router-link to="/ads" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
            </svg>
            <span class="nav-text">Ads</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="px-6 py-4">
            <router-link to="/dashboard" class="btn btn-sm btn-ghost w-full">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
              Back to Dashboard
            </router-link>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="app-main flex-1">
        <div class="content-wrapper">
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
            <h1 class="text-lg font-semibold text-secondary-900">Ads</h1>
          </div>

          <div class="page-header">
            <h1 class="page-title">Ads</h1>
            <p class="page-description">Manage your advertising content</p>
          </div>

          <!-- Loading State -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <div class="spinner spinner-lg"></div>
            <span class="ml-3 text-secondary-600">Loading ads...</span>
          </div>

          <!-- Error State -->
          <div v-else-if="error" class="alert alert-error mb-6">
            <div class="alert-title">Error loading ads</div>
            <div class="alert-message">{{ error }}</div>
            <button @click="loadAds" class="btn btn-sm btn-secondary mt-3">
              Try Again
            </button>
          </div>

          <!-- Ads Content -->
          <div v-else>
            <!-- Create Ad Button -->
            <div class="mb-6">
              <router-link to="/ad/create" class="btn btn-primary">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                </svg>
                Create Ad
              </router-link>
            </div>

            <!-- Empty State -->
            <div v-if="ads.length === 0" class="card">
              <div class="card-body text-center py-12">
                <svg class="w-16 h-16 text-neutral-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                </svg>
                <h3 class="text-xl font-semibold text-secondary-900 mb-2">No ads yet</h3>
                <p class="text-secondary-600 mb-6">Create your first ad to start advertising</p>
                <router-link to="/ad/create" class="btn btn-primary">
                  Create Your First Ad
                </router-link>
              </div>
            </div>

            <!-- Ads List -->
            <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
              <div v-for="ad in ads" :key="ad.id" class="card hover:shadow-lg transition-shadow duration-200 flex flex-col">
                <div class="card-body flex flex-col justify-between h-full p-3">
                  <div>
                    <!-- Ad Image Preview -->
                    <div v-if="ad.imageUrl" class="mb-2">
                      <div class="aspect-video bg-neutral-100 rounded-lg overflow-hidden">
                        <img 
                          :src="ad.imageUrl" 
                          :alt="ad.name"
                          class="w-full h-full object-cover"
                          @error="handleImageError"
                        />
                      </div>
                    </div>
                    
                    <div class="flex items-start justify-between mb-2">
                      <div class="flex-1">
                        <h3 class="text-base font-semibold text-secondary-900">{{ ad.name }}</h3>
                        <p class="text-xs text-secondary-600">{{ ad.campaignName || 'No Campaign' }}</p>
                      </div>
                      <span :class="getStatusBadgeClass(ad.status)">
                        {{ ad.status }}
                      </span>
                    </div>
                    
                    <div class="mb-2">
                      <span class="badge badge-neutral">{{ ad.adType?.replace('_', ' ') || 'Unknown Type' }}</span>
                    </div>

                    <!-- Ad Content Preview -->
                    <div v-if="ad.headline || ad.description" class="mb-2 p-1 bg-neutral-50 rounded-lg">
                      <div v-if="ad.headline" class="mb-1">
                        <p class="text-xs text-secondary-500 mb-0">Headline</p>
                        <p class="text-sm font-medium text-secondary-900">{{ ad.headline }}</p>
                      </div>
                      <div v-if="ad.description">
                        <p class="text-xs text-secondary-500 mb-0">Description</p>
                        <p class="text-sm text-secondary-700">{{ ad.description }}</p>
                      </div>
                    </div>

                    <div class="grid grid-cols-2 gap-2 mb-2">
                      <div>
                        <p class="text-xs text-secondary-500 mb-1">Call to Action</p>
                        <p class="text-sm font-medium text-secondary-900">{{ ad.callToAction || 'None' }}</p>
                      </div>
                      <div>
                        <p class="text-xs text-secondary-500 mb-1">Created</p>
                        <p class="text-sm font-medium text-secondary-900">{{ formatDate(ad.createdDate) }}</p>
                      </div>
                    </div>
                  </div>
                  
                  <div class="flex items-center justify-between pt-2 border-t border-neutral-200">
                    <div class="flex gap-1">
                      <button @click="showEditAdModal(ad)" class="btn btn-xs btn-outline-secondary">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                        </svg>
                        Edit
                      </button>
                      <button @click="confirmDeleteAd(ad.id)" class="btn btn-xs btn-outline-error">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                        Delete
                      </button>
                    </div>
                    <button @click="showAdDetails(ad)" class="btn btn-xs btn-primary">
                      View Details
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Pagination -->
            <div v-if="totalPages > 1" class="flex justify-center mt-8">
              <Paginator :rows="size" :totalRecords="totalItems" :rowsPerPageOptions="[5, 10, 20]" @page="onPageChange"></Paginator>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Ad Detail Modal -->
    <Dialog v-model:visible="showDetailModal" modal header="Ad Details" :style="{ width: '90vw', maxWidth: '1200px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">
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
          <label class="block text-sm font-medium text-secondary-700 mb-1">Image URL:</label>
          <img v-if="selectedAd.imageUrl" :src="selectedAd.imageUrl" class="w-full h-auto object-cover rounded-lg" />
          <p v-else class="text-secondary-900">N/A</p>
        </div>
        <div class="field mb-4">
          <label class="block text-sm font-medium text-secondary-700 mb-1">Created Date:</label>
          <p class="text-secondary-900">{{ formatDate(selectedAd.createdDate) }}</p>
        </div>
      </div>
      <template #footer>
        <button @click="showDetailModal = false" class="btn btn-primary">OK</button>
      </template>
    </Dialog>

    <!-- Edit Ad Modal -->
    <Dialog v-model:visible="showEditModal" modal header="Edit Ad" :style="{ width: '90vw', maxWidth: '1200px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">   <div v-if="editingAd">
        <div class="field mb-4">
          <label for="editAdName" class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
          <InputText id="editAdName" v-model="editingAd.name" class="w-full" />
        </div>
        <div class="field mb-4">
          <label for="editAdHeadline" class="block text-sm font-medium text-secondary-700 mb-1">Headline:</label>
          <InputText id="editAdHeadline" v-model="editingAd.headline" class="w-full" />
        </div>
        <div class="field mb-4">
          <label for="editAdDescription" class="block text-sm font-medium text-secondary-700 mb-1">Description:</label>
          <Textarea id="editAdDescription" v-model="editingAd.description" rows="3" class="w-full" />
        </div>
        <div class="field mb-4">
          <label for="editAdPrimaryText" class="block text-sm font-medium text-secondary-700 mb-1">Primary Text:</label>
          <Textarea id="editAdPrimaryText" v-model="editingAd.primaryText" rows="3" class="w-full" />
        </div>
        <div class="field mb-4">
          <label for="editAdCallToAction" class="block text-sm font-medium text-secondary-700 mb-1">Call to Action:</label>
          <Dropdown id="editAdCallToAction" v-model="editingAd.callToAction" :options="['SHOP_NOW', 'LEARN_MORE', 'SIGN_UP', 'DOWNLOAD', 'CONTACT_US']" placeholder="Select Call to Action" class="w-full" />
        </div>
        <div class="field mb-4">
          <label for="editAdStatus" class="block text-sm font-medium text-secondary-700 mb-1">Status:</label>
          <Dropdown id="editAdStatus" v-model="editingAd.status" :options="['DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED']" placeholder="Select Status" class="w-full" />
        </div>
      </div>
      <template #footer>
        <button @click="confirmCancelEdit" class="btn btn-secondary">Cancel</button>
        <button @click="saveEditedAd" class="btn btn-primary">Save</button>
      </template>
    </Dialog>

    <ConfirmDialog></ConfirmDialog>
    <Toast />
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
import Textarea from 'primevue/textarea';

export default {
  name: "Ads",
  components: {
    Paginator,
    Dialog,
    ConfirmDialog,
    Toast,
    InputText,
    Dropdown,
    Textarea
  },
  data() {
    return {
      sidebarOpen: false,
      page: 0,
      size: 5,
      showDetailModal: false,
      selectedAd: null,
      showEditModal: false,
      editingAd: null
    }
  },
  computed: {
    ...mapState("ad", [
      "ads",
      "loading",
      "error",
      "totalItems",
      "totalPages",
      "currentPage",
    ]),
  },
  async mounted() {
    await this.loadAds()
  },
  created() {
    this.$confirm = this.$root.$confirm; // Ensure $confirm is available
  },
  methods: {
    ...mapActions("ad", ["fetchAds", "deleteAd", "updateAd"]),
    
    async loadAds() {
      try {
        await this.fetchAds({ page: this.page, size: this.size })
      } catch (error) {
        console.error("Failed to load ads:", error)
      }
    },
    
    onPageChange(event) {
      this.page = event.page;
      this.size = event.rows;
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
      this.editingAd = { ...ad }; // Create a copy to avoid direct mutation
      this.showEditModal = true;
    },

    async saveEditedAd() {
      try {
        await this.updateAd({
          adId: this.editingAd.id,
          adData: this.editingAd,
        });
        this.showEditModal = false;
        this.$toast.add({ severity: 'success', summary: 'Success', detail: 'Ad updated successfully', life: 3000 });
        await this.loadAds(); // Refresh list after update
      } catch (error) {
        console.error("Failed to save ad:", error);
        this.$toast.add({ severity: 'error', summary: 'Error', detail: error.message || 'Failed to update ad', life: 3000 });
      }
    },
    confirmDeleteAd(adId) {
      this.$confirm.require({
        message: 'Do you really want to delete this ad? This action cannot be undone.',
        header: 'Delete Confirmation',
        icon: 'pi pi-info-circle',
        acceptClass: 'p-button-danger',
        accept: async () => {
          try {
            await this.deleteAd(adId);
            this.$toast.add({ severity: 'success', summary: 'Success', detail: 'Ad deleted successfully', life: 3000 });
            await this.loadAds(); // Refresh list after delete
          } catch (error) {
            console.error('Failed to delete ad:', error);
            this.$toast.add({ severity: 'error', summary: 'Error', detail: error.message || 'Failed to delete ad', life: 3000 });
          }
        },
        reject: () => {
          this.$toast.add({ severity: 'info', summary: 'Cancelled', detail: 'Delete operation cancelled', life: 3000 });
        }
      });
    },
    confirmCancelEdit() {
      this.$confirm.require({
        message: 'Are you sure you want to cancel? Your unsaved changes will be lost.',
        header: 'Confirmation',
        icon: 'pi pi-exclamation-triangle',
        accept: () => {
          this.showEditModal = false;
          this.$toast.add({ severity: 'info', summary: 'Cancelled', detail: 'Changes discarded', life: 3000 });
        },
        reject: () => {
          // Do nothing, stay in modal
        }
      });
    },
    
    getStatusBadgeClass(status) {
      const baseClass = "badge badge-sm"
      switch (status?.toLowerCase()) {
        case "active":
          return `${baseClass} badge-success`
        case "paused":
          return `${baseClass} badge-warning`
        case "draft":
          return `${baseClass} badge-neutral`
        case "completed":
          return `${baseClass} badge-info`
        case "failed":
          return `${baseClass} badge-error`
        default:
          return `${baseClass} badge-neutral`
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
    }
  }
}
</script>

<style scoped>
.mobile-header {
  @apply flex items-center gap-4 p-4 border-b border-neutral-200 bg-white;
}

.sidebar-header {
  @apply border-b border-neutral-200;
}

.sidebar-footer {
  @apply mt-auto border-t border-neutral-200;
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

.aspect-video {
  aspect-ratio: 16 / 9;
}

@media (max-width: 1023px) {
  .app-sidebar {
    @apply fixed inset-y-0 left-0 z-50 transform -translate-x-full transition-transform duration-300 ease-in-out;
  }
  
  .app-sidebar.open {
    @apply translate-x-0;
  }
}
</style>




