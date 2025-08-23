<template>
  <header class="header bg-white dark:bg-neutral-900 border-b border-neutral-200 dark:border-neutral-700 shadow-soft">
    <div class="header-left">
      <div class="header-brand">
        <h1 class="brand-text text-neutral-900 dark:text-neutral-100">Ads Creative</h1>
      </div>
    </div>
    <div class="header-right">
      <DarkModeToggle class="mr-3" />
      <div class="notification-dropdown-wrapper" style="position: relative; display: inline-block;">
        <button 
          class="icon-btn notification-btn text-neutral-600 dark:text-neutral-400 hover:text-primary-600 dark:hover:text-primary-400" 
          @click="toggleNotificationDropdown" 
          @keydown.enter="toggleNotificationDropdown"
          @keydown.space.prevent="toggleNotificationDropdown"
          @keydown.escape="closeNotificationDropdown"
          :aria-expanded="showNotificationDropdown"
          aria-haspopup="true"
          aria-label="Notifications"
          :aria-describedby="notificationCount > 0 ? 'notification-count' : undefined"
          tabindex="0" 
          title="Notifications"
        >
          <i class="pi pi-bell" aria-hidden="true"></i>
          <span 
            v-if="notificationCount > 0" 
            id="notification-count"
            class="badge bg-primary-500 text-white" 
            role="status" 
            aria-live="polite"
            :aria-label="`${notificationCount} unread notifications`"
          >
            {{ notificationCount }}
          </span>
        </button>
        <transition name="fade">
          <div 
            v-if="showNotificationDropdown" 
            class="notification-dropdown bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 shadow-strong" 
            role="menu"
            aria-label="Notifications menu"
            @click.stop
            @keydown.escape="closeNotificationDropdown"
          >
            <div class="notification-header">
              <h3 class="text-neutral-900 dark:text-neutral-100 font-semibold">Thông báo</h3>
            </div>
            <div v-if="recentNotifications.length > 0" class="notification-list" role="list">
              <div 
                v-for="noti in recentNotifications" 
                :key="noti.id" 
                class="notification-item hover:bg-neutral-50 dark:hover:bg-neutral-700"
                role="listitem"
                tabindex="0"
                @keydown.enter="handleNotificationClick(noti)"
                @keydown.space.prevent="handleNotificationClick(noti)"
                @click="handleNotificationClick(noti)"
                :aria-label="`Notification: ${noti.title || noti.type}. ${noti.message}. ${formatTime(noti.timestamp)}`"
              >
                <span class="noti-icon" :class="noti.type" aria-hidden="true">
                  <i v-if="noti.type==='success'" class="pi pi-check-circle text-success-500"></i>
                  <i v-else-if="noti.type==='error'" class="pi pi-times-circle text-error-500"></i>
                  <i v-else-if="noti.type==='warning'" class="pi pi-exclamation-triangle text-warning-500"></i>
                  <i v-else class="pi pi-info-circle text-primary-500"></i>
                </span>
                <div class="noti-content">
                  <div class="noti-title text-neutral-900 dark:text-neutral-100">{{ noti.title || noti.type }}</div>
                  <div class="noti-message text-neutral-600 dark:text-neutral-400">{{ noti.message }}</div>
                  <div class="noti-time text-neutral-500 dark:text-neutral-500">{{ formatTime(noti.timestamp) }}</div>
                </div>
              </div>
            </div>
            <div v-else class="notification-empty text-neutral-500 dark:text-neutral-400" role="status">Không có thông báo nào</div>
            <div class="notification-footer border-t border-neutral-200 dark:border-neutral-700">
              <button 
                class="check-all-btn text-primary-600 dark:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/20" 
                @click="goAllNotifications"
                @keydown.enter="goAllNotifications"
                @keydown.space.prevent="goAllNotifications"
                role="menuitem"
                aria-label="View all notifications"
              >
                Xem tất cả thông báo
              </button>
            </div>
          </div>
        </transition>
      </div>
      <div 
        class="user-menu hover:bg-neutral-100 dark:hover:bg-neutral-800" 
        @click="toggleDropdown" 
        tabindex="0" 
        role="button"
        :aria-expanded="showDropdown"
        aria-haspopup="true"
        aria-label="User menu"
        @keydown.enter="toggleDropdown" 
        @keydown.space.prevent="toggleDropdown"
        @keydown.escape="closeDropdown"
        @keydown.down.prevent="focusFirstMenuItem"
      >
        <div class="user-avatar">
          <div class="avatar-circle bg-primary-500 text-white" aria-hidden="true">
            {{ username.charAt(0).toUpperCase() }}
          </div>
        </div>
        <span class="username text-neutral-900 dark:text-neutral-100">{{ username }}</span>
        <i class="pi pi-chevron-down text-neutral-500 dark:text-neutral-400" aria-hidden="true"></i>
        <div 
          v-if="showDropdown" 
          class="dropdown bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 shadow-strong" 
          role="menu" 
          aria-label="User dropdown"
          @keydown.escape="closeDropdown"
          @keydown.up.prevent="focusPreviousMenuItem"
          @keydown.down.prevent="focusNextMenuItem"
        >
          <a 
            href="#" 
            @click.prevent="goProfile" 
            @keydown.enter="goProfile"
            @keydown.space.prevent="goProfile"
            tabindex="0" 
            role="menuitem" 
            class="text-neutral-900 dark:text-neutral-100 hover:bg-neutral-50 dark:hover:bg-neutral-700 dropdown-item"
            ref="profileMenuItem"
          >
            <i class="pi pi-user" aria-hidden="true"></i>
            Hồ sơ
          </a>
          <a 
            href="#" 
            @click.prevent="confirmLogout" 
            @keydown.enter="confirmLogout"
            @keydown.space.prevent="confirmLogout"
            tabindex="0" 
            role="menuitem" 
            class="text-error-600 dark:text-error-400 hover:bg-error-50 dark:hover:bg-error-900/20 dropdown-item"
            ref="logoutMenuItem"
          >
            <i class="pi pi-sign-out" aria-hidden="true"></i>
            Đăng xuất
          </a>
        </div>
      </div>
    </div>
    
    <!-- Logout confirmation dialog -->
    <div 
      v-if="showLogoutDialog" 
      class="logout-dialog-overlay" 
      @click="cancelLogout"
      role="dialog"
      aria-modal="true"
      aria-labelledby="logout-dialog-title"
      aria-describedby="logout-dialog-description"
    >
      <div class="logout-dialog" @click.stop @keydown.escape="cancelLogout">
        <div class="logout-dialog-content">
          <h3 id="logout-dialog-title">Xác nhận đăng xuất</h3>
          <p id="logout-dialog-description">Bạn có chắc chắn muốn đăng xuất?</p>
          <div class="logout-dialog-actions">
            <button 
              class="btn btn-secondary" 
              @click="cancelLogout"
              @keydown.enter="cancelLogout"
              ref="cancelButton"
              aria-label="Cancel logout"
            >
              Hủy
            </button>
            <button 
              class="btn btn-primary" 
              @click="doLogout"
              @keydown.enter="doLogout"
              aria-label="Confirm logout"
            >
              Đăng xuất
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
import { mapGetters } from 'vuex'
import DarkModeToggle from './DarkModeToggle.vue'
export default {
  name: 'AppHeader',
  components: {
    DarkModeToggle
  },
  data() {
    return {
      showDropdown: false,
      showNotificationDropdown: false,
      showLogoutDialog: false,
      defaultAvatar: 'https://avatars.githubusercontent.com/u/9919?s=200&v=4'
    }
  },
  computed: {
    ...mapGetters('auth', ['user']),
    ...mapGetters('toast', ['toasts']),
    avatar() {
      return this.user && this.user.avatar ? this.user.avatar : this.defaultAvatar
    },
    username() {
      return this.user && (this.user.username || this.user.name) ? (this.user.username || this.user.name) : 'User'
    },
    notificationCount() {
      return this.toasts.length
    },
    recentNotifications() {
      return [...this.toasts].sort((a, b) => b.timestamp - a.timestamp).slice(0, 5)
    }
  },
  methods: {
    toggleDropdown() {
      this.showDropdown = !this.showDropdown
    },
    closeDropdown() {
      this.showDropdown = false
    },
    goProfile() {
      this.showDropdown = false
      this.$router.push('/profile')
    },
    confirmLogout() {
      this.showDropdown = false
      this.showLogoutDialog = true
    },
    cancelLogout() {
      this.showLogoutDialog = false
    },
    doLogout() {
      this.showLogoutDialog = false
      this.$store.dispatch('auth/logout')
    },
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown
      if (this.showNotificationDropdown) {
        document.addEventListener('click', this.closeNotificationDropdown)
      }
    },
    closeNotificationDropdown(e) {
      if (!e || !e.target.closest('.notification-dropdown-wrapper')) {
        this.showNotificationDropdown = false
        document.removeEventListener('click', this.closeNotificationDropdown)
      }
    },
    goAllNotifications() {
      this.showNotificationDropdown = false
      this.$router.push('/notifications')
    },
    formatTime(ts) {
      if (!ts) return ''
      const d = new Date(ts)
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    },
    onSearch() {
      this.$emit('search', this.search)
    },
    handleNotificationClick(notification) {
      // Handle notification click - could navigate to specific page or mark as read
      console.log('Notification clicked:', notification)
      this.closeNotificationDropdown()
    },
    focusFirstMenuItem() {
      if (this.showDropdown) {
        this.$nextTick(() => {
          const firstItem = this.$refs.profileMenuItem
          if (firstItem) {
            firstItem.focus()
          }
        })
      }
    },
    focusNextMenuItem(event) {
      const menuItems = [this.$refs.profileMenuItem, this.$refs.logoutMenuItem]
      const currentIndex = menuItems.findIndex(item => item === event.target)
      const nextIndex = (currentIndex + 1) % menuItems.length
      menuItems[nextIndex]?.focus()
    },
    focusPreviousMenuItem(event) {
      const menuItems = [this.$refs.profileMenuItem, this.$refs.logoutMenuItem]
      const currentIndex = menuItems.findIndex(item => item === event.target)
      const prevIndex = currentIndex === 0 ? menuItems.length - 1 : currentIndex - 1
      menuItems[prevIndex]?.focus()
    }
  },
  mounted() {
    // Focus management for dialogs
    this.$watch('showLogoutDialog', (newVal) => {
      if (newVal) {
        this.$nextTick(() => {
          const cancelButton = this.$refs.cancelButton
          if (cancelButton) {
            cancelButton.focus()
          }
        })
      }
    })
  }
}
</script>

<style scoped>
/* Enhanced header styles */
.header {
  width: 100%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  position: sticky;
  top: 0;
  z-index: 40;
  backdrop-filter: blur(8px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Header brand */
.header-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.025em;
  background: linear-gradient(135deg, theme('colors.primary.600'), theme('colors.secondary.500'));
  background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.dark .brand-text {
  background: linear-gradient(135deg, theme('colors.primary.400'), theme('colors.secondary.400'));
  background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Header layout */
.header-left {
  flex: 1;
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* Icon buttons */
.icon-btn {
  background: none;
  border: none;
  position: relative;
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  background: theme('colors.neutral.100');
  transform: translateY(-1px);
}

.dark .icon-btn:hover {
  background: theme('colors.neutral.800');
}

.icon-btn:active {
  transform: translateY(0);
}

/* Badge */
.badge {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 0.75rem;
  border-radius: 9999px;
  padding: 0.125rem 0.375rem;
  font-weight: 600;
  line-height: 1;
  min-width: 1.25rem;
  text-align: center;
  border: 2px solid theme('colors.white');
}

.dark .badge {
  border-color: theme('colors.neutral.900');
}

/* User menu */
.user-menu {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  position: relative;
  cursor: pointer;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.user-menu:hover {
  border-color: theme('colors.neutral.200');
  transform: translateY(-1px);
  box-shadow: 0 4px 12px -2px rgb(0 0 0 / 10%);
}

.dark .user-menu:hover {
  border-color: theme('colors.neutral.700');
}

/* User avatar */
.user-avatar {
  position: relative;
}

.avatar-circle {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
  border: 2px solid theme('colors.white');
  box-shadow: 0 2px 8px rgb(0 0 0 / 10%);
}

.dark .avatar-circle {
  border-color: theme('colors.neutral.800');
}

/* Username */
.username {
  font-weight: 600;
  font-size: 0.875rem;
  letter-spacing: 0.025em;
}

/* Dropdown */
.dropdown {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 12rem;
  border-radius: 0.75rem;
  z-index: 50;
  display: flex;
  flex-direction: column;
  padding: 0.5rem;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.95);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.dropdown a {
  padding: 0.75rem 1rem;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: 0.5rem;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dropdown a:hover {
  transform: translateX(2px);
}

/* Focus styles */
.icon-btn:focus,
.user-menu:focus,
.dropdown a:focus {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 2px;
}

/* Notification dropdown */
.notification-dropdown-wrapper {
  position: relative;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 0.75rem);
  right: 0;
  min-width: 22rem;
  max-width: 24rem;
  border-radius: 1rem;
  z-index: 1000;
  max-height: 28rem;
  overflow: hidden;
  animation: slideDown 0.2s ease-out;
  backdrop-filter: blur(8px);
}

.notification-header {
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid theme('colors.neutral.200');
}

.dark .notification-header {
  border-bottom-color: theme('colors.neutral.700');
}

.notification-header h3 {
  font-size: 1rem;
  margin: 0;
}

.notification-list {
  max-height: 20rem;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid theme('colors.neutral.100');
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.dark .notification-item {
  border-bottom-color: theme('colors.neutral.700');
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  transform: translateX(2px);
}

.noti-icon {
  font-size: 1.25rem;
  margin-top: 0.125rem;
  flex-shrink: 0;
}

.noti-content {
  flex: 1;
  min-width: 0;
}

.noti-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
  line-height: 1.25;
}

.noti-message {
  font-size: 0.8125rem;
  line-height: 1.4;
  margin-bottom: 0.25rem;
}

.noti-time {
  font-size: 0.75rem;
  opacity: 0.8;
}

.notification-footer {
  padding: 0.75rem 1.25rem;
  text-align: center;
}

.check-all-btn {
  background: none;
  border: none;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  width: 100%;
}

.check-all-btn:hover {
  transform: translateY(-1px);
}

.notification-empty {
  text-align: center;
  padding: 2rem 1.25rem;
  font-size: 0.875rem;
  opacity: 0.8;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.18s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* Logout Dialog Styles */
.logout-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 60%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.logout-dialog {
  background: white;
  border-radius: 1rem;
  box-shadow: 0 20px 25px -5px rgb(0 0 0 / 10%), 0 10px 10px -5px rgb(0 0 0 / 4%);
  max-width: 28rem;
  width: 90%;
  animation: scaleIn 0.2s ease-out;
  transform-origin: center;
}

.dark .logout-dialog {
  background: theme('colors.neutral.800');
  border: 1px solid theme('colors.neutral.700');
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

.logout-dialog-content {
  padding: 2rem;
  text-align: center;
}

.logout-dialog-content h3 {
  margin: 0 0 1rem;
  font-size: 1.25rem;
  font-weight: 700;
  color: theme('colors.neutral.900');
  line-height: 1.25;
}

.dark .logout-dialog-content h3 {
  color: theme('colors.neutral.100');
}

.logout-dialog-content p {
  margin: 0 0 2rem;
  color: theme('colors.neutral.600');
  font-size: 0.875rem;
  line-height: 1.6;
  opacity: 0.9;
}

.dark .logout-dialog-content p {
  color: theme('colors.neutral.400');
}

.logout-dialog-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.logout-dialog-actions .btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.75rem;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 5rem;
}

.logout-dialog-actions .btn-secondary {
  background: theme('colors.neutral.100');
  color: theme('colors.neutral.700');
}

.logout-dialog-actions .btn-secondary:hover {
  background: theme('colors.neutral.200');
  transform: translateY(-1px);
}

.dark .logout-dialog-actions .btn-secondary {
  background: theme('colors.neutral.700');
  color: theme('colors.neutral.300');
}

.dark .logout-dialog-actions .btn-secondary:hover {
  background: theme('colors.neutral.600');
}

.logout-dialog-actions .btn-primary {
  background: theme('colors.error.500');
  color: white;
}

.logout-dialog-actions .btn-primary:hover {
  background: theme('colors.error.600');
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgb(239 68 68 / 40%);
}

/* Responsive Design */
@media (width <= 768px) {
  .header {
    padding: 0 1rem;
  }
  
  .header-right {
    gap: 0.5rem;
  }
  
  .username {
    display: none;
  }
  
  .notification-dropdown {
    min-width: 18rem;
    right: -1rem;
  }
  
  .dropdown {
    min-width: 10rem;
    right: -0.5rem;
  }
  
  .logout-dialog {
    width: 95%;
    margin: 1rem;
  }
  
  .logout-dialog-content {
    padding: 1.5rem;
  }
  
  .logout-dialog-actions {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .logout-dialog-actions .btn {
    width: 100%;
  }
}

@media (width <= 480px) {
  .header {
    padding: 0 0.75rem;
  }
  
  .brand-text {
    font-size: 1.25rem;
  }
  
  .icon-btn {
    padding: 0.5rem;
    font-size: 1.125rem;
  }
  
  .avatar-circle {
    width: 2rem;
    height: 2rem;
    font-size: 0.875rem;
  }
  
  .notification-dropdown {
    min-width: calc(100vw - 2rem);
    right: -0.75rem;
    left: -0.75rem;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .header {
    border-bottom-width: 2px;
  }
  
  .icon-btn:focus,
  .user-menu:focus,
  .dropdown a:focus,
  .check-all-btn:focus,
  .notification-item:focus {
    outline: 3px solid currentColor;
    outline-offset: 2px;
  }
  
  .badge {
    border-width: 2px;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .header,
  .icon-btn,
  .user-menu,
  .dropdown a,
  .notification-item,
  .check-all-btn,
  .logout-dialog-actions .btn {
    transition: none;
  }
  
  .dropdown,
  .notification-dropdown,
  .logout-dialog-overlay,
  .logout-dialog {
    animation: none;
  }
  
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}

/* Focus visible for better keyboard navigation */
.icon-btn:focus-visible,
.user-menu:focus-visible,
.dropdown a:focus-visible,
.check-all-btn:focus-visible,
.notification-item:focus-visible,
.logout-dialog-actions .btn:focus-visible {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 2px;
}

/* Improve dropdown item styling */
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dropdown-item i {
  font-size: 0.875rem;
  width: 1rem;
  text-align: center;
}

/* Notification item focus styles */
.notification-item:focus {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: -2px;
  background: theme('colors.primary.50');
}

.dark .notification-item:focus {
  background: theme('colors.primary.900/20');
}

/* Ensure minimum touch target size for mobile */
@media (pointer: coarse) {
  .icon-btn,
  .user-menu,
  .dropdown a,
  .check-all-btn,
  .notification-item {
    min-height: 44px;
    min-width: 44px;
  }
  
  .dropdown a,
  .check-all-btn {
    padding: 0.75rem 1rem;
  }
}
</style>