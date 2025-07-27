import api from './api'

/**
 * Analytics API service for comprehensive dashboard analytics
 */
export const analyticsAPI = {
  /**
   * Get comprehensive analytics dashboard
   * @param {string} timeRange - Time range (7d, 30d, 90d, 1y)
   * @returns {Promise} Analytics response
   */
  async getDashboard(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/dashboard', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching analytics dashboard:', error)
      throw error
    }
  },

  /**
   * Get KPI metrics only
   * @param {string} timeRange - Time range
   * @returns {Promise} KPI metrics
   */
  async getKPIMetrics(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/kpis', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching KPI metrics:', error)
      throw error
    }
  },

  /**
   * Get performance trends
   * @param {string} timeRange - Time range
   * @param {string} metric - Specific metric to track
   * @returns {Promise} Performance trends data
   */
  async getPerformanceTrends(timeRange = '30d', metric = null) {
    try {
      const params = { timeRange }
      if (metric) params.metric = metric

      const response = await api.get('/analytics/trends', { params })
      return response.data
    } catch (error) {
      console.error('Error fetching performance trends:', error)
      throw error
    }
  },

  /**
   * Get campaign analytics with filtering
   * @param {Object} filters - Filter options
   * @returns {Promise} Campaign analytics data
   */
  async getCampaignAnalytics(filters = {}) {
    try {
      const params = {
        timeRange: filters.timeRange || '30d',
        ...(filters.status && { status: filters.status }),
        ...(filters.objective && { objective: filters.objective })
      }

      const response = await api.get('/analytics/campaigns', { params })
      return response.data
    } catch (error) {
      console.error('Error fetching campaign analytics:', error)
      throw error
    }
  },

  /**
   * Get ad analytics with filtering
   * @param {Object} filters - Filter options
   * @returns {Promise} Ad analytics data
   */
  async getAdAnalytics(filters = {}) {
    try {
      const params = {
        timeRange: filters.timeRange || '30d',
        ...(filters.status && { status: filters.status }),
        ...(filters.adType && { adType: filters.adType }),
        ...(filters.aiProvider && { aiProvider: filters.aiProvider })
      }

      const response = await api.get('/analytics/ads', { params })
      return response.data
    } catch (error) {
      console.error('Error fetching ad analytics:', error)
      throw error
    }
  },

  /**
   * Get AI provider analytics
   * @param {string} timeRange - Time range
   * @returns {Promise} AI provider analytics data
   */
  async getAIProviderAnalytics(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/ai-providers', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching AI provider analytics:', error)
      throw error
    }
  },

  /**
   * Get budget analytics
   * @param {string} timeRange - Time range
   * @returns {Promise} Budget analytics data
   */
  async getBudgetAnalytics(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/budget', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching budget analytics:', error)
      throw error
    }
  },

  /**
   * Get content analytics
   * @param {string} timeRange - Time range
   * @returns {Promise} Content analytics data
   */
  async getContentAnalytics(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/content', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching content analytics:', error)
      throw error
    }
  },

  /**
   * Get analytics summary for quick overview
   * @param {string} timeRange - Time range
   * @returns {Promise} Analytics summary
   */
  async getAnalyticsSummary(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/summary', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching analytics summary:', error)
      throw error
    }
  },

  /**
   * Get available filter options
   * @returns {Promise} Filter options
   */
  async getFilterOptions() {
    try {
      const response = await api.get('/analytics/filters')
      return response.data
    } catch (error) {
      console.error('Error fetching filter options:', error)
      throw error
    }
  },

  /**
   * Export analytics data
   * @param {string} type - Export type (campaigns, ads, content)
   * @param {Object} filters - Filter options
   * @param {string} format - Export format (csv, xlsx, pdf)
   * @returns {Promise} Export data
   */
  async exportData(type, filters = {}, format = 'csv') {
    try {
      const params = {
        ...filters,
        format
      }

      const response = await api.get(`/analytics/export/${type}`, {
        params,
        responseType: 'blob' // For file downloads
      })

      return response
    } catch (error) {
      console.error('Error exporting analytics data:', error)
      throw error
    }
  },

  /**
   * Get real-time analytics updates
   * @param {string} timeRange - Time range
   * @returns {Promise} Real-time analytics data
   */
  async getRealTimeAnalytics(timeRange = '24h') {
    try {
      const response = await api.get('/analytics/realtime', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching real-time analytics:', error)
      throw error
    }
  },

  /**
   * Get analytics insights and recommendations
   * @param {string} timeRange - Time range
   * @returns {Promise} Analytics insights
   */
  async getInsights(timeRange = '30d') {
    try {
      const response = await api.get('/analytics/insights', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching analytics insights:', error)
      throw error
    }
  },

  /**
   * Get comparative analytics (period over period)
   * @param {string} currentPeriod - Current period
   * @param {string} comparisonPeriod - Comparison period
   * @returns {Promise} Comparative analytics data
   */
  async getComparativeAnalytics(currentPeriod = '30d', comparisonPeriod = '30d') {
    try {
      const response = await api.get('/analytics/compare', {
        params: { 
          current: currentPeriod,
          comparison: comparisonPeriod
        }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching comparative analytics:', error)
      throw error
    }
  },

  /**
   * Get analytics for specific campaign
   * @param {number} campaignId - Campaign ID
   * @param {string} timeRange - Time range
   * @returns {Promise} Campaign-specific analytics
   */
  async getCampaignSpecificAnalytics(campaignId, timeRange = '30d') {
    try {
      const response = await api.get(`/analytics/campaign/${campaignId}`, {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching campaign-specific analytics:', error)
      throw error
    }
  },

  /**
   * Get analytics for specific ad
   * @param {number} adId - Ad ID
   * @param {string} timeRange - Time range
   * @returns {Promise} Ad-specific analytics
   */
  async getAdSpecificAnalytics(adId, timeRange = '30d') {
    try {
      const response = await api.get(`/analytics/ad/${adId}`, {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching ad-specific analytics:', error)
      throw error
    }
  },

  /**
   * Save analytics dashboard configuration
   * @param {Object} config - Dashboard configuration
   * @returns {Promise} Save response
   */
  async saveDashboardConfig(config) {
    try {
      const response = await api.post('/analytics/dashboard/config', config)
      return response.data
    } catch (error) {
      console.error('Error saving dashboard config:', error)
      throw error
    }
  },

  /**
   * Get saved analytics dashboard configuration
   * @returns {Promise} Dashboard configuration
   */
  async getDashboardConfig() {
    try {
      const response = await api.get('/analytics/dashboard/config')
      return response.data
    } catch (error) {
      console.error('Error fetching dashboard config:', error)
      throw error
    }
  }
}

export default analyticsAPI
