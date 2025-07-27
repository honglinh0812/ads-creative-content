<template>
  <div id="app" class="app-layout dark:bg-gray-900 dark:text-white">
    <!-- Show loading when checking authentication -->
    <div v-if="authLoading" class="auth-loading dark:bg-gray-900 dark:text-white">
      <ProgressSpinner />
      <p>Loading...</p>
    </div>
    
    <!-- Show main layout only when authenticated -->
    <template v-else-if="isAuthenticated">
      <MobileHeader @toggle-mobile-menu="toggleMobileMenu" />
      <div class="main-content dark:bg-gray-900 dark:text-white dark:border-gray-700" role="main" aria-label="Main content">
        <Header @search="onHeaderSearch" />
        <transition name="fade-slide" mode="out-in">
          <router-view />
        </transition>
        <ToastNotifications />
      </div>
    </template>

    <!-- Show simple layout for unauthenticated users -->
    <template v-else>
      <transition name="fade-slide" mode="out-in">
    <router-view />
      </transition>
    <ToastNotifications />
    </template>

    <!-- Modern Toast Container -->
    <ToastContainer />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Header from './components/Header.vue'
import MobileHeader from './components/MobileHeader.vue'
import ToastNotifications from './components/ToastNotifications.vue'
import ToastContainer from './components/ToastContainer.vue'

export default {
  name: 'App',
  components: {
    Header,
    MobileHeader,
    ToastNotifications,
    ToastContainer
  },
  computed: {
    ...mapGetters('auth', ['isAuthenticated', 'loading']),
    authLoading() {
      return this.loading
    }
  },
  methods: {
    onHeaderSearch(query) {
      // Xử lý logic search toàn cục ở đây (ví dụ: chuyển trang search, filter, ...)
      console.log('Header search:', query)
    },

    toggleMobileMenu() {
      // Toggle mobile menu through sidebar component
      if (this.$refs.sidebar) {
        this.$refs.sidebar.toggleMobileMenu()
      }
    }
  }
}
</script>

<style>
#app.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

/* Auth loading styles */
.auth-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg);
}

.auth-loading p {
  margin-top: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--text-lg);
}

/* Main layout styles */
.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  padding: 2rem 2.5rem 1.5rem 2.5rem;
  padding-left: 2.5rem; /* Thêm khoảng trắng giữa sidebar và nội dung */
}

/* Enhanced Mobile Responsiveness */

/* Tablet and small desktop adjustments */
@media (max-width: 1024px) {
  .main-content {
    padding: var(--space-4) var(--space-6);
  }
}

/* Tablet portrait */
@media (max-width: 768px) {
  #app.app-layout {
    flex-direction: column;
  }

  .main-content {
    margin-left: 0;
    padding: 0;
    width: 100%;
    min-height: calc(100vh - 60px); /* Account for mobile header */
  }

  /* Hide desktop header on mobile */
  .main-content > header {
    display: none;
  }

  /* Add padding to router-view content */
  .main-content > .fade-slide-enter-active,
  .main-content > .fade-slide-leave-active,
  .main-content > * {
    padding: var(--space-4);
  }
}

/* Mobile phones */
@media (max-width: 640px) {
  .main-content > .fade-slide-enter-active,
  .main-content > .fade-slide-leave-active,
  .main-content > * {
    padding: var(--space-3);
  }
}

/* Small mobile phones */
@media (max-width: 480px) {
  .main-content > .fade-slide-enter-active,
  .main-content > .fade-slide-leave-active,
  .main-content > * {
    padding: var(--space-2);
  }
}

/* Landscape mobile adjustments */
@media (max-width: 768px) and (orientation: landscape) {
  .main-content {
    min-height: calc(100vh - 50px); /* Smaller header in landscape */
  }
}

/* Touch-friendly improvements */
@media (hover: none) and (pointer: coarse) {
  /* Increase touch targets */
  button,
  .btn,
  .clickable,
  a {
    min-height: 44px;
    min-width: 44px;
  }

  /* Remove hover effects on touch devices */
  .hover\:shadow-lg:hover,
  .hover\:transform:hover,
  .hover\:scale-105:hover {
    box-shadow: none;
    transform: none;
  }
}

/* Simple layout for unauthenticated users */
#app:not(.app-layout) {
  min-height: 100vh;
  background: #f6f8fa;
}

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: opacity 0.35s cubic-bezier(0.4,0,0.2,1), transform 0.35s cubic-bezier(0.4,0,0.2,1);
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(24px);
}
.fade-slide-enter-to, .fade-slide-leave-from {
  opacity: 1;
  transform: translateY(0);
}
</style>

