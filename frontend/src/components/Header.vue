<template>
  <header class="header bg-white dark:bg-neutral-900 border-b border-neutral-200 dark:border-neutral-700 shadow-soft">
    <div class="header-left">
      <div class="header-brand" @click="handleBrandClick">
        <h1 class="brand-text text-neutral-900 dark:text-neutral-100" :class="{ 'brand-sparkle': brandClicks >= 5 }">
          Ads Creative
          <span v-if="brandClicks >= 3" class="brand-easter-egg">{{ getBrandEasterEgg() }}</span>
        </h1>
        <div v-if="showDeveloperSignature" class="developer-signature-popup">
          <div class="signature-content">
            <div class="signature-emoji">👨‍💻</div>
            <p class="signature-text">{{ getDeveloperMessage() }}</p>
            <small class="signature-credit">{{ getDeveloperCredit() }}</small>
          </div>
        </div>
      </div>
      <!-- Search functionality removed as requested -->
    </div>
    <div class="header-right">
      <!-- Theme Toggle -->
      <ThemeToggle />

      <div class="notification-dropdown-wrapper" style="position: relative; display: inline-block;">
        <button 
          class="icon-btn notification-btn text-neutral-600 dark:text-neutral-400 hover:text-primary-600 dark:hover:text-primary-400" 
          @click="toggleNotificationDropdown" 
          @keydown.enter="toggleNotificationDropdown"
          @keydown.space.prevent="toggleNotificationDropdown"
          @keydown.escape="closeNotificationDropdown"
          :aria-expanded="showNotificationDropdown"
          aria-haspopup="true"
          :aria-label="$t('notifications.title')"
          :aria-describedby="notificationCount > 0 ? 'notification-count' : undefined"
          tabindex="0" 
          :title="$t('notifications.title')"
        >
          <i class="fas fa-bell" aria-hidden="true"></i>
          <span 
            v-if="notificationCount > 0" 
            id="notification-count"
            class="badge bg-primary-500 text-white" 
            role="status" 
            aria-live="polite"
            :aria-label="$t('notifications.unreadAria', { count: notificationCount })"
          >
            {{ notificationCount > 99 ? '99+' : notificationCount }}
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
              <h3 class="text-neutral-900 dark:text-neutral-100 font-semibold">{{ $t('notifications.title') }}</h3>
            </div>
            <div v-if="recentNotifications.length > 0" class="notification-list" role="list">
              <div 
                v-for="noti in recentNotifications" 
                :key="noti.id" 
                :class="[
                  'notification-item',
                  getNotificationTypeClass(noti),
                  { 'notification-read': noti.read }
                ]"
                role="listitem"
                tabindex="0"
                @keydown.enter="handleNotificationClick(noti)"
                @keydown.space.prevent="handleNotificationClick(noti)"
                @click="handleNotificationClick(noti)"
                :aria-label="`${$t('notifications.title')}: ${getNotificationTitle(noti)}. ${noti.message}. ${formatTime(noti.timestamp)}`"
              >
                <span class="noti-icon" :class="normalizeNotificationType(noti)" aria-hidden="true">
                  <i :class="getNotificationIcon(normalizeNotificationType(noti))"></i>
                </span>
                <div class="noti-content">
                  <div class="noti-title text-neutral-900 dark:text-neutral-100">{{ getNotificationTitle(noti) }}</div>
                  <div class="noti-message text-neutral-600 dark:text-neutral-400">{{ noti.message }}</div>
                  <div class="noti-time text-neutral-500 dark:text-neutral-500">{{ formatTime(noti.timestamp) }}</div>
                </div>
              </div>
            </div>
            <div v-else class="notification-empty text-neutral-500 dark:text-neutral-400" role="status">
              <i class="fas fa-bell-slash mb-2 text-2xl"></i>
              <p>{{ $t('notifications.noNotifications') }}</p>
              <p class="text-xs mt-1">{{ $t('notifications.emptyDescription') }}</p>
            </div>
            <div v-if="recentNotifications.length > 0" class="notification-footer border-t border-neutral-200 dark:border-neutral-700">
              <button
                class="check-all-btn text-primary-600 dark:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/20"
                @click="markAllAsRead"
                @keydown.enter="markAllAsRead"
                @keydown.space.prevent="markAllAsRead"
                role="menuitem"
                :aria-label="$t('notifications.markAllAsRead')"
              >
                {{ $t('notifications.markAllAsRead') }}
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
            @click.prevent="goSettings" 
            @keydown.enter="goSettings"
            @keydown.space.prevent="goSettings"
            tabindex="0" 
            role="menuitem" 
            class="text-neutral-900 dark:text-neutral-100 hover:bg-neutral-50 dark:hover:bg-neutral-700 dropdown-item"
            ref="settingsMenuItem"
          >
            <i class="pi pi-cog" aria-hidden="true"></i>
            {{ $t('navigation.settings') }}
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
            {{ $t('auth.logout') }}
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
          <h3 id="logout-dialog-title">{{ $t('auth.logoutTitle') }}</h3>
          <p id="logout-dialog-description">{{ $t('auth.logoutConfirmation') }}</p>
          <div class="logout-dialog-actions">
            <button 
              class="btn btn-secondary" 
              @click="cancelLogout"
              @keydown.enter="cancelLogout"
              ref="cancelButton"
              aria-label="Cancel logout"
            >
              {{ $t('common.cancel') }}
            </button>
            <button
              class="btn btn-primary"
              @click="doLogout"
              @keydown.enter="doLogout"
              aria-label="Confirm logout"
            >
              {{ $t('auth.logout') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<script>
import { mapGetters } from 'vuex'
import ThemeToggle from './ThemeToggle.vue'

export default {
  name: 'AppHeader',
  components: {
    ThemeToggle
  },
  data() {
    return {
      showDropdown: false,
      showNotificationDropdown: false,
      showLogoutDialog: false,
      defaultAvatar: 'https://avatars.githubusercontent.com/u/9919?s=200&v=4',
      brandClicks: 0,
      showDeveloperSignature: false,
      signatureTimeout: null
    }
  },
  computed: {
    ...mapGetters('auth', ['user']),
    ...mapGetters('toast', ['toasts', 'unreadCount']),
    avatar() {
      return this.user && this.user.avatar ? this.user.avatar : this.defaultAvatar
    },
    username() {
      if (!this.user) return this.$t('user.defaultName')
      return this.user.fullName || this.user.username || this.user.name || this.user.email?.split('@')[0] || this.$t('user.defaultName')
    },
    notificationCount() {
      return this.unreadCount
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
    goSettings() {
      this.showDropdown = false
      this.$router.push('/settings')
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
      // Notifications page has been removed, notifications are now handled in header modal only
    },
    markAllAsRead() {
      this.showNotificationDropdown = false
      this.$store.dispatch('toast/markAllAsRead')
    },
    getNotificationIcon(type) {
      const normalized = this.normalizeNotificationType(type)
      const iconMap = {
        success: 'pi pi-check-circle text-success-500',
        error: 'pi pi-times-circle text-error-500',
        warning: 'pi pi-exclamation-triangle text-warning-500',
        info: 'pi pi-info-circle text-primary-500'
      }
      return iconMap[normalized] || iconMap.info
    },
    normalizeNotificationType(notificationOrType) {
      const fallback = 'info'
      const allowed = ['success', 'error', 'warning', 'info']
      const rawType = typeof notificationOrType === 'string'
        ? notificationOrType
        : notificationOrType?.type
      if (!rawType) {
        return fallback
      }
      const normalized = String(rawType).toLowerCase()
      return allowed.includes(normalized) ? normalized : fallback
    },
    getNotificationTypeClass(notification) {
      const type = this.normalizeNotificationType(notification)
      return `notification-item--${type}`
    },
    getNotificationTitle(notification) {
      if (notification?.title) {
        return notification.title
      }
      const type = this.normalizeNotificationType(notification)
      const translationKey = `notifications.defaultTitle.${type}`
      const translated = this.$t(translationKey)
      if (translated && translated !== translationKey) {
        return translated
      }
      return type.charAt(0).toUpperCase() + type.slice(1)
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
      if (notification?.id) {
        this.$store.dispatch('toast/markToastRead', notification.id)
      }
      this.closeNotificationDropdown()
    },
    focusFirstMenuItem() {
      if (this.showDropdown) {
        this.$nextTick(() => {
          const firstItem = this.$refs.settingsMenuItem
          if (firstItem) {
            firstItem.focus()
          }
        })
      }
    },
    focusNextMenuItem(event) {
      const menuItems = [this.$refs.settingsMenuItem, this.$refs.logoutMenuItem]
      const currentIndex = menuItems.findIndex(item => item === event.target)
      const nextIndex = (currentIndex + 1) % menuItems.length
      menuItems[nextIndex]?.focus()
    },
    focusPreviousMenuItem(event) {
      const menuItems = [this.$refs.settingsMenuItem, this.$refs.logoutMenuItem]
      const currentIndex = menuItems.findIndex(item => item === event.target)
      const prevIndex = currentIndex === 0 ? menuItems.length - 1 : currentIndex - 1
      menuItems[prevIndex]?.focus()
    },

    // Easter Egg Methods
    handleBrandClick() {
      this.brandClicks++

      if (this.brandClicks === 7) {
        this.showDeveloperSignature = true
        this.signatureTimeout = setTimeout(() => {
          this.showDeveloperSignature = false
          this.brandClicks = 0
        }, 5000)
      } else if (this.brandClicks > 10) {
        // Super secret easter egg
        this.brandClicks = 0
        this.triggerSuperEasterEgg()
      }
    },

    getBrandEasterEgg() {
      const easterEggs = ['✨', '🎉', '🚀', '💎', '⭐', '🌟', '✨']
      return easterEggs[this.brandClicks % easterEggs.length]
    },

    getDeveloperMessage() {
      const messages = [
        "Hey there! You found the developer easter egg! ",
        "Curious developer, aren't you? I like that! 🕵️",
        "7 clicks to unlock this message... you're persistent! ",
        "Welcome to the secret developer club! 🤝",
        "Achievement unlocked: Professional Button Clicker! 🏆"
      ]
      return messages[Math.floor(Math.random() * messages.length)]
    },

    getDeveloperCredit() {
      const credits = [
        "Handcrafted with ❤️ and too much coffee",
        "Built by someone who reads documentation",
        "Made with love, bugs, and Stack Overflow",
        "100% authentic code, 0% AI templates",
        "Powered by determination and energy drinks"
      ]
      return credits[Math.floor(Math.random() * credits.length)]
    },

    triggerSuperEasterEgg() {
      // Add some fun visual effects
      document.body.style.animation = 'rainbow-background 2s ease-in-out'
      setTimeout(() => {
        document.body.style.animation = ''
      }, 2000)

      // Show a special message
      this.$store.dispatch('toast/showToast', {
        type: 'success',
        title: 'Super Easter Egg!',
        message: 'You discovered the ultimate easter egg! You must be a developer too. 🎊',
        duration: 5000
      })
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
  z-index: 500;
  transition: background-color 0.2s ease;
  backdrop-filter: blur(8px);
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
  color: theme('colors.primary.600');
}

.dark .brand-text {
  color: theme('colors.primary.400');
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
  background: transparent;
  border: 1px solid transparent;
  position: relative;
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  color: theme('colors.neutral.700');
}

.dark .icon-btn {
  color: theme('colors.neutral.300');
}

.icon-btn:hover {
  background: theme('colors.neutral.100');
  border-color: theme('colors.neutral.200');
  color: theme('colors.primary.600');
  box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
}

.dark .icon-btn:hover {
  background: theme('colors.neutral.800');
  border-color: theme('colors.neutral.700');
  color: theme('colors.primary.400');
}

.icon-btn:active {
  transform: scale(0.95);
}

/* Badge */
.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 0.7rem;
  border-radius: 9999px;
  padding: 0.2rem 0.4rem;
  font-weight: 700;
  line-height: 1;
  min-width: 1.25rem;
  text-align: center;
  border: 2px solid theme('colors.white');
  background: theme('colors.error.500');
  box-shadow: 0 2px 6px rgb(239 68 68 / 50%);
  animation: pulse-badge 2s ease-in-out infinite;
}

.dark .badge {
  border-color: theme('colors.neutral.900');
}

@keyframes pulse-badge {
  0%, 100% {
    box-shadow: 0 2px 6px rgb(239 68 68 / 50%);
  }
  50% {
    box-shadow: 0 2px 12px rgb(239 68 68 / 70%);
  }
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
  transition: background-color 0.2s ease;
  border: 1px solid transparent;
}

.user-menu:hover {
  border-color: theme('colors.neutral.200');
  opacity: 0.8;
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
    opacity: 0;
  }

  to {
    opacity: 1;
    opacity: 1;
  }
}

.dropdown a {
  padding: 0.75rem 1rem;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: 0.5rem;
  transition: background-color 0.15s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.dropdown a:hover {
  background: theme('colors.primary.50');
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
  z-index: 900;
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 0.75rem);
  right: 0;
  min-width: 22rem;
  max-width: 24rem;
  border-radius: 1rem;
  z-index: 1100;
  max-height: 28rem;
  overflow: visible;
  animation: slideDown 0.2s ease-out;
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 40px rgb(0 0 0 / 20%);
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
  padding: 0.75rem 1rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid theme('colors.neutral.200');
  border-left-width: 4px;
  border-left-color: theme('colors.primary.400');
  border-radius: 0.9rem;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  cursor: pointer;
  box-shadow: 0 4px 12px rgb(15 23 42 / 6%);
  background: theme('colors.white');
}

.dark .notification-item {
  border-color: theme('colors.neutral.700');
  border-left-color: theme('colors.primary.500');
  background: theme('colors.neutral.900');
  box-shadow: 0 12px 20px rgb(0 0 0 / 35%);
}

.notification-item.notification-read {
  opacity: 0.75;
  border-style: dashed;
}

.notification-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgb(15 23 42 / 12%);
}

.notification-item--success {
  border-left-color: theme('colors.success.500');
}

.notification-item--error {
  border-left-color: theme('colors.error.500');
}

.notification-item--warning {
  border-left-color: theme('colors.warning.500');
}

.notification-item--info {
  border-left-color: theme('colors.primary.500');
}

.noti-icon {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: theme('colors.neutral.100');
  flex-shrink: 0;
  box-shadow: inset 0 0 0 1px rgb(15 23 42 / 6%);
}

.notification-item--success .noti-icon {
  background: rgb(34 197 94 / 12%);
}

.notification-item--error .noti-icon {
  background: rgb(239 68 68 / 12%);
}

.notification-item--warning .noti-icon {
  background: rgb(245 158 11 / 15%);
}

.notification-item--info .noti-icon {
  background: rgb(59 130 246 / 12%);
}

.dark .noti-icon {
  background: theme('colors.neutral.800');
  box-shadow: inset 0 0 0 1px rgb(255 255 255 / 8%);
}

.noti-content {
  flex: 1;
  min-width: 0;
}

.noti-title {
  font-weight: 700;
  font-size: 0.95rem;
  margin-bottom: 0.35rem;
  line-height: 1.3;
}

.notification-item--success .noti-title {
  color: theme('colors.success.600');
}

.notification-item--error .noti-title {
  color: theme('colors.error.600');
}

.notification-item--warning .noti-title {
  color: theme('colors.warning.600');
}

.notification-item--info .noti-title {
  color: theme('colors.primary.600');
}

.noti-message {
  font-size: 0.85rem;
  line-height: 1.5;
  margin-bottom: 0.25rem;
  color: theme('colors.neutral.700');
  font-weight: 500;
}

.dark .noti-message {
  color: theme('colors.neutral.200');
}

.noti-time {
  font-size: 0.75rem;
  opacity: 0.85;
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
  transition: background-color 0.15s ease;
  width: 100%;
}

.check-all-btn:hover {
  opacity: 0.8;
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
  opacity: 1;
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

/* Simplified animations */

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
  transition: background-color 0.15s ease;
  min-width: 5rem;
}

.logout-dialog-actions .btn-secondary {
  background: theme('colors.neutral.100');
  color: theme('colors.neutral.700');
}

.logout-dialog-actions .btn-secondary:hover {
  background: theme('colors.neutral.200');
  opacity: 0.8;
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
  opacity: 0.8;
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

/* Easter Egg Styles */
.header-brand {
  cursor: pointer;
  position: relative;
  user-select: none;
}

.brand-sparkle {
  animation: sparkle-dance 2s ease-in-out infinite;
}

.brand-easter-egg {
  display: inline-block;
  margin-left: 0.5rem;
  animation: easter-egg-bounce 1s ease-in-out infinite;
  font-size: 1.2rem;
}

.developer-signature-popup {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 1rem;
  background: white;
  border: 2px solid theme('colors.primary.500');
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  z-index: 50;
  animation: signature-appear 0.5s ease-out;
  min-width: 280px;
}

.signature-content {
  padding: 1.5rem;
  text-align: center;
}

.signature-emoji {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.signature-text {
  color: theme('colors.neutral.800');
  font-size: 0.9rem;
  margin: 0 0 0.75rem 0;
  font-weight: 500;
}

.signature-credit {
  color: theme('colors.neutral.600');
  font-size: 0.75rem;
  font-style: italic;
}

/* Dark mode styles for signature popup */
.dark .developer-signature-popup {
  background: theme('colors.neutral.800');
  border-color: theme('colors.primary.400');
}

.dark .signature-text {
  color: theme('colors.neutral.200');
}

.dark .signature-credit {
  color: theme('colors.neutral.400');
}

/* Easter Egg Animations */
@keyframes sparkle-dance {
  0%, 100% {
    text-shadow: 0 0 5px theme('colors.primary.400'),
                 2px 2px 10px theme('colors.primary.300');
  }
  50% {
    text-shadow: 0 0 15px theme('colors.primary.500'),
                 2px 2px 20px theme('colors.primary.400');
  }
}

@keyframes easter-egg-bounce {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-3px) scale(1.1); }
}

@keyframes signature-appear {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.8) translateY(-10px);
  }
  100% {
    opacity: 1;
    transform: translateX(-50%) scale(1) translateY(0);
  }
}

/* Global rainbow background easter egg */
@keyframes rainbow-background {
  0% { background-color: #ff0000; }
  16% { background-color: #ff8800; }
  32% { background-color: #ffff00; }
  48% { background-color: #88ff00; }
  64% { background-color: #00ff88; }
  80% { background-color: #0088ff; }
  100% { background-color: #8800ff; }
}
</style>
