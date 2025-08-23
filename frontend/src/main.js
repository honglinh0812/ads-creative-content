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

const app = createApp(App)

// Use plugins
app.use(store)
app.use(router)
app.use(Antd)
app.use(ToastService)
app.use(ApiService)

// Ant Design Vue is globally registered, no need for individual component registration

// Global error handler
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Info:', info)
  store.dispatch('toast/showError', {
    title: 'Application Error',
    message: 'An unexpected error occurred. Please try again later.'
  })
}

// All Ant Design Vue components are available globally

// Initialize authentication before mounting app
async function initializeApp() {
  try {
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

