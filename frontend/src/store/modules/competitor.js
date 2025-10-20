/**
 * Competitor Insights Vuex Module (Phase 3)
 * Manages state for competitor ad searches, analysis, and comparisons
 */

import api from '../../services/api'

const state = {
  // Search results
  searchResults: [],
  selectedCompetitorAds: [],
  searchHistory: [],

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
  /**
   * Search for competitor ads by brand name
   */
  async searchCompetitorAds({ commit }, { brandName, region = 'US', limit = 5 }) {
    commit('SET_SEARCHING', true)
    commit('SET_SEARCH_ERROR', null)
    commit('CLEAR_SEARCH_RESULTS')

    try {
      const response = await api.competitors.search(brandName, region, limit)

      commit('SET_SEARCH_RESULTS', response.data.ads || [])
      commit('SET_LAST_SEARCH_PARAMS', { brand: brandName, region, limit })

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to search competitor ads'
      commit('SET_SEARCH_ERROR', errorMessage)
      console.error('Search competitor ads error:', error)
      throw error
    } finally {
      commit('SET_SEARCHING', false)
    }
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
