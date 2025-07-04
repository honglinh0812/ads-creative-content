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
            <div class="flex items-center gap-3 mb-3">
              <div class="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                <span class="text-primary-700 font-medium text-sm">{{ userInitials }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-secondary-900 truncate">{{ userName }}</p>
                <p class="text-xs text-secondary-500 truncate">{{ userEmail }}</p>
              </div>
            </div>
            <button @click="logout" class="btn btn-sm btn-ghost w-full">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              Logout
            </button>
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
            <h1 class="text-lg font-semibold text-secondary-900">Dashboard</h1>
          </div>

          <!-- Loading State -->
          <div v-if="dashboardLoading" class="flex items-center justify-center py-12">
            <div class="spinner spinner-lg"></div>
            <span class="ml-3 text-secondary-600">Loading dashboard...</span>
          </div>

          <!-- Error State -->
          <div v-else-if="dashboardError" class="alert alert-error mb-6">
            <div class="alert-title">Error loading dashboard</div>
            <div class="alert-message">{{ dashboardError }}</div>
            <button @click="loadDashboardData" class="btn btn-sm btn-secondary mt-3">
              Try Again
            </button>
          </div>

          <!-- Dashboard Content -->
          <div v-else>

            <!-- Stats Cards -->
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8 flex flex-wrap">
              <div class="card">
                <div class="card-body">
                    <div class="flex flex-col h-full">
                      <div class="flex items-center justify-between mb-2">
                        <div>
                          <p class="text-sm text-secondary-600 mb-1">Total Campaigns</p>
                          <p class="text-2xl font-bold text-secondary-900">{{ stats.totalCampaigns || 0 }}</p>
                        </div>
                        <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                          </svg>
                        </div>
                      </div>
                    </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body">
                    <div class="flex flex-col h-full">
                      <div class="flex items-center justify-between mb-2">
                        <div>
                          <p class="text-sm text-secondary-600 mb-1">Total Ads</p>
                          <p class="text-2xl font-bold text-secondary-900">{{ stats.totalAds || 0 }}</p>
                        </div>
                        <div class="w-10 h-10 bg-success-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <svg class="w-5 h-5 text-success-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                          </svg>
                        </div>
                      </div>
                    </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body">
                    <div class="flex flex-col h-full">
                      <div class="flex items-center justify-between mb-2">
                        <div>
                          <p class="text-sm text-secondary-600 mb-1">Active Campaigns</p>
                          <p class="text-2xl font-bold text-secondary-900">{{ stats.activeCampaigns || 0 }}</p>
                        </div>
                        <div class="w-10 h-10 bg-warning-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <svg class="w-5 h-5 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                          </svg>
                        </div>
                      </div>
                    </div>
                </div>
              </div>

              <div class="card">
                <div class="card-body">
                    <div class="flex flex-col h-full">
                      <div class="flex items-center justify-between mb-2">
                        <div>
                          <p class="text-sm text-secondary-600 mb-1">Active Ads</p>
                          <p class="text-2xl font-bold text-secondary-900">{{ stats.activeAds || 0 }}</p>
                        </div>
                        <div class="w-10 h-10 bg-error-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <svg class="w-5 h-5 text-error-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                          </svg>
                        </div>
                      </div>
                    </div>
                </div>
              </div>
            </div>

            <!-- Quick Actions -->
            <div class="section mb-8">
              <div class="section-header mb-6">
                <h2 class="section-title">Quick Actions</h2>
                <p class="section-description">Get started with creating your campaigns and ads</p>
              </div>
              
              <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                <div v-for="action in quickActions" :key="action.name" class="card hover:shadow-lg transition-shadow duration-200 flex flex-col">
                  <div class="card-body flex flex-col justify-between h-full p-3">
                    <div class="flex items-start gap-4">
                      <div class="w-10 h-10 bg-primary-100 rounded-lg flex items-center justify-center flex-shrink-0">
                        <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="action.icon"></path>
                        </svg>
                      </div>
                      <div class="flex-1">
                        <h3 class="text-base font-semibold text-secondary-900 mb-2">{{ action.name }}</h3>
                        <p class="text-xs text-secondary-600 mb-4">{{ action.description }}</p>
                      </div>
                    </div>
                    <router-link :to="action.link" :class="`btn btn-sm ${action.buttonClass} w-full`">
                      {{ action.buttonText }}
                    </router-link>
                  </div>
                </div>
              </div>

            <!-- Recent Campaigns -->
            <div class="section mb-8">
              <div class="section-header mb-6">
                <div class="flex items-center justify-between">
                  <div>
                    <h2 class="section-title">Recent Campaigns</h2>
                    <p class="section-description">Your latest campaigns and their performance</p>
                  </div>
                  <router-link to="/campaigns" class="btn btn-sm btn-secondary">
                    View All
                  </router-link>
                </div>
              </div>

              <div v-if="campaigns.length === 0" class="card">
                <div class="card-body text-center py-12">
                  <svg class="w-16 h-16 text-neutral-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                  </svg>
                  <h3 class="text-xl font-semibold text-secondary-900 mb-2">No campaigns yet</h3>
                  <p class="text-secondary-600 mb-6">Create your first campaign to get started with Facebook Ads</p>
                  <router-link to="/campaign/create" class="btn btn-sm btn-primary">
                    Create Your First Campaign
                  </router-link>
                </div>
              </div>

              <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                <div v-for="campaign in campaigns" :key="campaign.id" class="card hover:shadow-lg transition-shadow duration-200 flex flex-col">
                  <div class="card-body flex flex-col justify-between h-full p-3">
                    <div>
                      <div class="flex items-start justify-between mb-2">
                        <div>
                          <h3 class="text-base font-semibold text-secondary-900 line-clamp-2">{{ campaign.name }}</h3>
                          <p class="text-xs text-secondary-600 line-clamp-1">{{ campaign.objective }}</p>
                        </div>
                        <span :class="getStatusBadgeClass(campaign.status)">
                          {{ campaign.status }}
                        </span>
                      </div>
                      
                      <div class="grid grid-cols-2 gap-2 mb-2">
                        <div>
                          <p class="text-xs text-secondary-500 mb-1">Budget</p>
                          <p class="text-sm font-medium text-secondary-900">${{ campaign.budget }}</p>
                        </div>
                        <div>
                          <p class="text-xs text-secondary-500 mb-1">Ads</p>
                          <p class="text-sm font-medium text-secondary-900">{{ campaign.adCount }}</p>
                        </div>
                      </div>
                    </div>
                    
                    <div class="flex items-center justify-between pt-2 border-t border-neutral-200">
                      <span class="text-xs text-secondary-500">{{ formatDate(campaign.createdAt) }}</span>
                      <router-link :to="`/campaigns/${campaign.id}`" class="btn btn-xs btn-primary">
                        View Details
                      </router-link>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Recent Ads -->
            <div class="section">
              <div class="section-header mb-6">
                <div class="flex items-center justify-between">
                  <div>
                    <h2 class="section-title">Recent Ads</h2>
                    <p class="section-description">Your latest ad creations</p>
                  </div>
                  <router-link to="/ads" class="btn btn-sm btn-secondary">
                    View All
                  </router-link>
                </div>
              </div>

              <div v-if="recentAds.length === 0" class="card">
                <div class="card-body text-center py-12">
                  <svg class="w-16 h-16 text-neutral-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                  </svg>
                  <h3 class="text-xl font-semibold text-secondary-900 mb-2">No ads yet</h3>
                  <p class="text-secondary-600 mb-6">Create your first ad to start advertising</p>
                  <router-link to="/ad/create" class="btn btn-sm btn-primary">
                    Create Your First Ad
                  </router-link>
                </div>
              </div>
              <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                <div v-for="ad in recentAds" :key="ad.id" class="card hover:shadow-lg transition-shadow duration-200 flex flex-col">
                  <div class="card-body flex flex-col justify-between h-full p-3">
                    <div>
                      <div class="flex items-start justify-between mb-2">
                        <div>
                          <h4 class="font-semibold text-base text-secondary-900 line-clamp-2">{{ ad.name }}</h4>
                          <p class="text-xs text-secondary-500 line-clamp-1">{{ ad.campaignName }}</p>
                        </div>
                        <span :class="getStatusBadgeClass(ad.status)">
                          {{ ad.status }}
                        </span>
                      </div>
                      
                      <div class="mb-2">
                        <span class="badge badge-neutral">{{ ad.adType?.replace("_", " ") }}</span>
                      </div>
                    </div>
                    
                    <div class="flex items-center justify-between pt-2 border-t border-neutral-200">
                      <span class="text-xs text-secondary-500">{{ formatDate(ad.createdDate) }}</span>
                      <router-link :to="`/ads/${ad.id}`" class="btn btn-xs btn-primary">
                        View
                      </router-link>
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
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'Dashboard',
  data() {
    return {
      sidebarOpen: false,
      quickActions: [
        {
          name: "Create Campaign",
          description: "Set up a new advertising campaign with your objectives and budget",
          icon: "M12 6v6m0 0v6m0-6h6m-6 0H6",
          link: "/campaign/create",
          buttonText: "Create Campaign",
          buttonClass: "btn-primary",
        },
        {
          name: "Create Ad",
          description: "Generate AI-powered ad content for your campaigns",
          icon: "M12 6v6m0 0v6m0-6h6m-6 0H6",
          link: "/ad/create",
          buttonText: "Create Ad",
          buttonClass: "btn-success",
        },
      ],
    };
  },
  computed: {
    ...mapState('auth', ['user']),
    ...mapState('dashboard', ['stats', 'campaigns', 'recentAds', 'loading', 'error']),
    
    dashboardLoading() {
      return this.loading
    },
    
    dashboardError() {
      return this.error
    },
    
    userName() {
      return this.user?.name || this.user?.email || 'User'
    },
    userEmail() {
      return this.user?.email || ''
    },
    userInitials() {
      if (!this.user?.name && !this.user?.email) return 'U'
      const name = this.user?.name || this.user?.email
      return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    }
  },
  async mounted() {
    await this.loadUserData()
    await this.loadDashboardData()
  },
  methods: {
    ...mapActions('dashboard', ['fetchDashboardData']),
    ...mapActions('auth', ['fetchUser', 'logout']),
    
    async loadUserData() {
      try {
        if (!this.user) {
          await this.fetchUser()
        }
      } catch (error) {
        console.error('Failed to load user data:', error)
      }
    },
    
    async loadDashboardData() {
      try {
        await this.fetchDashboardData()
      } catch (error) {
        console.error('Failed to load dashboard data:', error)
      }
    },
    
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    
    getStatusBadgeClass(status) {
      const baseClass = 'badge badge-sm'
      switch (status?.toLowerCase()) {
        case 'active':
          return `${baseClass} badge-success`
        case 'paused':
          return `${baseClass} badge-warning`
        case 'draft':
          return `${baseClass} badge-neutral`
        default:
          return `${baseClass} badge-neutral`
      }
    },
    
    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
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

@media (max-width: 1023px) {
  .app-sidebar {
    @apply fixed inset-y-0 left-0 z-50 transform -translate-x-full transition-transform duration-300 ease-in-out;
  }
  
  .app-sidebar.open {
    @apply translate-x-0;
  }
}
</style>

