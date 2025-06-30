<template>
  <div class="ad-generation-page container">
    <h1>Tạo quảng cáo Facebook</h1>
    
    <div class="generation-options">
      <AdForm 
        :loading="loading"
        @submit="handleFormSubmit" 
      />
    </div>
    
    <!-- Ad Previews -->
    <div v-if="adVariations && adVariations.length > 0" class="ad-previews">
      <h2>Xem trước quảng cáo</h2>
      <AdPreviewList 
        :previews="adVariations" 
        @select="selectAd"
      />
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script>
import { mapState, mapActions, mapGetters } from 'vuex';
import AdForm from '@/components/AdForm.vue';
import AdPreviewList from '@/components/AdPreviewList.vue';

export default {
  name: 'AdGenerationPage',
  
  components: {
    AdForm,
    AdPreviewList
  },
  
  data() {
    return {
      // adPreviews: [], // Use adVariations from store directly
      selectedAdLocal: null // Use local state for selection if needed, or rely on store
    };
  },
  
  computed: {
    ...mapState('ad', ['loading', 'error', 'adVariations']), // Map adVariations from store
    ...mapGetters('aiProvider', ['shouldGenerateVideo'])
  },
  
  methods: {
    // Map the correct actions from the 'ad' module
    ...mapActions('ad', ['updateAdRequest', 'generateAd', 'selectAd']),
    ...mapActions('videoGeneration', ['generateVideo', 'checkJobStatus']), // Keep video actions
    
    async handleFormSubmit(formData) {
      // 1. Update the ad request state in the store
      await this.updateAdRequest({
        adType: formData.adType,
        prompt: formData.prompt,
        textProvider: formData.textProvider, // Assuming text provider is the main one for ad content
        numberOfVariations: formData.variations,
        // Add other relevant fields if needed by the backend
        imageProvider: formData.imageProvider,
        videoProvider: formData.videoProvider,
        generateVideo: formData.generateVideo,
        videoDuration: formData.videoDuration
      });

      // 2. Trigger the ad generation action (which uses the updated state)
      try {
        await this.generateAd(); // This action now uses the state updated above
        
        // After generateAd completes, adVariations state should be updated.
        // The template will reactively display the previews.
        
        // 3. Handle video generation if requested
        if (formData.generateVideo && formData.videoProvider) {
          try {
            const videoJob = await this.generateVideo({
              prompt: formData.prompt,
              durationSeconds: formData.videoDuration || 10 // Use selected duration or default
            });
            
            if (videoJob && videoJob.id) {
              // Start polling for video status
              this.pollVideoStatus(videoJob.id, formData.videoProvider);
            }
          } catch (videoError) {
            console.error('Failed to initiate video generation:', videoError);
            // Optionally show a user-facing error
            this.$store.commit('ad/SET_ERROR', 'Lỗi khi bắt đầu tạo video.');
          }
        }
      } catch (error) {
        console.error('Failed to generate ad content:', error);
        // Error is already set in the store action, so no need to set it here unless more specific message is needed
      }
    },

    pollVideoStatus(jobId, videoProvider) {
      const pollInterval = setInterval(async () => {
        try {
          const updatedJob = await this.checkJobStatus(jobId);
          
          if (updatedJob.status === 'COMPLETED' && updatedJob.resultUrl) {
            // When video is ready, add it to the beginning of previews
            // Note: Modifying store state directly is not ideal. 
            // Consider a mutation or action to add video preview.
            const videoPreview = {
              id: `video-${updatedJob.id}`,
              headline: 'Video Ad',
              description: this.$store.state.ad.adRequest.prompt, // Get prompt from store
              primaryText: '',
              callToAction: 'Tìm hiểu thêm',
              // Find a relevant image URL from existing previews as fallback/thumbnail?
              imageUrl: this.adVariations.length > 0 ? this.adVariations[0].imageUrl : null,
              videoUrl: updatedJob.resultUrl,
              aiProvider: videoProvider,
              type: 'video',
              // Ensure it has necessary fields to be treated like other variations
              selected: false 
            };
            
            // Use a mutation to add the video preview to the store state
            this.$store.commit('ad/ADD_VIDEO_PREVIEW', videoPreview);
            clearInterval(pollInterval);

          } else if (updatedJob.status === 'FAILED') {
            console.error('Video generation failed for job:', jobId);
            this.$store.commit('ad/SET_ERROR', `Tạo video thất bại (Job ID: ${jobId}).`);
            clearInterval(pollInterval);
          }
        } catch (pollError) {
            console.error('Error polling video status:', pollError);
            // Stop polling on error to prevent infinite loops
            clearInterval(pollInterval);
        }
      }, 5000); // Check every 5 seconds

      // Clear interval after 5 minutes (timeout)
      setTimeout(() => {
        clearInterval(pollInterval);
        // Check if job is still pending/processing and mark as timed out if necessary
      }, 5 * 60 * 1000);
    },
    
    // Method to handle selection from AdPreviewList, calls the Vuex action
    handleAdSelection(ad) {
      this.selectAd(ad);
      this.selectedAdLocal = ad; // Update local state if needed
    }
  }
};
</script>

<style scoped>
/* Use styles from global theme */
.ad-generation-page {
  padding-top: 40px;
  padding-bottom: 40px;
}

h1 {
  text-align: center;
  margin-bottom: 40px;
}

.generation-options {
  margin-bottom: 40px;
}

.ad-previews {
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid var(--tp-color-grey-medium);
}

h2 {
  margin-bottom: 20px;
  text-align: center;
}
</style>
