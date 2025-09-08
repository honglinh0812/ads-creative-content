<template>
  <div id="app" class="app-layout">
    <!-- Loading screen với Ant Design -->
    <div v-if="authLoading" class="auth-loading">
      <div class="loading-content">
        <div class="loading-logo">
          <a-icon type="thunderbolt" style="font-size: 48px; color: #1890ff;" />
        </div>
        <a-spin size="large" />
        <p class="loading-text">Đang tải ứng dụng...</p>
      </div>
    </div>
    
    <!-- Layout chính cho người dùng đã đăng nhập -->
    <template v-else-if="isAuthenticated">
      <a-layout class="main-layout">
        <!-- Sidebar -->
        <a-layout-sider 
          v-model:collapsed="collapsed" 
          :trigger="null" 
          collapsible
          class="sidebar"
          :width="240"
          theme="light"
        >
          <div class="logo">
            <img src="/logo.svg" alt="Logo" class="logo-img" />
            <span v-if="!collapsed" class="logo-text">Ads Creative</span>
          </div>
          
          <a-menu 
            mode="inline" 
            :selected-keys="selectedKeys"
            :open-keys="openKeys"
            @select="onMenuSelect"
            class="sidebar-menu"
          >
            <a-menu-item key="dashboard">
              <template #icon><a-icon type="dashboard" /></template>
              <span>Dashboard</span>
            </a-menu-item>
            
            <a-sub-menu key="campaigns">
              <template #icon><a-icon type="project" /></template>
              <template #title>Campaigns</template>
              <a-menu-item key="campaigns-list">All Campaigns</a-menu-item>
              <a-menu-item key="campaigns-create">Create Campaign</a-menu-item>
            </a-sub-menu>
            
            <a-sub-menu key="ads">
              <template #icon><a-icon type="picture" /></template>
              <template #title>Ads</template>
              <a-menu-item key="ads-list">All Ads</a-menu-item>
              <a-menu-item key="ads-create">Create Ad</a-menu-item>
            </a-sub-menu>
            
            <a-menu-item key="analytics">
              <template #icon><a-icon type="bar-chart" /></template>
              <span>Analytics</span>
            </a-menu-item>
            
            <a-menu-item key="optimization">
              <template #icon><a-icon type="rocket" /></template>
              <span>Optimization</span>
            </a-menu-item>
          </a-menu>
        </a-layout-sider>
        
        <!-- Main content -->
        <a-layout>
          <!-- Header -->
          <a-layout-header class="header">
            <div class="header-left">
              <a-button 
                type="text" 
                @click="collapsed = !collapsed"
                class="trigger"
              >
                <template #icon>
                  <a-icon :type="collapsed ? 'menu-unfold' : 'menu-fold'" />
                </template>
              </a-button>
              
              <a-breadcrumb class="breadcrumb">
                <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
                  <router-link v-if="item.path" :to="item.path">{{ item.name }}</router-link>
                  <span v-else>{{ item.name }}</span>
                </a-breadcrumb-item>
              </a-breadcrumb>
            </div>
            
            <div class="header-right">
              <!-- Notifications -->
              <a-badge :count="notificationCountFunc" class="notification-badge">
                <a-button type="text" shape="circle" @click="showNotifications">
                  <template #icon><a-icon type="bell" /></template>
                </a-button>
              </a-badge>
              <transition name="fade">
                <div v-if="showNotificationDropdown" class="notification-dropdown">
                  <div class="notification-header">Thông báo</div>
                  <div v-if="recentNotifications.length > 0" class="notification-list">
                    <div v-for="noti in recentNotifications" :key="noti.id" class="notification-item">
                      <span :class="noti.type">{{ noti.title || noti.type }}</span>
                      <div>{{ noti.message }}</div>
                      <div>{{ formatTime(noti.timestamp) }}</div>
                    </div>
                  </div>
                  <div v-else>Chưa có thông báo nào</div>
                  <div class="notification-footer">
                    <button @click="markAllAsRead">Đánh dấu tất cả đã đọc</button>
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
                  <a-icon type="down" />
                </a-button>
                <template #overlay>
                  <a-menu @click="onUserMenuClick">
                    <a-menu-item key="profile">
                      <a-icon type="user" />
                      Profile
                    </a-menu-item>
                    <a-menu-item key="settings">
                      <a-icon type="setting" />
                      Settings
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="logout">
                      <a-icon type="logout" />
                      Logout
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
      </a-layout>
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
</template>

<script>
import { mapGetters } from 'vuex'
import ToastNotifications from './components/ToastNotifications.vue'

export default {
  name: 'App',
  components: {
    ToastNotifications
  },
  data() {
    return {
      collapsed: false,
      selectedKeys: ['dashboard'],
      openKeys: [],
      breadcrumbs: [
        { name: 'Dashboard', path: '/dashboard' }
      ],
      showNotificationDropdown: false,
      notificationCount: 0
    }
  },
  computed: {
    ...mapGetters('auth', ['isAuthenticated', 'loading', 'user']),
    ...mapGetters('toast', ['toasts']),
    notificationCountFunc() {
      return this.toasts.length
    },
    recentNotifications() {
      return [...this.toasts].sort((a, b) => b.timestamp - a.timestamp).slice(0, 5)
    },
    authLoading() {
      return this.loading
    },
    username() {
      if (!this.user) return 'Người dùng'
      return this.user.name || this.user.username || this.user.email?.split('@')[0] || 'Người dùng'
    }
  },
  methods: {

    onMenuSelect({ key }) {
      this.selectedKeys = [key]
      // Handle menu navigation
      if (key === 'campaigns-list') {
        this.$router.push('/campaigns')
      } else if (key === 'campaigns-create') {
        this.$router.push('/campaign/create')
      } else if (key === 'ads-list') {
        this.$router.push('/ads')
      } else if (key === 'ads-create') {
        this.$router.push('/ad/create')
      } else {
        this.$router.push(`/${key}`)
      }
    },
    showNotifications() {
      this.showNotificationDropdown = !this.showNotificationDropdown
    },
    closeNotificationDropdown(event) {
      if (!event.target.closest('.notification-badge')) {
        this.showNotificationDropdown = false
      }
    },
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleString()
    },
    onUserMenuClick({ key }) {
      if (key === 'logout') {
        this.$store.dispatch('auth/logout')
        this.$router.push('/login')
      } else if (key === 'profile') {
        this.$router.push('/profile')
      } else if (key === 'settings') {
        this.$router.push('/settings')
      }
    },
    toggleMobileMenu() {
      // Toggle mobile menu through sidebar component
      if (this.$refs.sidebar) {
        this.$refs.sidebar.toggleMobileMenu()
      }
    }
  },
  async created() {
    // Fetch user data if authenticated
    if (this.isAuthenticated && !this.user) {
      try {
        await this.$store.dispatch('auth/fetchUser')
      } catch (error) {
        console.error('Failed to fetch user data:', error)
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

.sidebar {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.logo-text {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #1890ff;
}

.sidebar-menu {
  border-right: none;
}

.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 99;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
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
  transition: color 0.3s;
}

.trigger:hover {
  color: #1890ff;
}

.breadcrumb {
  margin: 0;
}

.notification-badge {
  margin-right: 8px;
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
  
  .sidebar {
    position: fixed;
    height: 100vh;
    z-index: 1000;
  }
  
  .logo-text {
    display: none;
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

