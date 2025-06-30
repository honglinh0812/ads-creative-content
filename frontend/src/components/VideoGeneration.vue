<!-- src/components/VideoGeneration.vue -->
<template>
  <div class="video-generation">
    <h2>Generate Video Ad</h2>
    
    <!-- Video Generation Form -->
    <div v-if="!currentJob || currentJob.status === 'FAILED'" class="video-form">
      <div class="form-group">
        <label for="prompt">Prompt</label>
        <textarea 
          id="prompt" 
          v-model="prompt" 
          placeholder="Describe the video you want to generate..."
          rows="4"
          class="form-control"
        ></textarea>
      </div>
      
      <div class="form-group">
        <label for="duration">Duration (seconds)</label>
        <input 
          id="duration" 
          v-model.number="durationSeconds" 
          type="number" 
          min="10" 
          max="15"
          class="form-control"
        />
        <small>Duration must be between 10 and 15 seconds</small>
      </div>
      
      <button 
        @click="generateVideo" 
        :disabled="!isFormValid || loading" 
        class="btn btn-primary"
      >
        {{ loading ? 'Generating...' : 'Generate Video' }}
      </button>
    </div>
    
    <!-- Video Generation Status -->
    <div v-if="currentJob" class="video-status">
      <div class="status-info">
        <h3>Video Generation Status</h3>
        <p><strong>Prompt:</strong> {{ currentJob.prompt }}</p>
        <p><strong>Duration:</strong> {{ currentJob.durationSeconds }} seconds</p>
        <p><strong>Status:</strong> {{ formatStatus(currentJob.status) }}</p>
        <p><strong>Created:</strong> {{ formatDate(currentJob.createdAt) }}</p>
      </div>
      
      <!-- Loading Indicator -->
      <div v-if="isJobInProgress" class="loading-indicator">
        <div class="spinner"></div>
        <p>{{ statusMessage }}</p>
        <button @click="checkStatus" class="btn btn-secondary">Refresh Status</button>
      </div>
      
      <!-- Video Result -->
      <div v-if="isJobComplete" class="video-result">
        <h3>Generated Video</h3>
        <video controls :src="currentJob.resultUrl" class="video-player"></video>
        <div class="action-buttons">
          <button @click="selectVideo" class="btn btn-success">Select This Video</button>
          <button @click="resetForm" class="btn btn-outline-primary">Generate Another</button>
        </div>
      </div>
      
      <!-- Error Message -->
      <div v-if="isJobFailed" class="error-message">
        <p>Video generation failed. Please try again with a different prompt.</p>
        <button @click="resetForm" class="btn btn-outline-primary">Try Again</button>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex';

export default {
  name: 'VideoGeneration',
  
  data() {
    return {
      prompt: '',
      durationSeconds: 10,
      pollingInterval: null
    };
  },
  
  computed: {
    ...mapState('videoGeneration', ['currentJob', 'loading', 'error']),
    ...mapGetters('videoGeneration', ['isJobComplete', 'isJobFailed', 'isJobInProgress']),
    
    isFormValid() {
      return this.prompt.trim().length > 0 && 
        this.durationSeconds >= 10 && 
        this.durationSeconds <= 15;
    },
    
    statusMessage() {
      if (!this.currentJob) return '';
      
      switch (this.currentJob.status) {
        case 'PENDING':
          return 'Your video is in the queue and will start processing soon...';
        case 'PROCESSING':
          return 'Your video is being generated. This may take a few minutes...';
        default:
          return '';
      }
    }
  },
  
  methods: {
    ...mapActions('videoGeneration', {
      generateVideoAction: 'generateVideo',
      checkJobStatusAction: 'checkJobStatus'
    }),
    
    async generateVideo() {
      if (!this.isFormValid) return;
      
      try {
        await this.generateVideoAction({
          prompt: this.prompt,
          durationSeconds: this.durationSeconds
        });
        
        // Start polling for status updates
        this.startPolling();
      } catch (error) {
        console.error('Failed to generate video:', error);
      }
    },
    
    async checkStatus() {
      if (!this.currentJob) return;
      
      try {
        await this.checkJobStatusAction(this.currentJob.id);
      } catch (error) {
        console.error('Failed to check job status:', error);
      }
    },
    
    startPolling() {
      // Clear any existing polling
      this.stopPolling();
      
      // Start new polling every 5 seconds
      this.pollingInterval = setInterval(() => {
        if (this.currentJob && this.isJobInProgress) {
          this.checkStatus();
        } else {
          this.stopPolling();
        }
      }, 5000);
    },
    
    stopPolling() {
      if (this.pollingInterval) {
        clearInterval(this.pollingInterval);
        this.pollingInterval = null;
      }
    },
    
    resetForm() {
      this.stopPolling();
      this.$store.commit('videoGeneration/SET_CURRENT_JOB', null);
      this.prompt = '';
      this.durationSeconds = 10;
    },
    
    selectVideo() {
      if (this.currentJob && this.isJobComplete) {
        // Emit event with video URL for parent component
        this.$emit('video-selected', {
          url: this.currentJob.resultUrl,
          prompt: this.currentJob.prompt
        });
      }
    },
    
    formatStatus(status) {
      if (!status) return '';
      
      const statusMap = {
        'PENDING': 'Pending',
        'PROCESSING': 'Processing',
        'COMPLETED': 'Completed',
        'FAILED': 'Failed'
      };
      
      return statusMap[status] || status;
    },
    
    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleString();
    }
  },
  
  beforeDestroy() {
    this.stopPolling();
  }
};
</script>

<style scoped>
.video-generation {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn {
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary {
  background-color: #4285f4;
  color: white;
  border: none;
}

.btn-secondary {
  background-color: #f1f1f1;
  color: #333;
  border: 1px solid #ddd;
}

.btn-success {
  background-color: #0f9d58;
  color: white;
  border: none;
}

.btn-outline-primary {
  background-color: transparent;
  color: #4285f4;
  border: 1px solid #4285f4;
}

.video-status {
  margin-top: 30px;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px 0;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top: 4px solid #4285f4;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.video-player {
  width: 100%;
  max-height: 450px;
  margin: 20px 0;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.error-message {
  color: #d93025;
  margin: 20px 0;
  padding: 15px;
  border: 1px solid #d93025;
  border-radius: 4px;
  background-color: rgba(217, 48, 37, 0.1);
}
</style>
