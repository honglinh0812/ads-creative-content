import { createStore } from 'vuex'
import auth from './modules/auth'
import campaign from './modules/campaign'
import ad from './modules/ad'
import notification from './modules/notification'
import toast from './modules/toast'
import api from '../services/api'

export default createStore({
  state: {
    user: null,
    loading: false,
    error: null
  },
  
  getters: {
    user: state => state.user,
    loading: state => state.loading,
    error: state => state.error
  },
  
  mutations: {
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    
    SET_ERROR(state, error) {
      state.error = error
    },
    
    SET_USER(state, user) {
      state.user = user
    }
  },
  
  actions: {
    async logoutUser({ commit }) {
      try {
        await api.auth.logout()
      } catch (error) {
        console.error('Logout error:', error)
      } finally {
        commit('SET_USER', null)
        localStorage.removeItem('token')
      }
    }
  },
  
  modules: {
    auth,
    campaign,
    ad,
    notification,
    toast,
    dashboard: {
      namespaced: true,
      state: {
        stats: {
          totalCampaigns: 0,
          totalAds: 0,
          activeCampaigns: 0,
          activeAds: 0
        },
        campaigns: [],
        recentAds: [],
        loading: false,
        error: null
      },
      
      getters: {
        stats: state => state.stats,
        campaigns: state => state.campaigns,
        recentAds: state => state.recentAds,
        loading: state => state.loading,
        error: state => state.error
      },
      
      mutations: {
        SET_LOADING(state, loading) {
          state.loading = loading
        },
        
        SET_ERROR(state, error) {
          state.error = error
        },
        
        SET_DASHBOARD_DATA(state, data) {
          if (data) {
            state.stats = data.stats || {
              totalCampaigns: 0,
              totalAds: 0,
              activeCampaigns: 0,
              activeAds: 0
            }
            state.campaigns = data.campaigns || []
            state.recentAds = data.recentAds || []
          }
        }
      },
      
      actions: {
        async fetchDashboardData({ commit }) {
          commit('SET_LOADING', true)
          commit('SET_ERROR', null)
          
          try {
            const response = await api.dashboard.getData()
            commit('SET_DASHBOARD_DATA', response.data)
          } catch (error) {
            console.error('Dashboard fetch error:', error)
            commit('SET_ERROR', error.message || 'Failed to load dashboard data')
            // Set default data to prevent undefined errors
            commit('SET_DASHBOARD_DATA', {
              stats: {
                totalCampaigns: 0,
                totalAds: 0,
                activeCampaigns: 0,
                activeAds: 0
              },
              campaigns: [],
              recentAds: []
            })
            throw error
          } finally {
            commit('SET_LOADING', false)
          }
        }
      }
    }
  }
})

