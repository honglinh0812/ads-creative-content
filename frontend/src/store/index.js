import { createStore } from 'vuex'
import auth from './modules/auth'
import campaign from './modules/campaign'
import ad from './modules/ad'
import toast from './modules/toast'
import dashboard from './modules/dashboard'
import adCreation from './modules/adCreation'
import fbExport from './modules/fbExport'
import fbImport from './modules/fbImport'
import api from '../services/api'

export default createStore({
  state: {
    user: null,
    loading: false,
    error: null,
    theme: localStorage.getItem('theme') || 'light',
    sidebarOpen: true
  },

  getters: {
    user: state => state.user,
    loading: state => state.loading,
    error: state => state.error,
    theme: state => state.theme,
    isDarkMode: state => state.theme === 'dark',
    sidebarOpen: state => state.sidebarOpen
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
    },

    SET_THEME(state, theme) {
      state.theme = theme
      localStorage.setItem('theme', theme)

      // Apply theme to document
      if (theme === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },

    TOGGLE_THEME(state) {
      const newTheme = state.theme === 'light' ? 'dark' : 'light'
      state.theme = newTheme
      localStorage.setItem('theme', newTheme)

      // Apply theme to document
      if (newTheme === 'dark') {
        document.documentElement.classList.add('dark')
      } else {
        document.documentElement.classList.remove('dark')
      }
    },

    TOGGLE_SIDEBAR(state) {
      state.sidebarOpen = !state.sidebarOpen
    },

    SET_SIDEBAR_OPEN(state, isOpen) {
      state.sidebarOpen = isOpen
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
    dashboard,
    adCreation,
    fbExport,
    fbImport
  }
})

