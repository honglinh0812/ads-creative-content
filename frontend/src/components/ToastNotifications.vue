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
              <svg v-if="toast.type === 'success'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <svg v-else-if="toast.type === 'error'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
              <svg v-else-if="toast.type === 'warning'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
              <svg v-else-if="toast.type === 'info'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
            
            <div class="toast-text">
              <div v-if="toast.title" class="toast-title">{{ toast.title }}</div>
              <div class="toast-message">{{ toast.message }}</div>
            </div>
            
            <button @click="removeToast(toast.id)" class="toast-close">
              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 400px;
  pointer-events: none;
}

.toast {
  background: rgba(255,255,255,0.98);
  border-radius: 0.75rem;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18), 0 1.5px 6px rgba(0,0,0,0.08);
  border: 1.5px solid #e5e7eb;
  min-width: 320px;
  font-size: 1.08rem;
  color: #22292f;
  font-weight: 500;
  pointer-events: auto;
  transition: box-shadow 0.2s;
}

.toast-success {
  border-left: 5px solid #10b981;
  background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
}
.toast-error {
  border-left: 5px solid #ef4444;
  background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
}
.toast-warning {
  border-left: 5px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
}
.toast-info {
  border-left: 5px solid #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
}
.toast-content {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.1rem 1.3rem 1.1rem 1.1rem;
}
.toast-icon {
  flex-shrink: 0;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  background: #f3f4f6;
}
.toast-success .toast-icon { background: #bbf7d0; color: #059669; }
.toast-error .toast-icon { background: #fecaca; color: #dc2626; }
.toast-warning .toast-icon { background: #fef08a; color: #d97706; }
.toast-info .toast-icon { background: #dbeafe; color: #2563eb; }
.toast-text {
  flex: 1;
  min-width: 0;
}
.toast-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 1.08rem;
  margin-bottom: 0.15rem;
}
.toast-message {
  color: #334155;
  font-size: 1.01rem;
  line-height: 1.5;
}
.toast-close {
  flex-shrink: 0;
  margin-left: 0.5rem;
  background: none;
  border: none;
  color: #64748b;
  border-radius: 0.5rem;
  padding: 0.25rem;
  cursor: pointer;
  font-size: 1.1rem;
  transition: background 0.15s;
  pointer-events: auto;
}
.toast-close:hover {
  background: #f1f5f9;
  color: #1e293b;
}
.toast-progress {
  height: 4px;
  background: #f1f5f9;
  position: relative;
  overflow: hidden;
}
.toast-progress-bar {
  height: 100%;
  background: currentColor;
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  animation: toast-progress linear forwards;
}
.toast-success .toast-progress-bar { color: #10b981; }
.toast-error .toast-progress-bar { color: #ef4444; }
.toast-warning .toast-progress-bar { color: #f59e0b; }
.toast-info .toast-progress-bar { color: #3b82f6; }
@keyframes toast-progress {
  from { transform: translateX(0%); }
  to { transform: translateX(-100%); }
}
.toast-enter-active { transition: all 0.3s ease-out; }
.toast-leave-active { transition: all 0.3s ease-in; }
.toast-enter-from { transform: translateX(100%); opacity: 0; }
.toast-leave-to { transform: translateX(100%); opacity: 0; }
.toast-move { transition: transform 0.3s ease; }
@media (max-width: 640px) {
  .toast-container {
    left: 0.5rem;
    right: 0.5rem;
    top: 0.5rem;
    max-width: none;
  }
  .toast { min-width: auto; }
}
</style>

