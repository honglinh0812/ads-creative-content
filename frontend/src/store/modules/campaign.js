import api from '../../services/api'

export default {
  namespaced: true,
  state: {
    campaigns: [],
    currentCampaign: null,
    loading: false,
    error: null,
    totalItems: 0,
    totalPages: 0,
    currentPage: 0
  },
  getters: {
    campaigns: state => state.campaigns,
    currentCampaign: state => state.currentCampaign,
    loading: state => state.loading,
    error: state => state.error,
    totalItems: state => state.totalItems,
    totalPages: state => state.totalPages,
    currentPage: state => state.currentPage
  },
  mutations: {
    SET_CAMPAIGNS(state, data) {
      state.campaigns = data.content
      state.totalItems = data.totalElements
      state.totalPages = data.totalPages
      state.currentPage = data.number
    },
    SET_CURRENT_CAMPAIGN(state, campaign) {
      state.currentCampaign = campaign
    },
    ADD_CAMPAIGN(state, campaign) {
      state.campaigns.unshift(campaign) // Add to beginning for newest first
    },
    UPDATE_CAMPAIGN(state, updatedCampaign) {
      const index = state.campaigns.findIndex(c => c.id === updatedCampaign.id)
      if (index !== -1) {
        state.campaigns.splice(index, 1, updatedCampaign)
      }
      if (state.currentCampaign && state.currentCampaign.id === updatedCampaign.id) {
        state.currentCampaign = updatedCampaign
      }
    },
    REMOVE_CAMPAIGN(state, campaignId) {
      state.campaigns = state.campaigns.filter(c => c.id !== campaignId)
      if (state.currentCampaign && state.currentCampaign.id === campaignId) {
        state.currentCampaign = null
      }
    },
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_ERROR(state, error) {
      state.error = error
    }
  },
  actions: {
    async fetchCampaigns({ commit }, { page = 0, size = 5 } = {}) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        const response = await api.campaigns.getAll(page, size)
        commit('SET_CAMPAIGNS', response.data)
      } catch (error) {
        console.error('Fetch campaigns error:', error)
        commit('SET_ERROR', error.message || 'Failed to fetch campaigns')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async fetchCampaign({ commit }, campaignId) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        const response = await api.campaigns.get(campaignId)
        commit('SET_CURRENT_CAMPAIGN', response.data)
        return response.data
      } catch (error) {
        console.error('Fetch campaign error:', error)
        commit('SET_ERROR', error.message || 'Failed to fetch campaign details')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async createCampaign({ commit, dispatch }, campaignData) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        console.log('Creating campaign with data:', campaignData)
        const response = await api.campaigns.create(campaignData)
        console.log('Campaign created successfully:', response.data)
        
        commit('ADD_CAMPAIGN', response.data)
        
        // Also refresh dashboard data if available
        try {
          await dispatch('dashboard/fetchDashboardData', null, { root: true })
        } catch (dashboardError) {
          console.warn('Failed to refresh dashboard after campaign creation:', dashboardError)
        }
        
        return response.data
      } catch (error) {
        console.error('Create campaign error:', error)
        commit('SET_ERROR', error.message || 'Failed to create campaign')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async updateCampaign({ commit }, { campaignId, campaignData }) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        const response = await api.campaigns.update(campaignId, campaignData)
        commit('UPDATE_CAMPAIGN', response.data)
        return response.data
      } catch (error) {
        console.error('Update campaign error:', error)
        commit('SET_ERROR', error.message || 'Failed to update campaign')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async deleteCampaign({ commit, dispatch }, campaignId) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        await api.campaigns.delete(campaignId)
        commit('REMOVE_CAMPAIGN', campaignId)
        
        // Refresh campaigns list to get updated pagination
        await dispatch('fetchCampaigns')
      } catch (error) {
        console.error('Delete campaign error:', error)
        commit('SET_ERROR', error.message || 'Failed to delete campaign')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    async searchCampaigns({ commit }, query) {
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      
      try {
        // Tìm kiếm local trong campaigns hiện tại
        // Hoặc có thể gọi API search nếu backend hỗ trợ
        const response = await api.campaigns.getAll(0, 100) // Lấy nhiều hơn để search
        const allCampaigns = response.data.content
        const filteredCampaigns = allCampaigns.filter(campaign => 
          campaign.name.toLowerCase().includes(query.toLowerCase()) ||
          campaign.status.toLowerCase().includes(query.toLowerCase())
        )
        
        commit('SET_CAMPAIGNS', {
          content: filteredCampaigns,
          totalElements: filteredCampaigns.length,
          totalPages: 1,
          number: 0
        })
      } catch (error) {
        console.error('Search campaigns error:', error)
        commit('SET_ERROR', error.message || 'Failed to search campaigns')
        throw error
      } finally {
        commit('SET_LOADING', false)
      }
    }
  }
}


