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
        title="Dashboard"
        sub-title="Overview of your campaigns and ads"
      >
        <template #extra>
          <a-space>
            <router-link to="/campaign/create">
              <a-button type="primary" size="large">
                <template #icon>
                  <plus-outlined />
                </template>
                New Campaign
              </a-button>
            </router-link>
            <router-link to="/ad/create">
              <a-button type="primary" size="large" style="background: #52c41a; border-color: #52c41a;">
                <template #icon>
                  <plus-outlined />
                </template>
                New Ad
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
        message="Error loading dashboard"
        :description="dashboardError"
        type="error"
        show-icon
        closable
      >
        <template #action>
          <a-button size="small" @click="loadDashboardData">
            Try Again
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
      
      <!-- Desktop Stats Cards -->
      <a-row v-else :gutter="[16, 16]" style="margin-bottom: 32px;">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card" hoverable>
            <a-statistic
              title="Total Campaigns"
              :value="stats.totalCampaigns || 0"
              :value-style="{ color: '#1890ff', fontSize: '32px', fontWeight: 'bold' }"
            >
              <template #prefix>
                <folder-outlined style="color: #1890ff; margin-right: 8px;" />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card" hoverable>
            <a-statistic
              title="Total Ads"
              :value="stats.totalAds || 0"
              :value-style="{ color: '#52c41a', fontSize: '32px', fontWeight: 'bold' }"
            >
              <template #prefix>
                <file-text-outlined style="color: #52c41a; margin-right: 8px;" />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card" hoverable>
            <a-statistic
              title="Active Campaigns"
              :value="stats.activeCampaigns || 0"
              :value-style="{ color: '#fa8c16', fontSize: '32px', fontWeight: 'bold' }"
            >
              <template #prefix>
                <thunderbolt-outlined style="color: #fa8c16; margin-right: 8px;" />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card class="stat-card" hoverable>
            <a-statistic
              title="Active Ads"
              :value="stats.activeAds || 0"
              :value-style="{ color: '#722ed1', fontSize: '32px', fontWeight: 'bold' }"
            >
              <template #prefix>
                <eye-outlined style="color: #722ed1; margin-right: 8px;" />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- Quick Actions -->
      <div class="section" style="margin-bottom: 32px;">
        <a-typography-title :level="2" style="margin-bottom: 8px;">Quick Actions</a-typography-title>
        <a-typography-text type="secondary" style="margin-bottom: 24px; display: block;">Get started with creating your campaigns and ads</a-typography-text>
        
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :md="6" v-for="action in quickActions" :key="action.name">
            <router-link :to="action.link" style="text-decoration: none;">
              <a-card class="action-card" hoverable style="height: 100%; border-radius: 12px; border: 1px solid #f0f0f0; transition: all 0.3s ease;">
                <div class="action-content" style="text-align: center; padding: 24px 16px;">
                  <div class="action-icon" :style="{ width: '64px', height: '64px', margin: '0 auto 16px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: action.color + '20', border: '2px solid ' + action.color + '40' }">
                    <component :is="action.icon" :style="{ fontSize: '28px', color: action.color }" />
                  </div>
                  <a-typography-title :level="4" style="margin-bottom: 8px; color: #262626;">{{ action.name }}</a-typography-title>
                  <a-typography-text type="secondary" style="font-size: 13px; line-height: 1.4; margin-bottom: 16px; display: block;">{{ action.description }}</a-typography-text>
                  <a-button :type="action.buttonType" block :style="{ borderColor: action.color, color: action.buttonType === 'primary' ? '#fff' : action.color, backgroundColor: action.buttonType === 'primary' ? action.color : 'transparent' }">
                    {{ action.buttonText }}
                  </a-button>
                </div>
              </a-card>
            </router-link>
          </a-col>
        </a-row>
      </div>

      <!-- Recent Campaigns -->
      <div class="section" style="margin-bottom: 32px;">
        <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
          <div>
            <a-typography-title :level="2" style="margin-bottom: 8px;">Recent Campaigns</a-typography-title>
            <a-typography-text type="secondary">Your latest campaigns and their performance</a-typography-text>
          </div>
          <router-link to="/campaigns">
            <a-button>View All</a-button>
          </router-link>
        </div>

        <div v-if="campaigns.length === 0">
          <a-empty description="No campaigns yet">
            <template #image>
              <folder-outlined style="font-size: 48px; color: #d9d9d9;" />
            </template>
            <router-link to="/campaign/create">
              <a-button type="primary">Create Your First Campaign</a-button>
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
                  <a-typography-text type="secondary" style="font-size: 12px;">Budget</a-typography-text>
                  <div style="font-weight: 500;">${{ campaign.budget }}</div>
                </a-col>
                <a-col :span="12">
                  <a-typography-text type="secondary" style="font-size: 12px;">Ads</a-typography-text>
                  <div style="font-weight: 500;">{{ campaign.adCount }}</div>
                </a-col>
              </a-row>
              
              <div style="display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid #f0f0f0;">
                <a-typography-text type="secondary" style="font-size: 12px;">{{ formatDate(campaign.createdDate) }}</a-typography-text>
                <router-link :to="`/campaigns/${campaign.id}`">
                  <a-button size="small" type="primary">View Details</a-button>
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
            <a-typography-title :level="2" style="margin-bottom: 8px;">Recent Ads</a-typography-title>
            <a-typography-text type="secondary">Your latest ad creations</a-typography-text>
          </div>
          <router-link to="/ads">
            <a-button>View All</a-button>
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
          <a-empty description="No ads yet">
            <template #image>
              <file-text-outlined style="font-size: 48px; color: #d9d9d9;" />
            </template>
            <router-link to="/ad/create">
              <a-button type="primary">Create Your First Ad</a-button>
            </router-link>
          </a-empty>
        </div>

        <!-- Ads Grid -->
        <a-row :gutter="[12, 12]" v-else>
          <a-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" v-for="ad in recentAds" :key="ad.id">
            <a-card class="ad-card hover:shadow-lg transition-all duration-200 hover:-translate-y-1" hoverable @click="viewAdDetail(ad)">
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
                    class="w-full h-24 sm:h-28 object-cover rounded-lg border border-gray-200 cursor-pointer transition-transform duration-200 group-hover:scale-105"
                    @error="handleImageError($event, ad.id)"
                    :key="ad.id + '-image'"
                    loading="lazy"
                  />
                  <div v-else class="w-full h-24 sm:h-28 bg-gradient-to-br from-blue-400 to-purple-500 rounded-lg flex items-center justify-center text-white font-bold text-lg shadow-sm">
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
                  <p class="text-xs font-medium text-gray-600 mb-1">Headline</p>
                  <p class="text-xs sm:text-sm font-semibold text-gray-800 line-clamp-1" :title="ad.headline">{{ ad.headline }}</p>
                </div>
                <div v-if="ad.description" class="mb-2">
                  <p class="text-xs font-medium text-gray-600 mb-1">Description</p>
                  <p class="text-xs sm:text-sm text-gray-700 line-clamp-2" :title="ad.description">{{ ad.description }}</p>
                </div>
                <div v-if="ad.primaryText">
                  <p class="text-xs font-medium text-gray-600 mb-1">Primary Text</p>
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
                  View
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
      title="Ad Details"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedAd">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="Name">{{ selectedAd.name }}</a-descriptions-item>
          <a-descriptions-item label="Campaign">{{ selectedAd.campaignName || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Ad Type">{{ selectedAd.adType?.replace('_', ' ') || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Status">
            <a-tag :color="getStatusColor(selectedAd.status)">{{ selectedAd.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="Headline">{{ selectedAd.headline || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Primary Text">{{ selectedAd.primaryText || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Description">{{ selectedAd.description || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Call to Action">{{ selectedAd.callToAction || 'N/A' }}</a-descriptions-item>
          <a-descriptions-item label="Media">
            <div v-if="selectedAd.imageUrl || selectedAd.videoUrl">
              <div v-if="selectedAd.imageUrl" style="display: flex; align-items: center; gap: 12px;">
                <img :src="selectedAd.imageUrl" style="width: 80px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #f0f0f0;" />
                <a-button @click="showMediaModal = true">
                  <template #icon>
                    <eye-outlined />
                  </template>
                  View Image
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
                  View Video
                </a-button>
              </div>
            </div>
            <span v-else>N/A</span>
          </a-descriptions-item>
          <a-descriptions-item label="Created Date">{{ formatDate(selectedAd.createdDate) }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- Media Modal -->
    <a-modal
      v-model:visible="showMediaModal"
      title="Ad Media"
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
          <a-typography-text type="secondary">No media available for this ad</a-typography-text>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
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
    LoadingSkeleton
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
          color: "#1890ff"
        },
        {
          name: "Create Ad",
          description: "Create ad content with AI for your campaign",
          icon: "FileTextOutlined",
          link: "/ad/create",
          buttonText: "Create Ad",
          buttonType: "primary",
          color: "#52c41a"
        },
        {
          name: "View Analytics",
          description: "Comprehensive insights and performance metrics for your campaigns",
          icon: "BarChartOutlined",
          link: "/analytics",
          buttonText: "View Analytics",
          buttonType: "default",
          color: "#722ed1"
        },
        {
          name: "Optimization Center",
          description: "AI-powered recommendations to optimize your campaigns",
          icon: "RocketOutlined",
          link: "/optimization",
          buttonText: "Optimize",
          buttonType: "default",
          color: "#fa8c16"
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
  },
  
  beforeUnmount() {
    window.removeEventListener('resize', this.checkMobile)
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
      // Thay thế ảnh bị lỗi bằng placeholder
      const img = event.target
      const placeholder = document.createElement('div')
      placeholder.style.cssText = 'width: 100px; height: 100px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; font-size: 14px;'
      
      const ad = this.recentAds.find(a => a.id === adId)
      placeholder.textContent = ad && ad.name ? ad.name.charAt(0).toUpperCase() : 'A'
      
      img.parentNode.replaceChild(placeholder, img)
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
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.loading-container,
.error-container {
  margin-bottom: 24px;
}

.stat-card {
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-card {
  height: 100%;
  transition: all 0.3s ease;
}

.action-card:hover {
  transform: translateY(-2px);
}

.action-content {
  text-align: center;
  padding: 16px;
}

.action-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 48px;
  height: 48px;
  background: #f0f9ff;
  border-radius: 50%;
  margin: 0 auto 12px;
}

.campaign-card,
.ad-card {
  height: 100%;
  transition: all 0.3s ease;
}

.campaign-card:hover,
.ad-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
  
  .section-header {
    flex-direction: column;
    align-items: flex-start !important;
    gap: 16px;
  }
}
</style>


    

