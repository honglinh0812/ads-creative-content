<template>
  <div id="app" class="app-layout bg-neutral-50 dark:bg-neutral-900 text-neutral-900 dark:text-neutral-100 transition-colors duration-300">
    <!-- Enhanced loading screen -->
    <div v-if="authLoading" class="auth-loading bg-gradient-to-br from-primary-500 to-secondary-600 dark:from-primary-600 dark:to-secondary-700">
      <div class="loading-content">
        <div class="loading-logo animate-pulse">
          <svg class="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <ProgressSpinner class="custom-spinner" />
        <p class="loading-text">Đang tải ứng dụng...</p>
      </div>
    </div>
    
    <!-- Enhanced main layout for authenticated users -->
    <template v-else-if="isAuthenticated">
      <MobileHeader @toggle-mobile-menu="toggleMobileMenu" />
      <div class="main-content bg-neutral-50 dark:bg-neutral-900 border-neutral-200 dark:border-neutral-700" role="main" aria-label="Main content">
        <Header @search="onHeaderSearch" />
        <main class="content-area">
          <transition name="fade-slide" mode="out-in">
            <router-view />
          </transition>
        </main>
        <ToastNotifications />
      </div>
    </template>

    <!-- Enhanced simple layout for unauthenticated users -->
    <template v-else>
      <div class="auth-layout bg-gradient-to-br from-primary-50 to-secondary-50 dark:from-neutral-900 dark:to-neutral-800">
        <transition name="fade-slide" mode="out-in">
          <router-view />
        </transition>
        <ToastNotifications />
      </div>
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
/* Global app styles */
#app.app-layout {
  display: flex;
  min-height: 100vh;
  font-family: Inter, sans-serif;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* Enhanced auth loading styles */
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

.custom-spinner {
  --p-progressspinner-color-1: rgb(255 255 255 / 80%);
  --p-progressspinner-color-2: rgb(255 255 255 / 40%);
  --p-progressspinner-color-3: rgb(255 255 255 / 20%);
  --p-progressspinner-color-4: rgb(255 255 255 / 10%);
}

/* Auth layout for unauthenticated users */
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

/* Enhanced main layout styles */
.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.content-area {
  flex: 1;
  padding: 2rem 2.5rem;
  overflow-x: hidden;
  background: transparent;
}

/* Content spacing and typography */
.content-area h1,
.content-area h2,
.content-area h3 {
  font-weight: 600;
  line-height: 1.25;
  margin-bottom: 1rem;
}

.content-area h1 {
  font-size: 2.25rem;
  color: theme('colors.neutral.900');
}

.content-area h2 {
  font-size: 1.875rem;
  color: theme('colors.neutral.800');
}

.content-area h3 {
  font-size: 1.5rem;
  color: theme('colors.neutral.700');
}

.dark .content-area h1 {
  color: theme('colors.neutral.100');
}

.dark .content-area h2 {
  color: theme('colors.neutral.200');
}

.dark .content-area h3 {
  color: theme('colors.neutral.300');
}

/* Enhanced Mobile Responsiveness */

/* Large desktop */
@media (width >= 1440px) {
  .content-area {
    padding: 2.5rem 3rem;
  }
}

/* Tablet and small desktop adjustments */
@media (width <= 1024px) {
  .content-area {
    padding: 1.5rem 2rem;
  }
}

/* Tablet portrait */
@media (width <= 768px) {
  #app.app-layout {
    flex-direction: column;
  }

  .main-content {
    margin-left: 0;
    width: 100%;
    min-height: calc(100vh - 60px);
  }

  .content-area {
    padding: 1rem 1.5rem;
  }

  /* Hide desktop header on mobile */
  .main-content > header {
    display: none;
  }

  /* Adjust typography for mobile */
  .content-area h1 {
    font-size: 1.875rem;
  }

  .content-area h2 {
    font-size: 1.5rem;
  }

  .content-area h3 {
    font-size: 1.25rem;
  }
}

/* Mobile phones */
@media (width <= 640px) {
  .content-area {
    padding: 1rem;
  }

  .content-area h1 {
    font-size: 1.75rem;
  }

  .content-area h2 {
    font-size: 1.375rem;
  }
}

/* Small mobile phones */
@media (width <= 480px) {
  .content-area {
    padding: 0.75rem;
  }

  .content-area h1 {
    font-size: 1.5rem;
  }
}

/* Landscape mobile adjustments */
@media (width <= 768px) and (orientation: landscape) {
  .main-content {
    min-height: calc(100vh - 50px);
  }

  .content-area {
    padding: 0.75rem 1rem;
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
    padding: 0.75rem;
  }

  /* Remove hover effects on touch devices */
  .hover\:shadow-lg:hover,
  .hover\:transform:hover,
  .hover\:scale-105:hover {
    box-shadow: none;
    transform: none;
  }

  /* Improve scrolling on touch devices */
  .content-area {
    -webkit-overflow-scrolling: touch;
    scroll-behavior: smooth;
  }
}

/* Enhanced transition effects */
.fade-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.98);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(1.02);
}

.fade-slide-enter-to,
.fade-slide-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}

/* Accessibility improvements */
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

/* Focus management */
.content-area:focus-within {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 4px;
}

/* Print styles */
@media print {
  .auth-loading,
  .loading-content {
    display: none;
  }

  .main-content {
    margin-left: 0;
    box-shadow: none;
  }

  .content-area {
    padding: 1rem;
  }
}
</style>

