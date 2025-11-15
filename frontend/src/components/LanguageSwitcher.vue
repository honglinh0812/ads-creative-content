<template>
  <a-dropdown placement="bottomRight">
    <a-button type="text" class="language-btn">
      <span class="language-flag">{{ currentLocaleFlag }}</span>
      <DownOutlined />
    </a-button>
    <template #overlay>
      <a-menu @click="onLanguageChange" :selected-keys="[currentLocale]">
        <a-menu-item
          v-for="locale in availableLocales"
          :key="locale.code"
          :class="{ 'active': locale.code === currentLocale }"
        >
          <span class="flag">{{ locale.flag }}</span>
          <span>{{ locale.name }}</span>
          <CheckOutlined v-if="locale.code === currentLocale" class="check-icon" />
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script>
import { computed } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import { DownOutlined, CheckOutlined } from '@ant-design/icons-vue'

export default {
  name: 'LanguageSwitcher',
  components: {
    DownOutlined,
    CheckOutlined
  },
  setup() {
    const store = useStore()
    const { t } = useI18n()

    // Get locale data from Vuex store (Issue: I18n Phase 1)
    const currentLocale = computed(() => store.getters['locale/currentLocale'])
    const currentLocaleName = computed(() => store.getters['locale/currentLocaleName'])
    const currentLocaleFlag = computed(() => store.getters['locale/currentLocaleFlag'])
    const availableLocales = computed(() => store.getters['locale/availableLocales'])

    const currentLanguageLabel = computed(() => {
      return currentLocale.value === 'vi' ? 'VI' : 'EN'
    })

    const onLanguageChange = ({ key }) => {
      if (key !== currentLocale.value) {
        const nextLocale = availableLocales.value.find(locale => locale.code === key)
        const languageName = nextLocale ? nextLocale.name : key

        // Dispatch Vuex action to change locale
        store.dispatch('locale/changeLocale', key)

        // Show success toast with localized content
        store.dispatch('toast/showSuccess', {
          title: t('languageSwitcher.changeSuccessTitle'),
          message: t('languageSwitcher.changeSuccessMessage', { language: languageName }),
          duration: 3000
        })
      }
    }

    return {
      currentLocale,
      currentLocaleName,
      currentLocaleFlag,
      currentLanguageLabel,
      availableLocales,
      onLanguageChange
    }
  }
}
</script>

<style scoped lang="scss">
.language-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all 0.3s ease;

  &:hover {
    background-color: rgba(0, 0, 0, 0.04);
  }

  .language-flag {
    font-size: 18px;
    line-height: 1;
  }

  .language-label {
    font-size: 13px;
    font-weight: 500;
  }
}

.flag {
  margin-right: 8px;
  font-size: 18px;
  line-height: 1;
}

.check-icon {
  margin-left: auto;
  color: #1890ff;
  font-size: 14px;
}

.active {
  background-color: #f0f2f5;
  font-weight: 600;
}

/* Dark mode support */
.dark {
  .language-btn:hover {
    background-color: rgba(255, 255, 255, 0.08);
  }

  .active {
    background-color: rgba(255, 255, 255, 0.08);
  }
}
</style>
