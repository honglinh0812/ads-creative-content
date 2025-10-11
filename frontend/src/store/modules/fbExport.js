/**
 * Facebook Export Store Module
 * Manages state for exporting ads to Facebook Ads Manager
 *
 * Security: User authorization handled by API
 * Performance: Caches preview data for 5 minutes
 * Maintainability: Clear separation of concerns
 */

import api from '../../services/api'

const state = {
  selectedAdIds: [],
  previewData: null,
  format: 'csv', // 'csv' or 'excel'
  isLoading: false,
  isExporting: false,
  showInstructionsModal: false,
  error: null,
  lastPreviewTime: null
}

const getters = {
  selectedAdIds: state => state.selectedAdIds,
  previewData: state => state.previewData,
  format: state => state.format,
  isLoading: state => state.isLoading,
  isExporting: state => state.isExporting,
  showInstructionsModal: state => state.showInstructionsModal,
  error: state => state.error,
  hasSelectedAds: state => state.selectedAdIds.length > 0,
  selectedCount: state => state.selectedAdIds.length,
  isPreviewStale: state => {
    if (!state.lastPreviewTime) return true
    const fiveMinutesAgo = Date.now() - (5 * 60 * 1000)
    return state.lastPreviewTime < fiveMinutesAgo
  }
}

const mutations = {
  SET_SELECTED_ADS(state, adIds) {
    state.selectedAdIds = adIds
  },

  ADD_SELECTED_AD(state, adId) {
    if (!state.selectedAdIds.includes(adId)) {
      state.selectedAdIds.push(adId)
    }
  },

  REMOVE_SELECTED_AD(state, adId) {
    state.selectedAdIds = state.selectedAdIds.filter(id => id !== adId)
  },

  CLEAR_SELECTED_ADS(state) {
    state.selectedAdIds = []
  },

  SET_PREVIEW_DATA(state, data) {
    state.previewData = data
    state.lastPreviewTime = Date.now()
  },

  CLEAR_PREVIEW_DATA(state) {
    state.previewData = null
    state.lastPreviewTime = null
  },

  SET_FORMAT(state, format) {
    state.format = format
  },

  SET_LOADING(state, loading) {
    state.isLoading = loading
  },

  SET_EXPORTING(state, exporting) {
    state.isExporting = exporting
  },

  SET_ERROR(state, error) {
    state.error = error
  },

  CLEAR_ERROR(state) {
    state.error = null
  },

  SHOW_INSTRUCTIONS_MODAL(state, show) {
    state.showInstructionsModal = show
  }
}

const actions = {
  /**
   * Set selected ad IDs for export
   */
  setSelectedAds({ commit }, adIds) {
    commit('SET_SELECTED_ADS', adIds)
    commit('CLEAR_PREVIEW_DATA') // Clear stale preview
  },

  /**
   * Add a single ad to selection
   */
  addSelectedAd({ commit }, adId) {
    commit('ADD_SELECTED_AD', adId)
    commit('CLEAR_PREVIEW_DATA')
  },

  /**
   * Remove a single ad from selection
   */
  removeSelectedAd({ commit }, adId) {
    commit('REMOVE_SELECTED_AD', adId)
    commit('CLEAR_PREVIEW_DATA')
  },

  /**
   * Clear all selected ads
   */
  clearSelection({ commit }) {
    commit('CLEAR_SELECTED_ADS')
    commit('CLEAR_PREVIEW_DATA')
  },

  /**
   * Set export format (csv or excel)
   */
  setFormat({ commit }, format) {
    if (format !== 'csv' && format !== 'excel') {
      console.warn('Invalid format:', format, '- using CSV')
      format = 'csv'
    }
    commit('SET_FORMAT', format)
  },

  /**
   * Preview export data for selected ads
   * Validates ads and returns preview data
   */
  async previewExport({ state, commit, getters }) {
    // Validate selection
    if (state.selectedAdIds.length === 0) {
      commit('SET_ERROR', 'Please select at least one ad to export')
      return null
    }

    // Use cached preview if not stale
    if (state.previewData && !getters.isPreviewStale) {
      console.log('Using cached preview data')
      return state.previewData
    }

    commit('SET_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      let response

      // Single ad preview
      if (state.selectedAdIds.length === 1) {
        response = await api.get(`/api/facebook-export/preview/ad/${state.selectedAdIds[0]}`)
      }
      // Multiple ads preview
      else {
        response = await api.post('/api/facebook-export/preview/ads/bulk', state.selectedAdIds)
      }

      commit('SET_PREVIEW_DATA', response.data)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to preview ads'
      commit('SET_ERROR', errorMessage)
      console.error('Preview export error:', error)
      throw error
    } finally {
      commit('SET_LOADING', false)
    }
  },

  /**
   * Download export file (CSV or Excel)
   * Returns blob for download
   */
  async downloadExport({ state, commit }) {
    if (state.selectedAdIds.length === 0) {
      commit('SET_ERROR', 'Please select at least one ad to export')
      throw new Error('No ads selected')
    }

    commit('SET_EXPORTING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.post('/api/facebook-export/ads/bulk/export',
        {
          adIds: state.selectedAdIds,
          format: state.format
        },
        {
          responseType: 'blob'
        }
      )

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to export ads'
      commit('SET_ERROR', errorMessage)
      console.error('Download export error:', error)
      throw error
    } finally {
      commit('SET_EXPORTING', false)
    }
  },

  /**
   * Complete export flow: download file and redirect to Facebook
   */
  async exportToFacebook({ state, dispatch, commit }) {
    try {
      commit('SET_EXPORTING', true)

      // 1. Download file
      const blob = await dispatch('downloadExport')

      // 2. Trigger download
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url

      const timestamp = Date.now()
      const extension = state.format === 'excel' ? 'xlsx' : 'csv'
      link.download = `facebook_ads_${timestamp}.${extension}`

      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      // 3. Redirect to Facebook Ads Manager
      const facebookAdsManagerUrl = 'https://business.facebook.com/adsmanager/manage/ads'
      window.open(facebookAdsManagerUrl, '_blank', 'noopener,noreferrer')

      // 4. Show instructions modal
      commit('SHOW_INSTRUCTIONS_MODAL', true)

      // 5. Clear selection after successful export
      setTimeout(() => {
        dispatch('clearSelection')
      }, 2000)

      return true
    } catch (error) {
      console.error('Export to Facebook error:', error)
      throw error
    } finally {
      commit('SET_EXPORTING', false)
    }
  },

  /**
   * Download only (no redirect)
   */
  async downloadOnly({ state, dispatch }) {
    try {
      // Download file
      const blob = await dispatch('downloadExport')

      // Trigger download
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url

      const timestamp = Date.now()
      const extension = state.format === 'excel' ? 'xlsx' : 'csv'
      link.download = `facebook_ads_${timestamp}.${extension}`

      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      return true
    } catch (error) {
      console.error('Download only error:', error)
      throw error
    }
  },

  /**
   * Show/hide instructions modal
   */
  showInstructions({ commit }, show) {
    commit('SHOW_INSTRUCTIONS_MODAL', show)
  },

  /**
   * Clear error state
   */
  clearError({ commit }) {
    commit('CLEAR_ERROR')
  },

  /**
   * Reset module state
   */
  reset({ commit }) {
    commit('CLEAR_SELECTED_ADS')
    commit('CLEAR_PREVIEW_DATA')
    commit('SET_FORMAT', 'csv')
    commit('SET_LOADING', false)
    commit('SET_EXPORTING', false)
    commit('SHOW_INSTRUCTIONS_MODAL', false)
    commit('CLEAR_ERROR')
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
