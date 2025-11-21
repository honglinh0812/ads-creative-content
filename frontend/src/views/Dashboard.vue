<template>
  <div class="dashboard-view">
    <MobileHeader v-if="isMobile" @toggle-mobile-menu="toggleMobileMenu" />

    <section class="hero-card surface-card">
      <div class="hero-text">
        <p class="eyebrow">{{ $t('dashboard.title') }}</p>
        <h1>{{ $t('dashboard.subtitle') }}</h1>
        <p class="hero-description">
          {{ $t('dashboard.readyToCreate') }}
        </p>
        <div class="hero-actions">
          <router-link to="/campaign/create">
            <a-button type="primary" size="large">
              <template #icon>
                <plus-outlined />
              </template>
              {{ $t('dashboard.newCampaign') }}
            </a-button>
          </router-link>
          <router-link to="/ad/create">
            <a-button size="large">
              {{ $t('dashboard.newAd') }}
            </a-button>
          </router-link>
        </div>
      </div>
      <div class="hero-meta">
        <div class="hero-stat">
          <p class="hero-stat-label">{{ $t('dashboard.totalCampaigns') }}</p>
          <p class="hero-stat-value">{{ stats?.totalCampaigns || 0 }}</p>
        </div>
        <div class="hero-stat">
          <p class="hero-stat-label">{{ $t('dashboard.activeAds') }}</p>
          <p class="hero-stat-value">{{ stats?.activeAds || 0 }}</p>
        </div>
      </div>
    </section>

    <a-alert
      v-if="dashboardError"
      type="error"
      show-icon
      :message="$t('dashboard.errorLoading')"
      :description="dashboardError"
      class="surface-card state-card"
    >
      <template #action>
        <a-button size="small" @click="loadDashboardData">
          {{ $t('dashboard.tryAgain') }}
        </a-button>
      </template>
    </a-alert>

    <div v-if="dashboardLoading" class="surface-card state-card">
      <a-skeleton active :paragraph="{ rows: 4 }" />
    </div>

    <template v-else>
      <section class="stat-grid" v-if="hasDashboardData">
        <div
          v-for="stat in overviewStats"
          :key="stat.key"
          class="stat-card surface-card"
        >
          <p class="stat-label">{{ stat.label }}</p>
          <p class="stat-value">{{ stat.value }}</p>
          <span class="stat-note">{{ stat.note }}</span>
        </div>
      </section>

      <div class="insight-grid">
        <a-card class="surface-card actions-card" :bordered="false">
          <div class="section-heading">
            <div>
              <p class="eyebrow">{{ $t('dashboard.readyToCreate') }}</p>
              <h2>{{ $t('dashboard.jumpIntoNext') }}</h2>
            </div>
          </div>
          <div class="action-list">
            <router-link
              v-for="action in quickActions"
              :key="action.name"
              :to="action.link"
              class="action-item"
            >
              <div class="action-content">
                <p class="action-title">{{ action.name }}</p>
                <p class="action-desc">{{ action.description }}</p>
              </div>
              <span class="action-link">{{ $t('dashboard.view') }}</span>
            </router-link>
          </div>
        </a-card>

        <a-card class="surface-card insight-panel" :bordered="false">
          <div class="section-heading">
            <div>
              <p class="eyebrow">{{ $t('dashboard.proTip') }}</p>
              <h2>{{ $t('dashboard.youDoingGreat') }}</h2>
            </div>
          </div>
          <p class="insight-message">
            {{ getContextualInsight() }}
          </p>
          <div class="activity-list" v-if="recentActivity.length">
            <div v-for="activity in recentActivity" :key="activity.id" class="activity-item">
              <div>
                <p class="activity-title">{{ activity.title }}</p>
                <p class="activity-desc">{{ activity.description }}</p>
              </div>
              <span class="activity-time">{{ formatDate(activity.timestamp) }}</span>
            </div>
          </div>
          <a-empty v-else :description="$t('dashboard.noData')" />
        </a-card>
      </div>

      <section class="surface-card section-card">
        <div class="section-heading">
          <div>
            <h2>{{ $t('dashboard.recentCampaigns') }}</h2>
            <p class="section-subtitle">{{ $t('dashboard.recentCampaignsDesc') }}</p>
          </div>
          <router-link to="/campaigns">
            <a-button>{{ $t('dashboard.viewAll') }}</a-button>
          </router-link>
        </div>
        <a-empty v-if="!campaigns.length" :description="$t('dashboard.noCampaignsYet')" />
        <div v-else class="card-grid">
          <article v-for="campaign in campaigns" :key="campaign.id" class="summary-card">
            <div class="summary-header">
              <p class="summary-title">{{ campaign.name }}</p>
              <a-tag :color="getStatusColor(campaign.status)">{{ campaign.status }}</a-tag>
            </div>
            <p class="summary-text">{{ campaign.objective }}</p>
            <div class="summary-meta">
              <div>
                <p class="meta-label">{{ $t('dashboard.budget') }}</p>
                <p class="meta-value">${{ campaign.budget }}</p>
              </div>
              <div>
                <p class="meta-label">{{ $t('dashboard.ads') }}</p>
                <p class="meta-value">{{ campaign.adCount }}</p>
              </div>
            </div>
            <div class="summary-footer">
              <span>{{ formatDate(campaign.createdDate) }}</span>
              <router-link :to="`/campaigns/${campaign.id}`">
                <a-button type="link" size="small">
                  {{ $t('dashboard.viewDetails') }}
                </a-button>
              </router-link>
            </div>
          </article>
        </div>
      </section>

      <section class="surface-card section-card">
        <div class="section-heading">
          <div>
            <h2>{{ $t('dashboard.recentAds') }}</h2>
            <p class="section-subtitle">{{ $t('dashboard.recentAdsDesc') }}</p>
          </div>
          <router-link to="/ads">
            <a-button>{{ $t('dashboard.viewAll') }}</a-button>
          </router-link>
        </div>

        <a-empty v-if="!recentAds.length" :description="$t('dashboard.noAdsYet')" />
        <div v-else class="card-grid">
          <article
            v-for="ad in recentAds"
            :key="ad.id"
            class="summary-card ad-summary"
            @click="viewAdDetail(ad)"
          >
            <div class="summary-header">
              <p class="summary-title">{{ ad.name }}</p>
              <a-tag :color="getStatusColor(ad.status)">{{ ad.status }}</a-tag>
            </div>
            <p class="summary-text">{{ ad.campaignName || $t('dashboard.noCampaign') }}</p>
            <div class="summary-body">
              <p v-if="ad.headline" class="summary-pill">
                {{ ad.headline }}
              </p>
              <p v-if="ad.primaryText" class="summary-pill">
                {{ ad.primaryText }}
              </p>
            </div>
            <div class="summary-footer">
              <span>{{ formatDate(ad.createdDate) }}</span>
              <a-button type="link" size="small" @click.stop="viewAdDetail(ad)">
                {{ $t('dashboard.view') }}
              </a-button>
            </div>
          </article>
        </div>
      </section>
    </template>

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
  EyeOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  BarChartOutlined,
  RocketOutlined
} from '@ant-design/icons-vue'
import MobileHeader from '@/components/MobileHeader.vue'

export default {
  name: 'Dashboard',
  components: {
    PlusOutlined,
    EyeOutlined,
    PictureOutlined,
    PlayCircleOutlined,
    BarChartOutlined,
    RocketOutlined,
    MobileHeader
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
    
    overviewStats() {
      const totals = this.stats || {}
      return [
        {
          key: 'campaigns',
          label: this.$t('dashboard.totalCampaigns'),
          value: totals.totalCampaigns || 0,
          note: this.$t('dashboard.viewAll')
        },
        {
          key: 'activeCampaigns',
          label: this.$t('dashboard.activeCampaigns'),
          value: totals.activeCampaigns || 0,
          note: this.$t('dashboard.readyToCreate')
        },
        {
          key: 'ads',
          label: this.$t('dashboard.totalAds'),
          value: totals.totalAds || 0,
          note: this.$t('dashboard.recentAds')
        },
        {
          key: 'activeAds',
          label: this.$t('dashboard.activeAds'),
          value: totals.activeAds || 0,
          note: this.$t('dashboard.jumpIntoNext')
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
.dashboard-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.surface-card {
  background: #fff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  padding: 32px;
  margin-bottom: 24px;
}

.hero-text h1 {
  margin: 8px 0 12px;
  font-size: 28px;
  color: #0f172a;
}

.eyebrow {
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.hero-description {
  margin: 0 0 20px;
  color: #475569;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-meta {
  display: grid;
  gap: 16px;
}

.hero-stat {
  padding: 16px 20px;
  border-radius: 16px;
  background: #f1f5f9;
}

.hero-stat-label {
  margin: 0;
  font-size: 13px;
  color: #475569;
}

.hero-stat-value {
  margin: 4px 0 0;
  font-size: 28px;
  font-weight: 600;
  color: #0f172a;
}

.state-card {
  padding: 24px;
  margin-bottom: 24px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 20px;
}

.stat-label {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #0f172a;
  margin: 8px 0;
}

.stat-note {
  color: #94a3b8;
  font-size: 12px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 4px 0 0;
  font-size: 20px;
  color: #0f172a;
}

.section-subtitle {
  margin: 4px 0 0;
  color: #64748b;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.action-item:hover {
  border-color: #94a3b8;
  transform: translateY(-2px);
}

.action-title {
  margin: 0 0 4px;
  font-weight: 600;
  color: #0f172a;
}

.action-desc {
  margin: 0;
  color: #475569;
  font-size: 14px;
}

.action-link {
  font-weight: 600;
  color: #1d4ed8;
}

.insight-panel {
  padding: 24px;
}

.insight-message {
  margin: 0 0 16px;
  color: #0f172a;
  font-weight: 500;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-title {
  margin: 0;
  font-weight: 600;
  color: #0f172a;
}

.activity-desc {
  margin: 2px 0 0;
  color: #475569;
  font-size: 14px;
}

.activity-time {
  color: #94a3b8;
  font-size: 12px;
}

.section-card {
  padding: 24px;
  margin-bottom: 24px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.summary-card {
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 16px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.summary-card:hover {
  border-color: #cbd5f5;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.summary-title {
  margin: 0;
  font-weight: 600;
  color: #0f172a;
}

.summary-text {
  margin: 0 0 12px;
  color: #475569;
  font-size: 14px;
}

.summary-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-label {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.meta-value {
  margin: 4px 0 0;
  font-weight: 600;
  color: #0f172a;
}

.summary-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #94a3b8;
  font-size: 13px;
}

.summary-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.summary-pill {
  margin: 0;
  padding: 8px 10px;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 13px;
}

@media (max-width: 768px) {
  .hero-card {
    flex-direction: column;
    padding: 24px;
  }

  .hero-actions {
    flex-direction: column;
    width: 100%;
  }

  .stat-grid,
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
