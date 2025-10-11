<template>
  <button
    @click="toggleTheme"
    @keydown.enter="toggleTheme"
    @keydown.space.prevent="toggleTheme"
    class="theme-toggle-btn"
    :class="{ 'dark-mode': isDark }"
    :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
    :title="isDark ? 'Light mode' : 'Dark mode'"
    tabindex="0"
  >
    <transition name="theme-icon-fade" mode="out-in">
      <svg
        v-if="isDark"
        key="sun"
        class="theme-icon"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"
        />
      </svg>
      <svg
        v-else
        key="moon"
        class="theme-icon"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"
        />
      </svg>
    </transition>
  </button>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'

export default {
  name: 'ThemeToggle',
  setup() {
    const store = useStore()

    const isDark = computed(() => store.state.theme === 'dark')

    const toggleTheme = () => {
      store.commit('TOGGLE_THEME')
    }

    return {
      isDark,
      toggleTheme
    }
  }
}
</script>

<style scoped>
.theme-toggle-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: var(--text-primary, #2a2a26);
}

.theme-toggle-btn:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.05));
  transform: scale(1.05);
}

.theme-toggle-btn:active {
  transform: scale(0.95);
}

.theme-toggle-btn:focus {
  outline: 2px solid var(--primary-500, #2d5aa0);
  outline-offset: 2px;
}

.theme-toggle-btn.dark-mode {
  color: var(--text-primary-dark, #fafafa);
}

.theme-icon {
  width: 1.25rem;
  height: 1.25rem;
  transition: transform 0.3s ease;
}

.theme-toggle-btn:hover .theme-icon {
  transform: rotate(15deg);
}

/* Theme icon transition */
.theme-icon-fade-enter-active,
.theme-icon-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.theme-icon-fade-enter-from {
  opacity: 0;
  transform: scale(0.8) rotate(-45deg);
}

.theme-icon-fade-leave-to {
  opacity: 0;
  transform: scale(0.8) rotate(45deg);
}

/* Dark mode support */
.dark .theme-toggle-btn {
  color: #fafafa;
}

.dark .theme-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .theme-toggle-btn,
  .theme-icon,
  .theme-icon-fade-enter-active,
  .theme-icon-fade-leave-active {
    transition: none;
  }

  .theme-toggle-btn:hover .theme-icon {
    transform: none;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .theme-toggle-btn {
    border: 2px solid currentColor;
  }

  .theme-toggle-btn:focus {
    outline-width: 3px;
  }
}

/* Touch device optimization */
@media (pointer: coarse) {
  .theme-toggle-btn {
    padding: 0.75rem;
    min-width: 44px;
    min-height: 44px;
  }

  .theme-icon {
    width: 1.5rem;
    height: 1.5rem;
  }
}
</style>
