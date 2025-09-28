import api from '../../services/api'
import { setSentryUser, clearSentryUser } from '../../plugins/sentry'

export default {
  namespaced: true,
  state: {
    user: null,
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null
  },
  getters: {
    isAuthenticated: state => !!state.token,
    user: state => state.user,
    loading: state => state.loading,
    error: state => state.error
  },
  mutations: {
    SET_USER(state, user) {
      state.user = user;
      
      // Set user context in Sentry for error tracking
      if (user) {
        setSentryUser({
          id: user.id,
          email: user.email,
          username: user.name || user.username,
        });
      }
    },
    SET_TOKEN(state, token) {
      state.token = token;
      if (token) {
        localStorage.setItem('token', token);
      } else {
        localStorage.removeItem('token');
      }
    },
    SET_LOADING(state, loading) {
      state.loading = loading;
    },
    SET_ERROR(state, error) {
      state.error = error;
    },
    CLEAR_AUTH(state) {
      state.user = null;
      state.token = null;
      state.error = null;
      localStorage.removeItem('token');
      
      // Clear user context from Sentry
      clearSentryUser();
    }
  },
  actions: {
    async login({ commit, dispatch }) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      
      try {
        // Redirect to Facebook OAuth login
        window.location.href = api.auth.login();
      } catch (error) {
        commit('SET_ERROR', error.message || 'Authentication failed');
        dispatch('toast/showError', {
          title: 'Login Failed',
          message: 'Failed to authenticate with Facebook. Please try again.'
        }, { root: true });
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async fetchUser({ commit }) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      try {
        console.log('Fetching user data...');
        const response = await api.auth.getUser();
        console.log('User data response:', response.data);
        
        commit('SET_USER', response.data);
        return response.data;
      } catch (error) {
        console.error('Fetch user error:', error);
        commit('SET_ERROR', error.message || 'Failed to fetch user data');
        
        // If token is invalid, clear authentication
        if (error.response && error.response.status === 401) {
          commit('CLEAR_AUTH');
        }
        
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async logout({ commit }) {
      commit('SET_LOADING', true);
      
      try {
        await api.auth.logout();
      } catch (error) {
        console.error('Logout error:', error);
      } finally {
        commit('CLEAR_AUTH');
        commit('SET_LOADING', false);
        window.location.href = '/';
      }
    },
    
    setToken({ commit }, token) {
      commit('SET_TOKEN', token);
    },
    
    setUser({ commit }, user) {
      commit('SET_USER', user);
    },
    
    clearAuth({ commit }) {
      commit('CLEAR_AUTH');
    }
  }
}

