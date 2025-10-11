<template>
  <div :class="containerClass">
    <div :class="spinnerClass">
      <div class="spinner-ring"></div>
      <div class="spinner-ring"></div>
      <div class="spinner-ring"></div>
      <div class="spinner-ring"></div>
    </div>
    <div v-if="showText && text" :class="textClass">
      {{ text }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoadingSpinner',
  props: {
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large', 'xlarge'].includes(value)
    },
    color: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'secondary', 'success', 'warning', 'error', 'white'].includes(value)
    },
    text: {
      type: String,
      default: ''
    },
    showText: {
      type: Boolean,
      default: true
    },
    center: {
      type: Boolean,
      default: false
    },
    overlay: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    containerClass() {
      return [
        'loading-spinner-container',
        {
          'is-centered': this.center,
          'is-overlay': this.overlay
        }
      ]
    },
    spinnerClass() {
      return [
        'loading-spinner',
        `spinner-${this.size}`,
        `spinner-${this.color}`
      ]
    },
    textClass() {
      return [
        'spinner-text',
        `text-${this.size}`,
        `text-${this.color}`
      ]
    }
  }
}
</script>

<style scoped>
.loading-spinner-container {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.loading-spinner-container.is-centered {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.loading-spinner-container.is-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.dark .loading-spinner-container.is-overlay {
  background: rgba(0, 0, 0, 0.9);
}

.loading-spinner {
  position: relative;
  display: inline-block;
}

/* Size variants */
.spinner-small {
  width: 20px;
  height: 20px;
}

.spinner-medium {
  width: 32px;
  height: 32px;
}

.spinner-large {
  width: 48px;
  height: 48px;
}

.spinner-xlarge {
  width: 64px;
  height: 64px;
}

.spinner-ring {
  position: absolute;
  border: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-small .spinner-ring {
  border-width: 2px;
}

.spinner-medium .spinner-ring {
  border-width: 3px;
}

.spinner-large .spinner-ring {
  border-width: 4px;
}

.spinner-xlarge .spinner-ring {
  border-width: 5px;
}

.spinner-ring:nth-child(1) {
  width: 100%;
  height: 100%;
  animation-delay: -0.45s;
}

.spinner-ring:nth-child(2) {
  width: 80%;
  height: 80%;
  top: 10%;
  left: 10%;
  animation-delay: -0.3s;
}

.spinner-ring:nth-child(3) {
  width: 60%;
  height: 60%;
  top: 20%;
  left: 20%;
  animation-delay: -0.15s;
}

.spinner-ring:nth-child(4) {
  width: 40%;
  height: 40%;
  top: 30%;
  left: 30%;
  animation-delay: 0s;
}

/* Color variants */
.spinner-primary .spinner-ring {
  border-top-color: theme('colors.primary.500');
}

.spinner-secondary .spinner-ring {
  border-top-color: theme('colors.gray.500');
}

.spinner-success .spinner-ring {
  border-top-color: theme('colors.success.500');
}

.spinner-warning .spinner-ring {
  border-top-color: theme('colors.warning.500');
}

.spinner-error .spinner-ring {
  border-top-color: theme('colors.error.500');
}

.spinner-white .spinner-ring {
  border-top-color: theme('colors.white');
}

/* Text styles */
.spinner-text {
  font-weight: 500;
  text-align: center;
  user-select: none;
}

.text-small {
  font-size: 0.75rem;
}

.text-medium {
  font-size: 0.875rem;
}

.text-large {
  font-size: 1rem;
}

.text-xlarge {
  font-size: 1.125rem;
}

.text-primary {
  color: theme('colors.primary.600');
}

.text-secondary {
  color: theme('colors.gray.600');
}

.text-success {
  color: theme('colors.success.600');
}

.text-warning {
  color: theme('colors.warning.600');
}

.text-error {
  color: theme('colors.error.600');
}

.text-white {
  color: theme('colors.white');
}

.dark .text-primary {
  color: theme('colors.primary.400');
}

.dark .text-secondary {
  color: theme('colors.gray.400');
}

.dark .text-success {
  color: theme('colors.success.400');
}

.dark .text-warning {
  color: theme('colors.warning.400');
}

.dark .text-error {
  color: theme('colors.error.400');
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Reduce motion support */
@media (prefers-reduced-motion: reduce) {
  .spinner-ring {
    animation: none;
    border-top-color: transparent;
    border-left-color: currentColor;
  }
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .loading-spinner-container {
    gap: 0.5rem;
  }

  .spinner-large {
    width: 40px;
    height: 40px;
  }

  .spinner-xlarge {
    width: 48px;
    height: 48px;
  }
}
</style>