/**
 * Facebook Export Store Module
 * Manages state for exporting ads to Facebook Ads Manager
 *
 * Security: User authorization handled by API
 * Performance: Caches preview data for 5 minutes
 * Maintainability: Clear separation of concerns
 */

import api from '../../services/api'

const ADS_MANAGER_URL = 'https://business.facebook.com/adsmanager/manage/ads'
const MIME_TYPES = {
  csv: 'text/csv;charset=utf-8;',
  excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
}

function base64ToBlob(base64Data, contentType) {
  if (!base64Data) return null
  const sliceSize = 512
  const byteCharacters = atob(base64Data)
  const byteArrays = []

  for (let offset = 0; offset < byteCharacters.length; offset += sliceSize) {
    const slice = byteCharacters.slice(offset, offset + sliceSize)
    const byteNumbers = new Array(slice.length)
    for (let i = 0; i < slice.length; i++) {
      byteNumbers[i] = slice.charCodeAt(i)
    }
    const byteArray = new Uint8Array(byteNumbers)
    byteArrays.push(byteArray)
  }

  return new Blob(byteArrays, { type: contentType })
}

function triggerFileDownload(responseData) {
  if (!responseData || !responseData.fileContent) {
    return null
  }

  const format = (responseData.format || 'csv').toLowerCase()
  const mimeType = MIME_TYPES[format] || MIME_TYPES.csv
  const blob = base64ToBlob(responseData.fileContent, mimeType)
  if (!blob) return null

  const extension = format === 'excel' || format === 'xlsx' ? 'xlsx' : 'csv'
  const filename = responseData.filename || `facebook_ads_${Date.now()}.${extension}`
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
  return { blob, filename }
}

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
        response = await api.facebookExport.previewAd(state.selectedAdIds[0])
      }
      // Multiple ads preview
      else {
        response = await api.facebookExport.previewMultipleAds(state.selectedAdIds)
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
      const response = await api.facebookExport.exportMultipleAds(
        state.selectedAdIds,
        state.format,
        { autoUpload: false }
      )

      triggerFileDownload(response.data)
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
   * Automatically upload ads to Facebook and return redirect metadata.
   */
  async exportToFacebook({ state, commit }, { adAccountId } = {}) {
    if (state.selectedAdIds.length === 0) {
      commit('SET_ERROR', 'Please select at least one ad to export')
      throw new Error('No ads selected')
    }

    commit('SET_EXPORTING', true)
    commit('CLEAR_ERROR')

    try {
      const response = await api.facebookExport.exportMultipleAds(
        state.selectedAdIds,
        state.format,
        { autoUpload: true, adAccountId }
      )
      const data = response.data || {}
      const autoUpload = data.autoUpload || {}
      const redirectUrl = autoUpload.adsManagerUrl || ADS_MANAGER_URL

      if (!autoUpload.status || autoUpload.status !== 'UPLOADED') {
        triggerFileDownload(data)
        commit('CLEAR_SELECTED_ADS')
        commit('CLEAR_PREVIEW_DATA')
      } else {
        commit('CLEAR_PREVIEW_DATA')
        commit('CLEAR_SELECTED_ADS')
      }

      return {
        redirectUrl,
        autoUpload
      }
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.message || 'Failed to upload ads'
      commit('SET_ERROR', errorMessage)
      console.error('Export to Facebook error:', error)
      throw error
    } finally {
      commit('SET_EXPORTING', false)
    }
  },

  /**
   * Download only (no redirect)
   */
  async downloadOnly({ dispatch }) {
    try {
      await dispatch('downloadExport')
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
  },

  async uploadAfterPreview({ dispatch }, payload) {
    await dispatch('previewExport')
    return dispatch('exportToFacebook', payload)
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
