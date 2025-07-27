<template>
  <button class="dark-toggle" :aria-label="isDark ? 'Switch to Light mode' : 'Switch to Dark mode'" @click="toggleDarkMode">
    <span v-if="isDark">
      <i class="pi pi-sun"></i>
    </span>
    <span v-else>
      <i class="pi pi-moon"></i>
    </span>
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
.toggle-btn {
  background: none;
  border: none;
  font-size: var(--text-2xl);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: var(--space-2);
  border-radius: var(--radius-full);
  transition: background 0.15s, color 0.15s;
}
.toggle-btn.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
</style> 