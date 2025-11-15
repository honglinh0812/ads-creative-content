import i18n from '@/i18n'

const MAX_HISTORY = 30

// Toast notification store module
const state = {
  toasts: []
}

const mutations = {
  ADD_TOAST(state, toast) {
    state.toasts.unshift(toast)
    if (state.toasts.length > MAX_HISTORY) {
      state.toasts.pop()
    }
  },

  DISMISS_TOAST(state, toastId) {
    const toast = state.toasts.find(t => t.id === toastId)
    if (toast) {
      toast.dismissed = true
    }
  },

  MARK_TOAST_READ(state, toastId) {
    const toast = state.toasts.find(t => t.id === toastId)
    if (toast) {
      toast.read = true
    }
  },

  MARK_ALL_AS_READ(state) {
    state.toasts.forEach(toast => {
      toast.read = true
    })
  },

  CLEAR_TOASTS(state) {
    state.toasts = []
  }
}

const translate = (key, params) => {
  if (i18n?.global?.t) {
    return i18n.global.t(key, params)
  }
  return key
}

const actions = {
  showToast({ commit }, toast) {
    const id = Date.now() + Math.random()
    const payload = {
      id,
      type: toast.type || 'info',
      title: toast.title,
      message: toast.message,
      duration: toast.duration ?? 5000,
      actions: toast.actions || [],
      persistent: toast.persistent || false,
      timestamp: Date.now(),
      read: false,
      dismissed: false
    }

    commit('ADD_TOAST', payload)

    if (payload.duration > 0 && !payload.persistent) {
      setTimeout(() => {
        commit('DISMISS_TOAST', id)
      }, payload.duration)
    }

    return id
  },

  showSuccess({ dispatch }, { title, message, duration = 5000 }) {
    dispatch('showToast', {
      type: 'success',
      title: title || translate('notifications.defaultTitle.success'),
      message,
      duration
    })
  },

  showError({ dispatch }, { title, message, duration = 8000 }) {
    dispatch('showToast', {
      type: 'error',
      title: title || translate('notifications.defaultTitle.error'),
      message,
      duration
    })
  },

  showWarning({ dispatch }, { title, message, duration = 6000 }) {
    dispatch('showToast', {
      type: 'warning',
      title: title || translate('notifications.defaultTitle.warning'),
      message,
      duration
    })
  },

  showInfo({ dispatch }, { title, message, duration = 5000 }) {
    dispatch('showToast', {
      type: 'info',
      title: title || translate('notifications.defaultTitle.info'),
      message,
      duration
    })
  },

  removeToast({ commit }, toastId) {
    commit('DISMISS_TOAST', toastId)
  },

  markToastRead({ commit }, toastId) {
    commit('MARK_TOAST_READ', toastId)
  },

  markAllAsRead({ commit }) {
    commit('MARK_ALL_AS_READ')
  },

  clearAllToasts({ commit }) {
    commit('CLEAR_TOASTS')
  },

  // Alias for compatibility
  clearAll({ commit }) {
    commit('CLEAR_TOASTS')
  },

  // Show toast with custom actions
  showToastWithActions({ dispatch }, { type = 'info', title, message, actions = [], duration = 0 }) {
    dispatch('showToast', {
      type,
      title,
      message,
      actions,
      duration,
      persistent: duration === 0 // If duration is 0, make it persistent
    })
  }
}

const getters = {
  toasts: state => state.toasts,
  hasToasts: state => state.toasts.length > 0,
  activeToasts: state => state.toasts.filter(t => !t.dismissed),
  unreadCount: state => state.toasts.filter(t => !t.read).length
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
