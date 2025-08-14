<template>
  <header class="mobile-header">
    <div class="mobile-header-content">
      <!-- Menu Toggle Button -->
      <button 
        class="mobile-menu-btn"
        @click="toggleMobileMenu"
        aria-label="Toggle navigation menu"
      >
        <i class="pi pi-bars"></i>
      </button>
      
      <!-- Logo and Title -->
      <div class="mobile-header-brand">
        <img src="/logo192.svg" alt="Logo" class="mobile-logo" />
        <div class="mobile-title-container">
          <h1 class="mobile-title">{{ pageTitle }}</h1>
          <p v-if="pageSubtitle" class="mobile-subtitle">{{ pageSubtitle }}</p>
        </div>
      </div>
      
      <!-- Action Buttons -->
      <div class="mobile-header-actions">
        <button 
          v-if="showCreateButton"
          @click="navigateToCreate"
          class="mobile-create-btn"
          aria-label="Create new ad"
        >
          <i class="pi pi-plus"></i>
        </button>
        
        <button 
          class="mobile-profile-btn"
          @click="toggleProfileMenu"
          aria-label="User profile menu"
        >
          <i class="pi pi-user"></i>
        </button>
      </div>
    </div>
    
    <!-- Profile Dropdown -->
    <div v-if="showProfileMenu" class="mobile-profile-menu">
      <div class="profile-menu-item" @click="navigateToProfile">
        <i class="pi pi-user"></i>
        <span>Profile</span>
      </div>
      <div class="profile-menu-item" @click="navigateToSettings">
        <i class="pi pi-cog"></i>
        <span>Settings</span>
      </div>
      <div class="profile-menu-divider"></div>
      <div class="profile-menu-item logout" @click="handleLogout">
        <i class="pi pi-sign-out"></i>
        <span>Logout</span>
      </div>
    </div>
  </header>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'

export default {
  name: 'MobileHeader',
  emits: ['toggle-mobile-menu'],
  
  setup(props, { emit }) {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    
    const showProfileMenu = ref(false)
    
    // Page title based on current route
    const pageTitle = computed(() => {
      const routeTitles = {
        '/dashboard': 'Dashboard',
        '/campaigns': 'Campaigns',
        '/ads': 'Ads',
        '/ad/create': 'Create Ad',
        '/ad/create-enhanced': 'Create Ad',
        '/profile': 'Profile',
        '/settings': 'Settings'
      }
      
      return routeTitles[route.path] || 'Ads Creative'
    })
    
    // Page subtitle based on current route
    const pageSubtitle = computed(() => {
      const routeSubtitles = {
        '/dashboard': 'Overview of your campaigns',
        '/campaigns': 'Manage your ad campaigns',
        '/ads': 'View and manage your ads',
        '/ad/create': 'Create new advertisement',
        '/ad/create-enhanced': 'AI-powered ad creation'
      }
      
      return routeSubtitles[route.path] || null
    })
    
    // Show create button on certain pages
    const showCreateButton = computed(() => {
      return ['/dashboard', '/campaigns', '/ads'].includes(route.path)
    })
    
    const toggleMobileMenu = () => {
      emit('toggle-mobile-menu')
    }
    
    const toggleProfileMenu = () => {
      showProfileMenu.value = !showProfileMenu.value
    }
    
    const navigateToCreate = () => {
      router.push('/ad/create-enhanced')
    }
    
    const navigateToProfile = () => {
      router.push('/profile')
      showProfileMenu.value = false
    }
    
    const navigateToSettings = () => {
      router.push('/settings')
      showProfileMenu.value = false
    }
    
    const handleLogout = async () => {
      try {
        await store.dispatch('auth/logout')
        router.push('/login')
      } catch (error) {
        console.error('Logout error:', error)
      }
      showProfileMenu.value = false
    }
    
    // Close profile menu when clicking outside
    const handleClickOutside = (event) => {
      if (!event.target.closest('.mobile-profile-btn') && 
          !event.target.closest('.mobile-profile-menu')) {
        showProfileMenu.value = false
      }
    }
    
    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })
    
    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
    })
    
    return {
      pageTitle,
      pageSubtitle,
      showCreateButton,
      showProfileMenu,
      toggleMobileMenu,
      toggleProfileMenu,
      navigateToCreate,
      navigateToProfile,
      navigateToSettings,
      handleLogout
    }
  }
}
</script>

<style scoped>
.mobile-header {
  display: none;
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.mobile-header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  gap: var(--space-3);
}

.mobile-menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: var(--radius-lg);
  background: var(--color-bg-tertiary);
  color: var(--color-text);
  transition: var(--transition-all);
  flex-shrink: 0;
}

.mobile-menu-btn:hover {
  background: var(--color-hover);
  color: var(--brand-primary);
}

.mobile-menu-btn i {
  font-size: 1.25rem;
}

.mobile-header-brand {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  min-width: 0;
}

.mobile-logo {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.mobile-title-container {
  min-width: 0;
  flex: 1;
}

.mobile-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0;
  line-height: var(--leading-tight);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-subtitle {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  margin: 0;
  line-height: var(--leading-tight);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mobile-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.mobile-create-btn,
.mobile-profile-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: var(--radius-lg);
  transition: var(--transition-all);
}

.mobile-create-btn {
  background: var(--brand-primary);
  color: white;
}

.mobile-create-btn:hover {
  background: var(--brand-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.mobile-profile-btn {
  background: var(--color-bg-tertiary);
  color: var(--color-text);
}

.mobile-profile-btn:hover {
  background: var(--color-hover);
  color: var(--brand-primary);
}

.mobile-create-btn i,
.mobile-profile-btn i {
  font-size: 1.125rem;
}

/* Profile Menu */
.mobile-profile-menu {
  position: absolute;
  top: 100%;
  right: var(--space-4);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  min-width: 200px;
  z-index: 1000;
  overflow: hidden;
}

.profile-menu-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  color: var(--color-text);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: var(--transition-colors);
  min-height: 44px;
}

.profile-menu-item:hover {
  background: var(--color-hover);
}

.profile-menu-item.logout {
  color: var(--error-600);
}

.profile-menu-item.logout:hover {
  background: var(--error-50);
}

.profile-menu-item i {
  font-size: 1rem;
  width: 16px;
  text-align: center;
}

.profile-menu-divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--space-1) 0;
}

/* Show mobile header on mobile devices */
@media (width <= 768px) {
  .mobile-header {
    display: block;
  }
}

/* Responsive adjustments */
@media (width <= 480px) {
  .mobile-header-content {
    padding: var(--space-3);
  }
  
  .mobile-title {
    font-size: var(--text-base);
  }
  
  .mobile-subtitle {
    display: none; /* Hide subtitle on very small screens */
  }
  
  .mobile-profile-menu {
    right: var(--space-3);
    left: var(--space-3);
    min-width: auto;
  }
}

@media (width <= 360px) {
  .mobile-header-actions {
    gap: var(--space-1);
  }
  
  .mobile-create-btn,
  .mobile-profile-btn {
    width: 40px;
    height: 40px;
  }
  
  .mobile-create-btn i,
  .mobile-profile-btn i {
    font-size: 1rem;
  }
}
</style>
