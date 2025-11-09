<template>
  <div class="prompt-validator">
    <!-- Validation Status Display -->
    <div v-if="showValidation && validationResult" class="validation-feedback">
      <!-- Quality Score Badge -->
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center space-x-2">
          <span class="text-sm font-medium text-gray-700">{{ $t('components.promptValidator.quality.label') }}:</span>
          <div
            :class="getQualityBadgeClass(validationResult.qualityLevel)"
            class="px-2 py-1 rounded-full text-xs font-semibold"
          >
            {{ validationResult.qualityScore }}/100 - {{ capitalizeFirst(validationResult.qualityLevel) }}
          </div>
        </div>

        <!-- Close button -->
        <button
          @click="hideValidation"
          class="text-gray-400 hover:text-gray-600 text-sm"
        >
          ✕
        </button>
      </div>

      <!-- Issues and Warnings -->
      <div v-if="validationResult.issues && validationResult.issues.length > 0" class="mb-3">
        <div 
          v-for="issue in validationResult.issues" 
          :key="issue.type"
          :class="getIssueClass(issue.severity)"
          class="flex items-start space-x-2 p-2 rounded mb-2 text-sm"
        >
          <span :class="getIssueIconClass(issue.severity)">
            {{ issue.severity === 'error' ? '⚠️' : '💡' }}
          </span>
          <div class="flex-1">
            <div class="font-medium">{{ issue.message }}</div>
            <div class="text-xs mt-1 opacity-80">{{ issue.suggestion }}</div>
          </div>
        </div>
      </div>

      <!-- General Suggestions -->
      <div v-if="validationResult.suggestions && validationResult.suggestions.length > 0" class="mb-3">
        <div class="text-sm font-medium text-gray-700 mb-2">{{ $t('components.promptValidator.suggestions.title') }}:</div>
        <ul class="text-sm text-gray-600 space-y-1">
          <li v-for="suggestion in validationResult.suggestions" :key="suggestion" class="flex items-start">
            <span class="mr-2">•</span>
            <span>{{ suggestion }}</span>
          </li>
        </ul>
      </div>

      <!-- Improved Prompt section removed - now using static suggestions only -->
    </div>

    <!-- Loading State -->
    <div v-if="isValidating" class="validation-feedback">
      <div class="flex items-center space-x-2 text-sm text-gray-600">
        <div class="animate-spin w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full"></div>
        <span>{{ $t('components.promptValidator.loading') }}</span>
      </div>
    </div>
  </div>
</template>

<script>
// Simple debounce function
function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

export default {
  name: 'PromptValidator',
  props: {
    prompt: {
      type: String,
      required: true
    },
    adType: {
      type: String,
      default: ''
    },
    language: {
      type: String,
      default: 'en'
    },
    targetAudience: {
      type: String,
      default: ''
    },
    industry: {
      type: String,
      default: ''
    },
    autoValidate: {
      type: Boolean,
      default: true
    }
  },
  emits: ['prompt-updated'],
  data() {
    return {
      validationResult: null,
      isValidating: false,
      showValidation: false,
      debouncedValidate: null
    }
  },
  computed: {
    shouldValidate() {
      return this.prompt && this.prompt.trim().length > 0
    }
  },
  watch: {
    prompt: {
      handler() {
        if (this.autoValidate && this.shouldValidate) {
          // Use static validation instead of API call
          this.debouncedValidate()
        } else if (!this.shouldValidate) {
          this.hideValidation()
        }
      },
      immediate: false
    }
  },
  created() {
    // Create debounced validation function
    this.debouncedValidate = debounce(this.validatePrompt, 800)
  },
  methods: {
    validatePrompt() {
      if (!this.shouldValidate) return

      // Don't validate if prompt is too short (< 10 chars)
      const trimmedPrompt = this.prompt.trim()
      if (trimmedPrompt.length < 10) {
        this.hideValidation()
        return
      }

      this.isValidating = true
      this.showValidation = true

      // Use static validation instead of API call
      setTimeout(() => {
        this.validationResult = this.getStaticValidation(trimmedPrompt)
        this.isValidating = false
      }, 100) // Small delay to show loading state
    },

    getStaticValidation(prompt) {
      const issues = []
      const suggestions = []
      let qualityScore = 70 // Base score

      // Client-side validation rules
      const wordCount = prompt.split(/\s+/).length
      const hasCallToAction = /\b(buy|shop|order|subscribe|sign up|download|get|try|visit|learn|discover|explore|click|register|join)\b/i.test(prompt)
      const hasNumbers = /\d+/.test(prompt)
      const hasBenefits = /\b(free|save|discount|new|best|quality|easy|fast|now|today|limited)\b/i.test(prompt)

      // Check prompt length
      if (wordCount < 5) {
        issues.push({
          type: 'length',
          severity: 'warning',
          message: this.language === 'vi' ? 'Prompt hơi ngắn' : 'Prompt is a bit short',
          suggestion: this.language === 'vi'
            ? 'Thêm chi tiết về sản phẩm/dịch vụ để quảng cáo hiệu quả hơn'
            : 'Add more details about your product/service for better ad quality'
        })
        qualityScore -= 15
      } else if (wordCount > 50) {
        issues.push({
          type: 'length',
          severity: 'warning',
          message: this.language === 'vi' ? 'Prompt khá dài' : 'Prompt is quite long',
          suggestion: this.language === 'vi'
            ? 'Rút gọn lại để tập trung vào thông điệp chính'
            : 'Consider making it more concise to focus on key message'
        })
        qualityScore -= 10
      } else {
        qualityScore += 10
      }

      // Check for call-to-action
      if (!hasCallToAction) {
        suggestions.push(
          this.language === 'vi'
            ? 'Thêm lời kêu gọi hành động (VD: "Mua ngay", "Đăng ký ngay", "Tìm hiểu thêm")'
            : 'Add a clear call-to-action (e.g., "Buy now", "Sign up", "Learn more")'
        )
        qualityScore -= 10
      } else {
        qualityScore += 10
      }

      // Check for specificity (numbers, percentages)
      if (!hasNumbers && !hasBenefits) {
        suggestions.push(
          this.language === 'vi'
            ? 'Thêm con số cụ thể hoặc lợi ích rõ ràng (VD: "giảm 50%", "miễn phí", "chỉ 99k")'
            : 'Add specific numbers or clear benefits (e.g., "50% off", "free shipping", "only $99")'
        )
        qualityScore -= 5
      } else {
        qualityScore += 5
      }

      // General best practice suggestions
      const generalTips = this.language === 'vi' ? [
        'Nhấn mạnh lợi ích cho khách hàng, không chỉ tính năng sản phẩm',
        'Tạo cảm giác khẩn cấp (thời gian có hạn, số lượng giới hạn)',
        'Sử dụng ngôn ngữ đơn giản, dễ hiểu',
        'Làm nổi bật điểm khác biệt so với đối thủ'
      ] : [
        'Emphasize customer benefits, not just product features',
        'Create urgency (limited time, limited quantity)',
        'Use simple, easy-to-understand language',
        'Highlight what makes you different from competitors'
      ]

      // Add 2-3 random tips to avoid repetition
      const shuffledTips = generalTips.sort(() => 0.5 - Math.random()).slice(0, 3)
      suggestions.push(...shuffledTips)

      // Determine quality level
      let qualityLevel
      if (qualityScore >= 85) qualityLevel = 'excellent'
      else if (qualityScore >= 70) qualityLevel = 'good'
      else if (qualityScore >= 50) qualityLevel = 'fair'
      else qualityLevel = 'poor'

      return {
        valid: true,
        qualityScore: Math.min(100, Math.max(0, qualityScore)),
        qualityLevel,
        issues,
        suggestions: suggestions.length > 0 ? suggestions : undefined
      }
    },


    hideValidation() {
      this.showValidation = false
      this.validationResult = null
    },

    capitalizeFirst(text) {
      if (!text) return ''
      return text.charAt(0).toUpperCase() + text.slice(1)
    },

    getQualityBadgeClass(level) {
      const classes = {
        'excellent': 'bg-green-100 text-green-800',
        'good': 'bg-blue-100 text-blue-800',
        'fair': 'bg-yellow-100 text-yellow-800',
        'poor': 'bg-red-100 text-red-800',
        'error': 'bg-gray-100 text-gray-800'
      }
      return classes[level] || classes['poor']
    },

    getIssueClass(severity) {
      return severity === 'error' 
        ? 'bg-red-50 border border-red-200' 
        : 'bg-yellow-50 border border-yellow-200'
    },

    getIssueIconClass(severity) {
      return severity === 'error' ? 'text-red-500' : 'text-yellow-500'
    },

    // Public method to manually trigger validation
    manualValidate() {
      if (this.shouldValidate) {
        this.validatePrompt()
      }
    }
  }
}
</script>

<style scoped>
.prompt-validator {
  margin-top: 8px;
}

.validation-feedback {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.validation-feedback .animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>