import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// Ant Design Vue
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

// Global CSS
import './assets/styles/main.scss'

// Toast Service
import ToastService from './services/toastService.js'

// API Service
import ApiService from './services/apiService.js'

// Sentry Error Monitoring
import { initSentry } from './plugins/sentry.js'

// i18n
import i18n from './i18n'

const app = createApp(App)

// Initialize Sentry (must be done before other configurations)
initSentry(app, router)

// Use plugins
app.use(store)
app.use(router)
app.use(i18n)
app.use(Antd)
app.use(ToastService)
app.use(ApiService)

// Expose store globally for API interceptors
window.store = store

// Ant Design Vue is globally registered, no need for individual component registration

// Global error handler (Sentry will also capture these errors)
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Info:', info)
  
  // Show user-friendly error message
  store.dispatch('toast/showError', {
    title: 'Application Error',
    message: 'An unexpected error occurred. Please try again later.'
  })
  
  // Sentry error handler is set up in plugins/sentry.js
}

// All Ant Design Vue components are available globally

// Initialize authentication before mounting app
async function initializeApp() {
  try {
    // Initialize theme from localStorage or system preference
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme) {
      store.commit('SET_THEME', savedTheme)
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      store.commit('SET_THEME', 'dark')
    } else {
      store.commit('SET_THEME', 'light')
    }

    // Listen for system theme changes
    if (window.matchMedia) {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        // Only auto-switch if user hasn't manually set a theme
        if (!localStorage.getItem('theme')) {
          store.commit('SET_THEME', e.matches ? 'dark' : 'light')
        }
      })
    }

    // Set loading state
    store.commit('auth/SET_LOADING', true)

    // Check if there's a token in localStorage
    const token = localStorage.getItem('token')
    if (token) {
      // Set token in store
      store.commit('auth/SET_TOKEN', token)

      // Try to fetch user data to validate token
      try {
        await store.dispatch('auth/fetchUser')
        console.log('User authenticated successfully')
      } catch (error) {
        console.log('Token invalid, clearing authentication')
        // Token is invalid, clear it
        store.dispatch('auth/clearAuth')
      }
    }
  } catch (error) {
    console.error('Error during app initialization:', error)
  } finally {
    // Clear loading state
    store.commit('auth/SET_LOADING', false)
  }

  // Mount app after authentication check
  app.mount("#app")
}

// Start the app
initializeApp()

