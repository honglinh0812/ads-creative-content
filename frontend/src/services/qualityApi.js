import api from './api';

export default {
  /**
   * Get quality score for a single ad content
   */
  getQualityScore(adContentId) {
    return api.get(`/api/quality/score/${adContentId}`);
  },

  /**
   * Get quality scores for multiple ad contents
   */
  getQualityScoreBatch(adContentIds) {
    return api.post('/api/quality/score/batch', adContentIds);
  },

  /**
   * Get quality statistics for all content variations of an ad
   */
  getAdQualityStats(adId) {
    return api.get(`/api/quality/stats/ad/${adId}`);
  }
};
