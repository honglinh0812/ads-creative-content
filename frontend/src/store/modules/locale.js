// Locale/Language store module (Issue: I18n Phase 1)
import i18n from '@/i18n'
import viVN from 'ant-design-vue/es/locale/vi_VN'
import enUS from 'ant-design-vue/es/locale/en_US'

const state = {
  locale: localStorage.getItem('locale') || 'vi',
  availableLocales: [
    { code: 'vi', name: 'Tiếng Việt', flag: '🇻🇳' },
    { code: 'en', name: 'English', flag: '🇺🇸' }
  ],
  antdLocale: localStorage.getItem('locale') === 'en' ? enUS : viVN
}

const mutations = {
  SET_LOCALE(state, locale) {
    state.locale = locale
    // Update Ant Design Vue locale
    state.antdLocale = locale === 'en' ? enUS : viVN
    // Persist to localStorage
    localStorage.setItem('locale', locale)
  }
}

const actions = {
  /**
   * Change application language
   * @param {Object} context - Vuex context
   * @param {string} locale - Language code ('vi' or 'en')
   */
  changeLocale({ commit }, locale) {
    if (!['vi', 'en'].includes(locale)) {
      console.error(`Invalid locale: ${locale}. Using 'vi' as fallback.`)
      locale = 'vi'
    }

    // Update Vuex state
    commit('SET_LOCALE', locale)

    // Update vue-i18n global locale
    i18n.global.locale.value = locale

    // Update document language attribute for accessibility
    document.documentElement.lang = locale

    console.log(`[Locale] Language changed to: ${locale}`)
  },

  /**
   * Initialize locale on app startup
   * Called from main.js during app initialization
   */
  initializeLocale({ dispatch }) {
    const savedLocale = localStorage.getItem('locale') || 'vi'
    dispatch('changeLocale', savedLocale)
    console.log(`[Locale] Initialized with locale: ${savedLocale}`)
  },

  /**
   * Toggle between Vietnamese and English
   */
  toggleLocale({ dispatch, state }) {
    const newLocale = state.locale === 'vi' ? 'en' : 'vi'
    dispatch('changeLocale', newLocale)
  }
}

const getters = {
  currentLocale: state => state.locale,
  currentLocaleName: state => {
    const locale = state.availableLocales.find(l => l.code === state.locale)
    return locale ? locale.name : 'Tiếng Việt'
  },
  currentLocaleFlag: state => {
    const locale = state.availableLocales.find(l => l.code === state.locale)
    return locale ? locale.flag : '🇻🇳'
  },
  availableLocales: state => state.availableLocales,
  antdLocale: state => state.antdLocale,
  isVietnamese: state => state.locale === 'vi',
  isEnglish: state => state.locale === 'en'
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
