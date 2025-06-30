<template>
  <div
    class="ad-preview-item"
    :class="{ 'selected': isSelected }"
    @click="$emit('select', adContent)"
  >
    <div class="ad-provider">
      {{ adContent.aiProvider }}
    </div>
    
    <!-- Video Preview (if available) -->
    <div v-if="adContent.videoUrl" class="ad-video">
      <video controls :src="adContent.videoUrl" class="video-player"></video>
    </div>
    
    <!-- Image Preview (if no video or as secondary) -->
    <div v-else-if="adContent.imageUrl" class="ad-image">
      <img :src="adContent.imageUrl" alt="Ad Image" />
    </div>
    
    <div class="ad-content">
      <h3 class="ad-headline">{{ adContent.headline }}</h3>
      <p class="ad-description">{{ adContent.description }}</p>
      <p class="ad-primary-text">{{ truncatedPrimaryText }}</p>
      <button class="ad-cta">{{ adContent.callToAction || 'Tìm hiểu thêm' }}</button>
    </div>
    
    <div class="selection-indicator" v-if="isSelected">
      <span class="checkmark">✓</span> Đã chọn
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdPreviewItem',
  props: {
    adContent: {
      type: Object,
      required: true
    },
    isSelected: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    truncatedPrimaryText() {
      if (!this.adContent.primaryText) return '';
      return this.adContent.primaryText.length > 100
        ? this.adContent.primaryText.substring(0, 100) + '...'
        : this.adContent.primaryText;
    }
  }
}
</script>

<style scoped>
.ad-preview-item {
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.ad-preview-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.ad-preview-item.selected {
  border: 2px solid #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.3);
}

.ad-provider {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 10;
}

.ad-image {
  height: 200px;
  overflow: hidden;
}

.ad-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ad-video {
  height: 250px;
  overflow: hidden;
  background-color: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-player {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ad-content {
  padding: 15px;
}

.ad-headline {
  margin: 0 0 10px;
  font-size: 18px;
  color: #333;
}

.ad-description {
  margin: 0 0 10px;
  font-size: 14px;
  color: #666;
}

.ad-primary-text {
  margin: 0 0 15px;
  font-size: 14px;
  color: #444;
  line-height: 1.4;
}

.ad-cta {
  background-color: #1877F2;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.selection-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #4CAF50;
  color: white;
  padding: 8px;
  text-align: center;
  font-weight: bold;
}

.checkmark {
  margin-right: 5px;
}
</style>
