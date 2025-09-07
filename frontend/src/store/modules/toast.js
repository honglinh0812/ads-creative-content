// Toast notification store module
const state = {
  toasts: []
}

const mutations = {
  ADD_TOAST(state, toast) {
    const id = Date.now() + Math.random()
    const fullToast = {
      id,
      ...toast,
      timestamp: Date.now()
    }
    state.toasts.push(fullToast)
    const duration = toast.duration || 5000
    if (duration > 0) {
      setTimeout(() => {
        state.toasts = state.toasts.filter(t => t.id !== id)
      }, duration)
    }
  },
  
  REMOVE_TOAST(state, toastId) {
    state.toasts = state.toasts.filter(toast => toast.id !== toastId)
  },
  
  CLEAR_TOASTS(state) {
    state.toasts = []
  }
}

const actions = {
  showToast({ commit }, toast) {
    commit('ADD_TOAST', toast)
    
    // Auto remove after duration
    const duration = toast.duration || 5000
    if (duration > 0) {
      setTimeout(() => {
        commit('REMOVE_TOAST', toast.id)
      }, duration)
    }
  },
  
  showSuccess({ dispatch }, { title, message, duration = 5000 }) {
    dispatch('showToast', {
      type: 'success',
      title: title || 'Success',
      message,
      duration
    })
  },
  
  showError({ dispatch }, { title, message, duration = 8000 }) {
    dispatch('showToast', {
      type: 'error',
      title: title || 'Error',
      message,
      duration
    })
  },
  
  showWarning({ dispatch }, { title, message, duration = 6000 }) {
    dispatch('showToast', {
      type: 'warning',
      title: title || 'Warning',
      message,
      duration
    })
  },
  
  showInfo({ dispatch }, { title, message, duration = 5000 }) {
    dispatch('showToast', {
      type: 'info',
      title: title || 'Info',
      message,
      duration
    })
  },
  
  removeToast({ commit }, toastId) {
    commit('REMOVE_TOAST', toastId)
  },
  
  clearAllToasts({ commit }) {
    commit('CLEAR_TOASTS')
  }
}

const getters = {
  toasts: state => state.toasts,
  hasToasts: state => state.toasts.length > 0
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}

