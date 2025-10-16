import axios from 'axios'

// Create axios instance
const apiClient = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json'
  }
})

// Add request interceptor for auth token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Add response interceptor for error handling
apiClient.interceptors.response.use(
  response => {
    return response
  },
  error => {
    // Handle session expiration - redirect to login
    if (error.response && error.response.status === 401) {
      // Clear token and redirect to login
      localStorage.removeItem('token')
      console.log('Token expired, redirecting to login')

      // Clear auth state in store if available
      if (window.store) {
        window.store.dispatch('auth/clearAuth')
      }

      // Redirect to login page
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    // Extract structured error information
    const errorData = error.response?.data || {}

    // Enhance error object with structured data
    error.message = errorData.message || error.message || 'An error occurred'
    error.fieldErrors = errorData.fieldErrors || {}
    error.errorCode = errorData.error || 'Unknown Error'
    error.requestId = errorData.requestId || null
    error.timestamp = errorData.timestamp || null

    // Log for debugging (include trace ID)
    if (process.env.NODE_ENV === 'development' || errorData.requestId) {
      console.error(`[${errorData.requestId || 'NO-ID'}] API Error:`, {
        message: error.message,
        fieldErrors: error.fieldErrors,
        errorCode: error.errorCode,
        path: errorData.path,
        timestamp: errorData.timestamp
      })
    }

    return Promise.reject(error)
  }
)

export default {
  // Generic HTTP methods
  get: (url, config) => apiClient.get(url, config),
  post: (url, data, config) => apiClient.post(url, data, config),
  put: (url, data, config) => apiClient.put(url, data, config),
  delete: (url, config) => apiClient.delete(url, config),
  
  // Auth endpoints
  auth: {
    login: () => `${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/oauth2/authorize/facebook`,
    callback: () => '/auth/oauth2/callback/facebook',
    logout: () => apiClient.post('/auth/logout'),
    getUser: () => apiClient.get('/auth/user'),
    getProfile: () => apiClient.get('/auth/profile'),
    updateProfile: (profileData) => apiClient.put('/auth/profile', profileData),
    changePassword: (passwordData) => apiClient.put('/auth/change-password', passwordData),
    deleteAccount: () => apiClient.delete('/auth/account')
  },
  
  // Settings endpoints
  settings: {
    getSettings: () => apiClient.get('/settings'),
    updateGeneralSettings: (generalSettings) => apiClient.put('/settings/general', generalSettings),
    updateAISettings: (aiSettings) => apiClient.put('/settings/ai', aiSettings),
    updateNotificationSettings: (notificationSettings) => apiClient.put('/settings/notifications', notificationSettings),
    exportData: () => apiClient.get('/settings/export', { responseType: 'blob' }),
    importData: (formData) => apiClient.post('/settings/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
    clearCache: () => apiClient.post('/settings/cache/clear')
  },
  
  // Campaign endpoints
  campaigns: {
    getAll: (page = 0, size = 5) => apiClient.get(`/campaigns?page=${page}&size=${size}`),
    get: (id) => apiClient.get(`/campaigns/${id}`),
    create: (campaign) => apiClient.post('/campaigns', campaign),
    update: (id, campaign) => apiClient.put(`/campaigns/${id}`, campaign),
    delete: (id) => apiClient.delete(`/campaigns/${id}`)
  },
  
  // Ad endpoints
  ads: {
    getAll: (page = 0, size = 5) => apiClient.get(`/ads?page=${page}&size=${size}`),
    get: (adId) => apiClient.get(`/ads/${adId}`),
    generate: (generationData) => apiClient.post('/ads/generate', generationData),
    saveExisting: (saveData) => apiClient.post('/ads/save-existing', saveData),
    create: (adData) => apiClient.post('/ads', adData),
    update: (adId, adData) => apiClient.put(`/ads/${adId}`, adData),
    selectContent: (adId, contentId) => 
      apiClient.post(`/ads/${adId}/select-content`, null, {
        params: { contentId }
      }),
    getContents: (adId) => apiClient.get(`/ads/${adId}/contents`),
    delete: (adId) => apiClient.delete(`/ads/${adId}`),
    extractFromLibrary: (extractionData) => apiClient.post('/ads/extract-from-library', extractionData)
  },

  // Facebook Export endpoints
  facebookExport: {
    exportAd: (adId, format = 'csv') => apiClient.get(`/facebook-export/ad/${adId}`, {
      params: { format },
      responseType: 'blob'
    }),
    exportMultipleAds: (adIds, format = 'csv') => apiClient.post('/facebook-export/ads/bulk/export', {
      adIds,
      format
    }, {
      responseType: 'blob'
    }),
    previewAd: (adId) => apiClient.get(`/facebook-export/preview/ad/${adId}`),
    previewMultipleAds: (adIds) => apiClient.post('/facebook-export/preview/ads/bulk', adIds)
  },
  
  // Meta Ad Library endpoints
  metaAdLibrary: {
    get: (id) => apiClient.get(`/meta-ad-library/${id}`),
    search: (query) => apiClient.get(`/meta-ad-library/search?q=${encodeURIComponent(query)}`)
  },
  
  // Provider endpoints
  providers: {
    getImageProviders: () => apiClient.get('/ai-providers/image'),
    getTextProviders: () => apiClient.get('/ai-providers/text'),
    getAllProviders: () => apiClient.get('/ai-providers'),
    getCallToActions: (language = 'en') => apiClient.get(`/ai-providers/call-to-actions?language=${language}`)
  },
  
  // Upload endpoints
  upload: {
    media: (formData) => apiClient.post('/upload/media', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  
  // Dashboard
  dashboard: {
    getData: () => apiClient.get('/dashboard')
  },
  
  // Image upload (merged with upload above)
  imageUpload: {
    image: (file, onUploadProgress) => {
      const formData = new FormData()
      formData.append('file', file)
      
      return apiClient.post('/upload/image', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress
      })
    }
  },


  // Data management endpoints
  data: {
    export: () => apiClient.get('/data/export', { responseType: 'blob' }),
    import: (file) => {
      const formData = new FormData()
      formData.append('file', file)
      return apiClient.post('/data/import', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
    },
    clearCache: () => apiClient.delete('/data/cache')
  },

  // Analytics endpoints
  analyticsAPI: {
    getDashboard: (timeRange = '30d') => apiClient.get(`/analytics/dashboard?timeRange=${timeRange}`),
    exportData: (type, params, format) => apiClient.get(`/analytics/export/${type}`, {
      params: { ...params, format },
      responseType: 'blob'
    })
  },

  // Optimization endpoints
  optimizationAPI: {
    getOptimizationSummary: () => apiClient.get('/optimization/summary'),
    getHighPriorityRecommendations: () => apiClient.get('/optimization/recommendations/high-priority'),
    acceptRecommendation: (id) => apiClient.post(`/optimization/recommendations/${id}/accept`),
    dismissRecommendation: (id, reason) => apiClient.post(`/optimization/recommendations/${id}/dismiss`, { reason }),
    updateRecommendationSettings: (settings) => apiClient.put('/optimization/settings', settings)
  },

  // Persona endpoints
  personas: {
    getAll: () => apiClient.get('/personas'),
    getPaged: (page = 0, size = 20, sort = 'createdAt,desc') =>
      apiClient.get(`/personas/paged?page=${page}&size=${size}&sort=${sort}`),
    get: (id) => apiClient.get(`/personas/${id}`),
    create: (persona) => apiClient.post('/personas', persona),
    update: (id, persona) => apiClient.put(`/personas/${id}`, persona),
    delete: (id) => apiClient.delete(`/personas/${id}`),
    search: (name) => apiClient.get(`/personas/search?name=${encodeURIComponent(name)}`)
  }
}