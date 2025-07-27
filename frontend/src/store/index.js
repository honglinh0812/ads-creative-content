import { createStore } from 'vuex'
import auth from './modules/auth'
import campaign from './modules/campaign'
import ad from './modules/ad'
import toast from './modules/toast'
import dashboard from './modules/dashboard'
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
    toast,
    dashboard
  }
})

