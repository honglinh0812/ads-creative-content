<template>
  <a-modal
    v-model:open="dialogVisible"
    title="Xem trước nội dung đã trích xuất"
    width="800px"
    :footer="null"
    @cancel="handleCancel"
  >
    <div class="extracted-content-preview">
      <!-- Success Summary -->
      <div v-if="summary" class="summary-card">
        <a-alert
          :message="`Tìm thấy nội dung từ ${summary.success}/${summary.total} link`"
          :description="summary.errors > 0 ? `${summary.errors} link gặp lỗi khi trích xuất` : 'Tất cả link đã được trích xuất thành công'"
          :type="summary.errors > 0 ? 'warning' : 'success'"
          show-icon
          style="margin-bottom: 20px;"
        />
      </div>

      <!-- Extracted Content Display -->
      <div v-if="hasValidContent" class="content-display">
        <h4 class="section-title">📄 Nội dung đã trích xuất:</h4>

        <div class="content-scroll">
          <div
            v-for="(item, index) in validExtractions"
            :key="index"
            class="content-item"
          >
            <div class="item-header">
              <span class="item-index">Quảng cáo {{ index + 1 }}</span>
              <span v-if="item.imageCount > 0" class="item-badge">
                🖼️ {{ item.imageCount }} ảnh
              </span>
            </div>

            <div class="item-text">
              {{ item.text }}
            </div>

            <div v-if="item.images && item.images.length > 0" class="item-images">
              <div
                v-for="(img, imgIdx) in item.images.slice(0, 3)"
                :key="imgIdx"
                class="image-thumb"
              >
                <img :src="img" :alt="`Image ${imgIdx + 1}`" @error="handleImageError" />
              </div>
              <span v-if="item.images.length > 3" class="more-images">
                +{{ item.images.length - 3 }} more
              </span>
            </div>
          </div>
        </div>

        <div class="preview-info">
          <a-alert
            message="Thông tin quan trọng"
            description="Nội dung này sẽ được thêm vào prompt của bạn để AI tham khảo phong cách viết. Không phải nội dung sao chép."
            type="info"
            show-icon
          />
        </div>
      </div>

      <!-- Error Display -->
      <div v-if="hasErrors" class="errors-display">
        <h4 class="section-title error-title">⚠️ Các link gặp lỗi:</h4>

        <div
          v-for="(error, index) in errorItems"
          :key="index"
          class="error-item"
        >
          <a-alert
            :message="error.message || 'Không thể trích xuất'"
            :description="getErrorDescription(error)"
            type="error"
            show-icon
          />
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="action-buttons">
        <a-button @click="handleCancel" size="large">
          Hủy & Quay lại
        </a-button>
        <a-button
          v-if="hasValidContent"
          type="primary"
          @click="handleAccept"
          size="large"
        >
          <template #icon><check-outlined /></template>
          Sử dụng nội dung này
        </a-button>
        <a-button
          v-else
          type="default"
          @click="handleSkip"
          size="large"
        >
          Bỏ qua & Tiếp tục
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import { CheckOutlined } from '@ant-design/icons-vue'

export default {
  name: 'ExtractedContentPreview',
  components: {
    CheckOutlined
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    extractedData: {
      type: Array,
      default: () => []
    },
    summary: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'accept', 'cancel', 'skip'],
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    validExtractions() {
      return this.extractedData.filter(item =>
        !item.error && item.hasContent && item.text
      )
    },
    errorItems() {
      return this.extractedData.filter(item => item.error)
    },
    hasValidContent() {
      return this.validExtractions.length > 0
    },
    hasErrors() {
      return this.errorItems.length > 0
    }
  },
  methods: {
    handleAccept() {
      this.$emit('accept', this.validExtractions)
      this.$emit('update:visible', false)
    },
    handleCancel() {
      this.$emit('cancel')
      this.$emit('update:visible', false)
    },
    handleSkip() {
      this.$emit('skip')
      this.$emit('update:visible', false)
    },
    getErrorDescription(error) {
      if (error.error === 'API_KEY_NOT_CONFIGURED') {
        return 'Hệ thống chưa được cấu hình API key. Vui lòng liên hệ admin.'
      } else if (error.error === 'NETWORK_ERROR') {
        return 'Không thể kết nối tới dịch vụ trích xuất.'
      } else if (error.error === 'NO_TEXT_FOUND') {
        return 'Link hợp lệ nhưng không tìm thấy nội dung text.'
      } else if (error.details) {
        return JSON.stringify(error.details)
      }
      return error.message || 'Lỗi không xác định'
    },
    handleImageError(event) {
      event.target.src = 'data:image/svg+xml,' + encodeURIComponent(`
        <svg width="80" height="60" xmlns="http://www.w3.org/2000/svg">
          <rect width="80" height="60" fill="#f0f0f0"/>
          <text x="50%" y="50%" text-anchor="middle" fill="#999" font-size="10">No Image</text>
        </svg>
      `)
    }
  }
}
</script>

<style lang="scss" scoped>
.extracted-content-preview {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-card {
  margin-bottom: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;

  &.error-title {
    color: #cf1322;
  }
}

.content-display {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-scroll {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
  background: #fafafa;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.content-item {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-index {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
}

.item-badge {
  font-size: 12px;
  background: #e6f7ff;
  color: #1890ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.item-text {
  font-size: 14px;
  line-height: 1.6;
  color: #262626;
  white-space: pre-wrap;
  word-break: break-word;
}

.item-images {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.image-thumb {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #d9d9d9;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.more-images {
  font-size: 12px;
  color: #8c8c8c;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.preview-info {
  margin-top: 8px;
}

.errors-display {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.error-item {
  margin-bottom: 8px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

/* Mobile responsive */
@media (max-width: 768px) {
  .content-scroll {
    max-height: 300px;
  }

  .action-buttons {
    flex-direction: column-reverse;

    button {
      width: 100%;
    }
  }

  .item-images {
    .image-thumb {
      width: 60px;
      height: 45px;
    }
  }
}
</style>
