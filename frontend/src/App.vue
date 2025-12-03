<template>
  <!-- Wrap entire app with ConfigProvider for Ant Design locale support (Issue: I18n Phase 1) -->
  <a-config-provider :locale="antdLocale">
    <div id="app" class="app-layout">
      <!-- Loading screen với Ant Design -->
      <div v-if="authLoading" class="auth-loading">
        <div class="loading-content">
          <div class="loading-logo">
            <font-awesome-icon icon="bolt" style="font-size: 48px; color: #1890ff;" />
          </div>
          <a-spin size="large" />
          <p class="loading-text">{{ $t('app.loading') }}</p>
        </div>
      </div>
    
    <!-- Layout chính cho người dùng đã đăng nhập -->
    <template v-else-if="isAuthenticated">
      <div class="main-layout">
        <AppSidebar :sidebarOpen="sidebarOpen" @toggle="toggleSidebar" />

        <!-- Main content -->
        <a-layout class="content-layout" :class="{ 'sidebar-open': sidebarOpen }">
          <!-- Header -->
          <a-layout-header class="header">
            <div class="header-right">
              <!-- Language Switcher -->
              <LanguageSwitcher />

              <!-- Notifications -->
              <a-badge :count="notificationCountFunc" class="notification-badge">
                <a-button type="text" shape="circle" @click="showNotifications">
                  <template #icon><font-awesome-icon icon="bell" /></template>
                </a-button>
              </a-badge>
              <transition name="fade">
                <div v-if="showNotificationDropdown" class="notification-dropdown">
                  <div class="notification-header">{{ $t('notifications.title') }}</div>
                  <div v-if="recentNotifications.length > 0" class="notification-list">
                    <div
                      v-for="noti in recentNotifications"
                      :key="noti.id"
                      :class="['notification-item', { 'notification-read': noti.read }]"
                      @click="handleNotificationClick(noti)"
                    >
                      <span :class="noti.type">{{ noti.title || noti.type }}</span>
                      <div>{{ noti.message }}</div>
                      <div>{{ formatTime(noti.timestamp) }}</div>
                    </div>
                  </div>
                  <div v-else>{{ $t('notifications.noNotifications') }}</div>
                  <div class="notification-footer">
                    <button @click="markAllAsRead">{{ $t('notifications.markAllAsRead') }}</button>
                  </div>
                </div>
              </transition>
              
              <!-- User menu -->
              <a-dropdown>
                <a-button type="text" class="user-menu">
                  <a-avatar size="small" :style="{ backgroundColor: '#1890ff' }">
                    {{ username.charAt(0).toUpperCase() }}
                  </a-avatar>
                  <span class="username">{{ username }}</span>
                  <font-awesome-icon icon="chevron-down" />
                </a-button>
                <template #overlay>
                  <a-menu @click="onUserMenuClick">
                    <a-menu-item key="settings">
                      <font-awesome-icon icon="gear" />
                      {{ $t('navigation.settings') }}
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="logout">
                      <font-awesome-icon icon="arrow-right-from-bracket" />
                      {{ $t('auth.logout') }}
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </a-layout-header>
          
          <!-- Content -->
          <a-layout-content class="content">
            <div class="content-wrapper">
              <transition name="fade-slide" mode="out-in">
                <router-view />
              </transition>
            </div>
          </a-layout-content>
        </a-layout>
      </div>
    </template>

    <!-- Layout cho người dùng chưa đăng nhập -->
    <template v-else>
      <div class="auth-layout">
        <transition name="fade-slide" mode="out-in">
          <router-view />
        </transition>
      </div>
    </template>

      <!-- Toast notifications -->
      <ToastNotifications />
    </div>
  </a-config-provider>
</template>

<script>
import { mapGetters } from 'vuex'
import ToastNotifications from './components/ToastNotifications.vue'
import LanguageSwitcher from './components/LanguageSwitcher.vue'
import AppSidebar from './components/AppSidebar.vue'

export default {
  name: 'App',
  components: {
    ToastNotifications,
    LanguageSwitcher,
    AppSidebar
  },
  data() {
    return {
      sidebarOpen: true,
      isMobile: false,
      showNotificationDropdown: false
    }
  },
  computed: {
    ...mapGetters('auth', ['isAuthenticated', 'loading', 'user']),
    ...mapGetters('toast', ['toasts', 'unreadCount']),
    ...mapGetters('locale', { antdLocale: 'antdLocale' }), // Issue: I18n Phase 1
    notificationCountFunc() {
      return this.unreadCount
    },
    recentNotifications() {
      return [...this.toasts].sort((a, b) => b.timestamp - a.timestamp).slice(0, 5)
    },
    authLoading() {
      return this.loading
    },
    username() {
      if (!this.user) return this.$t('user.defaultName')
      return this.user.name || this.user.username || this.user.email?.split('@')[0] || this.$t('user.defaultName')
    }
  },
  methods: {
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    handleResize() {
      if (typeof window === 'undefined') return
      const previousIsMobile = this.isMobile
      this.isMobile = window.innerWidth <= 900
      if (this.isMobile) {
        this.sidebarOpen = false
      } else if (previousIsMobile) {
        this.sidebarOpen = true
      }
    },
    showNotifications() {
      this.showNotificationDropdown = !this.showNotificationDropdown
    },
    closeNotificationDropdown(event) {
      if (!event || !event.target.closest('.notification-badge')) {
        this.showNotificationDropdown = false
      }
    },
    markAllAsRead() {
      this.showNotificationDropdown = false
      this.$store.dispatch('toast/markAllAsRead')
    },
    handleNotificationClick(notification) {
      if (notification?.id) {
        this.$store.dispatch('toast/markToastRead', notification.id)
      }
      this.showNotificationDropdown = false
    },
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString()
    },
    onUserMenuClick({ key }) {
      if (key === 'logout') {
        this.$store.dispatch('auth/logout')
        this.$router.push('/login')
      } else if (key === 'settings') {
        this.$router.push('/settings')
      }
    }
  },
  mounted() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.handleResize)
      this.handleResize()
    }
  },
  beforeUnmount() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.handleResize)
    }
  },
  async created() {
    // Fetch user data if authenticated
    if (this.isAuthenticated && !this.user) {
      try {
        await this.$store.dispatch('auth/fetchUser')
      } catch (error) {
        console.error('Failed to fetch user data:', error)
        // If fetchUser fails due to 401, clear auth and redirect
        if (error.response && error.response.status === 401) {
          this.$store.dispatch('auth/logout')
          this.$router.push('/login')
        }
      }
    }
  }
}
</script>

<style>
/* Global app styles */
#app.app-layout {
  min-height: 100vh;
  font-family: Inter, sans-serif;
}

/* Auth loading styles */
.auth-loading {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  background: linear-gradient(135deg, #1890ff, #722ed1);
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  text-align: center;
}

.loading-logo {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.loading-text {
  color: white;
  font-size: 1.125rem;
  font-weight: 500;
  opacity: 0.9;
}

/* Auth layout for unauthenticated users */
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: #f0f2f5;
}

/* Ant Design Layout Styles */
.main-layout {
  min-height: 100vh;
}

.content-layout {
  min-height: 100vh;
  margin-left: 0;
  transition: margin-left 0.2s ease;
}

@media (min-width: 900px) {
  .content-layout.sidebar-open {
    margin-left: 240px;
  }
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 500;
  backdrop-filter: blur(8px);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.trigger {
  font-size: 18px;
  line-height: 64px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.65);
}

.trigger:hover {
  color: #1890ff;
  background: #f0f7ff;
  border-color: #d6e4ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.trigger:active {
  transform: scale(0.95);
}

/* Icon trong header */
.trigger svg,
.notification-badge svg {
  vertical-align: middle;
}

.trigger svg {
  width: 20px;
  height: 20px;
}

.notification-badge svg {
  width: 18px;
  height: 18px;
}

.notification-badge {
  margin-right: 8px;
  position: relative;
  z-index: 600;
  display: inline-block;
}

.notification-badge .ant-btn {
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.notification-badge .ant-btn:hover {
  border-color: #1890ff;
  background: #f0f7ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.notification-badge .ant-badge-count {
  background: #ff4d4f;
  box-shadow: 0 2px 6px rgba(255, 77, 79, 0.5);
  animation: pulse-badge 2s ease-in-out infinite;
}

@keyframes pulse-badge {
  0%, 100% {
    box-shadow: 0 2px 6px rgba(255, 77, 79, 0.5);
  }
  50% {
    box-shadow: 0 2px 12px rgba(255, 77, 79, 0.7);
  }
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  min-width: 320px;
  max-width: 400px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
}

.notification-header {
  padding: 16px 20px;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.notification-item:hover {
  background: #f5f5f5;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.notification-read {
  opacity: 0.7;
}

.notification-footer {
  padding: 12px 20px;
  text-align: center;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.notification-footer button {
  background: none;
  border: none;
  color: #1890ff;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.notification-footer button:hover {
  background: #e6f7ff;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  height: auto;
  padding: 8px 12px;
}

.username {
  margin-left: 8px;
  margin-right: 4px;
}

/* User menu icons */
.ant-dropdown-menu-item svg {
  margin-right: 8px;
  width: 14px;
  height: 14px;
  vertical-align: middle;
}

.user-menu svg {
  width: 12px;
  height: 12px;
}

.content {
  margin: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.content-wrapper {
  padding: 24px;
  min-height: calc(100vh - 112px);
}

/* Transition effects */
.fade-slide-enter-active {
  transition: all 0.3s ease-out;
}

.fade-slide-leave-active {
  transition: all 0.3s ease-in;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.fade-slide-enter-to,
.fade-slide-leave-from {
  opacity: 1;
  transform: translateY(0);
}

/* Animations */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }
  
  .content {
    margin: 16px;
  }
  
  .content-wrapper {
    padding: 16px;
  }
}

@media (max-width: 576px) {
  .header {
    padding: 0 12px;
  }
  
  .content {
    margin: 12px;
  }
  
  .content-wrapper {
    padding: 12px;
  }
  
  .header-right .ant-input-search {
    display: none;
  }
}

/* Accessibility */
@media (prefers-reduced-motion: reduce) {
  .fade-slide-enter-active,
  .fade-slide-leave-active {
    transition: opacity 0.2s ease;
  }
  
  .fade-slide-enter-from,
  .fade-slide-leave-to {
    transform: none;
  }
  
  .loading-logo {
    animation: none;
  }
}
</style>
