<template>
  <div class="prompt-validator">
    <!-- Validation Status Display -->
    <div v-if="showValidation && validationResult" class="validation-feedback">
      <!-- Quality Score Badge -->
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center space-x-2">
          <span class="text-sm font-medium text-gray-700">Prompt Quality:</span>
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
        <div class="text-sm font-medium text-gray-700 mb-2">💡 Suggestions:</div>
        <ul class="text-sm text-gray-600 space-y-1">
          <li v-for="suggestion in validationResult.suggestions" :key="suggestion" class="flex items-start">
            <span class="mr-2">•</span>
            <span>{{ suggestion }}</span>
          </li>
        </ul>
      </div>

      <!-- Improved Prompt -->
      <div v-if="validationResult.improvedPrompt" class="border-t pt-3">
        <div class="text-sm font-medium text-gray-700 mb-2">✨ Improved Version:</div>
        <div class="bg-blue-50 border border-blue-200 rounded p-3 text-sm">
          <div class="text-gray-700 mb-2">{{ validationResult.improvedPrompt }}</div>
          <button 
            @click="useImprovedPrompt"
            class="text-blue-600 hover:text-blue-800 underline text-xs font-medium"
          >
            Use this improved prompt
          </button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isValidating" class="validation-feedback">
      <div class="flex items-center space-x-2 text-sm text-gray-600">
        <div class="animate-spin w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full"></div>
        <span>Analyzing prompt quality...</span>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/services/api'

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
    async validatePrompt() {
      if (!this.shouldValidate) return

      // Don't validate if prompt is too short (< 10 chars as per backend validation)
      const trimmedPrompt = this.prompt.trim()
      if (trimmedPrompt.length < 10) {
        this.hideValidation()
        return
      }

      this.isValidating = true
      this.showValidation = true

      try {
        const response = await api.post('/prompt/validate', {
          prompt: trimmedPrompt,
          adType: this.adType,
          language: this.language,
          targetAudience: this.targetAudience,
          industry: this.industry
        })

        this.validationResult = response.data
        console.log('Prompt validation result:', this.validationResult)

      } catch (error) {
        console.error('Error validating prompt:', error)

        // Handle 400 validation errors specifically
        if (error.response && error.response.status === 400) {
          // Don't show error UI for validation failures - just hide the validation
          this.hideValidation()
        } else if (error.response && error.response.status === 401) {
          // Authentication error - user not logged in
          console.warn('Authentication required for prompt validation')
          this.hideValidation()
        } else {
          // Other errors - show generic error
          this.validationResult = {
            valid: false,
            qualityScore: 0,
            qualityLevel: 'error',
            issues: [],
            suggestions: ['Unable to validate prompt. Please try again.']
          }
        }
      } finally {
        this.isValidating = false
      }
    },

    useImprovedPrompt() {
      if (this.validationResult && this.validationResult.improvedPrompt) {
        this.$emit('prompt-updated', this.validationResult.improvedPrompt)
        // Re-validate the improved prompt
        this.$nextTick(() => {
          this.debouncedValidate()
        })
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