<template>
  <div :class="['page-wrapper', { 'sidebar-closed': !sidebarOpen }]">
    <AppSidebar :sidebarOpen="sidebarOpen" @toggle="toggleSidebar" @logout="handleLogout" />
    <div class="main-content-wrapper" :style="mainContentStyle">
          <!-- Page Header -->
          <div class="page-header flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 gap-4">
            <div>
              <h1 class="text-3xl font-bold text-secondary-900">Dashboard</h1>
              <p class="text-secondary-600">Overview of your campaigns and ads</p>
            </div>
            <div class="flex gap-2">
              <router-link to="/campaign/create" class="btn btn-primary btn-lg flex items-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                </svg>
                New Campaign
              </router-link>
              <router-link to="/ad/create" class="btn btn-success btn-lg flex items-center gap-2">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
                </svg>
                New Ad
              </router-link>
            </div>
          </div>

          <!-- Loading State -->
          <div v-if="dashboardLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <LoadingSkeleton v-for="i in 4" :key="i" type="card" :width="'100%'" :height="'180px'" />
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
          <div v-else-if="hasDashboardData">

            <!-- Stats Cards -->
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              <div class="card stat-card bg-gradient-to-br from-blue-50 to-blue-100 border-0 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
                <div class="card-body p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-blue-600 mb-1">Total Campaigns</p>
                      <p class="text-3xl font-bold text-blue-900">{{ stats.totalCampaigns || 0 }}</p>
                    </div>
                    <div class="w-6 h-6 sm:w-8 h-8 bg-blue-500 rounded-xl flex items-center justify-center shadow-lg">
                      <svg class="w-4 h-4 sm:w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card stat-card bg-gradient-to-br from-green-50 to-green-100 border-0 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
                <div class="card-body p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-green-600 mb-1">Total Ads</p>
                      <p class="text-3xl font-bold text-green-900">{{ stats.totalAds || 0 }}</p>
                    </div>
                    <div class="w-6 h-6 sm:w-8 h-8 bg-green-500 rounded-xl flex items-center justify-center shadow-lg">
                      <svg class="w-4 h-4 sm:w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card stat-card bg-gradient-to-br from-orange-50 to-orange-100 border-0 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
                <div class="card-body p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-orange-600 mb-1">Active Campaigns</p>
                      <p class="text-3xl font-bold text-orange-900">{{ stats.activeCampaigns || 0 }}</p>
                    </div>
                    <div class="w-6 h-6 sm:w-8 h-8 bg-orange-500 rounded-xl flex items-center justify-center shadow-lg">
                      <svg class="w-4 h-4 sm:w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>

              <div class="card stat-card bg-gradient-to-br from-purple-50 to-purple-100 border-0 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
                <div class="card-body p-6">
                  <div class="flex items-center justify-between">
                    <div>
                      <p class="text-sm font-medium text-purple-600 mb-1">Active Ads</p>
                      <p class="text-3xl font-bold text-purple-900">{{ stats.activeAds || 0 }}</p>
                    </div>
                    <div class="w-6 h-6 sm:w-8 h-8 bg-purple-500 rounded-xl flex items-center justify-center shadow-lg">
                      <svg class="w-4 h-4 sm:w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Quick Actions -->
            <div class="section mb-8">
              <div class="section-header mb-6">
                <h2 class="section-title text-2xl font-bold text-gray-900 mb-2">Quick Actions</h2>
                <p class="section-description text-gray-600">Get started with creating your campaigns and ads</p>
              </div>
              
              <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
                <div v-for="action in quickActions" :key="action.name" class="group">
                  <div class="card hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2 border-0 bg-white shadow-md group-hover:shadow-2xl">
                    <div class="card-body p-4">
                      <div class="flex flex-col items-center text-center">
                        <div class="w-6 h-6 sm:w-8 h-8 bg-gradient-to-br from-primary-500 to-primary-600 rounded-xl flex items-center justify-center mb-3 shadow-lg group-hover:scale-110 transition-transform duration-300">
                          <svg class="w-4 h-4 sm:w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="action.icon"></path>
                          </svg>
                        </div>
                        <h3 class="text-sm font-semibold text-gray-900 mb-1 group-hover:text-primary-600 transition-colors">{{ action.name }}</h3>
                        <p class="text-xs text-gray-500 mb-3 line-clamp-2">{{ action.description }}</p>
                        <router-link :to="action.link" :class="`btn btn-sm ${action.buttonClass} w-full group-hover:scale-105 transition-transform`">
                          {{ action.buttonText }}
                        </router-link>
                      </div>
                      </div>
                    </div>
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
                  <svg class="w-10 h-10 sm:w-12 h-12 text-neutral-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                      <span class="text-xs text-secondary-500">{{ formatDate(campaign.createdDate) }}</span>
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
                  <svg class="w-10 h-10 sm:w-12 h-12 text-neutral-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
                  </svg>
                  <h3 class="text-xl font-semibold text-secondary-900 mb-2">No ads yet</h3>
                  <p class="text-secondary-600 mb-6">Create your first ad to start advertising</p>
                  <router-link to="/ad/create" class="btn btn-sm btn-primary">
                    Create Your First Ad
                  </router-link>
                </div>
              </div>
              <div v-else class="flex flex-wrap gap-4 justify-center">
                <div v-for="ad in recentAds" :key="ad.id" class="card hover:shadow-lg transition-shadow duration-200 flex flex-col items-center max-w-[220px] w-full mx-auto">
                  <div class="card-body flex flex-col justify-between h-full p-3 w-full">
                    <div>
                      <!-- Ad Image Preview - Fixed size 192x128 -->
                      <div v-if="ad.imageUrl || ad.mediaFileUrl" class="mb-3 flex justify-center">
                        <div class="w-[192px] h-[128px] bg-neutral-100 rounded-lg overflow-hidden border border-gray-200 shadow-sm cursor-pointer flex items-center justify-center" @click="viewAdDetail(ad)">
                          <img 
                            :src="ad.imageUrl || ad.mediaFileUrl" 
                            :alt="ad.name"
                            class="max-w-full max-h-full object-contain"
                            @error="handleImageError"
                          />
                        </div>
                      </div>
                      <!-- Placeholder when no image -->
                      <div v-else class="mb-3">
                        <div class="w-[192px] h-[128px] bg-gradient-to-br from-gray-100 to-gray-200 rounded-lg mx-auto border border-gray-200 shadow-sm flex items-center justify-center">
                          <svg class="w-4 h-4 sm:w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                          </svg>
                        </div>
                      </div>
                      
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
                      
                      <!-- Ad Content Preview -->
                      <div v-if="ad.headline || ad.description" class="mb-3 p-2 bg-gradient-to-r from-gray-50 to-gray-100 rounded-lg">
                        <div v-if="ad.headline" class="mb-1">
                          <p class="text-xs text-gray-500 mb-1 font-medium">Headline</p>
                          <p class="text-xs font-semibold text-gray-900 line-clamp-2">{{ ad.headline }}</p>
                        </div>
                        <div v-if="ad.description">
                          <p class="text-xs text-gray-500 mb-1 font-medium">Description</p>
                          <p class="text-xs text-gray-700 line-clamp-2">{{ ad.description }}</p>
                        </div>
                      </div>
                    </div>
                    
                    <div class="flex items-center justify-between pt-2 border-t border-neutral-200">
                      <span class="text-xs text-secondary-500">{{ formatDate(ad.createdDate) }}</span>
                      <button @click="viewAdDetail(ad)" class="btn btn-xs btn-primary">
                        View
                      </button>
                    </div>
                  </div>              
                </div>
              </div>
            </div>

            <!-- Ad Detail Modal -->
            <Dialog v-model:visible="showDetailModal" modal header="Ad Details" :style="{ width: '80vw', maxWidth: '800px' }" :breakpoints="{ '960px': '90vw', '641px': '100vw' }">
              <div v-if="selectedAd" class="space-y-4">
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Name:</label>
                  <p class="text-gray-900">{{ selectedAd.name }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Campaign:</label>
                  <p class="text-gray-900">{{ selectedAd.campaignName || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Ad Type:</label>
                  <p class="text-gray-900">{{ selectedAd.adType?.replace("_", " ") || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Status:</label>
                  <span :class="getStatusBadgeClass(selectedAd.status)">{{ selectedAd.status }}</span>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Headline:</label>
                  <p class="text-gray-900">{{ selectedAd.headline || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Primary Text:</label>
                  <p class="text-gray-900">{{ selectedAd.primaryText || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Description:</label>
                  <p class="text-gray-900">{{ selectedAd.description || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Call to Action:</label>
                  <p class="text-gray-900">{{ selectedAd.callToAction || 'N/A' }}</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Media:</label>
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
                        <svg class="w-4 h-4 sm:w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                  <p v-else class="text-gray-900">N/A</p>
                </div>
                <div class="field mb-4">
                  <label class="block text-sm font-medium text-gray-700 mb-1">Created Date:</label>
                  <p class="text-gray-900">{{ formatDate(selectedAd.createdDate) }}</p>
                </div>
              </div>
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
          </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import Dialog from 'primevue/dialog';
import LoadingSkeleton from '../components/LoadingSkeleton.vue'
import AppSidebar from '@/components/AppSidebar.vue'

export default {
  name: 'Dashboard',
  components: {
    Dialog,
    LoadingSkeleton,
    AppSidebar
  },
  data() {
    return {
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
          description: "Create ad content with AI for your campaign",
          icon: "M12 6v6m0 0v6m0-6h6m-6 0H6",
          link: "/ad/create",
          buttonText: "Create Ad",
          buttonClass: "btn-success",
        },
      ],
      showDetailModal: false,
      showMediaModal: false,
      selectedAd: null,
      sidebarOpen: true
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
    
    // Add computed to check if data exists
    hasDashboardData() {
      return this.stats && this.campaigns && this.recentAds
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
    },
    mainContentStyle() {
      return this.sidebarOpen ? { marginLeft: '240px' } : { marginLeft: '0' }
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
    },
    
    viewAdDetail(ad) {
      console.log('Opening ad detail modal for:', ad);
      this.selectedAd = ad;
      this.showDetailModal = true;
    },
    
    handleImageError(event) {
      // Hide image if it fails to load
      event.target.style.display = 'none'
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


    

