import api from './api';

export default {
  /**
   * Get quality score for a single ad content
   */
  getQualityScore(adContentId) {
    return api.get(`/quality/score/${adContentId}`);
  },

  /**
   * Get quality scores for multiple ad contents
   */
  getQualityScoreBatch(adContentIds) {
    return api.post('/quality/score/batch', adContentIds);
  },

  /**
   * Get quality statistics for all content variations of an ad
   */
  getAdQualityStats(adId) {
    return api.get(`/quality/stats/ad/${adId}`);
  }
};
