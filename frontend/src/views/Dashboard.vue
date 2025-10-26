<template>
  <div class="dashboard-container">
    <!-- Mobile Header -->
    <MobileHeader 
      v-if="isMobile" 
      @toggle-mobile-menu="toggleMobileMenu"
    />
    
    <!-- Desktop Page Header -->
    <div v-if="!isMobile" class="page-header">
      <a-page-header
        :title="$t('dashboard.title')"
        :sub-title="$t('dashboard.subtitle')"
      >
        <template #extra>
          <a-space>
            <router-link to="/campaign/create">
              <a-button type="primary" size="large">
                <template #icon>
                  <plus-outlined />
                </template>
                {{ $t('dashboard.newCampaign') }}
              </a-button>
            </router-link>
            <router-link to="/ad/create">
              <a-button type="primary" size="large" style="background: #52c41a; border-color: #52c41a;">
                <template #icon>
                  <plus-outlined />
                </template>
                {{ $t('dashboard.newAd') }}
              </a-button>
            </router-link>
          </a-space>
        </template>
      </a-page-header>
    </div>

    <!-- Loading State -->
    <div v-if="dashboardLoading" class="loading-container">
      <LoadingSkeleton v-if="isMobile" type="card" :rows="3" />
      <a-row v-else :gutter="[16, 16]">
        <a-col :span="6" v-for="i in 4" :key="i">
          <a-skeleton active>
            <a-skeleton-input style="width: 100%; height: 180px;" active />
          </a-skeleton>
        </a-col>
      </a-row>
    </div>

    <!-- Error State -->
    <div v-else-if="dashboardError" class="error-container">
      <a-alert
        :message="$t('dashboard.errorLoading')"
        :description="dashboardError"
        type="error"
        show-icon
        closable
      >
        <template #action>
          <a-button size="small" @click="loadDashboardData">
            {{ $t('dashboard.tryAgain') }}
          </a-button>
        </template>
      </a-alert>
    </div>

    <!-- Dashboard Content -->
    <div v-else-if="hasDashboardData">
      <!-- Mobile Dashboard Stats -->
      <MobileDashboardStats 
        v-if="isMobile"
        :stats="mobileStats"
        :quick-actions="quickActions"
        :recent-activity="recentActivity"
        @view-all-activity="navigateToActivity"
      />
      
      <!-- What's Next Section -->
      <DashboardNextSteps v-if="!isMobile" :stats="stats" />

      <!-- Creative Asymmetric Stats Layout -->
      <div v-else class="creative-stats-container">
        <!-- Main Featured Stat (Creative Asymmetric Position) - Clickable -->
        <router-link to="/campaigns" class="featured-stat-link">
          <div class="stat-card-creative primary-stat">
            <div class="stat-visual">
              <folder-outlined class="stat-icon primary-icon" />
              <div class="stat-pattern"></div>
            </div>
            <div class="stat-content">
              <div class="stat-label">{{ $t('dashboard.totalCampaigns') }}</div>
              <div class="stat-number primary-number">{{ stats.totalCampaigns || 0 }}</div>
              <div class="stat-growth">{{ stats.totalCampaigns > 0 ? $t('dashboard.growthThisMonth', { percent: 12 }) : $t('dashboard.startCreating') }}</div>
              <div class="stat-action">{{ $t('dashboard.viewAll') }} →</div>
            </div>
          </div>
        </router-link>

        <!-- Secondary Stats Grid (Breaking Standard Grid) -->
        <div class="secondary-stats">
          <router-link to="/ads" class="stat-card-link">
            <div class="stat-card-creative secondary-stat success">
              <file-text-outlined class="stat-icon" />
              <div class="stat-info">
                <div class="stat-number">{{ stats.totalAds || 0 }}</div>
                <div class="stat-label">{{ $t('dashboard.totalAds') }}</div>
              </div>
              <div class="stat-accent"></div>
            </div>
          </router-link>

          <router-link to="/campaigns" class="stat-card-link">
            <div class="stat-card-creative secondary-stat warning">
              <thunderbolt-outlined class="stat-icon" />
              <div class="stat-info">
                <div class="stat-number">{{ stats.activeCampaigns || 0 }}</div>
                <div class="stat-label">{{ $t('dashboard.activeCampaigns') }}</div>
              </div>
              <div class="stat-accent"></div>
            </div>
          </router-link>

          <router-link to="/ads" class="stat-card-link">
            <div class="stat-card-creative secondary-stat danger">
              <eye-outlined class="stat-icon" />
              <div class="stat-info">
                <div class="stat-number">{{ stats.activeAds || 0 }}</div>
                <div class="stat-label">{{ $t('dashboard.activeAds') }}</div>
              </div>
              <div class="stat-accent"></div>
            </div>
          </router-link>
        </div>

        <!-- Creative Quick Insight Box -->
        <div class="insight-card">
          <div class="insight-content">
            <div class="insight-emoji">🎯</div>
            <div class="insight-text">
              <div class="insight-title">{{ $t('dashboard.proTip') }}</div>
              <div class="insight-message">{{ getContextualInsight() }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Creative Quick Actions -->
      <div class="creative-actions-section">
        <div class="section-header-creative">
          <div class="header-content">
            <h2 class="section-title-creative">{{ $t('dashboard.readyToCreate') }}</h2>
            <p class="section-subtitle">{{ $t('dashboard.jumpIntoNext') }}</p>
          </div>
          <div class="header-decoration">✨</div>
        </div>

        <div class="actions-grid-creative">
          <!-- Primary Action - Create Campaign (Featured) -->
          <router-link :to="quickActions[0].link" class="action-card-primary">
            <div class="action-primary-content">
              <div class="action-visual">
                <folder-outlined class="action-main-icon" />
                <div class="action-glow"></div>
              </div>
              <div class="action-text">
                <h3 class="action-title">{{ quickActions[0].name }}</h3>
                <p class="action-desc">{{ quickActions[0].description }}</p>
                <div class="action-cta">
                  <span>Let's go!</span>
                  <div class="arrow-icon">→</div>
                </div>
              </div>
            </div>
          </router-link>

          <!-- Secondary Actions - Creative Mini Cards -->
          <div class="actions-secondary">
            <router-link
              v-for="(action, index) in quickActions.slice(1)"
              :key="action.name"
              :to="action.link"
              class="action-card-mini"
              :class="`action-${index + 1}`"
            >
              <component :is="action.icon" class="mini-icon" />
              <div class="mini-content">
                <div class="mini-title">{{ action.name.replace(' ', '\n') }}</div>
                <div class="mini-arrow">↗</div>
              </div>
            </router-link>
          </div>

          <!-- Fun Motivational Card -->
          <div class="motivation-card">
            <div class="motivation-content">
              <div class="motivation-icon">{{ getMoodEmoji() }}</div>
              <div class="motivation-text">
                <div class="motivation-title">{{ $t('dashboard.youDoingGreat') }}</div>
                <div class="motivation-message">{{ getMotivationalMessage() }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Campaigns -->
      <div class="section" style="margin-bottom: 32px;">
        <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
          <div>
            <a-typography-title :level="2" style="margin-bottom: 8px;">{{ $t('dashboard.recentCampaigns') }}</a-typography-title>
            <a-typography-text type="secondary">{{ $t('dashboard.recentCampaignsDesc') }}</a-typography-text>
          </div>
          <router-link to="/campaigns">
            <a-button>{{ $t('dashboard.viewAll') }}</a-button>
          </router-link>
        </div>

        <div v-if="campaigns.length === 0">
          <a-empty :description="$t('dashboard.noCampaignsYet')">
            <template #image>
              <folder-outlined style="font-size: 48px; color: #d9d9d9;" />
            </template>
            <router-link to="/campaign/create">
              <a-button type="primary">{{ $t('dashboard.createFirstCampaign') }}</a-button>
            </router-link>
          </a-empty>
        </div>

        <a-row :gutter="[16, 16]" v-else>
          <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="campaign in campaigns" :key="campaign.id">
            <a-card class="campaign-card" hoverable>
              <div class="campaign-header">
                <a-typography-title :level="4" style="margin-bottom: 4px;">{{ campaign.name }}</a-typography-title>
                <a-tag :color="getStatusColor(campaign.status)">{{ campaign.status }}</a-tag>
              </div>
              <a-typography-text type="secondary" style="display: block; margin-bottom: 12px;">{{ campaign.objective }}</a-typography-text>
              
              <a-row :gutter="16" style="margin-bottom: 12px;">
                <a-col :span="12">
                  <a-typography-text type="secondary" style="font-size: 12px;">{{ $t('dashboard.budget') }}</a-typography-text>
                  <div style="font-weight: 500;">${{ campaign.budget }}</div>
                </a-col>
                <a-col :span="12">
                  <a-typography-text type="secondary" style="font-size: 12px;">{{ $t('dashboard.ads') }}</a-typography-text>
                  <div style="font-weight: 500;">{{ campaign.adCount }}</div>
                </a-col>
              </a-row>

              <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid #f0f0f0;">
                <a-typography-text type="secondary" style="font-size: 12px;">{{ formatDate(campaign.createdDate) }}</a-typography-text>
                <router-link :to="`/campaigns/${campaign.id}`">
                  <a-button size="small" type="primary">{{ $t('dashboard.viewDetails') }}</a-button>
                </router-link>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- Recent Ads -->
      <div class="section">
        <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
          <div>
            <a-typography-title :level="2" style="margin-bottom: 8px;">{{ $t('dashboard.recentAds') }}</a-typography-title>
            <a-typography-text type="secondary">{{ $t('dashboard.recentAdsDesc') }}</a-typography-text>
          </div>
          <router-link to="/ads">
            <a-button>{{ $t('dashboard.viewAll') }}</a-button>
          </router-link>
        </div>

        <!-- Loading State for Recent Ads -->
        <div v-if="loading">
          <a-row :gutter="[12, 12]">
            <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="i in 8" :key="i">
              <a-card class="ad-card">
                <a-skeleton active>
                  <template #avatar>
                    <div style="width: 100px; height: 100px; background: #f0f0f0; border-radius: 8px; margin: 0 auto 12px;"></div>
                  </template>
                  <template #title>
                    <div style="height: 16px; background: #f0f0f0; border-radius: 4px; margin-bottom: 8px;"></div>
                  </template>
                  <template #paragraph>
                    <div style="height: 12px; background: #f0f0f0; border-radius: 4px; margin-bottom: 8px; width: 60%;"></div>
                    <div style="height: 12px; background: #f0f0f0; border-radius: 4px; margin-bottom: 8px; width: 80%;"></div>
                    <div style="height: 12px; background: #f0f0f0; border-radius: 4px; width: 40%;"></div>
                  </template>
                </a-skeleton>
              </a-card>
            </a-col>
          </a-row>
        </div>

        <!-- Empty State -->
        <div v-else-if="recentAds.length === 0">
          <a-empty :description="$t('dashboard.noAdsYet')">
            <template #image>
              <file-text-outlined style="font-size: 48px; color: #d9d9d9;" />
            </template>
            <router-link to="/ad/create">
              <a-button type="primary">{{ $t('dashboard.createFirstAd') }}</a-button>
            </router-link>
          </a-empty>
        </div>

        <!-- Ads Grid -->
        <a-row :gutter="[12, 12]" v-else>
          <a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="ad in recentAds" :key="ad.id">
            <a-card class="ad-card" hoverable @click="viewAdDetail(ad)">
              <!-- Ad Image Preview -->
              <div class="ad-image relative" style="margin-bottom: 12px; text-align: center;">
                <!-- Status Badge -->
                <a-tag :color="getStatusColor(ad.status)" class="absolute top-2 right-2 z-10 text-xs">
                  {{ ad.status }}
                </a-tag>
                
                <!-- Image with Error Handling -->
                <div class="relative group">
                  <img 
                    v-if="ad.imageUrl || ad.mediaFileUrl"
                    :src="ad.imageUrl || ad.mediaFileUrl" 
                    :alt="ad.name"
                    class="w-full h-24 sm:h-28 object-cover rounded-lg border border-gray-200 cursor-pointer"
                    @error="handleImageError($event, ad.id)"
                    :key="ad.id + '-image'"
                    loading="lazy"
                  />
                  <div v-else class="w-full h-24 sm:h-28 bg-primary-100 rounded-lg flex items-center justify-center text-primary-600 font-semibold text-lg border border-primary-200">
                    {{ ad.name ? ad.name.charAt(0).toUpperCase() : 'A' }}
                  </div>
                  
                  <!-- Overlay on hover -->
                  <div class="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-all duration-200 rounded-lg flex items-center justify-center">
                    <svg class="w-6 h-6 text-white opacity-0 group-hover:opacity-100 transition-opacity duration-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                  </div>
                </div>
              </div>
              
              <!-- Ad Header -->
              <div class="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-3 gap-2">
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-sm sm:text-base text-gray-800 truncate mb-1" :title="ad.name">{{ ad.name }}</h3>
                  <p class="text-xs text-gray-500 truncate" :title="ad.campaignName">{{ ad.campaignName || 'No campaign' }}</p>
                </div>
              </div>
              
              <!-- Ad Type Badge -->
              <div class="mb-3">
                <a-tag size="small" class="text-xs">
                  <svg class="w-3 h-3 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path>
                  </svg>
                  {{ ad.adType?.replace('_', ' ') || 'Unknown' }}
                </a-tag>
              </div>
              
              <!-- Ad Content Preview -->
              <div v-if="ad.headline || ad.description || ad.primaryText" class="bg-gray-50 p-2 sm:p-3 rounded-lg mb-3">
                <div v-if="ad.headline" class="mb-2">
                  <p class="text-xs font-medium text-gray-600 mb-1">{{ $t('dashboard.headline') }}</p>
                  <p class="text-xs sm:text-sm font-semibold text-gray-800 line-clamp-1" :title="ad.headline">{{ ad.headline }}</p>
                </div>
                <div v-if="ad.description" class="mb-2">
                  <p class="text-xs font-medium text-gray-600 mb-1">{{ $t('dashboard.description') }}</p>
                  <p class="text-xs sm:text-sm text-gray-700 line-clamp-2" :title="ad.description">{{ ad.description }}</p>
                </div>
                <div v-if="ad.primaryText">
                  <p class="text-xs font-medium text-gray-600 mb-1">{{ $t('dashboard.primaryText') }}</p>
                  <p class="text-xs sm:text-sm text-gray-700 line-clamp-2" :title="ad.primaryText">{{ ad.primaryText }}</p>
                </div>
              </div>

              <!-- Footer -->
              <div class="flex justify-between items-center pt-3 border-t border-gray-100">
                <p class="text-xs text-gray-500">{{ formatDate(ad.createdDate) }}</p>
                <a-button size="small" type="primary" class="text-xs" @click.stop="viewAdDetail(ad)">
                  <svg class="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  {{ $t('dashboard.view') }}
                </a-button>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- Ad Detail Modal -->
    <a-modal
      v-model:visible="showDetailModal"
      :title="$t('dashboard.adDetails')"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedAd">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="Name">{{ selectedAd.name }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.campaign')">{{ selectedAd.campaignName || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.adType')">{{ selectedAd.adType?.replace('_', ' ') || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.status')">
            <a-tag :color="getStatusColor(selectedAd.status)">{{ selectedAd.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.headline')">{{ selectedAd.headline || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.primaryText')">{{ selectedAd.primaryText || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.description')">{{ selectedAd.description || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.callToAction')">{{ getCTALabel(selectedAd.callToAction) || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.media')">
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl">
              <div v-if="selectedAd.imageUrl" style="display: flex; align-items: center; gap: 12px;">
                <img :src="selectedAd.imageUrl" style="width: 80px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #f0f0f0;" />
                <a-button @click="showMediaModal = true">
                  <template #icon>
                    <eye-outlined />
                  </template>
                  {{ $t('dashboard.viewImage') }}
                </a-button>
              </div>
              <div v-else-if="selectedAd.videoUrl" style="display: flex; align-items: center; gap: 12px;">
                <div style="width: 80px; height: 80px; background: #f5f5f5; border-radius: 6px; display: flex; align-items: center; justify-content: center;">
                  <play-circle-outlined style="font-size: 24px; color: #1890ff;" />
                </div>
                <a-button @click="showMediaModal = true">
                  <template #icon>
                    <eye-outlined />
                  </template>
                  {{ $t('dashboard.viewVideo') }}
                </a-button>
              </div>
            </div>
            <span v-else>N/A</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('dashboard.createdDate')">{{ formatDate(selectedAd.createdDate) }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- Media Modal -->
    <a-modal
      v-model:visible="showMediaModal"
      :title="$t('dashboard.adMedia')"
      width="1000px"
      :footer="null"
    >
      <div v-if="selectedAd" style="text-align: center;">
        <div v-if="selectedAd.imageUrl">
          <img :src="selectedAd.imageUrl" :alt="selectedAd.name" style="max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);" />
          <a-typography-text type="secondary" style="display: block; margin-top: 16px;">{{ selectedAd.name }}</a-typography-text>
        </div>
        <div v-else-if="selectedAd.videoUrl">
          <video controls style="max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);">
            <source :src="selectedAd.videoUrl" type="video/mp4">
            <source :src="selectedAd.videoUrl" type="video/webm">
            Your browser does not support the video tag.
          </video>
          <a-typography-text type="secondary" style="display: block; margin-top: 16px;">{{ selectedAd.name }}</a-typography-text>
        </div>
        <div v-else style="padding: 48px;">
          <picture-outlined style="font-size: 48px; color: #d9d9d9; margin-bottom: 16px;" />
          <a-typography-text type="secondary">{{ $t('dashboard.noMediaAvailable') }}</a-typography-text>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapState, mapActions, mapGetters } from 'vuex'
import {
  PlusOutlined,
  FolderOutlined,
  FileTextOutlined,
  ThunderboltOutlined,
  EyeOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  BarChartOutlined,
  RocketOutlined
} from '@ant-design/icons-vue'
import MobileHeader from '@/components/MobileHeader.vue'
import MobileDashboardStats from '@/components/MobileDashboardStats.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import DashboardNextSteps from '@/components/DashboardNextSteps.vue'

export default {
  name: 'Dashboard',
  components: {
    PlusOutlined,
    FolderOutlined,
    FileTextOutlined,
    ThunderboltOutlined,
    EyeOutlined,
    PictureOutlined,
    PlayCircleOutlined,
    BarChartOutlined,
    RocketOutlined,
    MobileHeader,
    MobileDashboardStats,
    LoadingSkeleton,
    DashboardNextSteps
  },
  data() {
    return {
      quickActions: [
        {
          name: "Create Campaign",
          description: "Set up a new advertising campaign with your objectives and budget",
          icon: "FolderOutlined",
          link: "/campaign/create",
          buttonText: "Create Campaign",
          buttonType: "primary",
          color: "#2d5aa0"
        },
        {
          name: "Create Ad",
          description: "Create ad content with AI for your campaign",
          icon: "FileTextOutlined",
          link: "/ad/create",
          buttonText: "Create Ad",
          buttonType: "primary",
          color: "#16a085"
        },
        {
          name: "View Analytics",
          description: "Comprehensive insights and performance metrics for your campaigns",
          icon: "BarChartOutlined",
          link: "/analytics",
          buttonText: "View Analytics",
          buttonType: "default",
          color: "#8e44ad"
        },
        {
          name: "Optimization Center",
          description: "AI-powered recommendations to optimize your campaigns",
          icon: "RocketOutlined",
          link: "/optimization",
          buttonText: "Optimize",
          buttonType: "default",
          color: "#f4a261"
        },
      ],
      showDetailModal: false,
      showMediaModal: false,
      selectedAd: null,
      isMobile: false
    };
  },
  computed: {
    ...mapState('auth', ['user']),
    ...mapState('dashboard', ['stats', 'campaigns', 'recentAds', 'loading', 'error']),
    ...mapGetters('cta', {
      allCTAs: 'allCTAs',
      ctaLoaded: 'isLoaded'
    }),

    /**
     * Get CTA display label from enum value
     * @param {string} value - CTA enum value (e.g., 'LEARN_MORE')
     * @returns {string} - Display label (e.g., 'Tìm hiểu thêm')
     */
    getCTALabel() {
      return (value) => {
        if (!value) return ''
        const cta = this.allCTAs.find(c => c.value === value)
        return cta ? cta.label : value
      }
    },

    dashboardLoading() {
      return this.loading
    },
    
    dashboardError() {
      return this.error
    },
    
    hasDashboardData() {
      return this.stats && this.campaigns && this.recentAds
    },
    
    mobileStats() {
      return [
        {
          key: 'campaigns',
          label: 'Total Campaigns',
          value: this.stats?.totalCampaigns || 0,
          icon: 'pi pi-briefcase',
          variant: 'primary',
          change: 12
        },
        {
          key: 'ads',
          label: 'Active Ads',
          value: this.stats?.activeAds || 0,
          icon: 'pi pi-megaphone',
          variant: 'success',
          change: 8
        },
        {
          key: 'totalAds',
          label: 'Total Ads',
          value: this.stats?.totalAds || 0,
          icon: 'pi pi-file',
          variant: 'info',
          change: -3
        },
        {
          key: 'activeCampaigns',
          label: 'Active Campaigns',
          value: this.stats?.activeCampaigns || 0,
          icon: 'pi pi-bolt',
          variant: 'warning',
          change: 5
        }
      ]
    },
    
    recentActivity() {
      const activities = []
      
      // Add recent campaigns
      if (this.campaigns) {
        this.campaigns.slice(0, 3).forEach(campaign => {
          activities.push({
            id: `campaign-${campaign.id}`,
            type: 'campaign',
            title: 'Campaign Created',
            description: campaign.name,
            timestamp: campaign.createdDate,
            status: campaign.status
          })
        })
      }
      
      // Add recent ads
      if (this.recentAds) {
        this.recentAds.slice(0, 2).forEach(ad => {
          activities.push({
            id: `ad-${ad.id}`,
            type: 'ad',
            title: 'Ad Created',
            description: ad.name,
            timestamp: ad.createdDate,
            status: ad.status
          })
        })
      }
      
      return activities.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)).slice(0, 5)
    }
  },
  async mounted() {
    this.checkMobile()
    window.addEventListener('resize', this.checkMobile)
    await this.loadUserData()
    await this.loadDashboardData()
    // Load CTAs for displaying ad details
    await this.loadCTAs({ language: 'vi' })
  },
  
  beforeUnmount() {
    window.removeEventListener('resize', this.checkMobile)
  },
  methods: {
    ...mapActions('dashboard', ['fetchDashboardData']),
    ...mapActions('auth', ['fetchUser', 'logout']),
    ...mapActions('cta', ['loadCTAs']),

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
    
    getStatusColor(status) {
      switch (status?.toLowerCase()) {
        case 'active':
          return 'green'
        case 'paused':
          return 'orange'
        case 'draft':
          return 'default'
        default:
          return 'default'
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
    
    handleImageError(event, adId) {
      // Set fallback placeholder image using inline SVG data URI
      const ad = this.recentAds.find(a => a.id === adId)
      const adInitial = ad && ad.name ? ad.name.charAt(0).toUpperCase() : 'A'

      event.target.src = 'data:image/svg+xml,' + encodeURIComponent(`
        <svg width="100" height="100" xmlns="http://www.w3.org/2000/svg">
          <rect width="100" height="100" fill="#f0f4f7" stroke="#dae4eb" stroke-width="2" rx="8"/>
          <text x="50%" y="50%" text-anchor="middle" fill="#2d5aa0" font-size="48" font-weight="600" font-family="Arial, sans-serif" dy="0.35em">
            ${adInitial}
          </text>
        </svg>
      `)
    },
    
    checkMobile() {
      this.isMobile = window.innerWidth <= 768
    },
    
    toggleMobileMenu() {
      this.$emit('toggle-mobile-menu')
    },
    
    onThemeChanged(theme) {
      console.log('Theme changed to:', theme)
    },
    
    navigateToActivity() {
      this.$router.push('/notifications')
    },

    getRandomTip() {
      const tips = [
        "Try A/B testing different ad creatives for better performance!",
        "Use compelling call-to-actions to increase engagement",
        "Target specific audiences for higher conversion rates",
        "Monitor your ad performance regularly and optimize",
        "Create multiple ad variations to find what works best",
        "Use high-quality images to grab attention",
        "Keep your ad copy concise and impactful"
      ]
      return tips[Math.floor(Math.random() * tips.length)]
    },

    getMoodEmoji() {
      const emojis = ['🚀', '💪', '🎉', '⭐', '🔥', '💎', '🎯', '✨']
      return emojis[Math.floor(Math.random() * emojis.length)]
    },

    getMotivationalMessage() {
      const messages = [
        "Every expert was once a beginner",
        "Great ads start with great ideas",
        "Your creativity is your superpower",
        "Small steps lead to big results",
        "Perfect is the enemy of done",
        "Innovation comes from experimentation"
      ]
      return messages[Math.floor(Math.random() * messages.length)]
    },

    getContextualInsight() {
      // Provide contextual insights based on current state
      if (this.stats.totalCampaigns === 0) {
        return this.$t('dashboard.insights.noCampaigns')
      } else if (this.stats.totalAds === 0) {
        return this.$t('dashboard.insights.noAds')
      } else if (this.stats.activeCampaigns === 0) {
        return this.$t('dashboard.insights.lowActivity')
      } else if (this.stats.totalCampaigns > 0 && this.stats.totalAds > 0) {
        return this.$t('dashboard.insights.growing')
      } else {
        return this.getRandomTip()
      }
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 18px 22px 30px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 26px;
}

.loading-container,
.error-container {
  margin-bottom: 24px;
}

/* Creative Asymmetric Stats Layout */
.creative-stats-container {
  display: grid;
  grid-template-columns: 2fr 1.2fr 0.8fr;
  grid-template-rows: auto auto;
  gap: 18px 22px;
  margin-bottom: 34px;
}

.featured-stat-link {
  grid-row: span 2;
  text-decoration: none;
  display: block;
  transition: transform 0.3s ease;
}

.featured-stat-link:hover {
  transform: translateY(-2px);
}

.stat-card-link {
  text-decoration: none;
  display: block;
  transition: transform 0.3s ease;
}

.stat-card-link:hover {
  transform: translateY(-2px);
}

.stat-card-creative.primary-stat {
  background: linear-gradient(135deg, #2d5aa0 0%, #1e3a6f 100%);
  border-radius: 18px;
  padding: 26px;
  color: white;
  position: relative;
  overflow: hidden;
  height: 200px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.featured-stat-link:hover .stat-card-creative.primary-stat {
  box-shadow: 0 12px 32px rgba(45, 90, 160, 0.3);
}

.stat-visual {
  position: relative;
  flex-shrink: 0;
}

.stat-icon.primary-icon {
  font-size: 42px;
  color: rgba(255, 255, 255, 0.9);
}

.stat-pattern {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 60px;
  height: 60px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  opacity: 0.6;
}

.stat-content .stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 6px;
}

.stat-number.primary-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-growth {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.15);
  padding: 4px 10px;
  border-radius: 12px;
  display: inline-block;
}

.stat-action {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: gap 0.2s ease;
}

.featured-stat-link:hover .stat-action {
  gap: 8px;
}

.secondary-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-card-creative.secondary-stat {
  background: #ffffff;
  border: 1px solid #f0f2f5;
  border-radius: 14px;
  padding: 18px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.3s ease;
  height: 82px;
  cursor: pointer;
}

.stat-card-link:hover .stat-card-creative.secondary-stat {
  border-color: #d9d9d9;
  box-shadow: 0 6px 20px rgba(45, 90, 160, 0.12);
}

.stat-card-creative.secondary-stat .stat-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.stat-card-creative.secondary-stat.success .stat-icon {
  color: #16a085;
}

.stat-card-creative.secondary-stat.warning .stat-icon {
  color: #f4a261;
}

.stat-card-creative.secondary-stat.danger .stat-icon {
  color: #e76f51;
}

.stat-info .stat-number {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: #262626;
}

.stat-info .stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.stat-accent {
  position: absolute;
  right: 12px;
  top: 12px;
  width: 4px;
  height: 20px;
  border-radius: 2px;
}

.secondary-stat.success .stat-accent {
  background: #16a085;
}

.secondary-stat.warning .stat-accent {
  background: #f4a261;
}

.secondary-stat.danger .stat-accent {
  background: #e76f51;
}

.insight-card {
  background: #fff8e1;
  border: 1px solid #f4a261;
  border-radius: 14px;
  padding: 18px;
  position: relative;
}

.insight-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.insight-emoji {
  font-size: 20px;
  flex-shrink: 0;
  margin-top: 2px;
}

.insight-title {
  font-weight: 600;
  color: #d46b08;
  font-size: 13px;
  margin-bottom: 4px;
}

.insight-message {
  font-size: 12px;
  color: #7a4f01;
  line-height: 1.4;
}

/* Creative Quick Actions */
.creative-actions-section {
  margin-bottom: 36px;
}

.section-header-creative {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.section-title-creative {
  font-size: 26px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 6px 0;
  line-height: 1.2;
}

.section-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.header-decoration {
  font-size: 24px;
  opacity: 0.8;
}

.actions-grid-creative {
  display: grid;
  grid-template-columns: 1.4fr 1fr 0.6fr;
  grid-template-rows: auto auto;
  gap: 18px;
}

.action-card-primary {
  grid-row: span 2;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border: 2px solid #91d5ff;
  border-radius: 18px;
  padding: 28px;
  text-decoration: none;
  display: block;
  position: relative;
  overflow: hidden;
  transition: all 0.4s ease;
}

.action-card-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(24, 144, 255, 0.15);
  border-color: #40a9ff;
}

.action-primary-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 160px;
}

.action-visual {
  position: relative;
  margin-bottom: 20px;
}

.action-main-icon {
  font-size: 36px;
  color: #1890ff;
}

.action-glow {
  position: absolute;
  top: -8px;
  left: -8px;
  right: -8px;
  bottom: -8px;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  z-index: -1;
}

.action-title {
  font-size: 20px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 8px 0;
}

.action-desc {
  font-size: 14px;
  color: #595959;
  line-height: 1.4;
  margin-bottom: auto;
}

.action-cta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1890ff;
  margin-top: 20px;
}

.arrow-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.action-card-primary:hover .arrow-icon {
  transform: translateX(4px);
}

.actions-secondary {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.action-card-mini {
  background: #ffffff;
  border: 1px solid #f0f2f5;
  border-radius: 12px;
  padding: 16px;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  height: 70px;
}

.action-card-mini:hover {
  border-color: #d9d9d9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.action-card-mini.action-1 {
  border-left: 4px solid #16a085;
}

.action-card-mini.action-2 {
  border-left: 4px solid #8e44ad;
}

.action-card-mini.action-3 {
  border-left: 4px solid #f4a261;
}

.mini-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.action-1 .mini-icon {
  color: #16a085;
}

.action-2 .mini-icon {
  color: #8e44ad;
}

.action-3 .mini-icon {
  color: #f4a261;
}

.mini-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mini-title {
  font-size: 13px;
  font-weight: 600;
  color: #262626;
  white-space: pre-line;
  line-height: 1.2;
}

.mini-arrow {
  font-size: 14px;
  color: #8c8c8c;
  transition: all 0.3s ease;
}

.action-card-mini:hover .mini-arrow {
  color: #262626;
  transform: translate(2px, -2px);
}

.motivation-card {
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border: 1px solid #ffadd2;
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: center;
}

.motivation-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.motivation-icon {
  font-size: 22px;
  flex-shrink: 0;
  margin-top: 2px;
}

.motivation-title {
  font-size: 14px;
  font-weight: 600;
  color: #cf1322;
  margin-bottom: 4px;
}

.motivation-message {
  font-size: 11px;
  color: #a8071a;
  line-height: 1.3;
}

/* Existing styles for other sections */
.campaign-card,
.ad-card {
  height: 100%;
  transition: all 0.3s ease;
}

.campaign-card:hover,
.ad-card:hover {
  border-color: #dae4eb;
  box-shadow: 0 2px 8px rgba(45, 90, 160, 0.08);
}

.campaign-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.ad-card {
  cursor: pointer;
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .creative-stats-container {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
    gap: 16px;
  }

  .featured-stat {
    grid-row: span 1;
  }

  .stat-card-creative.primary-stat {
    height: 140px;
    padding: 20px;
    flex-direction: row;
  }

  .secondary-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .actions-grid-creative {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .action-card-primary {
    grid-row: span 1;
  }

  .actions-secondary {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .section-title-creative {
    font-size: 22px;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 16px;
  }
}

@media (max-width: 480px) {
  .secondary-stats {
    grid-template-columns: 1fr;
  }

  .actions-secondary {
    grid-template-columns: 1fr;
  }
}
</style>


    

