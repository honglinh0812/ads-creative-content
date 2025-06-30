<template>
  <div class="ai-provider-selector">
    <select
      :value="value"
      @input="$emit('input', $event.target.value)"
      class="form-control"
      :disabled="isLoading"
    >
      <option
        v-for="provider in providers"
        :key="provider.id"
        :value="provider.id"
      >
        {{ provider.name }}
      </option>
    </select>
    
    <div v-if="isLoading" class="loading-indicator">
      Đang tải danh sách nhà cung cấp AI...
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex';

export default {
  name: 'AIProviderSelector',
  props: {
    value: {
      type: String,
      default: 'openai'
    }
  },
  computed: {
    ...mapState('aiProvider', ['providers', 'isLoading', 'error'])
  },
  methods: {
    ...mapActions('aiProvider', ['fetchProviders'])
  },
  created() {
    this.fetchProviders();
  }
}
</script>

<style scoped>
.loading-indicator {
  margin-top: 5px;
  font-size: 14px;
  color: #666;
}

.error-message {
  margin-top: 5px;
  font-size: 14px;
  color: #c62828;
}

.form-control {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  background-color: white;
}
</style>
