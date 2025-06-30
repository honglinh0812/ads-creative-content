<template>
  <teleport to="body">
    <div class="toast-container">
      <transition-group name="toast" tag="div">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="getToastClass(toast.type)"
          class="toast"
        >
          <div class="toast-content">
            <div class="toast-icon">
              <svg v-if="toast.type === 'success'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <svg v-else-if="toast.type === 'error'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <svg v-else-if="toast.type === 'warning'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
              <svg v-else-if="toast.type === 'info'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
            
            <div class="toast-text">
              <div v-if="toast.title" class="toast-title">{{ toast.title }}</div>
              <div class="toast-message">{{ toast.message }}</div>
            </div>
            
            <button @click="removeToast(toast.id)" class="toast-close">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
          
          <!-- Progress bar for auto-dismiss -->
          <div v-if="toast.duration > 0" class="toast-progress">
            <div 
              class="toast-progress-bar"
              :style="{ animationDuration: toast.duration + 'ms' }"
            ></div>
          </div>
        </div>
      </transition-group>
    </div>
  </teleport>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'ToastNotifications',
  computed: {
    ...mapState('toast', ['toasts'])
  },
  methods: {
    ...mapActions('toast', ['removeToast']),
    
    getToastClass(type) {
      const baseClass = 'toast'
      switch (type) {
        case 'success':
          return `${baseClass} toast-success`
        case 'error':
          return `${baseClass} toast-error`
        case 'warning':
          return `${baseClass} toast-warning`
        case 'info':
          return `${baseClass} toast-info`
        default:
          return `${baseClass} toast-info`
      }
    }
  }
}
</script>

<style scoped>
.toast-container {
  @apply fixed top-4 right-4 z-50 space-y-3;
  max-width: 400px;
}

.toast {
  @apply bg-white rounded-lg shadow-lg border overflow-hidden;
  min-width: 300px;
}

.toast-success {
  @apply border-success-200;
}

.toast-error {
  @apply border-error-200;
}

.toast-warning {
  @apply border-warning-200;
}

.toast-info {
  @apply border-info-200;
}

.toast-content {
  @apply flex items-start gap-3 p-4;
}

.toast-icon {
  @apply flex-shrink-0 w-6 h-6 rounded-full flex items-center justify-center;
}

.toast-success .toast-icon {
  @apply bg-success-100 text-success-600;
}

.toast-error .toast-icon {
  @apply bg-error-100 text-error-600;
}

.toast-warning .toast-icon {
  @apply bg-warning-100 text-warning-600;
}

.toast-info .toast-icon {
  @apply bg-info-100 text-info-600;
}

.toast-text {
  @apply flex-1 min-w-0;
}

.toast-title {
  @apply font-semibold text-secondary-900 text-sm mb-1;
}

.toast-message {
  @apply text-secondary-700 text-sm leading-relaxed;
}

.toast-close {
  @apply flex-shrink-0 p-1 hover:bg-neutral-100 rounded-lg transition-colors text-secondary-400 hover:text-secondary-600;
}

.toast-progress {
  @apply h-1 bg-neutral-100 relative overflow-hidden;
}

.toast-progress-bar {
  @apply h-full bg-current absolute top-0 left-0;
  width: 100%;
  animation: toast-progress linear forwards;
}

.toast-success .toast-progress-bar {
  @apply text-success-500;
}

.toast-error .toast-progress-bar {
  @apply text-error-500;
}

.toast-warning .toast-progress-bar {
  @apply text-warning-500;
}

.toast-info .toast-progress-bar {
  @apply text-info-500;
}

@keyframes toast-progress {
  from {
    transform: translateX(0%);
  }
  to {
    transform: translateX(-100%);
  }
}

/* Transition animations */
.toast-enter-active {
  transition: all 0.3s ease-out;
}

.toast-leave-active {
  transition: all 0.3s ease-in;
}

.toast-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.toast-move {
  transition: transform 0.3s ease;
}

@media (max-width: 640px) {
  .toast-container {
    @apply left-4 right-4 top-4;
    max-width: none;
  }
  
  .toast {
    min-width: auto;
  }
}
</style>

