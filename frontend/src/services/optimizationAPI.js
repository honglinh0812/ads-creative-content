import api from './api'

/**
 * Optimization API service for automated recommendations
 */
export const optimizationAPI = {
  /**
   * Get comprehensive optimization recommendations
   * @param {string} timeRange - Time range for analysis (7d, 30d, 90d)
   * @returns {Promise} Optimization recommendations response
   */
  async getRecommendations(timeRange = '30d') {
    try {
      const response = await api.get('/api/optimization/recommendations', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching optimization recommendations:', error)
      throw error
    }
  },

  /**
   * Get recommendations by specific type
   * @param {string} type - Recommendation type
   * @param {string} timeRange - Time range for analysis
   * @returns {Promise} Filtered recommendations
   */
  async getRecommendationsByType(type, timeRange = '30d') {
    try {
      const response = await api.get(`/api/optimization/recommendations/type/${type}`, {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching recommendations by type:', error)
      throw error
    }
  },

  /**
   * Get high priority recommendations only
   * @param {string} timeRange - Time range for analysis
   * @returns {Promise} High priority recommendations
   */
  async getHighPriorityRecommendations(timeRange = '30d') {
    try {
      const response = await api.get('/api/optimization/recommendations/priority/high', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching high priority recommendations:', error)
      throw error
    }
  },

  /**
   * Accept a recommendation for implementation
   * @param {string} recommendationId - ID of the recommendation
   * @param {Object} parameters - Optional implementation parameters
   * @returns {Promise} Acceptance response
   */
  async acceptRecommendation(recommendationId, parameters = {}) {
    try {
      const response = await api.post(`/api/optimization/recommendations/${recommendationId}/accept`, parameters)
      return response.data
    } catch (error) {
      console.error('Error accepting recommendation:', error)
      throw error
    }
  },

  /**
   * Dismiss a recommendation
   * @param {string} recommendationId - ID of the recommendation
   * @param {string} reason - Reason for dismissal
   * @returns {Promise} Dismissal response
   */
  async dismissRecommendation(recommendationId, reason = '') {
    try {
      const response = await api.post(`/api/optimization/recommendations/${recommendationId}/dismiss`, {
        reason: reason
      })
      return response.data
    } catch (error) {
      console.error('Error dismissing recommendation:', error)
      throw error
    }
  },

  /**
   * Schedule a recommendation for later implementation
   * @param {string} recommendationId - ID of the recommendation
   * @param {Object} scheduleData - Schedule information
   * @returns {Promise} Schedule response
   */
  async scheduleRecommendation(recommendationId, scheduleData) {
    try {
      const response = await api.post(`/api/optimization/recommendations/${recommendationId}/schedule`, scheduleData)
      return response.data
    } catch (error) {
      console.error('Error scheduling recommendation:', error)
      throw error
    }
  },

  /**
   * Get recommendation implementation status
   * @param {string} recommendationId - ID of the recommendation
   * @returns {Promise} Status information
   */
  async getRecommendationStatus(recommendationId) {
    try {
      const response = await api.get(`/api/optimization/recommendations/${recommendationId}/status`)
      return response.data
    } catch (error) {
      console.error('Error fetching recommendation status:', error)
      throw error
    }
  },

  /**
   * Get optimization summary statistics
   * @param {string} timeRange - Time range for analysis
   * @returns {Promise} Summary statistics
   */
  async getOptimizationSummary(timeRange = '30d') {
    try {
      const response = await api.get('/api/optimization/summary', {
        params: { timeRange }
      })
      return response.data
    } catch (error) {
      console.error('Error fetching optimization summary:', error)
      throw error
    }
  },

  /**
   * Get available recommendation types and descriptions
   * @returns {Promise} Recommendation types information
   */
  async getRecommendationTypes() {
    try {
      const response = await api.get('/api/optimization/types')
      return response.data
    } catch (error) {
      console.error('Error fetching recommendation types:', error)
      throw error
    }
  },

  /**
   * Batch accept multiple recommendations
   * @param {Array} recommendationIds - Array of recommendation IDs
   * @param {Object} parameters - Optional implementation parameters
   * @returns {Promise} Batch acceptance response
   */
  async batchAcceptRecommendations(recommendationIds, parameters = {}) {
    try {
      const promises = recommendationIds.map(id => 
        this.acceptRecommendation(id, parameters)
      )
      const responses = await Promise.allSettled(promises)
      
      const successful = responses.filter(r => r.status === 'fulfilled').length
      const failed = responses.filter(r => r.status === 'rejected').length
      
      return {
        success: true,
        message: `${successful} recommendations accepted successfully${failed > 0 ? `, ${failed} failed` : ''}`,
        data: {
          successful,
          failed,
          total: recommendationIds.length,
          results: responses
        }
      }
    } catch (error) {
      console.error('Error batch accepting recommendations:', error)
      throw error
    }
  },

  /**
   * Batch dismiss multiple recommendations
   * @param {Array} recommendationIds - Array of recommendation IDs
   * @param {string} reason - Reason for dismissal
   * @returns {Promise} Batch dismissal response
   */
  async batchDismissRecommendations(recommendationIds, reason = '') {
    try {
      const promises = recommendationIds.map(id => 
        this.dismissRecommendation(id, reason)
      )
      const responses = await Promise.allSettled(promises)
      
      const successful = responses.filter(r => r.status === 'fulfilled').length
      const failed = responses.filter(r => r.status === 'rejected').length
      
      return {
        success: true,
        message: `${successful} recommendations dismissed successfully${failed > 0 ? `, ${failed} failed` : ''}`,
        data: {
          successful,
          failed,
          total: recommendationIds.length,
          results: responses
        }
      }
    } catch (error) {
      console.error('Error batch dismissing recommendations:', error)
      throw error
    }
  },

  /**
   * Get recommendation effectiveness tracking
   * @param {string} timeRange - Time range for tracking
   * @returns {Promise} Effectiveness metrics
   */
  async getRecommendationEffectiveness(timeRange = '30d') {
    // TODO: Backend doesn't have effectiveness endpoint yet
    return {
      success: true,
      message: 'Recommendation effectiveness retrieved successfully',
      data: {
        totalImplemented: 15,
        averageImpact: 12.5,
        successRate: 78.3,
        topPerformingType: 'BUDGET_REALLOCATION',
        implementationsByType: {
          'BUDGET_REALLOCATION': { implemented: 8, averageImpact: 15.2 },
          'AI_PROVIDER_SWITCH': { implemented: 4, averageImpact: 8.7 },
          'CAMPAIGN_OBJECTIVE_OPTIMIZATION': { implemented: 3, averageImpact: 11.4 }
        },
        timeRange
      }
    }
  },

  /**
   * Get personalized recommendation settings
   * @returns {Promise} User's recommendation preferences
   */
  async getRecommendationSettings() {
    // TODO: Backend doesn't have settings endpoint yet
    return {
        success: true,
        message: 'Recommendation settings retrieved successfully',
        data: {
          autoAcceptLowRisk: false,
          notificationFrequency: 'daily',
          minimumConfidence: 0.7,
          excludedTypes: [],
          priorityThresholds: {
            high: 0.8,
            medium: 0.6,
            low: 0.4
          }
        }
    }
  },

  async updateRecommendationSettings(settings) {
    // TODO: Backend doesn't have settings endpoint yet
    return {
      success: true,
      message: 'Recommendation settings updated successfully',
      data: settings
    }
  },

  /**
   * Export recommendations data
   * @param {Object} filters - Export filters
   * @param {string} format - Export format (csv, xlsx, pdf)
   * @returns {Promise} Export data
   */
  async exportRecommendations(filters = {}, format = 'csv') {
    try {
      const params = {
        ...filters,
        format
      }

      const response = await api.get('/api/optimization/export', {
        params,
        responseType: 'blob' // For file downloads
      })

      return response
    } catch (error) {
      console.error('Error exporting recommendations:', error)
      throw error
    }
  },

  /**
   * Get recommendation history for a user
   * @param {string} timeRange - Time range for history
   * @param {number} limit - Maximum number of records
   * @returns {Promise} Recommendation history
   */
  async getRecommendationHistory(timeRange = '90d', limit = 50) {
    try {
      // This would be a real endpoint in production
      return {
        success: true,
        message: 'Recommendation history retrieved successfully',
        data: {
          recommendations: [],
          totalCount: 0,
          timeRange,
          limit
        }
      }
    } catch (error) {
      console.error('Error fetching recommendation history:', error)
      throw error
    }
  },

  /**
   * Provide feedback on a recommendation
   * @param {string} recommendationId - ID of the recommendation
   * @param {Object} feedback - Feedback data
   * @returns {Promise} Feedback response
   */
  async provideFeedback(recommendationId, feedback) {
    try {
      // This would be a real endpoint in production
      return {
        success: true,
        message: 'Feedback submitted successfully',
        data: {
          recommendationId,
          feedback,
          submittedAt: new Date().toISOString()
        }
      }
    } catch (error) {
      console.error('Error providing feedback:', error)
      throw error
    }
  }
}

export default optimizationAPI
