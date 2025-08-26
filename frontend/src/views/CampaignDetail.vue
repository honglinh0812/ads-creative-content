<template>
  <div class="campaign-detail-page">
    <div class="campaign-detail-content">

          <!-- Breadcrumb Navigation -->
          <nav class="breadcrumb mb-6" aria-label="Breadcrumb">
            <ol class="flex items-center space-x-2 text-sm text-secondary-600">
              <li>
                <router-link to="/dashboard" class="hover:text-primary-600 transition-colors">
                  <svg class="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2 2z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V5a2 2 0 012-2h4a2 2 0 012 2v2m-6 4h4"></path>
                  </svg>
                  Dashboard
                </router-link>
              </li>
              <li class="flex items-center">
                <svg class="w-4 h-4 text-secondary-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
                </svg>
              </li>
              <li>
                <router-link to="/campaigns" class="hover:text-primary-600 transition-colors">
                  Campaigns
                </router-link>
              </li>
              <li class="flex items-center">
                <svg class="w-4 h-4 text-secondary-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path>
                </svg>
              </li>
              <li class="text-secondary-900 font-medium" aria-current="page">
                {{ campaign?.name || 'Campaign Details' }}
              </li>
            </ol>
          </nav>

          <!-- Loading State -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <div class="spinner spinner-lg"></div>
            <span class="ml-3 text-secondary-600">Loading campaign details...</span>
          </div>

          <!-- Error State -->
          <div v-else-if="error" class="alert alert-error mb-6">
            <div class="alert-title">Error loading campaign</div>
            <div class="alert-message">{{ error }}</div>
            <button @click="loadCampaign" class="btn btn-sm btn-secondary mt-3">
              Try Again
            </button>
          </div>

          <!-- Campaign Details -->
          <div v-else-if="campaign">
            <!-- Campaign Header -->
            <div class="page-header mb-8">
              <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
                <div class="flex-1 min-w-0">
                  <h1 class="text-2xl sm:text-3xl font-bold text-secondary-900 break-words">{{ campaign.name }}</h1>
                  <div class="flex flex-wrap items-center gap-3 mt-3">
                    <span :class="getStatusBadgeClass(campaign.status)">
                      {{ campaign.status }}
                    </span>
                    <span class="text-sm text-secondary-600">
                      Created {{ formatDate(campaign.createdDate) }}
                    </span>
                  </div>
                </div>
                <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 lg:flex-shrink-0">
                  <router-link 
                    :to="`/campaigns/${campaign.id}/edit`" 
                    class="btn btn-secondary btn-lg flex items-center justify-center gap-2 w-full sm:w-auto"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                    </svg>
                    <span class="hidden sm:inline">Edit</span>
                  </router-link>
                  <button 
                    @click="confirmDeleteCampaign" 
                    class="btn btn-error btn-lg flex items-center justify-center gap-2 w-full sm:w-auto"
                    :disabled="deleting"
                  >
                    <div v-if="deleting" class="spinner spinner-sm"></div>
                    <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                    </svg>
                    <span class="hidden sm:inline">{{ deleting ? 'Deleting...' : 'Delete' }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Campaign Stats -->
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 mb-8">
              <div class="card hover:shadow-lg transition-shadow duration-200">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div class="min-w-0 flex-1">
                      <p class="text-sm text-secondary-600 mb-1">Budget</p>
                      <p class="text-lg sm:text-xl font-semibold text-secondary-900 truncate">
                        ${{ campaign.budget || '0' }}
                      </p>
                    </div>
                    <div class="w-10 h-10 sm:w-12 sm:h-12 bg-success-100 rounded-lg flex items-center justify-center flex-shrink-0">
                      <svg class="w-4 h-4 sm:w-5 sm:h-5 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card hover:shadow-lg transition-shadow duration-200">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div class="min-w-0 flex-1">
                      <p class="text-sm text-secondary-600 mb-1">Total Ads</p>
                      <p class="text-lg sm:text-xl font-semibold text-secondary-900">
                        {{ ads.length }}
                      </p>
                    </div>
                    <div class="w-10 h-10 sm:w-12 sm:h-12 bg-info-100 rounded-lg flex items-center justify-center flex-shrink-0">
                      <svg class="w-4 h-4 sm:w-5 sm:h-5 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card hover:shadow-lg transition-shadow duration-200 sm:col-span-2 lg:col-span-1">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div class="min-w-0 flex-1">
                      <p class="text-sm text-secondary-600 mb-1">Duration</p>
                      <p class="text-lg sm:text-xl font-semibold text-secondary-900 truncate">
                        {{ formatDuration(campaign) }}
                      </p>
                    </div>
                    <div class="w-10 h-10 sm:w-12 sm:h-12 bg-warning-100 rounded-lg flex items-center justify-center flex-shrink-0">
                      <svg class="w-4 h-4 sm:w-5 sm:h-5 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Campaign Description -->
            <div v-if="campaign.targetAudience" class="card mb-8">
              <div class="card-body">
                <h3 class="text-lg font-semibold text-secondary-900 mb-3">Target Audience</h3>
                <p class="text-secondary-700">{{ campaign.targetAudience }}</p>
              </div>
            </div>

            <!-- Ads Section -->
            <div class="card">
              <div class="card-header">
                <div class="flex items-center justify-between">
                  <h3 class="text-lg font-semibold text-secondary-900">Campaign Ads</h3>
                  <router-link 
                    to="/ad/create" 
                    class="btn btn-sm btn-primary"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                    </svg>
                    Create Ad
                  </router-link>
                </div>
              </div>
              
              <div class="card-body">
                <!-- Empty State -->
                <div v-if="ads.length === 0" class="text-center py-12">
                  <div class="w-12 h-12 bg-secondary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-4 h-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                    </svg>
                  </div>
                  <h4 class="text-lg font-medium text-secondary-900 mb-2">No ads yet</h4>
                  <p class="text-secondary-600 mb-6">Create your first ad to get started with this campaign.</p>
                  <router-link to="/ad/create" class="btn btn-primary">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                    </svg>
                    Create First Ad
                  </router-link>
                </div>

                <!-- Loading State for Ads -->
                <div v-if="adsLoading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
                  <div v-for="i in 6" :key="i" class="card animate-pulse">
                    <div class="card-body">
                      <div class="w-full h-32 bg-secondary-200 rounded-lg mb-4"></div>
                      <div class="h-4 bg-secondary-200 rounded mb-2"></div>
                      <div class="h-3 bg-secondary-200 rounded mb-3 w-3/4"></div>
                      <div class="flex justify-between items-center">
                        <div class="h-3 bg-secondary-200 rounded w-16"></div>
                        <div class="flex gap-2">
                          <div class="w-6 h-6 bg-secondary-200 rounded"></div>
                          <div class="w-6 h-6 bg-secondary-200 rounded"></div>
                          <div class="w-6 h-6 bg-secondary-200 rounded"></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Ads Grid -->
                <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
                  <div 
                    v-for="ad in ads" 
                    :key="ad.id" 
                    class="card hover:shadow-lg transition-all duration-200 hover:-translate-y-1"
                  >
                    <div class="card-body relative">
                      <!-- Status Badge -->
                      <span :class="getStatusBadgeClass(ad.status) + ' absolute top-2 right-2 z-10 text-xs'">
                        {{ ad.status }}
                      </span>
                      
                      <!-- Ad Preview Image - Responsive size -->
                      <div v-if="ad.imageUrl || ad.mediaFileUrl" class="mb-4">
                        <div class="w-full aspect-video bg-secondary-100 rounded-lg overflow-hidden border border-gray-200 shadow-sm cursor-pointer flex items-center justify-center group" @click="viewAdDetail(ad)">
                          <img 
                            :src="ad.imageUrl || ad.mediaFileUrl" 
                            :alt="ad.name"
                            class="max-w-full max-h-full object-contain group-hover:scale-105 transition-transform duration-200"
                            @error="handleImageError"
                          >
                        </div>
                      </div>
                      <!-- Placeholder when no image -->
                      <div v-else class="mb-4">
                        <div class="w-full aspect-video bg-gradient-to-br from-gray-100 to-gray-200 rounded-lg border border-gray-200 shadow-sm flex items-center justify-center">
                          <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                          </svg>
                        </div>
                      </div>

                      <!-- Ad Info -->
                      <div class="flex-1">
                        <h4 class="font-medium text-secondary-900 mb-2 line-clamp-2 text-sm sm:text-base" :title="ad.name">{{ ad.name }}</h4>
                        <p class="text-xs sm:text-sm text-secondary-600 mb-3 line-clamp-3" :title="ad.description">{{ ad.description || 'No description available' }}</p>
                        
                        <!-- Ad Type Info -->
                        <div class="flex items-center gap-1 mb-3 text-xs text-secondary-500">
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path>
                          </svg>
                          <span>{{ ad.type || 'Unknown' }}</span>
                        </div>
                        
                        <div class="flex items-center justify-between">
                          <div class="flex items-center gap-1 sm:gap-2">
                            <button 
                              @click="viewAdDetails(ad)" 
                              class="btn btn-xs btn-secondary flex items-center gap-1"
                              title="View Details"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                              </svg>
                              <span class="hidden sm:inline text-xs">View</span>
                            </button>
                            <button 
                              @click="editAd(ad)" 
                              class="btn btn-xs btn-secondary flex items-center gap-1"
                              title="Edit"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                              </svg>
                              <span class="hidden sm:inline text-xs">Edit</span>
                            </button>
                            <button 
                              @click="confirmDeleteAd(ad)" 
                              class="btn btn-xs btn-error flex items-center gap-1"
                              title="Delete"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                              </svg>
                              <span class="hidden sm:inline text-xs">Delete</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
    </div>

    <!-- Delete Campaign Modal -->
    <div v-if="showDeleteCampaignModal" class="modal-overlay" @click="cancelDeleteCampaign">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">Delete Campaign</h3>
          <button @click="cancelDeleteCampaign" class="modal-close">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="flex items-start gap-4">
            <div class="w-8 h-8 bg-error-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
            </div>
            <div class="flex-1">
              <h4 class="text-lg font-medium text-secondary-900 mb-2">Are you sure?</h4>
              <p class="text-secondary-600">
                This will permanently delete the campaign "{{ campaign?.name }}" and all its ads. This action cannot be undone.
              </p>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="cancelDeleteCampaign" class="btn btn-secondary">Cancel</button>
          <button @click="handleDeleteCampaign" :disabled="deleting" class="btn btn-error">
            <span v-if="deleting" class="spinner spinner-sm mr-2"></span>
            {{ deleting ? 'Deleting...' : 'Delete Campaign' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Ad Modal -->
    <div v-if="showDeleteAdModal" class="modal-overlay" @click="cancelDeleteAd">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">Delete Ad</h3>
          <button @click="cancelDeleteAd" class="modal-close">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="flex items-start gap-4">
            <div class="w-8 h-8 bg-error-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
            </div>
            <div class="flex-1">
              <h4 class="text-lg font-medium text-secondary-900 mb-2">Are you sure?</h4>
              <p class="text-secondary-600">
                This will permanently delete the ad "{{ adToDelete?.name }}". This action cannot be undone.
              </p>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="cancelDeleteAd" class="btn btn-secondary">Cancel</button>
          <button @click="handleDeleteAd" :disabled="deleting" class="btn btn-error">
            <span v-if="deleting" class="spinner spinner-sm mr-2"></span>
            {{ deleting ? 'Deleting...' : 'Delete Ad' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'


export default {
  name: 'CampaignDetail',
  components: {},
  props: {
    id: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      showDeleteCampaignModal: false,
      showDeleteAdModal: false,
      adToDelete: null,
      adsLoading: false,
      deleting: false
    }
  },
  computed: {
    ...mapState('campaign', ['campaigns', 'currentCampaign', 'loading', 'error']),
    ...mapState('ad', ['ads']),
    campaign() {
      return this.currentCampaign || this.campaigns.find(c => c.id === parseInt(this.id)) || null
    },

  },
  async mounted() {
    await this.loadCampaign()
    await this.loadAds()
  },
  methods: {
    ...mapActions('campaign', ['fetchCampaign', 'deleteCampaign']),
    ...mapActions('ad', ['fetchAds', 'deleteAd']),
    ...mapActions('toast', ['showSuccess', 'showError']),
    

    
    async loadCampaign() {
      try {
        await this.fetchCampaign(this.id)
      } catch (error) {
        console.error('Failed to load campaign:', error)
        this.showError({
          title: 'Error',
          message: 'Failed to load campaign details'
        })
      }
    },
    
    async loadAds() {
      try {
        this.adsLoading = true
        await this.fetchAds()
      } catch (error) {
        console.error('Failed to load ads:', error)
        this.showError({
          title: 'Error',
          message: 'Failed to load campaign ads'
        })
      } finally {
        this.adsLoading = false
      }
    },
    
    confirmDeleteCampaign() {
      this.showDeleteCampaignModal = true
    },
    
    cancelDeleteCampaign() {
      this.showDeleteCampaignModal = false
    },
    
    async handleDeleteCampaign() {
      if (!this.campaign) return
      
      this.deleting = true
      try {
        await this.deleteCampaign(this.campaign.id)
        this.showSuccess({
          title: 'Success',
          message: 'Campaign deleted successfully'
        })
        await this.$store.dispatch('dashboard/fetchDashboardData', null, { root: true })
        this.$router.push('/campaigns')
      } catch (error) {
        console.error('Failed to delete campaign:', error)
        this.showError({
          title: 'Error',
          message: 'Failed to delete campaign'
        })
      } finally {
        this.deleting = false
        this.showDeleteCampaignModal = false
      }
    },
    
    confirmDeleteAd(ad) {
      this.adToDelete = ad
      this.showDeleteAdModal = true
    },
    
    cancelDeleteAd() {
      this.showDeleteAdModal = false
      this.adToDelete = null
    },
    
    async handleDeleteAd() {
      if (!this.adToDelete) return
      
      this.deleting = true
      try {
        await this.deleteAd(this.adToDelete.id)
        this.showSuccess({
          title: 'Success',
          message: 'Ad deleted successfully'
        })
        await this.loadAds() // Refresh ads list
      } catch (error) {
        console.error('Failed to delete ad:', error)
        this.showError({
          title: 'Error',
          message: 'Failed to delete ad'
        })
      } finally {
        this.deleting = false
        this.showDeleteAdModal = false
        this.adToDelete = null
      }
    },
    
    viewAdDetails(ad) {
      this.$router.push(`/ads/${ad.id}`)
    },
    
    editAd(ad) {
      this.$router.push(`/ads/${ad.id}/edit`)
    },
    
    getStatusBadgeClass(status) {
      const baseClasses = 'px-2 py-1 rounded-full font-medium'
      switch (status?.toLowerCase()) {
        case 'active':
          return `${baseClasses} bg-green-100 text-green-800`
        case 'paused':
          return `${baseClasses} bg-yellow-100 text-yellow-800`
        case 'completed':
          return `${baseClasses} bg-blue-100 text-blue-800`
        case 'draft':
          return `${baseClasses} bg-gray-100 text-gray-800`
        default:
          return `${baseClasses} bg-gray-100 text-gray-600`
      }
    },
    
    formatDate(dateString) {
      if (!dateString) return 'N/A'
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    },
    
    formatDuration(campaign) {
      if (!campaign?.startDate || !campaign?.endDate) return 'N/A'
      const start = new Date(campaign.startDate)
      const end = new Date(campaign.endDate)
      const diffTime = Math.abs(end - start)
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      return `${diffDays} days`
    },

    handleImageError(event) {
      event.target.src = 'https://via.placeholder.com/100x150'; // Fallback to placeholder
    },
    handleLogout() {
      this.$store.dispatch('auth/logout')
    }
  }
}
</script>

<style scoped>
.campaign-detail-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}
</style>

