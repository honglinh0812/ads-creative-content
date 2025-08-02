<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="toast" tag="div">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'toast',
            `toast-${toast.type}`
          ]"
        >
          <div class="toast-icon">
            <component :is="getIcon(toast.type)" />
          </div>
          
          <div class="toast-content">
            <div v-if="toast.title" class="toast-title">
              {{ toast.title }}
            </div>
            <div class="toast-message">
              {{ toast.message }}
            </div>
          </div>
          
          <div v-if="toast.actions && toast.actions.length > 0" class="toast-actions">
            <button
              v-for="action in toast.actions"
              :key="action.label"
              @click="handleAction(action, toast)"
              class="toast-action-btn"
              :class="action.style || 'primary'"
            >
              {{ action.label }}
            </button>
          </div>
          
          <button
            @click="removeToast(toast.id)"
            class="toast-close"
            aria-label="Close notification"
          >
            <svg viewBox="0 0 20 20" fill="currentColor">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script>
import { inject, computed } from 'vue'
import { CheckCircleIcon, XCircleIcon, ExclamationTriangleIcon, InformationCircleIcon } from '@heroicons/vue/24/outline'

export default {
  name: 'ToastContainer',
  setup() {
    const toastService = inject('toast')
    // Nếu toastService null, trả về mảng rỗng để tránh lỗi
    const toasts = computed(() => toastService ? toastService.getToasts() : [])
    
    const getIcon = (type) => {
      const icons = {
        success: CheckCircleIcon,
        error: XCircleIcon,
        warning: ExclamationTriangleIcon,
        info: InformationCircleIcon
      }
      return icons[type] || icons.info
    }
    
    const removeToast = (id) => {
      toastService.remove(id)
    }
    
    const handleAction = (action, toast) => {
      if (action.handler) {
        action.handler(toast)
      }
      
      if (action.closeOnClick !== false) {
        removeToast(toast.id)
      }
    }
    
    return {
      toasts,
      getIcon,
      removeToast,
      handleAction
    }
  }
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: var(--space-4);
  right: var(--space-4);
  z-index: var(--z-50, 50);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-width: 400px;
  width: 100%;
  pointer-events: none;
}

.toast {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  padding: var(--space-4);
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  pointer-events: auto;
  position: relative;
  overflow: hidden;
}

.toast::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.toast-success::before {
  background: var(--success-500);
}

.toast-error::before {
  background: var(--error-500);
}

.toast-warning::before {
  background: var(--warning-500);
}

.toast-info::before {
  background: var(--primary-500);
}

.toast-icon {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.toast-success .toast-icon {
  color: var(--success-600);
}

.toast-error .toast-icon {
  color: var(--error-600);
}

.toast-warning .toast-icon {
  color: var(--warning-600);
}

.toast-info .toast-icon {
  color: var(--primary-600);
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-weight: var(--font-semibold);
  font-size: var(--text-sm);
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.toast-message {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  word-wrap: break-word;
}

.toast-actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.toast-action-btn {
  padding: var(--space-1) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: var(--transition-colors);
}

.toast-action-btn.primary {
  background: var(--brand-primary);
  color: white;
}

.toast-action-btn.primary:hover {
  background: var(--brand-primary-hover);
}

.toast-action-btn.secondary {
  background: var(--color-bg-tertiary);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}

.toast-action-btn.secondary:hover {
  background: var(--color-hover);
}

.toast-close {
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  transition: var(--transition-colors);
  flex-shrink: 0;
  width: 0.375rem;
  height: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast-close:hover {
  background: var(--color-hover);
  color: var(--color-text);
}

.toast-close svg {
  width: 0.25rem;
  height: 0.25rem;
}

/* Toast Transitions */
.toast-enter-active {
  transition: all 0.3s ease-out;
}

.toast-leave-active {
  transition: all 0.3s ease-in;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.toast-move {
  transition: transform 0.3s ease;
}

/* Responsive */
@media (max-width: 640px) {
  .toast-container {
    left: var(--space-4);
    right: var(--space-4);
    max-width: none;
  }
  
  .toast {
    padding: var(--space-3);
  }
  
  .toast-actions {
    flex-direction: column;
  }
  
  .toast-action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
