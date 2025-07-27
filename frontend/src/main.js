import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// PrimeVue
import PrimeVue from 'primevue/config'
import 'primevue/resources/themes/saga-blue/theme.css'
import 'primevue/resources/primevue.min.css'
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'

// PrimeVue Components
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Dropdown from 'primevue/dropdown'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import ConfirmationService from 'primevue/confirmationservice';
import ConfirmDialog from 'primevue/confirmdialog';
import Calendar from 'primevue/calendar';
import ProgressSpinner from 'primevue/progressspinner'
import TabView from 'primevue/tabview'
import TabPanel from 'primevue/tabpanel'
import FileUpload from 'primevue/fileupload'
import Divider from 'primevue/divider'
import InputNumber from 'primevue/inputnumber'

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
app.use(PrimeVue, { ripple: true })
app.use(ConfirmationService)
app.use(ToastService)
app.use(ApiService)

// Register PrimeVue components
app.component('Button', Button)
app.component('InputText', InputText)
app.component('Textarea', Textarea)
app.component('Dropdown', Dropdown)
app.component('Card', Card)
app.component('DataTable', DataTable)
app.component('Column', Column)
app.component('Dialog', Dialog)
app.component('ProgressSpinner', ProgressSpinner)
app.component('TabView', TabView)
app.component('TabPanel', TabPanel)
app.component('FileUpload', FileUpload)
app.component('Divider', Divider)
app.component('InputNumber', InputNumber)

// Global error handler
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue Error:', err)
  console.error('Info:', info)
  store.dispatch('toast/showError', {
    title: 'Application Error',
    message: 'An unexpected error occurred. Please try again later.'
  })
}

app.component("ConfirmDialog", ConfirmDialog);
app.component("PvcCalendar", Calendar);

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

