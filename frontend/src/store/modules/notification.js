export default {
  namespaced: true,
  state: {
    notifications: []
  },
  mutations: {
    ADD_NOTIFICATION(state, notification) {
      state.notifications.push({
        id: Date.now(),
        ...notification
      });
    },
    REMOVE_NOTIFICATION(state, id) {
      state.notifications = state.notifications.filter(n => n.id !== id);
    }
  },
  actions: {
    showSuccess({ commit }, { title, message }) {
      commit('ADD_NOTIFICATION', {
        type: 'success',
        title,
        message,
        autoClose: true
      });
    },
    showError({ commit }, { title, message }) {
      commit('ADD_NOTIFICATION', {
        type: 'error',
        title,
        message,
        autoClose: false
      });
    },
    showInfo({ commit }, { title, message }) {
      commit('ADD_NOTIFICATION', {
        type: 'info',
        title,
        message,
        autoClose: true
      });
    },
    showWarning({ commit }, { title, message }) {
      commit('ADD_NOTIFICATION', {
        type: 'warning',
        title,
        message,
        autoClose: true
      });
    },
    removeNotification({ commit }, id) {
      commit('REMOVE_NOTIFICATION', id);
    }
  }
}
