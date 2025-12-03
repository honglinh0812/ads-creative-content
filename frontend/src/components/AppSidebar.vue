<template>
  <button v-if="!sidebarOpen" class="sidebar-hamburger-fixed" @click="$emit('toggle')" :aria-label="$t('sidebar.openSidebar')">
    <MenuOutlined class="w-6 h-6" />
</button>
  <aside :class="['app-sidebar', { open: sidebarOpen }]">
    <button v-if="sidebarOpen" class="sidebar-hamburger" @click="$emit('toggle')" :aria-label="$t('sidebar.closeSidebar')">
      <MenuOutlined class="w-6 h-6" />
    </button>
    <div v-if="sidebarOpen" class="sidebar-content">
      <nav class="sidebar-menu" aria-label="Main navigation">
        <div
          v-for="item in menu"
          :key="item.path"
          class="sidebar-menu-group"
        >
          <router-link
            :to="item.path"
            class="sidebar-link"
            :class="{ active: isActive(item) }"
          >
            <span class="icon" v-if="item.icon"><component :is="item.icon" class="w-5 h-5 mr-2" /></span>
            <span>{{ item.label }}</span>
          </router-link>

          <div v-if="item.children" class="sidebar-submenu">
            <router-link
              v-for="child in item.children"
              :key="child.path"
              :to="child.path"
              class="sidebar-sublink"
              :class="{ active: isChildActive(child) }"
            >
              <span>{{ child.label }}</span>
            </router-link>
          </div>
        </div>
      </nav>
      <div class="sidebar-actions">
        <button v-if="!isDashboard" class="sidebar-back" @click="goDashboard">
          <ArrowLeftOutlined class="w-4 h-4 mr-1" />
          {{ $t('navigation.backToDashboard') }}
        </button>
      </div>
    </div>
    <!-- Overlay when sidebar is closed on mobile/tablet -->
    <div v-if="!sidebarOpen" class="sidebar-overlay" @click="$emit('toggle')"></div>

  </aside>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { HomeOutlined, ThunderboltOutlined, FileTextOutlined, MenuOutlined, ArrowLeftOutlined, TeamOutlined, FundOutlined, BulbOutlined, BarChartOutlined } from '@ant-design/icons-vue'

export default {
  name: 'AppSidebar',
  components: {
    HomeOutlined,
    ThunderboltOutlined,
    FileTextOutlined,
    MenuOutlined,
    TeamOutlined,
    FundOutlined,
    BulbOutlined,
    BarChartOutlined,
    ArrowLeftOutlined
  },
  props: {
    sidebarOpen: {
      type: Boolean,
      required: true
    }
  },
  emits: ['toggle'],
  setup() {
    const store = useStore()
    const route = useRoute()
    const router = useRouter()

    const { t } = useI18n()
    const translate = (key, fallback) => {
      const value = t(key)
      if (!value) return fallback
      return value === key ? fallback : value
    }
    const userName = computed(() => store.getters['auth/user']?.username || store.getters['auth/user']?.name || t('user.defaultName'))
    const userInitials = computed(() => userName.value.split(' ').map(w => w[0]).join('').toUpperCase())
    // Main menu items
    const menu = computed(() => [
      { label: translate('navigation.dashboard', 'Dashboard'), path: '/dashboard', icon: HomeOutlined, match: ['dashboard'] },
      {
        label: translate('navigation.campaigns', 'Campaigns'),
        path: '/campaigns',
        icon: ThunderboltOutlined,
        match: ['campaign', 'campaigns'],
        children: [
          { label: translate('navigation.allCampaigns', 'Campaign list'), path: '/campaigns' },
          { label: translate('navigation.createCampaign', 'Create campaign'), path: '/campaign/create' }
        ]
      },
      {
        label: translate('navigation.ads', 'Ads'),
        path: '/ads',
        icon: FileTextOutlined,
        match: ['/ads'],
        children: [
          { label: translate('navigation.createAd', 'Create ad'), path: '/ad/create' },
          { label: translate('navigation.mimicAds', 'Mimic ads'), path: '/ads/learn' }
        ]
      },
      { label: translate('navigation.analytics', 'Analytics'), path: '/analytics', icon: BarChartOutlined, match: ['analytics'] },
      { label: translate('navigation.optimization', 'Optimization'), path: '/optimization', icon: BulbOutlined, match: ['optimization'] },
      { label: translate('navigation.personas', 'Personas'), path: '/personas', icon: TeamOutlined, match: ['persona', 'personas'] },
      { label: translate('navigation.competitors', 'Competitors'), path: '/competitors', icon: FundOutlined, match: ['competitor', 'competitors'] }
    ])
    // Determine if menu item is active
    function isActive(item) {
      const directMatch = item.match.some(m => route.path.includes(m))
      if (item.children) {
        const childMatch = item.children.some(child => route.path.startsWith(child.path))
        return directMatch || childMatch
      }
      return directMatch
    }

    function isChildActive(child) {
      return route.path.startsWith(child.path)
    }
    const isDashboard = computed(() => route.path === '/dashboard')

    const goDashboard = () => {
      router.push('/dashboard')
    }

    return {
      menu,
      userName,
      userInitials,
      isActive,
      isChildActive,
      isDashboard,
      goDashboard,

    }
  }
}
</script>

<style scoped>
.app-sidebar {
  width: 248px;
  background: #f8fafc;
  color: #0f172a;
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 20px rgba(15, 23, 42, 0.08);
  transform: translateX(0);
  transition: transform 0.3s ease;
  font-family: 'Inter', 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-size: 15px;
}

.app-sidebar:not(.open) {
  transform: translateX(-100%);
}

.sidebar-hamburger-fixed,
.sidebar-hamburger {
  border: none;
  background: #fff;
  border-radius: 12px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  color: #0f172a;
}

.sidebar-hamburger-fixed {
  position: fixed;
  top: 24px;
  left: 24px;
  z-index: 1200;
}

.sidebar-hamburger {
  margin: 24px 0 16px 24px;
  align-self: flex-start;
}

.sidebar-content {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  margin-bottom: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
}

.logo-text {
  font-weight: 700;
  color: #0f172a;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
}

.menu-label {
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 12px;
  color: #94a3b8;
  margin: 0 0 12px;
  font-weight: 600;
}

.sidebar-menu-group {
  margin-bottom: 8px;
}

.sidebar-link,
.sidebar-sublink {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  color: #0f172a;
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
  transition: background 0.2s ease, color 0.2s ease;
}

.icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: inherit;
}

.sidebar-link:hover,
.sidebar-sublink:hover {
  background: #e4ecff;
  color: #1d4ed8;
}

.sidebar-link.active,
.sidebar-sublink.active {
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 600;
}

.sidebar-sublink {
  margin-left: 34px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.sidebar-actions {
  margin-top: 16px;
}

.sidebar-back {
  width: 100%;
  border: 1px solid #cbd5f5;
  border-radius: 12px;
  padding: 10px 14px;
  background: transparent;
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 15px;
}

.btn {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  background: transparent;
  color: #0f172a;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-outline {
  border: 1px solid #cbd5f5;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #1d4ed8;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.user-name {
  font-weight: 600;
  color: #0f172a;
  font-size: 15px;
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
}

@media (max-width: 992px) {
  .app-sidebar {
    width: 100%;
  }
}
</style>
