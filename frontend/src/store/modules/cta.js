/**
 * Vuex Store Module for Call-to-Action (CTA) Management
 *
 * Provides centralized CTA data management across all components.
 * Handles language-aware CTA loading and caching.
 */

import api from '@/services/api'

const state = {
  // List of available CTAs with {value, label} structure
  ctas: [],

  // Current language for CTA labels
  language: 'vi',

  // Loading state
  loading: false,

  // Error state
  error: null,

  // Last loaded timestamp for cache invalidation
  lastLoaded: null,

  // Cache duration in milliseconds (5 minutes)
  cacheDuration: 5 * 60 * 1000
}

const getters = {
  /**
   * Get all CTAs
   */
  allCTAs: (state) => state.ctas,

  /**
   * Get CTA label by value
   * @param {string} value - CTA enum value (e.g., 'LEARN_MORE')
   * @returns {string} - Display label (e.g., 'Tìm hiểu thêm') or value as fallback
   */
  getCTALabel: (state) => (value) => {
    if (!value) return ''

    const cta = state.ctas.find(c => c.value === value)
    return cta ? cta.label : value // Fallback to raw value if not found
  },

  /**
   * Get CTA object by value
   * @param {string} value - CTA enum value
   * @returns {object|null} - CTA object with {value, label} or null
   */
  getCTAByValue: (state) => (value) => {
    if (!value) return null
    return state.ctas.find(c => c.value === value) || null
  },

  /**
   * Check if CTAs are loaded
   */
  isLoaded: (state) => state.ctas.length > 0,

  /**
   * Check if cache is still valid
   */
  isCacheValid: (state) => {
    if (!state.lastLoaded) return false
    const now = Date.now()
    return (now - state.lastLoaded) < state.cacheDuration
  },

  /**
   * Get current language
   */
  currentLanguage: (state) => state.language
}

const mutations = {
  /**
   * Set CTAs list
   */
  SET_CTAS(state, ctas) {
    state.ctas = ctas
    state.lastLoaded = Date.now()
  },

  /**
   * Set language
   */
  SET_LANGUAGE(state, language) {
    state.language = language
  },

  /**
   * Set loading state
   */
  SET_LOADING(state, loading) {
    state.loading = loading
  },

  /**
   * Set error
   */
  SET_ERROR(state, error) {
    state.error = error
  },

  /**
   * Clear CTAs (for cache invalidation)
   */
  CLEAR_CTAS(state) {
    state.ctas = []
    state.lastLoaded = null
  }
}

const actions = {
  /**
   * Load CTAs from API
   * @param {object} context - Vuex context
   * @param {string} language - Language code ('vi' or 'en')
   * @param {boolean} forceReload - Force reload even if cached
   */
  async loadCTAs({ commit, state, getters }, { language = 'vi', forceReload = false } = {}) {
    // Check if already loaded with same language and cache is valid
    if (!forceReload &&
        state.language === language &&
        getters.isLoaded &&
        getters.isCacheValid) {
      console.log('[CTA Store] Using cached CTAs')
      return state.ctas
    }

    commit('SET_LOADING', true)
    commit('SET_ERROR', null)

    try {
      console.log(`[CTA Store] Loading CTAs for language: ${language}`)

      const response = await api.providers.getCallToActions(language)
      const ctas = response.data.ctas || response.data || []

      if (ctas.length === 0) {
        console.warn('[CTA Store] API returned empty CTAs, using fallback')
        commit('SET_CTAS', getFallbackCTAs(language))
      } else {
        commit('SET_CTAS', ctas)
      }

      commit('SET_LANGUAGE', language)

      console.log(`[CTA Store] Loaded ${state.ctas.length} CTAs`)
      return state.ctas

    } catch (error) {
      console.error('[CTA Store] Failed to load CTAs:', error)
      commit('SET_ERROR', error.message)

      // Use fallback CTAs on error
      commit('SET_CTAS', getFallbackCTAs(language))
      commit('SET_LANGUAGE', language)

      return state.ctas
    } finally {
      commit('SET_LOADING', false)
    }
  },

  /**
   * Change CTA language and reload
   * @param {object} context - Vuex context
   * @param {string} language - New language code
   */
  async setLanguage({ dispatch }, language) {
    console.log(`[CTA Store] Changing language to: ${language}`)
    await dispatch('loadCTAs', { language, forceReload: true })
  },

  /**
   * Refresh CTAs (clear cache and reload)
   * @param {object} context - Vuex context
   */
  async refreshCTAs({ commit, state, dispatch }) {
    console.log('[CTA Store] Refreshing CTAs')
    commit('CLEAR_CTAS')
    await dispatch('loadCTAs', { language: state.language, forceReload: true })
  }
}

/**
 * Fallback CTAs when API fails
 * @param {string} language - Language code
 * @returns {array} - Array of CTA objects
 */
function getFallbackCTAs(language) {
  const isVietnamese = language === 'vi'

  return [
    {
      value: 'SHOP_NOW',
      label: isVietnamese ? 'Mua ngay' : 'Shop Now'
    },
    {
      value: 'LEARN_MORE',
      label: isVietnamese ? 'Tìm hiểu thêm' : 'Learn More'
    },
    {
      value: 'SIGN_UP',
      label: isVietnamese ? 'Đăng ký' : 'Sign Up'
    },
    {
      value: 'DOWNLOAD',
      label: isVietnamese ? 'Tải xuống' : 'Download'
    },
    {
      value: 'CONTACT_US',
      label: isVietnamese ? 'Liên hệ' : 'Contact Us'
    },
    {
      value: 'APPLY_NOW',
      label: isVietnamese ? 'Ứng tuyển ngay' : 'Apply Now'
    },
    {
      value: 'BOOK_NOW',
      label: isVietnamese ? 'Đặt ngay' : 'Book Now'
    },
    {
      value: 'GET_OFFER',
      label: isVietnamese ? 'Nhận ưu đãi' : 'Get Offer'
    },
    {
      value: 'MESSAGE_PAGE',
      label: isVietnamese ? 'Nhắn tin' : 'Message Page'
    },
    {
      value: 'SUBSCRIBE',
      label: isVietnamese ? 'Theo dõi' : 'Subscribe'
    }
  ]
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
