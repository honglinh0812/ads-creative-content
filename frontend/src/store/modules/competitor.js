/**
 * Competitor Insights Vuex Module (Phase 3)
 * Manages state for competitor ad searches, analysis, and comparisons
 */

import api from '../../services/api'

const PLATFORM_LABELS = {
  facebook: 'Facebook & Instagram',
  google: 'Google Ads & YouTube',
  tiktok: 'TikTok'
}

const PLATFORM_IFRAME_BUILDERS = {
  google: (brand, region = 'US') => {
    const keyword = encodeURIComponent(brand || '')
    return `https://adstransparency.google.com/?region=${region || 'US'}&q=${keyword}`
  },
  tiktok: (brand) => {
    const keyword = encodeURIComponent(brand || '')
    return `https://ads.tiktok.com/business/creativecenter/inspiration/topads/pc/en?keyword=${keyword}`
  }
}

const BRAND_PLACEHOLDER = 'thương hiệu này'

const formatBrandPart = (brandName) => {
  if (!brandName || !brandName.trim()) {
    return ` cho ${BRAND_PLACEHOLDER}`
  }
  return ` cho "${brandName.trim()}"`
}

const formatRegionPart = (region) => {
  if (!region || region.trim().length === 0 || region.toUpperCase() === 'GLOBAL') {
    return ''
  }
  return ` tại ${region.trim().toUpperCase()}`
}

const getPlatformLabel = (platform) => PLATFORM_LABELS[platform] || platform || 'Competitor Ads'

const buildUserFriendlyMessage = (response) => {
  const mode = (response.mode || '').toString().toLowerCase()
  const errorCode = (response.errorCode || '').toString().toLowerCase()
  const serverMessage = response.message
  const platformLabel = getPlatformLabel(response.platform)
  const brandPart = formatBrandPart(response.brandName)
  const regionPart = formatRegionPart(response.region)
  const ads = response.ads || []
  const defaultError = `Không thể tải dữ liệu ${platformLabel}. Vui lòng thử lại sau hoặc mở trực tiếp trang ${platformLabel}.`

  if (serverMessage && ['data', 'empty', 'iframe'].includes(mode)) {
    return serverMessage
  }

  if (serverMessage && ['config_missing', 'validation_error', 'region_unsupported', 'no_results'].includes(errorCode)) {
    return serverMessage
  }

  if (mode === 'data' && ads.length) {
    return serverMessage || `Đã tìm thấy ${ads.length} quảng cáo trên ${platformLabel}${brandPart}${regionPart}.`
  }

  if (mode === 'empty' || errorCode === 'no_results') {
    return serverMessage || `Không tìm thấy quảng cáo ${platformLabel}${brandPart}${regionPart}.`
  }

  if (mode === 'iframe') {
    return serverMessage || `Đang hiển thị ${platformLabel} ở chế độ nhúng.`
  }

  switch (errorCode) {
    case 'config_missing':
      return 'Tính năng này chưa được cấu hình đầy đủ. Vui lòng cập nhật API key hoặc liên hệ quản trị viên.'
    case 'region_unsupported':
      return `${platformLabel} chưa hỗ trợ khu vực ${response.region || ''}. Vui lòng thử khu vực như US hoặc SG.`
    case 'rate_limited':
    case 'quota_exceeded':
      return `${platformLabel} đang bị giới hạn truy vấn. Thử lại sau ít phút.`
    case 'validation_error':
      return `Không thể tìm quảng cáo ${platformLabel} vì thông tin tìm kiếm chưa hợp lệ. Hãy kiểm tra lại từ khóa hoặc khu vực.`
    case 'provider_error':
    case 'client_error':
    case 'temporary_error':
      return `Không thể tải dữ liệu ${platformLabel} lúc này. Vui lòng thử lại sau hoặc sử dụng chế độ iframe.`
    default:
      break
  }

  if (serverMessage) {
    return serverMessage
  }

  return defaultError
}

const createPlatformResponse = (overrides = {}) => {
  const base = {
    platform: overrides.platform || 'facebook',
    mode: overrides.mode || 'empty',
    success: overrides.success || false,
    ads: overrides.ads || [],
    totalResults: overrides.totalResults || 0,
    iframeUrl: overrides.iframeUrl || null,
    message: overrides.message || '',
    friendlySuggestion: overrides.friendlySuggestion || '',
    fallbackRegions: overrides.fallbackRegions || [],
    errorCode: overrides.errorCode || null,
    metadata: overrides.metadata || {},
    brandName: overrides.brandName || null,
    region: overrides.region || null,
    retryable: typeof overrides.retryable === 'boolean' ? overrides.retryable : null,
    timestamp: overrides.timestamp || null
  }

  return {
    ...base,
    userMessage: overrides.userMessage || buildUserFriendlyMessage(base)
  }
}

const normalizeFacebookResponse = (payload = {}, params) => {
  const ads = payload.ads || []
  const totalResults = payload.totalResults ?? ads.length
  const success = ads.length > 0
  return createPlatformResponse({
    platform: 'facebook',
    mode: success ? 'data' : 'empty',
    success,
    ads,
    totalResults,
    message: payload.message || (success
      ? `Found ${totalResults} Facebook ads for ${params.brandName}`
      : `Không tìm thấy quảng cáo Facebook cho ${params.brandName}.`),
    friendlySuggestion: success ? '' : 'Thử sử dụng từ khóa khác hoặc chọn khu vực khác để có nhiều kết quả hơn.',
    brandName: params.brandName,
    region: params.region,
    timestamp: new Date().toISOString()
  })
}

const normalizeProviderResponse = (platform, payload = {}, params) => {
  const ads = payload.ads || []
  const inferredMode = payload.mode || (ads.length ? 'data' : (payload.iframeUrl ? 'iframe' : 'empty'))
  const builder = PLATFORM_IFRAME_BUILDERS[platform]
  const iframeUrl = payload.iframeUrl || (builder ? builder(params.brandName, params.region) : null)

  return createPlatformResponse({
    platform,
    mode: inferredMode,
    success: payload.success ?? inferredMode === 'data',
    ads,
    totalResults: payload.totalResults ?? payload.total ?? ads.length,
    iframeUrl,
    message: payload.message || (ads.length
      ? `Found ${ads.length} ${PLATFORM_LABELS[platform] || platform} ads`
      : `Không tìm thấy quảng cáo ${PLATFORM_LABELS[platform] || platform} cho ${params.brandName}.`),
    friendlySuggestion: payload.friendlySuggestion || (
      inferredMode !== 'data'
        ? 'Bạn có thể xem trực tiếp bằng iframe hoặc thử vùng/khoá khác.'
        : ''
    ),
    fallbackRegions: payload.fallbackRegions || [],
    errorCode: payload.errorCode || null,
    metadata: payload.metadata || {},
    brandName: payload.brandName || params.brandName,
    region: payload.region || params.region,
    retryable: typeof payload.retryable === 'boolean' ? payload.retryable : null,
    timestamp: new Date().toISOString()
  })
}

const buildDefaultIframeUrl = (platform, brandName, region) => {
  const builder = PLATFORM_IFRAME_BUILDERS[platform]
  return builder ? builder(brandName, region) : null
}

const state = {
  // Search results
  searchResults: [],
  selectedCompetitorAds: [],
  searchHistory: [],
  platformResponses: {
    facebook: createPlatformResponse({ platform: 'facebook', mode: 'empty' }),
    google: createPlatformResponse({ platform: 'google', mode: 'empty' }),
    tiktok: createPlatformResponse({ platform: 'tiktok', mode: 'empty' })
  },
  recentPlatformStatuses: [],

  // AI Analysis results
  aiSuggestion: null,
  aiAnalysis: null,
  patterns: null,
  abTestVariations: [],

  // UI state
  isSearching: false,
  isFetchingAds: false,
  isAnalyzing: false,
  isLoadingHistory: false,

  // Autocomplete
  brandSuggestions: [],
  isLoadingSuggestions: false,

  // Errors
  searchError: null,
  fetchError: null,
  analysisError: null,

  // Search params
  lastSearchBrand: null,
  lastSearchRegion: 'US',
  lastSearchLimit: 5
}

const getters = {
  searchResults: state => state.searchResults,
  selectedCompetitorAds: state => state.selectedCompetitorAds,
  searchHistory: state => state.searchHistory,
  aiSuggestion: state => state.aiSuggestion,
  aiAnalysis: state => state.aiAnalysis,
  patterns: state => state.patterns,
  abTestVariations: state => state.abTestVariations,
  brandSuggestions: state => state.brandSuggestions,
  platformResponses: state => state.platformResponses,
  recentPlatformStatuses: state => state.recentPlatformStatuses,

  hasSearchResults: state => state.searchResults.length > 0,
  hasSelectedAds: state => state.selectedCompetitorAds.length > 0,
  selectedCount: state => state.selectedCompetitorAds.length,

  isLoading: state => state.isSearching || state.isFetchingAds || state.isAnalyzing,

  searchError: state => state.searchError,
  fetchError: state => state.fetchError,
  analysisError: state => state.analysisError
}

const mutations = {
  // Search results
  SET_SEARCH_RESULTS(state, results) {
    state.searchResults = results
  },

  CLEAR_SEARCH_RESULTS(state) {
    state.searchResults = []
  },

  SET_PLATFORM_RESPONSE(state, { platform, response }) {
    if (!platform) return
    state.platformResponses = {
      ...state.platformResponses,
      [platform]: createPlatformResponse({ platform, ...response })
    }
  },

  UPDATE_PLATFORM_IFRAME_URL(state, { platform, iframeUrl }) {
    if (!platform || !state.platformResponses[platform]) return
    const current = state.platformResponses[platform]
    state.platformResponses = {
      ...state.platformResponses,
      [platform]: {
        ...current,
        iframeUrl,
        mode: current.mode === 'data' ? current.mode : 'iframe'
      }
    }
  },

  PUSH_PLATFORM_STATUS(state, status) {
    if (!status) return
    const nextFeed = [
      { ...status, id: `${status.platform}-${status.timestamp}` },
      ...state.recentPlatformStatuses
    ]
    state.recentPlatformStatuses = nextFeed.slice(0, 5)
  },

  // Selected ads
  ADD_SELECTED_AD(state, ad) {
    const exists = state.selectedCompetitorAds.find(a => a.adLibraryId === ad.adLibraryId)
    if (!exists) {
      state.selectedCompetitorAds.push(ad)
    }
  },

  REMOVE_SELECTED_AD(state, adLibraryId) {
    state.selectedCompetitorAds = state.selectedCompetitorAds.filter(
      ad => ad.adLibraryId !== adLibraryId
    )
  },

  CLEAR_SELECTED_ADS(state) {
    state.selectedCompetitorAds = []
  },

  SET_SELECTED_ADS(state, ads) {
    state.selectedCompetitorAds = ads
  },

  // Search history
  SET_SEARCH_HISTORY(state, history) {
    state.searchHistory = history
  },

  ADD_TO_HISTORY(state, searchRecord) {
    state.searchHistory.unshift(searchRecord)
  },

  // AI results
  SET_AI_SUGGESTION(state, suggestion) {
    state.aiSuggestion = suggestion
  },

  SET_AI_ANALYSIS(state, analysis) {
    state.aiAnalysis = analysis
  },

  SET_PATTERNS(state, patterns) {
    state.patterns = patterns
  },

  SET_AB_TEST_VARIATIONS(state, variations) {
    state.abTestVariations = variations
  },

  CLEAR_AI_RESULTS(state) {
    state.aiSuggestion = null
    state.aiAnalysis = null
    state.patterns = null
    state.abTestVariations = []
  },

  // Autocomplete
  SET_BRAND_SUGGESTIONS(state, suggestions) {
    state.brandSuggestions = suggestions
  },

  CLEAR_BRAND_SUGGESTIONS(state) {
    state.brandSuggestions = []
  },

  // Loading states
  SET_SEARCHING(state, isSearching) {
    state.isSearching = isSearching
  },

  SET_FETCHING_ADS(state, isFetching) {
    state.isFetchingAds = isFetching
  },

  SET_ANALYZING(state, isAnalyzing) {
    state.isAnalyzing = isAnalyzing
  },

  SET_LOADING_HISTORY(state, isLoading) {
    state.isLoadingHistory = isLoading
  },

  SET_LOADING_SUGGESTIONS(state, isLoading) {
    state.isLoadingSuggestions = isLoading
  },

  // Errors
  SET_SEARCH_ERROR(state, error) {
    state.searchError = error
  },

  SET_FETCH_ERROR(state, error) {
    state.fetchError = error
  },

  SET_ANALYSIS_ERROR(state, error) {
    state.analysisError = error
  },

  CLEAR_ERRORS(state) {
    state.searchError = null
    state.fetchError = null
    state.analysisError = null
  },

  // Search params
  SET_LAST_SEARCH_PARAMS(state, { brand, region, limit }) {
    state.lastSearchBrand = brand
    state.lastSearchRegion = region
    state.lastSearchLimit = limit
  }
}

const actions = {
  async searchPlatformAds({ commit }, { platform = 'facebook', brandName, region = 'US', limit = 5 } = {}) {
    commit('SET_SEARCHING', true)
    commit('SET_SEARCH_ERROR', null)

    const params = { brandName, region, limit }

    try {
      let response
      if (platform === 'google') {
        response = await api.competitors.searchGoogle(brandName, region, limit)
      } else if (platform === 'tiktok') {
        response = await api.competitors.searchTikTok(brandName, region, limit)
      } else {
        response = await api.competitors.search(brandName, region, limit)
      }

      const normalized = platform === 'facebook'
        ? normalizeFacebookResponse(response.data, params)
        : normalizeProviderResponse(platform, response.data, params)

      commit('SET_PLATFORM_RESPONSE', { platform, response: normalized })
      if (normalized.mode === 'data') {
        commit('SET_SEARCH_RESULTS', normalized.ads || [])
      } else {
        commit('CLEAR_SEARCH_RESULTS')
      }

      commit('SET_LAST_SEARCH_PARAMS', { brand: brandName, region, limit })

      commit('PUSH_PLATFORM_STATUS', {
        platform,
        platformLabel: PLATFORM_LABELS[platform] || platform,
        success: normalized.success,
        mode: normalized.mode,
        message: normalized.message,
        userMessage: normalized.userMessage,
        friendlySuggestion: normalized.friendlySuggestion,
        timestamp: normalized.timestamp
      })

      return normalized

    } catch (error) {
      const fallbackPayload = error?.response?.data

      if (fallbackPayload && (fallbackPayload.mode || fallbackPayload.message)) {
        const normalizedError = platform === 'facebook'
          ? normalizeFacebookResponse(fallbackPayload, params)
          : normalizeProviderResponse(platform, fallbackPayload, params)

        commit('SET_PLATFORM_RESPONSE', { platform, response: normalizedError })
        if (normalizedError.mode === 'data') {
          commit('SET_SEARCH_RESULTS', normalizedError.ads || [])
        } else {
          commit('CLEAR_SEARCH_RESULTS')
        }

        if (normalizedError.mode === 'error') {
          commit('SET_SEARCH_ERROR', normalizedError.userMessage || normalizedError.message)
        }

        commit('SET_LAST_SEARCH_PARAMS', { brand: brandName, region, limit })

        commit('PUSH_PLATFORM_STATUS', {
          platform,
          platformLabel: PLATFORM_LABELS[platform] || platform,
          success: normalizedError.success,
          mode: normalizedError.mode,
          message: normalizedError.message,
          userMessage: normalizedError.userMessage,
          friendlySuggestion: normalizedError.friendlySuggestion,
          timestamp: normalizedError.timestamp || new Date().toISOString()
        })

        return normalizedError
      }

      const errorMessage = error.response?.data?.error || error.message || 'Failed to search competitor ads'
      const fallbackIframe = buildDefaultIframeUrl(platform, brandName, region)
      const fallbackResponse = createPlatformResponse({
        platform,
        mode: fallbackIframe && platform !== 'facebook' ? 'iframe' : 'error',
        iframeUrl: fallbackIframe,
        success: false,
        ads: [],
        totalResults: 0,
        message: errorMessage,
        friendlySuggestion: platform === 'facebook'
          ? 'Vui lòng thử lại sau hoặc kiểm tra kết nối.'
          : 'Đang chuyển sang iframe do API gặp sự cố.',
        errorCode: 'client_error',
        brandName,
        region,
        timestamp: new Date().toISOString()
      })

      commit('SET_PLATFORM_RESPONSE', { platform, response: fallbackResponse })
      commit('SET_LAST_SEARCH_PARAMS', { brand: brandName, region, limit })
      commit('SET_SEARCH_ERROR', fallbackResponse.userMessage || errorMessage)
      commit('CLEAR_SEARCH_RESULTS')
      commit('PUSH_PLATFORM_STATUS', {
        platform,
        platformLabel: PLATFORM_LABELS[platform] || platform,
        success: false,
        mode: fallbackResponse.mode,
        message: fallbackResponse.message,
        userMessage: fallbackResponse.userMessage,
        friendlySuggestion: fallbackResponse.friendlySuggestion,
        timestamp: fallbackResponse.timestamp
      })

      throw error

    } finally {
      commit('SET_SEARCHING', false)
    }
  },

  /**
   * Search for competitor ads by brand name
   */
  async searchCompetitorAds({ dispatch }, payload) {
    return dispatch('searchPlatformAds', { platform: 'facebook', ...payload })
  },

  /**
   * Fetch specific competitor ads by URLs
   */
  async fetchAdsByUrls({ commit }, adUrls) {
    commit('SET_FETCHING_ADS', true)
    commit('SET_FETCH_ERROR', null)

    try {
      const response = await api.competitors.fetchByUrls(adUrls)

      const fetchedAds = response.data.ads || []
      commit('SET_SELECTED_ADS', fetchedAds)

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch ads'
      commit('SET_FETCH_ERROR', errorMessage)
      console.error('Fetch ads by URLs error:', error)
      throw error
    } finally {
      commit('SET_FETCHING_ADS', false)
    }
  },

  /**
   * Load search history
   */
  async loadSearchHistory({ commit }, { page = 0, size = 20 } = {}) {
    commit('SET_LOADING_HISTORY', true)

    try {
      const response = await api.competitors.getHistory(page, size)

      commit('SET_SEARCH_HISTORY', response.data.content || [])

      return response.data
    } catch (error) {
      console.error('Load search history error:', error)
      throw error
    } finally {
      commit('SET_LOADING_HISTORY', false)
    }
  },

  /**
   * Get brand name suggestions for autocomplete
   */
  async loadBrandSuggestions({ commit }, query) {
    if (!query || query.length < 2) {
      commit('CLEAR_BRAND_SUGGESTIONS')
      return
    }

    commit('SET_LOADING_SUGGESTIONS', true)

    try {
      const response = await api.competitors.getSuggestions(query)

      commit('SET_BRAND_SUGGESTIONS', response.data.suggestions || [])

      return response.data.suggestions
    } catch (error) {
      console.error('Load brand suggestions error:', error)
      commit('CLEAR_BRAND_SUGGESTIONS')
    } finally {
      commit('SET_LOADING_SUGGESTIONS', false)
    }
  },

  /**
   * Generate AI suggestion based on competitor ad
   */
  async generateSuggestion({ commit }, { competitorAd, myAd, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    commit('SET_ANALYSIS_ERROR', null)

    try {
      const response = await api.competitors.generateSuggestion(competitorAd, myAd, aiProvider)

      commit('SET_AI_SUGGESTION', response.data)

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to generate suggestion'
      commit('SET_ANALYSIS_ERROR', errorMessage)
      console.error('Generate suggestion error:', error)
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  /**
   * Analyze single competitor ad
   */
  async analyzeCompetitorAd({ commit }, { competitorAd, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    commit('SET_ANALYSIS_ERROR', null)

    try {
      const response = await api.competitors.analyze(competitorAd, aiProvider)

      commit('SET_AI_ANALYSIS', response.data)

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to analyze ad'
      commit('SET_ANALYSIS_ERROR', errorMessage)
      console.error('Analyze competitor ad error:', error)
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  /**
   * Identify patterns across multiple ads
   */
  async identifyPatterns({ commit }, { competitorAds, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    commit('SET_ANALYSIS_ERROR', null)

    try {
      const response = await api.competitors.identifyPatterns(competitorAds, aiProvider)

      commit('SET_PATTERNS', response.data)

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to identify patterns'
      commit('SET_ANALYSIS_ERROR', errorMessage)
      console.error('Identify patterns error:', error)
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  /**
   * Generate A/B test variations
   */
  async generateABTest({ commit }, { competitorAd, myAd, variationCount = 3, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    commit('SET_ANALYSIS_ERROR', null)

    try {
      const response = await api.competitors.generateABTest(competitorAd, myAd, variationCount, aiProvider)

      commit('SET_AB_TEST_VARIATIONS', response.data.variations || [])

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to generate A/B test variations'
      commit('SET_ANALYSIS_ERROR', errorMessage)
      console.error('Generate A/B test error:', error)
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  /**
   * Toggle ad selection
   */
  toggleAdSelection({ commit, state }, ad) {
    const isSelected = state.selectedCompetitorAds.find(a => a.adLibraryId === ad.adLibraryId)

    if (isSelected) {
      commit('REMOVE_SELECTED_AD', ad.adLibraryId)
    } else {
      commit('ADD_SELECTED_AD', ad)
    }
  },

  /**
   * Clear all selections
   */
  clearSelection({ commit }) {
    commit('CLEAR_SELECTED_ADS')
  },

  /**
   * Clear all AI results
   */
  clearAIResults({ commit }) {
    commit('CLEAR_AI_RESULTS')
  },

  /**
   * Clear all errors
   */
  clearErrors({ commit }) {
    commit('CLEAR_ERRORS')
  },

  /**
   * Reset module state
   */
  reset({ commit }) {
    commit('CLEAR_SEARCH_RESULTS')
    commit('CLEAR_SELECTED_ADS')
    commit('CLEAR_AI_RESULTS')
    commit('CLEAR_BRAND_SUGGESTIONS')
    commit('CLEAR_ERRORS')
    commit('SET_LAST_SEARCH_PARAMS', { brand: null, region: 'US', limit: 5 })
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
