import axios from 'axios'
import store from '@/store'

// Create axios instance with base configuration
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = store.getters['auth/token']
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      store.dispatch('auth/clearAuth')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API service methods
const apiService = {
  // Campaign methods
  campaigns: {
    getAll() {
      return api.get('/campaigns')
    },
    
    getById(id) {
      return api.get(`/campaigns/${id}`)
    },
    
    create(data) {
      return api.post('/campaigns', data)
    },
    
    update(id, data) {
      return api.put(`/campaigns/${id}`, data)
    },
    
    delete(id) {
      return api.delete(`/campaigns/${id}`)
    }
  },

  // Ad methods
  ads: {
    getAll(params = {}) {
      return api.get('/ads', { params })
    },
    
    getById(id) {
      return api.get(`/ads/${id}`)
    },
    
    create(data) {
      return api.post('/ads', data)
    },
    
    generate(data) {
      return api.post('/ads/generate', data)
    },
    
    saveExisting(data) {
      return api.post('/ads/save-existing', data)
    },
    
    update(id, data) {
      return api.put(`/ads/${id}`, data)
    },
    
    delete(id) {
      return api.delete(`/ads/${id}`)
    },
    
    getByCampaign(campaignId) {
      return api.get(`/ads/campaign/${campaignId}`)
    }
  },

  // AI Provider methods
  providers: {
    getTextProviders() {
      return api.get('/ai-providers/text')
    },
    
    getImageProviders() {
      return api.get('/ai-providers/image')
    },
    
    getAllProviders() {
      return api.get('/ai-providers')
    }
  },

  // File upload methods
  upload: {
    media(file, onProgress) {
      const formData = new FormData()
      formData.append('file', file)
      
      return api.post('/upload/media', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress) {
            const percentCompleted = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            )
            onProgress(percentCompleted)
          }
        }
      })
    }
  },

  // User methods
  user: {
    getProfile() {
      return api.get('/user/profile')
    },
    
    updateProfile(data) {
      return api.put('/user/profile', data)
    }
  },

  // Analytics methods
  analytics: {
    getDashboardStats() {
      return api.get('/analytics/dashboard')
    },
    
    getCampaignStats(campaignId, dateRange) {
      return api.get(`/analytics/campaigns/${campaignId}`, {
        params: dateRange
      })
    },
    
    getAdStats(adId, dateRange) {
      return api.get(`/analytics/ads/${adId}`, {
        params: dateRange
      })
    }
  },

  // Meta Ad Library integration
  meta: {
    searchAds(query, params = {}) {
      return api.get('/meta/ads/search', {
        params: { query, ...params }
      })
    },
    
    getAdDetails(adId) {
      return api.get(`/meta/ads/${adId}`)
    }
  },

  // ScrapeCreators integration
  scrape: {
    getCreators(params = {}) {
      return api.get('/scrape/creators', { params })
    },
    
    getCreatorAds(creatorId) {
      return api.get(`/scrape/creators/${creatorId}/ads`)
    }
  }
}

// Install as Vue plugin
export default {
  install(app) {
    app.config.globalProperties.$api = apiService
    app.provide('api', apiService)
  }
}

export { apiService, api }
