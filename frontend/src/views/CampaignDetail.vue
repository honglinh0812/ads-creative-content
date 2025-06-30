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
            <router-link to="/campaigns" class="btn btn-sm btn-ghost w-full">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
              Back to Campaigns
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
            <h1 class="text-lg font-semibold text-secondary-900">Campaign Details</h1>
          </div>

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
            <div class="page-header">
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <h1 class="page-title">{{ campaign.name }}</h1>
                  <div class="flex items-center gap-4 mt-2">
                    <span :class="getStatusBadgeClass(campaign.status)">
                      {{ campaign.status }}
                    </span>
                    <span class="text-sm text-secondary-600">
                      Created {{ formatDate(campaign.createdAt) }}
                    </span>
                  </div>
                </div>
                
                <div class="flex items-center gap-3">
                  <router-link 
                    :to="`/campaigns/${campaign.id}/edit`" 
                    class="btn btn-sm btn-secondary"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                    </svg>
                    Edit
                  </router-link>
                  <button 
                    @click="confirmDeleteCampaign" 
                    class="btn btn-sm btn-error"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1-1H8a1 1 0 00-1 1v3M4 7h16"></path>
                    </svg>
                    Delete
                  </button>
                </div>
              </div>
            </div>

            <!-- Campaign Stats -->
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
              <div class="card">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm text-secondary-600 mb-1">Budget</p>
                      <p class="text-lg font-semibold text-secondary-900">
                        ${{ campaign.budget || '0' }}
                      </p>
                    </div>
                    <div class="w-10 h-10 bg-success-100 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm text-secondary-600 mb-1">Total Ads</p>
                      <p class="text-lg font-semibold text-secondary-900">
                        {{ ads.length }}
                      </p>
                    </div>
                    <div class="w-10 h-10 bg-info-100 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm text-secondary-600 mb-1">Duration</p>
                      <p class="text-lg font-semibold text-secondary-900">
                        {{ formatDuration(campaign) }}
                      </p>
                    </div>
                    <div class="w-10 h-10 bg-warning-100 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                  <div class="w-16 h-16 bg-secondary-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <svg class="w-8 h-8 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

                <!-- Ads Grid -->
                <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <div 
                    v-for="ad in ads" 
                    :key="ad.id" 
                    class="card hover:shadow-lg transition-shadow duration-200"
                  >
                    <div class="card-body">
                      <!-- Ad Preview Image -->
                      <div class="aspect-video bg-secondary-100 rounded-lg mb-4 overflow-hidden">
                        <img 
                          v-if="ad.imageUrl" 
                          :src="ad.imageUrl" 
                          :alt="ad.name"
                          class="w-full h-full object-cover"
                        >
                        <div v-else class="w-full h-full flex items-center justify-center">
                          <svg class="w-8 h-8 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                          </svg>
                        </div>
                      </div>

                      <!-- Ad Info -->
                      <div class="flex-1">
                        <h4 class="font-medium text-secondary-900 mb-2 line-clamp-2">{{ ad.name }}</h4>
                        <p class="text-sm text-secondary-600 mb-3 line-clamp-3">{{ ad.description }}</p>
                        
                        <div class="flex items-center justify-between">
                          <span :class="getStatusBadgeClass(ad.status)">
                            {{ ad.status }}
                          </span>
                          <div class="flex items-center gap-2">
                            <button 
                              @click="viewAdDetails(ad)" 
                              class="btn btn-xs btn-secondary"
                              title="View Details"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                              </svg>
                            </button>
                            <button 
                              @click="editAd(ad)" 
                              class="btn btn-xs btn-secondary"
                              title="Edit"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                              </svg>
                            </button>
                            <button 
                              @click="confirmDeleteAd(ad)" 
                              class="btn btn-xs btn-error"
                              title="Delete"
                            >
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1-1H8a1 1 0 00-1 1v3M4 7h16"></path>
                              </svg>
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
      </main>
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
            <div class="w-12 h-12 bg-error-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
            <div class="w-12 h-12 bg-error-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-6 h-6 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'CampaignDetail',
  props: {
    id: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      sidebarOpen: false,
      showDeleteCampaignModal: false,
      showDeleteAdModal: false,
      adToDelete: null,
      deleting: false
    }
  },
  computed: {
    ...mapState('campaign', ['campaigns', 'currentCampaign', 'loading', 'error']),
    ...mapState('ad', ['ads']),
    campaign() {
      return this.currentCampaign || this.campaigns.find(c => c.id === parseInt(this.id)) || null
    }
  },
  async mounted() {
    await this.loadCampaign()
    await this.loadAds()
  },
  methods: {
    ...mapActions('campaign', ['fetchCampaign', 'deleteCampaign']),
    ...mapActions('ad', ['fetchAds', 'deleteAd']),
    ...mapActions('toast', ['showSuccess', 'showError']),
    
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    
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
        await this.fetchAds()
      } catch (error) {
        console.error('Failed to load ads:', error)
        this.showError({
          title: 'Error',
          message: 'Failed to load campaign ads'
        })
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
      const baseClasses = 'badge'
      switch (status?.toLowerCase()) {
        case 'active':
          return `${baseClasses} badge-success`
        case 'paused':
          return `${baseClasses} badge-warning`
        case 'draft':
          return `${baseClasses} badge-secondary`
        case 'completed':
          return `${baseClasses} badge-info`
        default:
          return `${baseClasses} badge-secondary`
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
    }
  }
}
</script>

