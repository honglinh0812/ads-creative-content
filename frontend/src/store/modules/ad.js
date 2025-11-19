import api from '../../services/api'

export default {
  namespaced: true,
  state: {
    ads: [],
    currentAd: null,
    adContents: [],
    loading: false,
    error: null,
    generatingContent: false,
    totalItems: 0,
    totalPages: 0,
    currentPage: 0
  },
  getters: {
    ads: state => state.ads,
    currentAd: state => state.currentAd,
    adContents: state => state.adContents,
    selectedContent: state => state.adContents.find(content => content.isSelected),
    loading: state => state.loading,
    generatingContent: state => state.generatingContent,
    error: state => state.error,
    totalItems: state => state.totalItems,
    totalPages: state => state.totalPages,
    currentPage: state => state.currentPage
  },
  mutations: {
    SET_ADS(state, data = {}) {
      const content = Array.isArray(data.content) ? data.content : []
      state.ads = content
      state.totalItems = typeof data.totalElements === 'number' ? data.totalElements : content.length
      state.totalPages = typeof data.totalPages === 'number' ? data.totalPages : 0
      state.currentPage = typeof data.number === 'number' ? data.number : 0
    },
    SET_CURRENT_AD(state, ad) {
      state.currentAd = ad
    },
    SET_AD_CONTENTS(state, contents) {
      state.adContents = contents
    },
    ADD_AD(state, ad) {
      state.ads.unshift(ad) // Add to beginning for newest first
    },
    ADD_AD_CONTENT(state, content) {
      state.adContents.push(content)
    },
    UPDATE_AD(state, updatedAd) {
      const index = state.ads.findIndex(a => a.id === updatedAd.id)
      if (index !== -1) {
        state.ads.splice(index, 1, updatedAd)
      }
      if (state.currentAd && state.currentAd.id === updatedAd.id) {
        state.currentAd = updatedAd
      }
    },
    SELECT_CONTENT(state, contentId) {
      state.adContents.forEach(content => {
        content.isSelected = content.id === contentId
      })
    },
    REMOVE_AD(state, adId) {
      state.ads = state.ads.filter(a => a.id !== adId)
      if (state.currentAd && state.currentAd.id === adId) {
        state.currentAd = null
      }
    },
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_GENERATING_CONTENT(state, generating) {
      state.generatingContent = generating
    },
    SET_ERROR(state, error) {
      state.error = error
    }
  },
  actions: {
    async fetchAds({ commit }, { page = 0, size = 5 } = {}) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        const response = await api.ads.getAll(page, size);
        commit("SET_ADS", response.data);
      } catch (error) {
        console.error("Fetch ads error:", error);
        commit("SET_ERROR", error.message || "Failed to fetch ads");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async fetchAd({ commit }, adId) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        const response = await api.ads.get(adId);
        commit("SET_CURRENT_AD", response.data);
        return response.data;
      } catch (error) {
        console.error("Fetch ad error:", error);
        commit("SET_ERROR", error.message || "Failed to fetch ad details");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async createAdWithContent({ commit, dispatch }, formData) {
      commit("SET_GENERATING_CONTENT", true);
      commit("SET_ERROR", null);
      
      try {
        console.log("Creating ad with form data:", formData);
        const response = await api.ads.generate(formData);
        console.log("Ad created successfully:", response.data);
        
        // The response from /ads/generate is AdGenerationResponse, not Ad
        // We need to fetch the ad details after generation if needed, or adapt the flow
        // For now, we'll just set the ad contents
        commit("SET_AD_CONTENTS", response.data.variations);
        
        // Also refresh dashboard data if available
        try {
          await dispatch("dashboard/fetchDashboardData", null, { root: true });
        } catch (dashboardError) {
          console.warn("Failed to refresh dashboard after ad creation:", dashboardError);
        }
        
        return response.data;
      } catch (error) {
        console.error("Create ad error:", error);
        commit("SET_ERROR", error.message || "Failed to create ad");
        throw error;
      } finally {
        commit("SET_GENERATING_CONTENT", false);
      }
    },
    
    async selectAdContent({ commit }, { adId, contentId }) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        const response = await api.ads.selectContent(adId, contentId);
        
        commit("UPDATE_AD", response.data.ad);
        commit("SELECT_CONTENT", contentId);
        
        return response.data;
      } catch (error) {
        console.error("Select content error:", error);
        commit("SET_ERROR", error.message || "Failed to select content");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async fetchAdContents({ commit }, adId) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        const response = await api.ads.getContents(adId);
        commit("SET_AD_CONTENTS", response.data);
        return response.data;
      } catch (error) {
        console.error("Fetch ad contents error:", error);
        commit("SET_ERROR", error.message || "Failed to fetch ad contents");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async deleteAd({ commit, dispatch }, adId) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        await api.ads.delete(adId);
        commit("REMOVE_AD", adId);
        
        // Also refresh dashboard data if available
        try {
          await dispatch("dashboard/fetchDashboardData", null, { root: true });
        } catch (dashboardError) {
          console.warn("Failed to refresh dashboard after ad deletion:", dashboardError);
        }
        
        return true;
      } catch (error) {
        console.error("Delete ad error:", error);
        commit("SET_ERROR", error.message || "Failed to delete ad");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },

    async updateAd({ commit }, { adId, adData }) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      try {
        const response = await api.ads.update(adId, adData);
        commit("UPDATE_AD", response.data);
        return response.data;
      } catch (error) {
        console.error("Update ad error:", error);
        commit("SET_ERROR", error.message || "Failed to update ad");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async searchAds({ commit }, query) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        // Tìm kiếm local trong ads hiện tại
        // Hoặc có thể gọi API search nếu backend hỗ trợ
        const response = await api.ads.getAll(0, 100); // Lấy nhiều hơn để search
        const allAds = response.data.content;
        const filteredAds = allAds.filter(ad => 
          ad.name?.toLowerCase().includes(query.toLowerCase()) ||
          ad.headline?.toLowerCase().includes(query.toLowerCase()) ||
          ad.description?.toLowerCase().includes(query.toLowerCase()) ||
          ad.primaryText?.toLowerCase().includes(query.toLowerCase())
        );
        
        commit("SET_ADS", {
          content: filteredAds,
          totalElements: filteredAds.length,
          totalPages: 1,
          number: 0
        });
      } catch (error) {
        console.error("Search ads error:", error);
        commit("SET_ERROR", error.message || "Failed to search ads");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    },
    
    async createAd({ commit }, adData) {
      commit("SET_LOADING", true);
      commit("SET_ERROR", null);
      
      try {
        const response = await api.ads.create(adData);
        commit("ADD_AD", response.data);
        return response.data;
      } catch (error) {
        console.error("Create ad error:", error);
        commit("SET_ERROR", error.message || "Failed to create ad");
        throw error;
      } finally {
        commit("SET_LOADING", false);
      }
    }
  }
}
