import api from '@/services/api'

const state = {
  stats: {
    totalCampaigns: 0,
    totalAds: 0,
    activeAds: 0,
    totalSpent: 0
  },
  campaigns: [],
  recentAds: [],
  loading: false,
  error: null,
  lastFetched: null,
  cacheTimeout: 5 * 60 * 1000 // 5 minutes
}

const mutations = {
  SET_LOADING(state, loading) {
    state.loading = loading
  },
  
  SET_ERROR(state, error) {
    state.error = error
  },
  
  SET_STATS(state, stats) {
    state.stats = stats
  },
  
  SET_CAMPAIGNS(state, campaigns) {
    state.campaigns = campaigns
  },
  
  SET_RECENT_ADS(state, ads) {
    state.recentAds = ads
  },
  
  SET_LAST_FETCHED(state, timestamp) {
    state.lastFetched = timestamp
  },
  
  CLEAR_CACHE(state) {
    state.lastFetched = null
    state.stats = {
      totalCampaigns: 0,
      totalAds: 0,
      activeAds: 0,
      totalSpent: 0
    }
    state.campaigns = []
    state.recentAds = []
  }
}

const actions = {
  async fetchDashboardData({ commit }) {
    // Set loading true ngay từ đầu để tránh hiển thị dữ liệu cũ
    commit('SET_LOADING', true)
    commit('SET_ERROR', null)
    
    // Xóa cache để đảm bảo luôn fetch dữ liệu mới
    commit('CLEAR_CACHE')
    
    try {
      // Fetch all data in parallel for better performance
      const [dashboardResponse] = await Promise.all([
        api.get('/dashboard'),
        api.get('/campaigns?page=0&size=5'),
        api.get('/ads?page=0&size=5')
      ])
      
      commit('SET_STATS', dashboardResponse.data.stats)
      commit('SET_CAMPAIGNS', dashboardResponse.data.campaigns || [])
      commit('SET_RECENT_ADS', dashboardResponse.data.recentAds || [])
      commit('SET_LAST_FETCHED', Date.now())
      
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
      commit('SET_ERROR', error.response?.data?.message || 'Failed to load dashboard data')
    } finally {
      commit('SET_LOADING', false)
    }
  },
  
  async refreshDashboardData({ commit, dispatch }) {
    commit('CLEAR_CACHE')
    await dispatch('fetchDashboardData')
  },
  
  clearCache({ commit }) {
    commit('CLEAR_CACHE')
  }
}

const getters = {
  isDataStale: (state) => {
    if (!state.lastFetched) return true
    const now = Date.now()
    return (now - state.lastFetched) > state.cacheTimeout
  },
  
  cacheAge: (state) => {
    if (!state.lastFetched) return null
    return Date.now() - state.lastFetched
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}

