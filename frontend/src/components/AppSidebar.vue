<template>
  <!-- Hamburger button outside sidebar, shown when sidebar is closed -->
  <button v-if="!sidebarOpen" class="sidebar-hamburger-fixed" @click="$emit('toggle')" :aria-label="$t('sidebar.openSidebar')">
    <MenuOutlined class="w-6 h-6" />
  </button>
  <aside :class="['app-sidebar', { open: sidebarOpen }]">
    <!-- Hamburger button inside sidebar, shown when sidebar is open -->
    <button v-if="sidebarOpen" class="sidebar-hamburger" @click="$emit('toggle')" :aria-label="$t('sidebar.closeSidebar')">
      <MenuOutlined class="w-6 h-6" />
    </button>
    <div v-if="sidebarOpen" class="sidebar-content">
      <div class="sidebar-header">
        <router-link to="/dashboard" class="logo">
          <img src="/logo.svg" alt="Logo" class="logo-img" />
          <span class="logo-text">Ads Creative</span>
        </router-link>
      </div>
      <nav class="sidebar-menu">
        <router-link
          v-for="item in menu"
          :key="item.path"
          :to="item.path"
          class="sidebar-link"
          :class="{ active: isActive(item) }"
        >
          <span class="icon" v-if="item.icon"><component :is="item.icon" class="w-5 h-5 mr-2" /></span>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="sidebar-actions">
        <button v-if="!isDashboard" class="btn btn-sm btn-outline w-full mb-2" @click="goDashboard">
          <ArrowLeftOutlined class="w-4 h-4 mr-1" />
          {{ $t('navigation.backToDashboard') }}
        </button>
      </div>
      <div class="sidebar-footer">
        <div class="user-info">
          <span class="user-avatar">{{ userInitials }}</span>
          <span class="user-name">{{ userName }}</span>
        </div>
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
import { HomeOutlined, ThunderboltOutlined, FileTextOutlined, MenuOutlined, ArrowLeftOutlined, TeamOutlined, FundOutlined } from '@ant-design/icons-vue'

export default {
  name: 'AppSidebar',
  components: {
    HomeOutlined,
    ThunderboltOutlined,
    FileTextOutlined,
    MenuOutlined,
    ArrowLeftOutlined,
    TeamOutlined,
    FundOutlined
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
    const userName = computed(() => store.getters['auth/user']?.username || store.getters['auth/user']?.name || t('user.defaultName'))
    const userInitials = computed(() => userName.value.split(' ').map(w => w[0]).join('').toUpperCase())
    // Main menu items
    const menu = computed(() => [
      { label: t('navigation.dashboard'), path: '/dashboard', icon: HomeOutlined, match: ['dashboard'] },
      { label: t('navigation.campaigns'), path: '/campaigns', icon: ThunderboltOutlined, match: ['campaign', 'campaigns'] },
      { label: t('navigation.ads'), path: '/ads', icon: FileTextOutlined, match: ['ad', 'ads'] },
      { label: t('navigation.personas') || 'Personas', path: '/personas', icon: TeamOutlined, match: ['persona', 'personas'] },
      { label: t('navigation.competitors') || 'Competitors', path: '/competitors', icon: FundOutlined, match: ['competitor', 'competitors'] }
    ])
    // Determine if menu item is active
    function isActive(item) {
      return item.match.some(m => route.path.includes(m))
    }
    // Check if current page is dashboard
    const isDashboard = computed(() => route.path === '/dashboard')
    // Navigate back to dashboard
    function goDashboard() {
      router.push('/dashboard')
    }

    return {
      menu,
      userName,
      userInitials,
      isActive,
      isDashboard,
      goDashboard,

    }
  }
}
</script>

<style scoped>
.app-sidebar {
  width: 240px;
  background: var(--sidebar-bg, #fff);
  color: var(--sidebar-text, #222);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgb(0 0 0 / 4%);
  transition: transform 0.2s cubic-bezier(.4,0,.2,1);
}

.app-sidebar:not(.open) {
  transform: translateX(-100%);
}

.sidebar-hamburger-fixed {
  position: fixed;
  top: 1rem;
  left: 1rem;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 210;
  box-shadow: 0 2px 8px rgb(0 0 0 / 4%);
  transition: all 0.2s ease;
}

.sidebar-hamburger {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 110;
  box-shadow: 0 2px 8px rgb(0 0 0 / 4%);
  transition: all 0.2s ease;
}

.sidebar-content {
  margin-top: 56px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
}

.sidebar-header {
  display: flex;
  align-items: center;
  padding: 1.5rem 1rem 1rem;
  border-bottom: 1px solid #eee;
}

.logo {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.logo-img {
  width: 32px;
  height: 32px;
  margin-right: 0.5rem;
}

.logo-text {
  font-weight: bold;
  font-size: 1.2rem;
  color: var(--sidebar-text, #222);
}

.sidebar-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 1rem 0;
}

.sidebar-link {
  display: flex;
  align-items: center;
  padding: 0.75rem 1.5rem;
  color: inherit;
  text-decoration: none;
  border-radius: 6px;
  margin-bottom: 0.25rem;
  transition: background 0.15s;
}

.sidebar-link.active, .sidebar-link:hover {
  background: var(--sidebar-active-bg, #f3f4f6);
  color: var(--sidebar-active-text, #2563eb);
}

.icon {
  display: flex;
  align-items: center;
}

.sidebar-actions {
  padding: 0 1rem 1rem;
}

.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid #eee;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: auto;
}

.theme-toggle-section {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.dark .theme-toggle-section {
  border-bottom-color: #374151;
}

.dark .sidebar-footer {
  border-top-color: #374151;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: #2563eb;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 0.5rem;
}

.user-name {
  font-size: 1rem;
  font-weight: 500;
}

.sidebar-overlay {
  display: none;
}

@media (width <= 900px) {
  .app-sidebar {
    transform: translateX(-100%);
    position: fixed;
    z-index: 200;
    width: 240px;
  }

  .app-sidebar.open {
    transform: translateX(0);
  }

  .sidebar-hamburger-fixed {
    position: fixed;
    top: 1rem;
    left: 1rem;
    z-index: 210;
  }

  .sidebar-hamburger {
    display: block;
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgb(0 0 0 / 10%);
    z-index: 99;
  }
}

.logout-dialog {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 20%);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-dialog-content {
  background: #fff;
  border-radius: 12px;
  padding: 2rem 2.5rem;
  box-shadow: 0 4px 32px rgb(0 0 0 / 12%);
  min-width: 300px;
  text-align: center;
}
</style>