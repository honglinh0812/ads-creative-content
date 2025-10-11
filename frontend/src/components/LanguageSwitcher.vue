<template>
  <a-dropdown placement="bottomRight">
    <a-button type="text" class="language-btn">
      <template #icon>
        <a-icon type="global" />
      </template>
      <span class="language-label">{{ currentLanguageLabel }}</span>
    </a-button>
    <template #overlay>
      <a-menu @click="onLanguageChange">
        <a-menu-item key="en" :class="{ 'active': currentLocale === 'en' }">
          <span class="flag">🇺🇸</span>
          <span>English</span>
        </a-menu-item>
        <a-menu-item key="vi" :class="{ 'active': currentLocale === 'vi' }">
          <span class="flag">🇻🇳</span>
          <span>Tiếng Việt</span>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script>
import { useI18n } from 'vue-i18n'
import { computed } from 'vue'

export default {
  name: 'LanguageSwitcher',
  setup() {
    const { locale } = useI18n()

    const currentLocale = computed(() => locale.value)

    const currentLanguageLabel = computed(() => {
      return locale.value === 'vi' ? 'VI' : 'EN'
    })

    const onLanguageChange = ({ key }) => {
      locale.value = key
      localStorage.setItem('locale', key)
      // Optionally reload the page to apply all translations
      // window.location.reload()
    }

    return {
      currentLocale,
      currentLanguageLabel,
      onLanguageChange
    }
  }
}
</script>

<style scoped>
.language-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.language-label {
  font-size: 13px;
  font-weight: 500;
}

.flag {
  margin-right: 8px;
  font-size: 16px;
}

.active {
  background-color: #f0f2f5;
  font-weight: 600;
}
</style>
