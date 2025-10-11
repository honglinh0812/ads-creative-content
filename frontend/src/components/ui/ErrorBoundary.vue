<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">
        <svg class="w-16 h-16 text-error-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
      </div>

      <div class="error-details">
        <h2 class="error-title">Oops! Something went wrong</h2>
        <p class="error-message">{{ userFriendlyMessage }}</p>

        <div v-if="showDetails" class="error-technical">
          <details class="error-expandable">
            <summary class="error-summary">Technical Details</summary>
            <div class="error-stack">
              <code>{{ error.message }}</code>
              <pre v-if="error.stack">{{ error.stack }}</pre>
            </div>
          </details>
        </div>
      </div>

      <div class="error-actions">
        <a-button type="primary" @click="handleRetry" :loading="retrying">
          <template #icon><reload-outlined /></template>
          Try Again
        </a-button>

        <a-button @click="handleGoHome">
          <template #icon><home-outlined /></template>
          Go to Dashboard
        </a-button>

        <a-button type="text" @click="handleReport" v-if="enableReporting">
          <template #icon><bug-outlined /></template>
          Report Issue
        </a-button>
      </div>
    </div>
  </div>

  <div v-else>
    <slot />
  </div>
</template>

<script>
import { ReloadOutlined, HomeOutlined, BugOutlined } from '@ant-design/icons-vue'

export default {
  name: 'ErrorBoundary',
  components: {
    ReloadOutlined,
    HomeOutlined,
    BugOutlined
  },
  props: {
    fallbackMessage: {
      type: String,
      default: 'We encountered an unexpected error. Please try refreshing the page.'
    },
    showDetails: {
      type: Boolean,
      default: false
    },
    enableReporting: {
      type: Boolean,
      default: true
    },
    onRetry: {
      type: Function,
      default: null
    },
    onReport: {
      type: Function,
      default: null
    }
  },
  data() {
    return {
      error: null,
      retrying: false
    }
  },
  computed: {
    userFriendlyMessage() {
      if (!this.error) return ''

      // Map common errors to user-friendly messages
      const errorMappings = {
        'Network Error': 'Unable to connect to the server. Please check your internet connection.',
        'TypeError': 'A technical issue occurred. Our team has been notified.',
        'ReferenceError': 'A component failed to load properly. Please refresh the page.',
        'ChunkLoadError': 'Failed to load application resources. Please refresh the page.',
        'timeout': 'The request took too long to complete. Please try again.',
        '404': 'The requested resource was not found.',
        '500': 'Server error occurred. Please try again later.',
        '403': 'You do not have permission to access this resource.',
        '401': 'Please log in to continue.'
      }

      const errorMessage = this.error.message || ''

      for (const [key, message] of Object.entries(errorMappings)) {
        if (errorMessage.includes(key) || this.error.name === key) {
          return message
        }
      }

      return this.fallbackMessage
    }
  },
  errorCaptured(error, instance, info) {
    console.error('Error captured by ErrorBoundary:', error, info)
    this.error = error

    // Report error to monitoring service
    this.reportError(error, info)

    return false // Prevent error from propagating
  },
  methods: {
    async handleRetry() {
      this.retrying = true

      try {
        if (this.onRetry) {
          await this.onRetry()
        } else {
          // Default retry: reload the page
          window.location.reload()
        }

        this.error = null
      } catch (retryError) {
        console.error('Retry failed:', retryError)
        this.$message.error('Retry failed. Please refresh the page manually.')
      } finally {
        this.retrying = false
      }
    },

    handleGoHome() {
      this.$router.push('/dashboard').catch(() => {
        window.location.href = '/dashboard'
      })
    },

    async handleReport() {
      try {
        if (this.onReport) {
          await this.onReport(this.error)
        } else {
          // Default reporting
          await this.reportError(this.error, 'user-reported')
        }

        this.$message.success('Error report sent. Thank you!')
      } catch (reportError) {
        console.error('Failed to report error:', reportError)
        this.$message.error('Failed to send error report.')
      }
    },

    async reportError(error, context) {
      // In a real app, this would send to error monitoring service
      // For now, just log to console
      const errorReport = {
        message: error.message,
        stack: error.stack,
        name: error.name,
        context,
        url: window.location.href,
        userAgent: navigator.userAgent,
        timestamp: new Date().toISOString()
      }

      console.warn('Error Report:', errorReport)

      // Example: Send to monitoring service
      // await fetch('/api/errors', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(errorReport)
      // })
    },

    clearError() {
      this.error = null
    }
  }
}
</script>

<style scoped>
.error-boundary {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: theme('colors.gray.50');
  border-radius: 1rem;
}

.dark .error-boundary {
  background: theme('colors.gray.900');
}

.error-content {
  max-width: 500px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.error-icon {
  opacity: 0.8;
}

.error-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.error-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: theme('colors.gray.900');
  margin: 0;
}

.dark .error-title {
  color: theme('colors.gray.100');
}

.error-message {
  font-size: 1rem;
  color: theme('colors.gray.600');
  line-height: 1.6;
  margin: 0;
}

.dark .error-message {
  color: theme('colors.gray.400');
}

.error-technical {
  margin-top: 1rem;
  text-align: left;
}

.error-expandable {
  border: 1px solid theme('colors.gray.200');
  border-radius: 0.5rem;
  padding: 0.75rem;
  background: theme('colors.white');
}

.dark .error-expandable {
  border-color: theme('colors.gray.700');
  background: theme('colors.gray.800');
}

.error-summary {
  cursor: pointer;
  font-weight: 500;
  color: theme('colors.gray.700');
  user-select: none;
}

.dark .error-summary {
  color: theme('colors.gray.300');
}

.error-summary:hover {
  color: theme('colors.primary.600');
}

.dark .error-summary:hover {
  color: theme('colors.primary.400');
}

.error-stack {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid theme('colors.gray.200');
}

.dark .error-stack {
  border-top-color: theme('colors.gray.600');
}

.error-stack code {
  display: block;
  padding: 0.5rem;
  background: theme('colors.gray.100');
  border-radius: 0.25rem;
  font-size: 0.875rem;
  color: theme('colors.error.700');
  word-break: break-word;
}

.dark .error-stack code {
  background: theme('colors.gray.700');
  color: theme('colors.error.400');
}

.error-stack pre {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: theme('colors.gray.100');
  border-radius: 0.25rem;
  font-size: 0.75rem;
  color: theme('colors.gray.600');
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.dark .error-stack pre {
  background: theme('colors.gray.700');
  color: theme('colors.gray.400');
}

.error-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: center;
}

/* Mobile responsive */
@media (max-width: 640px) {
  .error-boundary {
    padding: 1rem;
    min-height: 300px;
  }

  .error-content {
    gap: 1rem;
  }

  .error-title {
    font-size: 1.25rem;
  }

  .error-message {
    font-size: 0.875rem;
  }

  .error-actions {
    flex-direction: column;
    width: 100%;
  }

  .error-actions .ant-btn {
    width: 100%;
  }
}

/* High contrast support */
@media (prefers-contrast: high) {
  .error-boundary {
    border: 2px solid theme('colors.gray.400');
  }

  .error-expandable {
    border-width: 2px;
  }
}

/* Accessibility improvements */
.error-summary:focus-visible {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 2px;
  border-radius: 0.25rem;
}

/* Animation for error appearance */
.error-boundary {
  animation: errorFadeIn 0.3s ease-out;
}

@keyframes errorFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Reduce motion support */
@media (prefers-reduced-motion: reduce) {
  .error-boundary {
    animation: none;
  }
}
</style>