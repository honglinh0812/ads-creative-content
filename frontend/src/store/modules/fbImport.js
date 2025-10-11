/**
 * Facebook Import Store Module
 * Manages state for importing Facebook Ads Manager performance reports
 *
 * Security: User authorization handled by API
 * Performance: Caches preview data, efficient matching algorithm
 * Maintainability: Clear separation of concerns, comprehensive error handling
 *
 * @author AI Engineering Panel
 * @since 2025-10-10
 */

import api from '../../services/api'

const state = {
  // File upload state
  selectedFile: null,
  fileType: null, // 'csv' or 'excel'

  // Import preview state
  previewData: null,
  isPreviewLoading: false,

  // Matched and unmatched reports
  matchedReports: [],
  unmatchedReports: [],

  // Import confirmation state
  isImporting: false,
  importResult: null,

  // Analytics state
  selectedAdId: null,
  selectedCampaignId: null,
  adAnalytics: null,
  campaignAnalytics: null,
  isAnalyticsLoading: false,

  // Report history
  adReports: [],
  campaignReports: [],
  isReportsLoading: false,

  // Error handling
  error: null,

  // UI state
  showImportModal: false,
  showAnalyticsModal: false
}

const getters = {
  selectedFile: state => state.selectedFile,
  fileType: state => state.fileType,
  previewData: state => state.previewData,
  isPreviewLoading: state => state.isPreviewLoading,
  matchedReports: state => state.matchedReports,
  unmatchedReports: state => state.unmatchedReports,
  hasMatchedReports: state => state.matchedReports.length > 0,
  hasUnmatchedReports: state => state.unmatchedReports.length > 0,
  matchedCount: state => state.matchedReports.length,
  unmatchedCount: state => state.unmatchedReports.length,
  totalRows: state => state.matchedReports.length + state.unmatchedReports.length,
  matchRate: state => {
    const total = state.matchedReports.length + state.unmatchedReports.length
    return total > 0 ? ((state.matchedReports.length / total) * 100).toFixed(1) : 0
  },
  isImporting: state => state.isImporting,
  importResult: state => state.importResult,
  adAnalytics: state => state.adAnalytics,
  campaignAnalytics: state => state.campaignAnalytics,
  isAnalyticsLoading: state => state.isAnalyticsLoading,
  adReports: state => state.adReports,
  campaignReports: state => state.campaignReports,
  isReportsLoading: state => state.isReportsLoading,
  error: state => state.error,
  showImportModal: state => state.showImportModal,
  showAnalyticsModal: state => state.showAnalyticsModal
}

const mutations = {
  SET_SELECTED_FILE(state, { file, type }) {
    state.selectedFile = file
    state.fileType = type
  },

  CLEAR_SELECTED_FILE(state) {
    state.selectedFile = null
    state.fileType = null
  },

  SET_PREVIEW_DATA(state, data) {
    state.previewData = data
    state.matchedReports = data.matchedReports || []
    state.unmatchedReports = data.unmatchedReports || []
  },

  CLEAR_PREVIEW_DATA(state) {
    state.previewData = null
    state.matchedReports = []
    state.unmatchedReports = []
  },

  SET_PREVIEW_LOADING(state, loading) {
    state.isPreviewLoading = loading
  },

  SET_IMPORTING(state, importing) {
    state.isImporting = importing
  },

  SET_IMPORT_RESULT(state, result) {
    state.importResult = result
  },

  CLEAR_IMPORT_RESULT(state) {
    state.importResult = null
  },

  SET_SELECTED_AD(state, adId) {
    state.selectedAdId = adId
  },

  SET_SELECTED_CAMPAIGN(state, campaignId) {
    state.selectedCampaignId = campaignId
  },

  SET_AD_ANALYTICS(state, analytics) {
    state.adAnalytics = analytics
  },

  SET_CAMPAIGN_ANALYTICS(state, analytics) {
    state.campaignAnalytics = analytics
  },

  SET_ANALYTICS_LOADING(state, loading) {
    state.isAnalyticsLoading = loading
  },

  SET_AD_REPORTS(state, reports) {
    state.adReports = reports
  },

  SET_CAMPAIGN_REPORTS(state, reports) {
    state.campaignReports = reports
  },

  SET_REPORTS_LOADING(state, loading) {
    state.isReportsLoading = loading
  },

  SET_ERROR(state, error) {
    state.error = error
  },

  CLEAR_ERROR(state) {
    state.error = null
  },

  SHOW_IMPORT_MODAL(state, show) {
    state.showImportModal = show
  },

  SHOW_ANALYTICS_MODAL(state, show) {
    state.showAnalyticsModal = show
  }
}

const actions = {
  /**
   * Set selected file for import
   */
  selectFile({ commit }, { file, type }) {
    if (!file) {
      commit('SET_ERROR', 'No file selected')
      return
    }

    // Validate file type
    const validTypes = ['csv', 'excel']
    if (!validTypes.includes(type)) {
      commit('SET_ERROR', 'Invalid file type. Please upload CSV or Excel files.')
      return
    }

    commit('SET_SELECTED_FILE', { file, type })
    commit('CLEAR_PREVIEW_DATA')
    commit('CLEAR_ERROR')
  },

  /**
   * Preview import - parse file and match ads
   */
  async previewImport({ state, commit }) {
    if (!state.selectedFile) {
      commit('SET_ERROR', 'Please select a file to import')
      return null
    }

    commit('SET_PREVIEW_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      const formData = new FormData()
      formData.append('file', state.selectedFile)

      const response = await api.post('/api/facebook-report/import/preview', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      commit('SET_PREVIEW_DATA', response.data)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to preview import'
      commit('SET_ERROR', errorMessage)
      console.error('Preview import error:', error)
      throw error
    } finally {
      commit('SET_PREVIEW_LOADING', false)
    }
  },

  /**
   * Confirm and save import
   */
  async confirmImport({ commit }, { matchedReports, source = 'FACEBOOK' }) {
    if (!matchedReports || matchedReports.length === 0) {
      commit('SET_ERROR', 'No matched reports to import')
      return null
    }

    commit('SET_IMPORTING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.post('/api/facebook-report/import/confirm', {
        matchedReports,
        source
      })

      commit('SET_IMPORT_RESULT', response.data)

      // Clear preview data after successful import
      commit('CLEAR_PREVIEW_DATA')
      commit('CLEAR_SELECTED_FILE')

      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.message || 'Failed to import reports'
      commit('SET_ERROR', errorMessage)
      console.error('Confirm import error:', error)
      throw error
    } finally {
      commit('SET_IMPORTING', false)
    }
  },

  /**
   * Fetch reports for a specific ad
   */
  async fetchAdReports({ commit }, adId) {
    commit('SET_REPORTS_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.get(`/api/facebook-report/ad/${adId}`)
      commit('SET_AD_REPORTS', response.data)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch ad reports'
      commit('SET_ERROR', errorMessage)
      console.error('Fetch ad reports error:', error)
      throw error
    } finally {
      commit('SET_REPORTS_LOADING', false)
    }
  },

  /**
   * Fetch reports for a campaign
   */
  async fetchCampaignReports({ commit }, campaignId) {
    commit('SET_REPORTS_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.get(`/api/facebook-report/campaign/${campaignId}`)
      commit('SET_CAMPAIGN_REPORTS', response.data)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch campaign reports'
      commit('SET_ERROR', errorMessage)
      console.error('Fetch campaign reports error:', error)
      throw error
    } finally {
      commit('SET_REPORTS_LOADING', false)
    }
  },

  /**
   * Fetch aggregated analytics for an ad
   */
  async fetchAdAnalytics({ commit }, adId) {
    commit('SET_ANALYTICS_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.get(`/api/facebook-report/ad/${adId}/analytics`)
      commit('SET_AD_ANALYTICS', response.data)
      commit('SET_SELECTED_AD', adId)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch ad analytics'
      commit('SET_ERROR', errorMessage)
      console.error('Fetch ad analytics error:', error)
      throw error
    } finally {
      commit('SET_ANALYTICS_LOADING', false)
    }
  },

  /**
   * Fetch aggregated analytics for a campaign
   */
  async fetchCampaignAnalytics({ commit }, campaignId) {
    commit('SET_ANALYTICS_LOADING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.get(`/api/facebook-report/campaign/${campaignId}/analytics`)
      commit('SET_CAMPAIGN_ANALYTICS', response.data)
      commit('SET_SELECTED_CAMPAIGN', campaignId)
      return response.data
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch campaign analytics'
      commit('SET_ERROR', errorMessage)
      console.error('Fetch campaign analytics error:', error)
      throw error
    } finally {
      commit('SET_ANALYTICS_LOADING', false)
    }
  },

  /**
   * Show/hide import modal
   */
  showImportModal({ commit }, show) {
    commit('SHOW_IMPORT_MODAL', show)
    if (!show) {
      // Clear data when closing modal
      commit('CLEAR_PREVIEW_DATA')
      commit('CLEAR_SELECTED_FILE')
      commit('CLEAR_ERROR')
    }
  },

  /**
   * Show/hide analytics modal
   */
  showAnalyticsModal({ commit }, show) {
    commit('SHOW_ANALYTICS_MODAL', show)
  },

  /**
   * Clear error state
   */
  clearError({ commit }) {
    commit('CLEAR_ERROR')
  },

  /**
   * Clear import result
   */
  clearImportResult({ commit }) {
    commit('CLEAR_IMPORT_RESULT')
  },

  /**
   * Reset module state
   */
  reset({ commit }) {
    commit('CLEAR_SELECTED_FILE')
    commit('CLEAR_PREVIEW_DATA')
    commit('CLEAR_IMPORT_RESULT')
    commit('SET_SELECTED_AD', null)
    commit('SET_SELECTED_CAMPAIGN', null)
    commit('SET_AD_ANALYTICS', null)
    commit('SET_CAMPAIGN_ANALYTICS', null)
    commit('SET_AD_REPORTS', [])
    commit('SET_CAMPAIGN_REPORTS', [])
    commit('SET_PREVIEW_LOADING', false)
    commit('SET_IMPORTING', false)
    commit('SET_ANALYTICS_LOADING', false)
    commit('SET_REPORTS_LOADING', false)
    commit('SHOW_IMPORT_MODAL', false)
    commit('SHOW_ANALYTICS_MODAL', false)
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
