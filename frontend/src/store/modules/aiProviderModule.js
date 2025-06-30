import aiProviderService from '@/services/aiProviderService'

export default {
  namespaced: true,
  state: {
    allProviders: [],
    selectedProviders: {
      TEXT: null,
      IMAGE: null,
      VIDEO: null
    },
    generateVideo: false,
    selectedVideoDuration: 10, // Default duration
    loading: false,
    error: null
  },
  mutations: {
    SET_ALL_PROVIDERS(state, providers) {
      state.allProviders = providers
    },
    CLEAR_ALL_PROVIDERS(state) {
      state.allProviders = []
    },
    SET_SELECTED_PROVIDER(state, { capability, providerId }) {
      // Ensure capability is uppercase for consistency
      const upperCapability = capability.toUpperCase();
      // Use safe hasOwnProperty check
      if (Object.prototype.hasOwnProperty.call(state.selectedProviders, upperCapability)) {
          state.selectedProviders[upperCapability] = providerId
      } else {
          console.warn(`Attempted to set provider for unknown capability: ${capability}`);
      }
    },
    CLEAR_SELECTED_PROVIDERS(state) {
      state.selectedProviders = {
        TEXT: null,
        IMAGE: null,
        VIDEO: null
      }
    },
    SET_GENERATE_VIDEO(state, value) {
      state.generateVideo = value
    },
    SET_SELECTED_VIDEO_DURATION(state, duration) {
      state.selectedVideoDuration = duration
    },
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_ERROR(state, error) {
      state.error = error
    }
  },
  actions: {
    async fetchAllProviders({ commit }) { // Removed state from params as it's not used directly here
      commit('SET_LOADING', true)
      commit('SET_ERROR', null)
      commit('CLEAR_ALL_PROVIDERS') // Clear providers before fetching
      commit('CLEAR_SELECTED_PROVIDERS') // Clear selections - ensures no default selection
      
      try {
        const response = await aiProviderService.getAllProviders()
        commit('SET_ALL_PROVIDERS', response.data)
        
        // --- Removed default selection logic --- 
        // No default providers will be selected upon fetching.
        // User must explicitly select a provider for each capability.
        
      } catch (error) {
        console.error('Error fetching AI providers:', error)
        commit('SET_ERROR', 'Có lỗi xảy ra khi lấy danh sách nhà cung cấp AI. Vui lòng kiểm tra kết nối backend.')
      } finally {
        commit('SET_LOADING', false)
      }
    },
    
    selectProvider({ commit }, { capability, providerId }) {
      commit('SET_SELECTED_PROVIDER', { capability, providerId })
    },
    
    toggleGenerateVideo({ commit, state }) {
      commit('SET_GENERATE_VIDEO', !state.generateVideo)
    },
    
    setGenerateVideo({ commit }, value) {
      commit('SET_GENERATE_VIDEO', value)
    },

    setSelectedVideoDuration({ commit }, duration) {
      commit('SET_SELECTED_VIDEO_DURATION', duration)
    }
  },
  getters: {
    allProviders: state => state.allProviders,
    
    providersByCapability: state => capability => {
      const upperCapability = capability.toUpperCase();
      return state.allProviders.filter(provider => 
        provider.capabilities && provider.capabilities.includes(upperCapability)
      )
    },
    
    selectedProvider: state => capability => {
      const upperCapability = capability.toUpperCase();
      // Use safe hasOwnProperty check
      if (Object.prototype.hasOwnProperty.call(state.selectedProviders, upperCapability)) {
          return state.selectedProviders[upperCapability]
      }
      return null; // Return null if capability doesn't exist
    },
    
    // Getter to check if a text provider is selected (for validation)
    isTextProviderSelected: state => {
        return state.selectedProviders.TEXT !== null;
    },

    shouldGenerateVideo: state => {
      // Generate video only if checkbox is checked AND a video provider is selected
      return state.generateVideo && state.selectedProviders.VIDEO !== null
    },

    getSelectedVideoDuration: state => {
      return state.selectedVideoDuration
    },
    
    isLoading: state => state.loading,
    
    error: state => state.error
  }
}