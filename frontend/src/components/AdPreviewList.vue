<template>
  <div class="ad-preview-list">
    <h2>Mẫu quảng cáo ({{ adVariations.length }})</h2>
    
    <div v-if="isLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>Đang sinh quảng cáo...</p>
    </div>
    
    <div v-else-if="adVariations.length === 0" class="empty-state">
      <p>Chưa có mẫu quảng cáo nào. Hãy nhập mô tả và nhấn "Sinh quảng cáo".</p>
    </div>
    
    <div v-else class="preview-grid">
      <ad-preview-item
        v-for="(adContent, index) in adVariations"
        :key="index"
        :ad-content="adContent"
        :is-selected="selectedAd && selectedAd.id === adContent.id"
        @select="selectAd"
      />
    </div>
    
    <div v-if="selectedAd" class="selected-actions">
      <button @click="saveSelectedAd" class="btn btn-success">
        Lưu quảng cáo đã chọn
      </button>
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex';
import AdPreviewItem from './AdPreviewItem.vue';

export default {
  name: 'AdPreviewList',
  components: {
    AdPreviewItem
  },
  computed: {
    ...mapState('ad', ['adVariations', 'selectedAd', 'isLoading', 'error'])
  },
  methods: {
    ...mapActions('ad', ['selectAd', 'saveSelectedAd'])
  }
}
</script>

<style scoped>
.ad-preview-list {
  margin-top: 30px;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #4CAF50;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  padding: 40px;
  text-align: center;
  background-color: #f9f9f9;
  border-radius: 8px;
  color: #666;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.selected-actions {
  margin-top: 20px;
  text-align: center;
}

.btn-success {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-success:hover {
  background-color: #45a049;
}

.error-message {
  margin-top: 15px;
  padding: 10px;
  background-color: #ffebee;
  color: #c62828;
  border-radius: 4px;
}
</style>
