/**
 * Competitor search module powered by SearchAPI.io
 */

import api from '@/services/api'
import { findLocationPreset } from '@/constants/searchLocations'

const WATCHLIST_STORAGE_KEY = 'competitorWatchlist'

const normalizeWatchlistItem = (item = {}) => {
  const preset = findLocationPreset(item.locationKey || item.country || item.location || 'global')
  return {
    ...item,
    engine: item.engine || 'linkedin_ad_library',
    advertiser: item.advertiser || item.query || '',
    advertiserId: item.advertiserId || '',
    sortBy: item.sortBy || '',
    keyword: item.keyword || '',
    locationKey: preset.key,
    location: preset.location,
    country: item.country || preset.country,
    limit: item.limit || 10
  }
}

const readWatchlist = () => {
  try {
    const raw = localStorage.getItem(WATCHLIST_STORAGE_KEY)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.map(normalizeWatchlistItem)
  } catch (error) {
    console.error('Failed to parse watchlist', error)
    return []
  }
}

const persistWatchlist = (items) => {
  try {
    localStorage.setItem(WATCHLIST_STORAGE_KEY, JSON.stringify(items))
  } catch (error) {
    console.error('Failed to persist watchlist', error)
  }
}

const defaultResults = () => ({
  ads: [],
  videos: [],
  content: []
})

const state = {
  engine: 'linkedin_ad_library',
  searchResults: defaultResults(),
  isSearching: false,
  searchError: null,
  selectedCompetitorAds: [],
  aiSuggestion: null,
  aiAnalysis: null,
  patterns: null,
  abTestVariations: [],
  isAnalyzing: false,
  history: [],
  isLoadingHistory: false,
  brandSuggestions: [],
  isLoadingSuggestions: false,
  watchlist: [],
  watchlistActivity: [],
  watchlistRefreshing: []
}

const getters = {
  ads: (state) => state.searchResults.ads,
  videos: (state) => state.searchResults.videos,
  content: (state) => state.searchResults.content,
  hasAds: (state) => state.searchResults.ads.length > 0,
  hasVideos: (state) => state.searchResults.videos.length > 0,
  hasContent: (state) => state.searchResults.content.length > 0,
  hasSelectedAds: (state) => state.selectedCompetitorAds.length > 0,
  selectedCount: (state) => state.selectedCompetitorAds.length,
  isWatchlistRefreshing: (state) => (id) => state.watchlistRefreshing.includes(id)
}

const mutations = {
  SET_ENGINE(state, engine) {
    state.engine = engine
  },
  SET_SEARCH_RESULTS(state, payload) {
    state.searchResults = payload || defaultResults()
  },
  SET_SEARCHING(state, status) {
    state.isSearching = status
  },
  SET_SEARCH_ERROR(state, error) {
    state.searchError = error
  },
  SET_SELECTED_ADS(state, ads) {
    state.selectedCompetitorAds = ads
  },
  ADD_SELECTED_AD(state, ad) {
    const exists = state.selectedCompetitorAds.find(item => item.id === ad.id)
    if (!exists) {
      state.selectedCompetitorAds.push(ad)
    }
  },
  REMOVE_SELECTED_AD(state, id) {
    state.selectedCompetitorAds = state.selectedCompetitorAds.filter(ad => ad.id !== id)
  },
  CLEAR_SELECTED_ADS(state) {
    state.selectedCompetitorAds = []
  },
  SET_ANALYZING(state, status) {
    state.isAnalyzing = status
  },
  SET_AI_SUGGESTION(state, data) {
    state.aiSuggestion = data
  },
  SET_AI_ANALYSIS(state, data) {
    state.aiAnalysis = data
  },
  SET_PATTERNS(state, data) {
    state.patterns = data
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
  SET_HISTORY(state, history) {
    state.history = history
  },
  SET_LOADING_HISTORY(state, status) {
    state.isLoadingHistory = status
  },
  SET_BRAND_SUGGESTIONS(state, suggestions) {
    state.brandSuggestions = suggestions
  },
  SET_LOADING_SUGGESTIONS(state, status) {
    state.isLoadingSuggestions = status
  },
  SET_WATCHLIST(state, items) {
    state.watchlist = items
  },
  ADD_WATCHLIST_ITEM(state, item) {
    state.watchlist = [item, ...state.watchlist]
  },
  UPDATE_WATCHLIST_ITEM(state, { id, changes }) {
    state.watchlist = state.watchlist.map(item => (item.id === id ? { ...item, ...changes } : item))
  },
  REMOVE_WATCHLIST_ITEM(state, id) {
    state.watchlist = state.watchlist.filter(item => item.id !== id)
  },
  SET_WATCHLIST_REFRESHING(state, id) {
    if (!state.watchlistRefreshing.includes(id)) {
      state.watchlistRefreshing.push(id)
    }
  },
  REMOVE_WATCHLIST_REFRESHING(state, id) {
    state.watchlistRefreshing = state.watchlistRefreshing.filter(item => item !== id)
  },
  PUSH_WATCHLIST_ACTIVITY(state, activity) {
    state.watchlistActivity = [activity, ...state.watchlistActivity].slice(0, 8)
  }
}

const actions = {
  async searchAcrossWeb({ commit }, payload) {
    const engine = payload.engine || 'linkedin_ad_library'
    const locationPreset = findLocationPreset(payload.locationKey || payload.country || 'global')
    const advertiser = payload.advertiser || payload.query || ''
    const keyword = payload.keyword || ''
    const rawCountry = payload.country || locationPreset.country || ''
    const normalizedCountry = engine === 'tiktok_ads_library' ? (rawCountry || 'ALL') : rawCountry
    const searchPayload = {
      engine,
      query: payload.query || keyword || advertiser || '',
      keyword,
      advertiser,
      advertiserId: payload.advertiserId || '',
      sortBy: payload.sortBy || '',
      country: normalizedCountry,
      timePeriod: payload.timePeriod || '',
      nextPageToken: payload.nextPageToken || '',
      limit: payload.limit || 10
    }
    commit('SET_SEARCHING', true)
    commit('SET_SEARCH_ERROR', null)
    try {
      const response = await api.competitors.search(searchPayload)
      const data = response.data.data || {}
      const engineUsed = data.engine || engine
      const normalized = {
        ads: (data.ads || []).map((item, index) => ({
          ...item,
          id: item.adId || item.id || item.link || `ad-${index}`,
          adId: item.adId || item.id || item.link || `ad-${index}`,
          adName: item.headline || item.title || item.advertiserName || item.advertiser || 'Ad',
          headline: item.headline || item.title || item.advertiserName,
          primaryText: item.description || item.cta || item.estimatedAudience || '',
          imageUrl: item.image || item.coverImage,
          coverImage: item.coverImage || item.image,
          videoUrl: item.videoLink,
          callToAction: item.cta || '',
          advertiserName: item.advertiserName || item.advertiser,
          advertiserThumbnail: item.advertiserThumbnail,
          adLibraryUrl: item.link || item.videoLink,
          firstShown: item.firstShown,
          lastShown: item.lastShown,
          estimatedAudience: item.estimatedAudience,
          estimatedAudienceMin: item.estimatedAudienceMin,
          estimatedAudienceMax: item.estimatedAudienceMax,
          adType: item.adType || (engineUsed === 'tiktok_ads_library' ? 'TikTok Video' : 'LinkedIn Ad')
        })),
        videos: data.videos || [],
        content: data.content || []
      }
      commit('SET_ENGINE', engineUsed)
      commit('SET_SEARCH_RESULTS', normalized)
      commit('CLEAR_SELECTED_ADS')
      return normalized
    } catch (error) {
      const message = error?.response?.data?.message || error.message || 'Failed to search competitors'
      commit('SET_SEARCH_ERROR', message)
      commit('SET_SEARCH_RESULTS', defaultResults())
      throw error
    } finally {
      commit('SET_SEARCHING', false)
    }
  },

  toggleAdSelection({ commit, state }, ad) {
    const exists = state.selectedCompetitorAds.find(item => item.id === ad.id)
    if (exists) {
      commit('REMOVE_SELECTED_AD', ad.id)
    } else {
      commit('ADD_SELECTED_AD', ad)
    }
  },

  clearSelection({ commit }) {
    commit('CLEAR_SELECTED_ADS')
  },

  clearAIResults({ commit }) {
    commit('CLEAR_AI_RESULTS')
  },

  async loadSearchHistory({ commit }, { page = 0, size = 20 } = {}) {
    commit('SET_LOADING_HISTORY', true)
    try {
      const response = await api.competitors.getHistory(page, size)
      commit('SET_HISTORY', response.data.content || [])
      return response.data
    } catch (error) {
      console.error('Failed to load competitor history', error)
      throw error
    } finally {
      commit('SET_LOADING_HISTORY', false)
    }
  },

  async loadBrandSuggestions({ commit }, query) {
    if (!query || query.length < 2) {
      commit('SET_BRAND_SUGGESTIONS', [])
      return
    }
    commit('SET_LOADING_SUGGESTIONS', true)
    try {
      const response = await api.competitors.getSuggestions(query)
      commit('SET_BRAND_SUGGESTIONS', response.data.suggestions || [])
    } catch (error) {
      console.error('Failed to load brand suggestions', error)
      commit('SET_BRAND_SUGGESTIONS', [])
    } finally {
      commit('SET_LOADING_SUGGESTIONS', false)
    }
  },

  async generateSuggestion({ commit }, { competitorAd, myAd, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    commit('SET_SEARCH_ERROR', null)
    try {
      const response = await api.competitors.generateSuggestion(competitorAd, myAd, aiProvider)
      commit('SET_AI_SUGGESTION', response.data)
      return response.data
    } catch (error) {
      commit('SET_SEARCH_ERROR', error?.message || 'Failed to generate suggestion')
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  async analyzeCompetitorAd({ commit }, { competitorAd, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    try {
      const response = await api.competitors.analyze(competitorAd, aiProvider)
      commit('SET_AI_ANALYSIS', response.data)
      return response.data
    } catch (error) {
      commit('SET_SEARCH_ERROR', error?.message || 'Failed to analyze competitor ad')
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  async identifyPatterns({ commit }, { competitorAds, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    try {
      const response = await api.competitors.identifyPatterns(competitorAds, aiProvider)
      commit('SET_PATTERNS', response.data)
      return response.data
    } catch (error) {
      commit('SET_SEARCH_ERROR', error?.message || 'Failed to identify patterns')
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  async generateABTest({ commit }, { competitorAd, myAd, variationCount = 3, aiProvider = 'openai' }) {
    commit('SET_ANALYZING', true)
    try {
      const response = await api.competitors.generateABTest(competitorAd, myAd, variationCount, aiProvider)
      commit('SET_AB_TEST_VARIATIONS', response.data.variations || [])
      return response.data
    } catch (error) {
      commit('SET_SEARCH_ERROR', error?.message || 'Failed to generate A/B test variations')
      throw error
    } finally {
      commit('SET_ANALYZING', false)
    }
  },

  initWatchlist({ commit }) {
    const items = readWatchlist()
    commit('SET_WATCHLIST', items)
    persistWatchlist(items)
  },

  addWatchlistItem({ commit, state }, payload) {
    const preset = findLocationPreset(payload.locationKey || payload.country || 'global')
    const query = payload.query?.trim() || ''
    const keyword = payload.keyword?.trim() || ''
    if (!query && !keyword) {
      throw new Error('Query or keyword is required')
    }
    const item = normalizeWatchlistItem({
      id: `watch-${Date.now()}`,
      query,
      keyword,
      advertiser: payload.advertiser?.trim() || query,
      advertiserId: payload.advertiserId || '',
      sortBy: payload.sortBy || '',
      engine: payload.engine || 'linkedin_ad_library',
      locationKey: preset.key,
      location: preset.location,
      country: preset.country,
      limit: payload.limit || 10,
      lastChecked: null,
      lastResultCount: null,
      hasNew: false,
      lastMessage: null
    })
    commit('ADD_WATCHLIST_ITEM', item)
    persistWatchlist(state.watchlist)
    return item
  },

  removeWatchlistItem({ commit, state }, id) {
    commit('REMOVE_WATCHLIST_ITEM', id)
    persistWatchlist(state.watchlist)
  },

  async refreshWatchlistItem({ commit, state }, item) {
    if (!item) return null
    const normalizedItem = normalizeWatchlistItem(item)
    commit('UPDATE_WATCHLIST_ITEM', {
      id: item.id,
      changes: {
        locationKey: normalizedItem.locationKey,
        location: normalizedItem.location,
        country: normalizedItem.country,
        limit: normalizedItem.limit
      }
    })
    persistWatchlist(state.watchlist)
    commit('SET_WATCHLIST_REFRESHING', item.id)
    try {
      const requestCountry = normalizedItem.engine === 'tiktok_ads_library'
        ? (normalizedItem.country || 'ALL')
        : normalizedItem.country
      const response = await api.competitors.search({
        query: normalizedItem.query,
        engine: normalizedItem.engine,
        advertiser: normalizedItem.advertiser || normalizedItem.query,
        keyword: normalizedItem.keyword || normalizedItem.query,
        advertiserId: normalizedItem.advertiserId,
        sortBy: normalizedItem.sortBy,
        country: requestCountry,
        limit: normalizedItem.limit
      })
      const data = response.data.data || {}
      const count = (data.ads?.length || 0) + (data.videos?.length || 0) + (data.content?.length || 0)
      const hasNew = item.lastResultCount !== null ? count > item.lastResultCount : count > 0
      commit('UPDATE_WATCHLIST_ITEM', {
        id: item.id,
        changes: {
          lastChecked: new Date().toISOString(),
          lastResultCount: count,
          hasNew,
          lastMessage: response.data.message || ''
        }
      })
      persistWatchlist(state.watchlist)
      commit('PUSH_WATCHLIST_ACTIVITY', {
        id: `${item.id}-${Date.now()}`,
        query: item.query,
        engine: item.engine,
        count,
        hasNew,
        timestamp: new Date().toISOString(),
        message: response.data.message || 'Refreshed'
      })
      return response
    } catch (error) {
      commit('PUSH_WATCHLIST_ACTIVITY', {
        id: `${item.id}-${Date.now()}`,
        query: item.query,
        engine: item.engine,
        count: item.lastResultCount || 0,
        hasNew: false,
        timestamp: new Date().toISOString(),
        message: error?.message || 'Failed to refresh',
        error: true
      })
      throw error
    } finally {
      commit('REMOVE_WATCHLIST_REFRESHING', item.id)
    }
  },

  async refreshAllWatchlist({ state, dispatch }) {
    for (const item of state.watchlist) {
      try {
        await dispatch('refreshWatchlistItem', item)
      } catch (error) {
        console.error('Watchlist refresh failed', error)
      }
    }
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
