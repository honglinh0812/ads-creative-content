<template>
  <div class="ai-provider-selector-group">
    <h3>{{ title }}</h3>

    <div v-if="isLoading && providerOptions.length === 0" class="loading-indicator">
      Đang tải danh sách nhà cung cấp AI...
    </div>

    <div v-if="error && providerOptions.length === 0" class="error-message">
      {{ error }}
    </div>

    <div v-if="!isLoading && providerOptions.length > 0" class="provider-options">
      <div
        v-for="provider in providerOptions"
        :key="provider.id"
        class="provider-option"
        :class="{ 'selected': selectedProviderId === provider.id }"
        @click="selectProvider(provider.id)"
      >
        <div class="provider-radio">
          <input
            type="radio"
            :id="`${capability}-${provider.id}`"
            :name="`${capability}-provider`"
            :value="provider.id"
            :checked="selectedProviderId === provider.id"
            @change="selectProvider(provider.id)"
          >
          <label :for="`${capability}-${provider.id}`">{{ provider.name }}</label>
        </div>
        <div class="provider-description">{{ provider.description }}</div>
      </div>
    </div>

    <!-- Video Generation Options (Checkbox and Duration) -->
    <!-- Ensure this section renders correctly -->
    <div v-if="capability === 'VIDEO' && providerOptions.length > 0" class="video-options">
       <hr v-if="providerOptions.length > 0" style="margin: 15px 0;"> <!-- Add a separator -->
      <div class="video-checkbox form-check"> <!-- Added form-check for potential styling -->
        <input
          type="checkbox"
          class="form-check-input"
          id="generate-video"
          v-model="generateVideoModel"
          :disabled="selectedProviderId === null" 
        >
        <label class="form-check-label" for="generate-video">Sinh video cho quảng cáo</label>
      </div>

      <!-- Video Duration Input (shown only if checkbox is checked) -->
      <div v-if="generateVideoModel" class="video-duration-input form-group">
        <label for="video-duration">Độ dài video (giây)</label>
        <input
          id="video-duration"
          type="number"
          min="10"
          max="15"
          :value="selectedVideoDurationState"
          @input="updateSelectedVideoDuration(parseInt($event.target.value))"
          class="form-control"
        />
        <small>Chọn độ dài từ 10 đến 15 giây</small>
      </div>
    </div>

     <!-- Add message if no providers found for this capability -->
     <div v-if="!isLoading && providerOptions.length === 0 && !error" class="no-providers-message">
        Không tìm thấy nhà cung cấp nào hỗ trợ chức năng này.
    </div>

  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex';

export default {
  name: 'AIProviderSelectorGroup',
  props: {
    capability: {
      type: String,
      required: true,
      validator: value => ['TEXT', 'IMAGE', 'VIDEO'].includes(value)
    },
    title: {
      type: String,
      required: true
    }
  },
  computed: {
    ...mapGetters('aiProvider', [
      'providersByCapability',
      'selectedProvider',
      'isLoading',
      'error',
      'getSelectedVideoDuration'
    ]),

    providerOptions() {
      const options = this.providersByCapability(this.capability);
      // Add logging here to debug video provider issue
      if (this.capability === 'VIDEO') {
        console.log(`[AIProviderSelectorGroup VIDEO] Capability: ${this.capability}`);
        console.log(`[AIProviderSelectorGroup VIDEO] Raw providers from getter:`, options);
        console.log(`[AIProviderSelectorGroup VIDEO] All providers in store state:`, this.$store.state.aiProvider.allProviders);
      }
      return options;
    },

    selectedProviderId() {
      return this.selectedProvider(this.capability);
    },

    selectedVideoDurationState() {
      return this.getSelectedVideoDuration;
    },

    // Computed property for v-model on the checkbox
    generateVideoModel: {
      get() {
        // Checkbox state depends on whether a video provider is selected AND the vuex flag
        return this.selectedProviderId !== null && this.$store.state.aiProvider.generateVideo;
      },
      set(value) {
        // Only allow setting the flag if a video provider is actually selected
        if (this.selectedProviderId !== null) {
            this.setGenerateVideo(value);
        } else {
            // If no provider is selected, force the flag to false
            this.setGenerateVideo(false);
        }
      }
    }
  },
  methods: {
    ...mapActions('aiProvider', [
      'fetchAllProviders',
      // 'selectProvider', // Action name conflict, use dispatch directly
      'setGenerateVideo',
      'setSelectedVideoDuration'
    ]),

    selectProvider(providerId) {
        console.log(`[AIProviderSelectorGroup ${this.capability}] Selecting provider:`, providerId);
      // Dispatch action with full namespace
      this.$store.dispatch('aiProvider/selectProvider', {
        capability: this.capability,
        providerId: providerId
      });
      // If unselecting the video provider, also uncheck the generate video box
      // Check if the currently selected provider for VIDEO is changing *away* from the clicked one
      if (this.capability === 'VIDEO' && this.selectedProviderId !== providerId) {
          console.log('[AIProviderSelectorGroup VIDEO] Unselecting video provider, setting generateVideo to false.');
          this.setGenerateVideo(false);
      }
    },

    updateSelectedVideoDuration(duration) {
      let newDuration = 10; // Default
      if (!isNaN(duration)) {
          if (duration < 10) newDuration = 10;
          else if (duration > 15) newDuration = 15;
          else newDuration = duration;
      }
      console.log(`[AIProviderSelectorGroup VIDEO] Updating video duration to:`, newDuration);
      this.setSelectedVideoDuration(newDuration);
    }
  },
  created() {
    console.log(`[AIProviderSelectorGroup ${this.capability}] Component created.`);
    // Fetch providers only if not already loaded or if there was an error previously
    // Check length of allProviders, not just for this capability
    if (this.$store.state.aiProvider.allProviders.length === 0 || this.error) {
        console.log(`[AIProviderSelectorGroup ${this.capability}] Fetching all providers...`);
        this.fetchAllProviders();
    } else {
        console.log(`[AIProviderSelectorGroup ${this.capability}] Providers already loaded.`);
    }
  }
}
</script>

<style scoped>
/* Styles adjusted slightly for clarity */
.ai-provider-selector-group {
  margin-bottom: 24px;
}

h3 {
  margin-bottom: 16px;
  font-size: 1.1rem; /* Slightly smaller */
  font-weight: 600; /* Bolder */
  color: var(--tp-color-blue);
}

.provider-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.provider-option {
  padding: 15px;
  border: 1px solid var(--tp-color-grey-medium);
  border-radius: var(--tp-border-radius);
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: var(--tp-color-white);
}

.provider-option:hover {
  border-color: var(--tp-color-green);
  box-shadow: 0 1px 3px rgba(0, 182, 122, 0.2);
}

.provider-option.selected {
  border-color: var(--tp-color-green);
  background-color: rgba(0, 182, 122, 0.05);
  box-shadow: 0 1px 3px rgba(0, 182, 122, 0.2);
}

.provider-radio {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}

.provider-radio input[type="radio"] {
  margin-right: 10px;
  accent-color: var(--tp-color-green);
}

.provider-radio label {
  font-weight: 500;
  cursor: pointer;
}

.provider-description {
  margin-left: 28px; /* Align with label text */
  font-size: 0.9rem;
  color: var(--tp-color-grey-dark);
}

.video-options {
  margin-top: 16px;
  /* padding-top: 16px; */ /* Removed padding top, using hr instead */
  /* border-top: 1px solid var(--tp-color-grey-medium); */ /* Removed border top, using hr instead */
}

.video-checkbox {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.video-checkbox input[type="checkbox"] {
  margin-right: 10px;
  width: 18px;
  height: 18px;
  accent-color: var(--tp-color-green);
}

.video-checkbox label {
  cursor: pointer;
  font-weight: 500;
}

.video-duration-input {
  margin-top: 10px;
}

.video-duration-input label {
  display: block;
  margin-bottom: 5px;
  font-size: 0.9rem;
  font-weight: 500;
}

.video-duration-input .form-control {
  width: 100px; /* Slightly wider */
  padding: 8px 12px;
  margin-bottom: 5px;
}

.video-duration-input small {
  display: block;
  margin-top: 5px;
  font-size: 0.8rem;
  color: var(--tp-color-grey-dark);
}

.loading-indicator,
.error-message,
.no-providers-message {
  margin-top: 12px;
  padding: 10px 15px;
  border-radius: var(--tp-border-radius);
  font-size: 0.9rem;
}

.loading-indicator {
    color: var(--tp-color-grey-dark);
    background-color: var(--tp-color-grey-light);
}

.error-message {
    color: var(--tp-color-error);
    background-color: var(--tp-color-error-bg);
    border: 1px solid var(--tp-color-error);
}

.no-providers-message {
    color: var(--tp-color-grey-dark);
    background-color: var(--tp-color-grey-light);
    font-style: italic;
}

/* Ensure form-group and form-control styles from global theme apply */
</style>