import { reactive } from 'vue'

// Toast notification service
class ToastService {
  constructor() {
    this.toasts = reactive([])
    this.nextId = 1
  }

  show(message, type = 'info', options = {}) {
    const toast = {
      id: this.nextId++,
      message,
      type,
      title: options.title,
      duration: options.duration || 5000,
      persistent: options.persistent || false,
      actions: options.actions || [],
      timestamp: Date.now()
    }

    this.toasts.push(toast)

    // Auto-remove toast after duration (unless persistent)
    if (!toast.persistent && toast.duration > 0) {
      setTimeout(() => {
        this.remove(toast.id)
      }, toast.duration)
    }

    return toast.id
  }

  success(message, options = {}) {
    return this.show(message, 'success', {
      title: 'Success',
      ...options
    })
  }

  error(message, options = {}) {
    return this.show(message, 'error', {
      title: 'Error',
      duration: 8000, // Longer duration for errors
      ...options
    })
  }

  warning(message, options = {}) {
    return this.show(message, 'warning', {
      title: 'Warning',
      duration: 6000,
      ...options
    })
  }

  info(message, options = {}) {
    return this.show(message, 'info', {
      title: 'Info',
      ...options
    })
  }

  remove(id) {
    const index = this.toasts.findIndex(toast => toast.id === id)
    if (index > -1) {
      this.toasts.splice(index, 1)
    }
  }

  clear() {
    this.toasts.splice(0)
  }

  // Get all toasts (reactive)
  getToasts() {
    return this.toasts
  }
}

// Create singleton instance
const toastService = new ToastService()

// Vue plugin
export default {
  install(app) {
    app.config.globalProperties.$toast = toastService
    app.provide('toast', toastService)
  }
}

export { toastService }
