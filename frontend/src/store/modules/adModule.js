import adService from '@/services/adService'

export default {
  namespaced: true,
  state: {
    adRequest: {
      adType: 'PAGE_POST',
      prompt: '',
      aiProvider: null, // Initialize as null, user must select
      numberOfVariations: 3,
      imageProvider: null,
      videoProvider: null,
      generateVideo: false,
      videoDuration: 10,
      // These might not be needed if we store files directly in uploadedMedia
      // uploadedImageFile: null, 
      // uploadedVideoFile: null
    },
    adVariations: [],
    selectedAd: null,
    loading: false,
    error: null,
    // Store uploaded files directly in the module state
    uploadedMedia: {
        IMAGE: null,
        VIDEO: null
    }
  },
  mutations: {
    SET_AD_REQUEST(state, adRequest) {
      state.adRequest = { ...state.adRequest, ...adRequest }
    },
    SET_AD_VARIATIONS(state, adVariations) {
      state.adVariations = adVariations.map(ad => ({
        ...ad,
        imageUrl: ad.imageUrl || '/img/placeholder.png',
        videoUrl: ad.videoUrl, // Assuming backend might return videoUrl
        selected: false
      }))
    },
    // Mutation to specifically handle adding/updating uploaded media preview
    SET_UPLOADED_MEDIA_PREVIEW(state, { mediaType, file }) {
        // Revoke previous object URL if exists to prevent memory leaks
        const existingPreviewIndex = state.adVariations.findIndex(ad => ad.isUpload && ad.id === `uploaded-${mediaType.toLowerCase()}`);
        if (existingPreviewIndex !== -1) {
            const existingPreview = state.adVariations[existingPreviewIndex];
            const oldUrl = mediaType === 'IMAGE' ? existingPreview.imageUrl : existingPreview.videoUrl;
            if (oldUrl && oldUrl.startsWith('blob:') && typeof URL.revokeObjectURL === 'function') {
                URL.revokeObjectURL(oldUrl);
            }
            // Remove the old preview
            state.adVariations.splice(existingPreviewIndex, 1);
        }

        // Add the new preview item at the beginning if a file is provided
        if (file) {
            const mediaUrl = URL.createObjectURL(file);
            const previewItem = {
                id: `uploaded-${mediaType.toLowerCase()}`,
                aiProvider: 'Uploaded',
                headline: `Uploaded ${mediaType === 'IMAGE' ? 'Image' : 'Video'}`,
                description: file.name,
                primaryText: '',
                callToAction: '',
                imageUrl: mediaType === 'IMAGE' ? mediaUrl : '/img/placeholder-video.png', // Use placeholder for video thumb if needed
                videoUrl: mediaType === 'VIDEO' ? mediaUrl : null,
                selected: false,
                isUpload: true // Flag to identify uploaded item
            };
            state.adVariations.unshift(previewItem);
        }
    },
    SET_SELECTED_AD(state, adContent) {
      state.selectedAd = adContent
      state.adVariations = state.adVariations.map(ad => ({
        ...ad,
        selected: ad.id === adContent.id
      }))
    },
    SET_LOADING(state, loading) {
      state.loading = loading
    },
    SET_ERROR(state, error) {
      state.error = error
    },
    CLEAR_ERROR(state) {
      state.error = null
    },
    CLEAR_VARIATIONS(state) {
        // Clear only AI variations, keep uploads
        state.adVariations = state.adVariations.filter(ad => ad.isUpload);
        state.selectedAd = null;
    },
    // Mutation to store the uploaded file reference
    SET_UPLOADED_MEDIA_FILE(state, { capability, file }) {
        // Use safe hasOwnProperty check
        if (Object.prototype.hasOwnProperty.call(state.uploadedMedia, capability)) {
            // Store the new file reference
            state.uploadedMedia[capability] = file;
        } else {
            console.warn(`Cannot set uploaded media for unknown capability: ${capability}`);
        }
    }
  },
  actions: {
    updateAdRequest({ commit }, adRequest) {
      commit('SET_AD_REQUEST', adRequest)
    },
    // Action called from AIProviderSelectorGroup to store the file and update preview
    setUploadedMedia({ commit }, { capability, file }) {
        // Commit file reference update
        commit('SET_UPLOADED_MEDIA_FILE', { capability, file });
        // Commit preview update
        commit('SET_UPLOADED_MEDIA_PREVIEW', { mediaType: capability, file });
    },
    async generateAd({ commit, state, rootState }) {
      commit('SET_LOADING', true)
      commit('CLEAR_ERROR')
      // Clear only AI generated variations before fetching new ones
      commit('SET_AD_VARIATIONS', state.adVariations.filter(ad => ad.isUpload));

      // --- Validation --- 
      const selectedTextProvider = rootState.aiProvider.selectedProviders.TEXT;
      if (!selectedTextProvider) {
          commit('SET_ERROR', 'Vui lòng chọn một nhà cung cấp để sinh văn bản.');
          commit('SET_LOADING', false);
          return; // Stop execution
      }
      // --- End Validation ---

      // Prepare request based on selections and uploads
      const requestData = {
          ...state.adRequest, // Includes prompt, numberOfVariations etc.
          aiProvider: selectedTextProvider, // Use the validated text provider
          imageProvider: rootState.aiProvider.selectedProviders.IMAGE,
          videoProvider: rootState.aiProvider.selectedProviders.VIDEO,
          generateVideo: rootState.aiProvider.generateVideo,
          videoDuration: rootState.aiProvider.selectedVideoDuration,
          // Indicate if media is uploaded (backend might use this)
          hasUploadedImage: !!state.uploadedMedia.IMAGE,
          hasUploadedVideo: !!state.uploadedMedia.VIDEO
      };

      // If media is uploaded, backend should ideally handle skipping AI generation

      try {
        const response = await adService.generateAd(requestData)

        if (response && response.data && response.data.adVariations) {
            // Add AI variations, keeping existing upload previews
            const aiVariations = response.data.adVariations.map(ad => ({
                ...ad,
                imageUrl: ad.imageUrl || '/img/placeholder.png',
                videoUrl: ad.videoUrl,
                selected: false,
                isUpload: false // Mark as AI generated
            }));
            // Combine uploaded previews (already in state) with new AI variations
            commit('SET_AD_VARIATIONS', [...state.adVariations.filter(ad => ad.isUpload), ...aiVariations]);
        } else {
            console.warn('No AI ad variations received from backend.');
            // Keep uploaded previews if they exist
            commit('SET_AD_VARIATIONS', state.adVariations.filter(ad => ad.isUpload));
        }

      } catch (error) {
        console.error('Error generating ad:', error)
        let errorMessage = 'Có lỗi xảy ra khi sinh quảng cáo. Vui lòng thử lại sau.'
        if (error.response && error.response.data) {
            if (typeof error.response.data === 'string') {
                errorMessage = error.response.data;
            } else if (error.response.data.message) {
                errorMessage = error.response.data.message;
            } else if (error.response.status === 429) {
                errorMessage = 'Lỗi Rate Limit: Hết hạn ngạch API. Vui lòng kiểm tra gói cước/thanh toán.';
            } else if (error.response.status === 404 && error.response.data.error && error.response.data.error.message) {
                 errorMessage = `Lỗi API: ${error.response.data.error.message}`;
            } else if (error.response.status === 400) {
                 errorMessage = `Lỗi Yêu cầu (400): ${error.response.data.error || error.response.data.message || 'Kiểm tra lại thông tin nhập.'}`;
            }
        }
        commit('SET_ERROR', errorMessage)
        // Keep uploaded previews even on error
        commit('SET_AD_VARIATIONS', state.adVariations.filter(ad => ad.isUpload));
      } finally {
        commit('SET_LOADING', false)
      }
    },
    selectAd({ commit }, adContent) {
      commit('SET_SELECTED_AD', adContent)
    },
    async saveSelectedAd({ commit, state }) {
      if (!state.selectedAd) {
        commit('SET_ERROR', 'Vui lòng chọn một mẫu quảng cáo trước khi lưu.')
        return
      }

      commit('SET_LOADING', true)
      commit('CLEAR_ERROR')

      try {
        // If the selected ad is an uploaded one, we might need different logic
        if (state.selectedAd.isUpload) {
            console.log("Saving uploaded media ad:", state.selectedAd);
            // Implement saving logic for uploaded media if needed
            // For now, just log it.
        } else {
            await adService.saveSelectedAd(state.selectedAd)
        }
        // Add success message or handling here if needed
      } catch (error) {
        console.error('Error saving selected ad:', error)
        commit('SET_ERROR', 'Có lỗi xảy ra khi lưu quảng cáo. Vui lòng thử lại sau.')
      } finally {
        commit('SET_LOADING', false)
      }
    }
  },
  getters: {
    adRequest: state => state.adRequest,
    adVariations: state => state.adVariations,
    selectedAd: state => state.selectedAd,
    isLoading: state => state.loading,
    error: state => state.error,
    // Getter to retrieve the stored uploaded file reference
    getUploadedMediaFile: state => capability => {
        // Use safe hasOwnProperty check
        if (Object.prototype.hasOwnProperty.call(state.uploadedMedia, capability)) {
            return state.uploadedMedia[capability] || null;
        }
        return null;
    }
  }
}