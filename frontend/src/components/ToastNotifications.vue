<template>
  <teleport to="body">
    <div class="toast-container">
      <transition-group name="toast" tag="div">
        <div
          v-for="toast in activeToasts"
          :key="toast.id"
          :class="getToastClass(toast.type)"
          class="toast bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 shadow-lg hover:shadow-lg transition-shadow duration-200 ease-out"
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
              <div v-if="toast.title" class="toast-title text-neutral-900 dark:text-neutral-100">{{ toast.title }}</div>
              <div class="toast-message text-neutral-700 dark:text-neutral-300">{{ toast.message }}</div>

              <!-- Action Buttons -->
              <div v-if="toast.actions && toast.actions.length" class="toast-actions">
                <button
                  v-for="(action, index) in toast.actions"
                  :key="index"
                  @click="handleAction(toast.id, action)"
                  :class="['toast-action-btn', action.primary ? 'action-primary' : 'action-secondary']"
                >
                  {{ action.label }}
                </button>
              </div>
            </div>

            <button
              @click="dismissToast(toast.id)"
              class="toast-close text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200 hover:bg-neutral-100 dark:hover:bg-neutral-700 transition-all duration-200"
              :aria-label="$t('notifications.toast.close')"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
          
          <!-- Progress bar for auto-dismiss -->
          <div v-if="toast.duration > 0" class="toast-progress bg-neutral-100 dark:bg-neutral-700">
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
import { mapGetters, mapActions } from 'vuex'

export default {
  name: 'ToastNotifications',
  computed: {
    ...mapGetters('toast', ['activeToasts'])
  },
  methods: {
    ...mapActions('toast', ['removeToast', 'markToastRead']),

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
    },

    handleAction(toastId, action) {
      // Execute the action callback
      if (action.handler && typeof action.handler === 'function') {
        action.handler()
      }

      // Remove toast if action is configured to do so
      if (action.dismissOnClick !== false) {
        this.dismissToast(toastId)
      }
    },

    dismissToast(toastId) {
      this.markToastRead(toastId)
      this.removeToast(toastId)
    }
  }
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  left: auto;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.75rem;
  max-width: 24rem;
  width: 100%;
  pointer-events: none;
}

.toast {
  border-radius: 1rem;
  min-width: 20rem;
  font-size: 0.875rem;
  font-weight: 500;
  pointer-events: auto;
  position: relative;
  overflow: hidden;
}

.toast::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 1;
}

/* Toast type styles */
.toast-success::before {
  background: theme('colors.success.500');
}

.toast-error::before {
  background: theme('colors.error.500');
}

.toast-warning::before {
  background: theme('colors.warning.500');
}

.toast-info::before {
  background: theme('colors.primary.500');
}

.toast-content {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  position: relative;
  z-index: 2;
}

.toast-icon {
  flex-shrink: 0;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
}

/* Removed scale transform effects */

.toast-success .toast-icon {
  background: theme('colors.success.100');
  color: theme('colors.success.600');
}

.dark .toast-success .toast-icon {
  background: theme('colors.success.900');
  color: theme('colors.success.400');
}

.toast-error .toast-icon {
  background: theme('colors.error.100');
  color: theme('colors.error.600');
}

.dark .toast-error .toast-icon {
  background: theme('colors.error.900');
  color: theme('colors.error.400');
}

.toast-warning .toast-icon {
  background: theme('colors.warning.100');
  color: theme('colors.warning.600');
}

.dark .toast-warning .toast-icon {
  background: theme('colors.warning.900');
  color: theme('colors.warning.400');
}

.toast-info .toast-icon {
  background: theme('colors.primary.100');
  color: theme('colors.primary.600');
}

.dark .toast-info .toast-icon {
  background: theme('colors.primary.900');
  color: theme('colors.primary.400');
}

.toast-text {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
  line-height: 1.25;
}

.toast-message {
  font-size: 0.8125rem;
  line-height: 1.5;
  margin-bottom: 0.5rem;
}

/* Action buttons */
.toast-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
  flex-wrap: wrap;
}

.toast-action-btn {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.375rem 0.875rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
  line-height: 1;
  min-height: 28px;
}

.toast-action-btn.action-primary {
  background: theme('colors.primary.500');
  color: white;
}

.toast-action-btn.action-primary:hover {
  background: theme('colors.primary.600');
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(45, 90, 160, 0.3);
}

.toast-action-btn.action-primary:active {
  transform: translateY(0);
}

.toast-action-btn.action-secondary {
  background: theme('colors.neutral.100');
  color: theme('colors.neutral.700');
  border: 1px solid theme('colors.neutral.300');
}

.toast-action-btn.action-secondary:hover {
  background: theme('colors.neutral.200');
  border-color: theme('colors.neutral.400');
}

.dark .toast-action-btn.action-secondary {
  background: theme('colors.neutral.700');
  color: theme('colors.neutral.200');
  border-color: theme('colors.neutral.600');
}

.dark .toast-action-btn.action-secondary:hover {
  background: theme('colors.neutral.600');
  border-color: theme('colors.neutral.500');
}

.toast-close {
  flex-shrink: 0;
  background: none;
  border: none;
  border-radius: 0.5rem;
  padding: 0.375rem;
  cursor: pointer;
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Progress bar */
.toast-progress {
  height: 3px;
  position: relative;
  overflow: hidden;
}

.toast-progress-bar {
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  animation: toast-progress linear forwards;
  border-radius: 0 0 1rem 1rem;
}

.toast-success .toast-progress-bar {
  background: theme('colors.success.500');
}

.toast-error .toast-progress-bar {
  background: theme('colors.error.500');
}

.toast-warning .toast-progress-bar {
  background: theme('colors.warning.500');
}

.toast-info .toast-progress-bar {
  background: theme('colors.primary.500');
}

@keyframes toast-progress {
  from {
    transform: translateX(0%);
  }

  to {
    transform: translateX(-100%);
  }
}

/* Simplified toast animations */
.toast-enter-active {
  transition: all 0.3s ease-out;
}

.toast-leave-active {
  transition: all 0.2s ease-in;
}

.toast-enter-from {
  transform: translateX(100%) scale(0.95);
  opacity: 0;
}

.toast-leave-to {
  transform: translateX(100%) scale(0.95);
  opacity: 0;
}

.toast-move {
  transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

/* Responsive design */
@media (width <= 768px) {
  .toast-container {
    left: 1rem;
    right: 1rem;
    top: 1rem;
    max-width: none;
  }
  
  .toast {
    min-width: auto;
  }
  
  .toast-content {
    padding: 0.875rem 1rem;
  }
}

@media (width <= 480px) {
  .toast-container {
    left: 0.5rem;
    right: 0.5rem;
    top: 0.5rem;
  }
  
  .toast-content {
    gap: 0.5rem;
    padding: 0.75rem;
  }
  
  .toast-icon {
    width: 1.75rem;
    height: 1.75rem;
  }
  
  .toast-title {
    font-size: 0.8125rem;
  }
  
  .toast-message {
    font-size: 0.75rem;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .toast-enter-active,
  .toast-leave-active,
  .toast-move {
    transition: none;
  }
  
  .toast:hover .toast-icon {
    transform: none;
  }
  
  .toast-progress-bar {
    animation: none;
  }
}
</style>
