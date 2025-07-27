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
            <h1 class="text-lg font-semibold text-secondary-900">Ads</h1>
          </div>

          <!-- Page Header -->
          <div class="page-header flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 gap-4">
            <div>
              <h1 class="text-3xl font-bold text-secondary-900">Ads</h1>
              <p class="text-secondary-600">Manage your advertising content</p>
            </div>
            <router-link to="/ad/create" class="btn btn-primary btn-lg flex items-center gap-2">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
              </svg>
              New Ad
            </router-link>
          </div>

          <!-- Search Bar -->
          <div class="mb-8 flex flex-col sm:flex-row gap-4 items-center justify-between">
            <div class="relative w-full sm:w-80">
              <input 
                v-model="searchQuery"
                type="text" 
                placeholder="Search ads..."
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
            <div class="alert-title">Error loading ads</div>
            <div class="alert-message">{{ error }}</div>
            <button @click="loadAds" class="btn btn-sm btn-secondary mt-3">
              Try Again
            </button>
          </div>

          <!-- Empty State -->
          <div v-else-if="ads.length === 0" class="card text-center py-12">
            <div class="card-body">
              <h3 class="text-xl font-semibold text-secondary-900 mb-2">No ads yet</h3>
              <p class="text-secondary-600 mb-6">Create your first ad to start advertising</p>
              <router-link to="/ad/create" class="btn btn-primary btn-lg">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                </svg>
                Create Your First Ad
              </router-link>
            </div>
          </div>

          <!-- Ads List -->
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            <div v-for="ad in displayedAds" :key="ad.id" class="group">
              <div class="card hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-2 border-0 bg-white shadow-lg group-hover:shadow-3xl overflow-hidden rounded-2xl">
                <div class="card-body p-0">
                  <!-- Ad Image Preview -->
                  <div v-if="ad.imageUrl || ad.mediaFileUrl" class="relative p-4 flex justify-center">
                    <div class="w-[192px] h-[128px] bg-gradient-to-br from-gray-100 to-gray-200 overflow-hidden border border-gray-200 shadow-sm rounded-lg cursor-pointer flex items-center justify-center" @click="showAdDetails(ad)">
                      <img 
                        :src="ad.imageUrl || ad.mediaFileUrl" 
                        :alt="ad.name"
                        class="max-w-full max-h-full object-contain"
                        @error="handleImageError"
                      />
                    </div>
                    <div class="absolute top-6 right-6">
                      <span :class="`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(ad.status)}`">
                        {{ ad.status }}
                      </span>
                    </div>
                  </div>
                  <!-- Placeholder when no image -->
                  <div v-else class="relative p-4">
                    <div class="w-[192px] h-[128px] bg-gradient-to-br from-gray-100 to-gray-200 overflow-hidden mx-auto border border-gray-200 shadow-sm rounded-lg flex items-center justify-center">
                      <svg class="w-4 h-4 sm:w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                      </svg>
                    </div>
                    <div class="absolute top-6 right-6">
                      <span :class="`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(ad.status)}`">
                        {{ ad.status }}
                      </span>
                    </div>
                  </div>
                  <div class="p-4">
                    <div class="flex items-start justify-between mb-3">
                      <div class="flex-1">
                        <h3 class="text-base font-semibold text-gray-900 line-clamp-2 mb-1">{{ ad.name }}</h3>
                        <p class="text-xs text-gray-500">{{ ad.campaignName || 'No Campaign' }}</p>
                      </div>
                    </div>
                    <div class="mb-3">
                      <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {{ ad.adType?.replace('_', ' ') || 'Unknown Type' }}
                      </span>
                    </div>
                    <div v-if="ad.headline || ad.description" class="mb-3 p-3 bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg">
                      <div v-if="ad.headline" class="mb-2">
                        <p class="text-xs text-gray-500 mb-1 font-medium">Headline</p>
                        <p class="text-sm font-semibold text-gray-900 line-clamp-2">{{ ad.headline }}</p>
                      </div>
                      <div v-if="ad.description">
                        <p class="text-xs text-gray-500 mb-1 font-medium">Description</p>
                        <p class="text-sm text-gray-700 line-clamp-3">{{ ad.description }}</p>
                      </div>
                    </div>
                    <div class="grid grid-cols-2 gap-3 mb-4">
                      <div>
                        <p class="text-xs text-gray-500 mb-1 font-medium">Call to Action</p>
                        <p class="text-sm font-semibold text-gray-900">{{ ad.callToAction || 'None' }}</p>
                      </div>
                      <div>
                        <p class="text-xs text-gray-500 mb-1 font-medium">Created</p>
                        <p class="text-sm font-semibold text-gray-900">{{ formatDate(ad.createdDate) }}</p>
                      </div>
                    </div>
                    <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                      <div class="flex gap-2">
                        <button @click="showEditAdModal(ad)" class="btn btn-xs btn-outline-secondary hover:bg-gray-100 transition-colors">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                          </svg>
                          Edit
                        </button>
                        <button @click="confirmDeleteAd(ad.id)" class="btn btn-xs btn-outline-error hover:bg-red-50 transition-colors">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                          </svg>
                          Delete
                        </button>
                      </div>
                      <button @click="showAdDetails(ad)" class="btn btn-xs btn-primary group-hover:scale-105 transition-transform">
                        View Details
                      </button>
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
            <label class="block text-sm font-medium text-secondary-700 mb-1">Media:</label>
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl" class="space-y-3">
              <!-- Image Preview -->
              <div v-if="selectedAd.imageUrl" class="flex items-center space-x-3">
                <div class="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden border">
                  <img :src="selectedAd.imageUrl" class="w-full h-full object-cover" />
                </div>
                <button @click="showMediaModal = true" class="btn btn-sm btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  View ad image/video
                </button>
              </div>
              <!-- Video Preview -->
              <div v-else-if="selectedAd.videoUrl" class="flex items-center space-x-3">
                <div class="w-20 h-20 bg-gray-100 rounded-lg overflow-hidden border flex items-center justify-center">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </div>
                <button @click="showMediaModal = true" class="btn btn-sm btn-secondary">
                  <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  View ad image/video
                </button>
              </div>
            </div>
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

      <!-- Media Modal -->
      <Dialog v-model:visible="showMediaModal" modal header="Ad Image/Video" :style="{ width: '90vw', maxWidth: '1000px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">
        <div v-if="selectedAd" class="text-center">
          <!-- Image Display -->
          <div v-if="selectedAd.imageUrl" class="space-y-4">
            <img :src="selectedAd.imageUrl" :alt="selectedAd.name" class="max-w-full h-auto rounded-lg mx-auto shadow-lg" />
            <p class="text-sm text-gray-600">{{ selectedAd.name }}</p>
          </div>
          <!-- Video Display -->
          <div v-else-if="selectedAd.videoUrl" class="space-y-4">
            <video controls class="max-w-full h-auto rounded-lg mx-auto shadow-lg">
              <source :src="selectedAd.videoUrl" type="video/mp4">
              <source :src="selectedAd.videoUrl" type="video/webm">
              Your browser does not support the video tag.
            </video>
            <p class="text-sm text-gray-600">{{ selectedAd.name }}</p>
          </div>
          <!-- No Media -->
          <div v-else class="py-8">
            <svg class="w-10 h-10 sm:w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
            </svg>
            <p class="text-gray-600">No image/video for this ad</p>
          </div>
        </div>
      </Dialog>

      <!-- Edit Ad Modal -->
      <Dialog v-model:visible="showEditModal" modal header="Edit Ad" :style="{ width: '90vw', maxWidth: '1200px' }" :breakpoints="{ '960px': '95vw', '641px': '100vw' }">   <div v-if="editingAd">
          <div class="field mb-4">
            <label for="editAdName" class="block text-sm font-medium text-secondary-700 mb-1">Name:</label>
            <InputText id="editAdName" v-model="editingAd.name" class="w-full" :class="{ 'border-red-500': errors.name }" @blur="validateEditAd()" />
            <p v-if="errors.name" class="form-error flex items-center gap-2 mt-1">
              <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
              {{ errors.name }}
            </p>
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
            <Dropdown id="editAdCallToAction" v-model="editingAd.callToAction" :options="standardCTAs" optionLabel="label" optionValue="value" placeholder="Select Call to Action" class="w-full" />
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
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import api from '@/services/api'

export default {
  name: "Ads",
  components: {
    Paginator,
    Dialog,
    ConfirmDialog,
    Toast,
    InputText,
    Dropdown,
    Textarea,
    LoadingSkeleton,
    AppSidebar
  },
  data() {
    return {
      sidebarOpen: true,
      page: 0,
      size: 5,
      showDetailModal: false,
      selectedAd: null,
      showEditModal: false,
      editingAd: null,
      showMediaModal: false,
      searchQuery: '',
      filteredAds: [],
      errors: {}, // New errors object
      standardCTAs: [],
    }
  },
  computed: {
    ...mapState("ad", [
      "ads",
      "totalAds",
      "loading",
      "error"
    ]),
    
    displayedAds() {
      if (!this.searchQuery.trim()) {
        return this.ads
      }
      return this.filteredAds
    },
    mainContentStyle() {
      return this.sidebarOpen ? { marginLeft: '240px' } : { marginLeft: '0' }
    }
  },
  async mounted() {
    await this.loadAds()
    await this.loadCallToActions()
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
    
    async loadCallToActions() {
      try {
        const response = await api.providers.getCallToActions('en') // Default to English
        this.standardCTAs = response.data
      } catch (error) {
        console.error("Failed to load call to actions:", error)
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
      this.errors = {}; // Clear previous errors
    },

    async saveEditedAd() {
      this.errors = {}; // Clear previous errors
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
        this.$store.dispatch('toast/showToast', {
          type: 'error',
          message: 'Please fill in all required fields.'
        });
        return;
      }

      try {
        // Chỉ gửi các trường cho phép update
        const { id, headline, primaryText, description, callToAction, imageUrl, name } = this.editingAd;
        await this.updateAd({
          adId: id,
          adData: { headline, primaryText, description, callToAction, imageUrl, name }
        });
        this.showEditModal = false;
        this.$store.dispatch('toast/showToast', {
          type: 'success',
          message: 'Ad updated successfully'
        });
        await this.loadAds(); // Refresh list after update
      } catch (error) {
        console.error('Failed to save ad:', error);
        this.$store.dispatch('toast/showToast', {
          type: 'error',
          message: error.message || 'Failed to update ad'
        });
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
            this.$store.dispatch('toast/showToast', {
              type: 'success',
              message: 'Ad deleted successfully'
            });
            await this.loadAds(); // Refresh list after delete
          } catch (error) {
            console.error('Failed to delete ad:', error);
            this.$store.dispatch('toast/showToast', {
              type: 'error',
              message: error.message || 'Failed to delete ad'
            });
          }
        },
        reject: () => {
          this.$store.dispatch('toast/showToast', {
            type: 'info',
            message: 'Delete operation cancelled'
          });
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
          this.$store.dispatch('toast/showToast', {
            type: 'info',
            message: 'Changes discarded'
          });
        },
        reject: () => {
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
    },
    
    handleImageError(event) {
      // Hide image if it fails to load
      event.target.style.display = 'none'
    },

    validateEditAd() {
      this.errors = {}; // Clear previous errors
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
    }
  }
}
</script>

<style scoped>
.page-wrapper.sidebar-closed .main-content-wrapper {
  margin-left: 0 !important;
}
</style>

