<template>
  <button 
    class="dark-toggle bg-neutral-100 hover:bg-neutral-200 dark:bg-neutral-700 dark:hover:bg-neutral-600 text-neutral-600 dark:text-neutral-300 transition-colors duration-200 ease-in-out rounded-lg p-2.5 border border-neutral-200 dark:border-neutral-600 shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 dark:focus:ring-offset-neutral-800" 
    :aria-label="isDark ? 'Chuyển sang chế độ sáng' : 'Chuyển sang chế độ tối'"
    :aria-pressed="isDark.toString()"
    role="switch"
    type="button"
    @click="toggleDarkMode"
    @keydown.enter="toggleDarkMode"
    @keydown.space.prevent="toggleDarkMode"
  >
    <transition name="icon-fade" mode="out-in">
      <i v-if="isDark" key="sun" class="pi pi-sun text-lg" aria-hidden="true"></i>
      <i v-else key="moon" class="pi pi-moon text-lg" aria-hidden="true"></i>
    </transition>
    <span class="sr-only">{{ isDark ? 'Chế độ tối đang bật' : 'Chế độ sáng đang bật' }}</span>
  </button>
</template>

<script>
export default {
  name: 'DarkModeToggle',
  data() {
    return {
      isDark: false
    }
  },
  mounted() {
    // Ưu tiên lấy theme từ localStorage, nếu không có thì lấy theo hệ điều hành
    const theme = localStorage.getItem('theme')
    if (theme === 'dark') {
      this.isDark = true
      document.documentElement.classList.add('dark')
    } else if (theme === 'light') {
      this.isDark = false
      document.documentElement.classList.remove('dark')
    } else {
      this.isDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      if (this.isDark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    }
  },
  methods: {
    toggleDarkMode() {
      this.isDark = !this.isDark
      localStorage.setItem('theme', this.isDark ? 'dark' : 'light')
      if (this.isDark) {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
      this.$emit('theme-changed', this.isDark ? 'dark' : 'light')
    }
  }
}
</script>

<style scoped>
.dark-toggle {
  position: relative;
  overflow: hidden;
}

/* Simplified button without complex effects */

/* Icon transition animations */
.icon-fade-enter-active,
.icon-fade-leave-active {
  transition: all 0.2s ease-in-out;
}

.icon-fade-enter-from,
.icon-fade-leave-to {
  opacity: 0;
}

.icon-fade-enter-to,
.icon-fade-leave-from {
  opacity: 1;
}

/* Responsive adjustments */
@media (width <= 768px) {
  .dark-toggle {
    padding: 0.625rem;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .dark-toggle {
    border-width: 2px;
  }
}

/* Screen reader only text */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* Focus styles for better accessibility */
.dark-toggle:focus-visible {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 2px;
  box-shadow: 0 0 0 4px theme('colors.primary.500/20');
}

/* Ensure minimum touch target size */
@media (pointer: coarse) {
  .dark-toggle {
    min-width: 44px;
    min-height: 44px;
    padding: 0.75rem;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .dark-toggle,
  .icon-fade-enter-active,
  .icon-fade-leave-active {
    transition: none;
  }
  
  .dark-toggle:hover {
    transform: none;
  }
  
  .dark-toggle::before {
    transition: none;
  }
}
</style>