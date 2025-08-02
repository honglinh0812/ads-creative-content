<template>
  <header class="header">
    <div class="header-left"></div>
    <div class="header-right">
      <DarkModeToggle class="mr-2" />
      <div class="notification-dropdown-wrapper" style="position: relative; display: inline-block;">
        <button class="icon-btn notification-btn" @click="toggleNotificationDropdown" aria-label="Notifications" tabindex="0" title="Notifications">
          <i class="pi pi-bell"></i>
          <span v-if="notificationCount > 0" class="badge" role="status" aria-live="polite">{{ notificationCount }}</span>
        </button>
        <transition name="fade">
          <div v-if="showNotificationDropdown" class="notification-dropdown" @click.stop>
            <div v-if="recentNotifications.length > 0">
              <div v-for="noti in recentNotifications" :key="noti.id" class="notification-item">
                <span class="noti-icon" :class="noti.type"><i v-if="noti.type==='success'" class="pi pi-check-circle"></i><i v-else-if="noti.type==='error'" class="pi pi-times-circle"></i><i v-else-if="noti.type==='warning'" class="pi pi-exclamation-triangle"></i><i v-else class="pi pi-info-circle"></i></span>
                <div class="noti-content">
                  <div class="noti-title">{{ noti.title || noti.type }}</div>
                  <div class="noti-message">{{ noti.message }}</div>
                  <div class="noti-time">{{ formatTime(noti.timestamp) }}</div>
                </div>
              </div>
            </div>
            <div v-else class="notification-empty">No notifications</div>
            <div class="notification-footer">
              <button class="check-all-btn" @click="goAllNotifications">Check all notifications</button>
            </div>
          </div>
        </transition>
      </div>
      <div class="user-menu" @click="toggleDropdown" tabindex="0" aria-label="User menu" @keydown.enter="toggleDropdown" @keydown.esc="closeDropdown">
        <span class="username">{{ username }}</span>
        <i class="pi pi-chevron-down"></i>
        <div v-if="showDropdown" class="dropdown" role="menu" aria-label="User dropdown">
          <a href="#" @click.prevent="goProfile" tabindex="0" role="menuitem">Profile</a>
          <a href="#" @click.prevent="confirmLogout" tabindex="0" role="menuitem">Logout</a>
        </div>
      </div>
    </div>
    
    <!-- Logout confirmation dialog -->
    <div v-if="showLogoutDialog" class="logout-dialog-overlay" @click="cancelLogout">
      <div class="logout-dialog" @click.stop>
        <div class="logout-dialog-content">
          <h3>Xác nhận đăng xuất</h3>
          <p>Bạn có chắc chắn muốn đăng xuất?</p>
          <div class="logout-dialog-actions">
            <button class="btn btn-secondary" @click="cancelLogout">Hủy</button>
            <button class="btn btn-primary" @click="doLogout">Đăng xuất</button>
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
    }
  }
}
</script>

<style scoped>
.header {
  width: 100%;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e1e4e8;
  box-shadow: 0 1px 4px rgba(27,31,35,0.03);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem 0 2rem;
  position: sticky;
  top: 0;
  z-index: 30;
}
.header.dark,
.dark .header {
  background: #1f2937;
  color: #fff;
  border-bottom: 1px solid #374151;
}
.header-left {
  flex: 1;
  display: flex;
  align-items: center;
}
.search-bar {
  width: 260px;
  padding: 7px 14px;
  border: 1px solid #d1d5da;
  border-radius: 6px;
  font-size: 1rem;
  background: #f6f8fa;
  color: #24292f;
  outline: none;
  transition: border 0.15s;
}
.search-bar:focus {
  border-color: #0969da;
  background: #fff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}
.icon-btn {
  background: none;
  border: none;
  position: relative;
  cursor: pointer;
  font-size: 1.3rem;
  color: #57606a;
  padding: 0.5rem;
  border-radius: 50%;
  transition: background 0.15s;
}
.icon-btn:hover {
  background: #eaf5ff;
  color: #0969da;
}
.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #2da44e;
  color: #fff;
  font-size: 0.7rem;
  border-radius: 999px;
  padding: 0 6px;
  font-weight: 600;
  line-height: 1.2;
}
.user-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  position: relative;
  cursor: pointer;
  padding: 0.25rem 0.75rem 0.25rem 0.5rem;
  border-radius: 6px;
  transition: background 0.15s;
}
.user-menu:hover {
  background: #eaf5ff;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e1e4e8;
}
.username {
  font-weight: 500;
  color: #24292f;
  font-size: 1rem;
}
.dropdown {
  position: absolute;
  top: 110%;
  right: 0;
  background: #fff;
  border: 1px solid #e1e4e8;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(27,31,35,0.08);
  min-width: 140px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0;
}
.dropdown a {
  padding: 0.5rem 1.25rem;
  color: #24292f;
  text-decoration: none;
  font-size: 1rem;
  transition: background 0.15s;
}
.dropdown a:hover {
  background: #f6f8fa;
}
.icon-btn:focus, .user-menu:focus, .dropdown a:focus {
  outline: 2px solid #0969da;
  outline-offset: 2px;
  background: #eaf5ff;
}
.notification-dropdown-wrapper {
  position: relative;
}
.notification-btn {
  position: relative;
  background: none;
  border: none;
  font-size: 1.5rem;
  margin-right: 1.2rem;
  cursor: pointer;
  color: #334155;
  transition: color 0.15s;
}
.notification-btn:hover {
  color: #2563eb;
}
.notification-dropdown {
  position: absolute;
  top: 2.5rem;
  right: 0;
  min-width: 340px;
  background: #fff;
  border-radius: 0.75rem;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 1.5px 6px rgba(0,0,0,0.08);
  border: 1.5px solid #e5e7eb;
  z-index: 1000;
  padding: 0.5rem 0 0.5rem 0;
  max-height: 420px;
  overflow-y: auto;
  animation: fadeIn 0.18s;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 0.7rem 1.2rem 0.7rem 1.2rem;
  border-bottom: 1px solid #f1f5f9;
}
.notification-item:last-child {
  border-bottom: none;
}
.noti-icon {
  font-size: 1.3rem;
  margin-top: 0.15rem;
}
.noti-icon.success { color: #10b981; }
.noti-icon.error { color: #ef4444; }
.noti-icon.warning { color: #f59e0b; }
.noti-icon.info { color: #3b82f6; }
.noti-content {
  flex: 1;
  min-width: 0;
}
.noti-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 1.01rem;
  margin-bottom: 0.1rem;
}
.noti-message {
  color: #334155;
  font-size: 0.97rem;
  line-height: 1.4;
}
.noti-time {
  color: #64748b;
  font-size: 0.85rem;
  margin-top: 0.1rem;
}
.notification-footer {
  padding: 0.7rem 1.2rem 0.2rem 1.2rem;
  border-top: 1px solid #f1f5f9;
  text-align: right;
}
.check-all-btn {
  background: none;
  border: none;
  color: #2563eb;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  border-radius: 0.5rem;
  transition: background 0.15s;
}
.check-all-btn:hover {
  background: #f1f5f9;
}
.notification-empty {
  text-align: center;
  color: #64748b;
  padding: 1.2rem 0.5rem 1.2rem 0.5rem;
  font-size: 1rem;
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
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.logout-dialog {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  max-width: 400px;
  width: 90%;
  animation: dialogFadeIn 0.2s ease-out;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.logout-dialog-content {
  padding: 24px;
  text-align: center;
}

.logout-dialog-content h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.logout-dialog-content p {
  margin: 0 0 24px 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

.logout-dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.logout-dialog-actions .btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.logout-dialog-actions .btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.logout-dialog-actions .btn-secondary:hover {
  background: #e5e7eb;
}

.logout-dialog-actions .btn-primary {
  background: #ef4444;
  color: white;
}

.logout-dialog-actions .btn-primary:hover {
  background: #dc2626;
}
@media (max-width: 600px) {
  .header {
    padding: 0 0.5rem;
  }
  .search-bar {
    width: 120px;
    font-size: 0.95rem;
  }
  .username {
    display: none;
  }
}
</style> 