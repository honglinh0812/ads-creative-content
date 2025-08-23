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
    // Handle session expiration - don't redirect directly, let Vue router handle it
    if (error.response && error.response.status === 401) {
      // Clear token but don't redirect - let the component handle the redirect
      localStorage.removeItem('token')
      console.log('Token expired, cleared from localStorage')
    }
    // Extract error message
    const message = error.response?.data?.message || error.message || 'An error occurred'
    error.message = message
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
    getUser: () => apiClient.get('/auth/user')
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
    exportAd: (adId) => apiClient.get(`/facebook-export/ad/${adId}`, {
      responseType: 'blob'
    }),
    exportMultipleAds: (adIds) => apiClient.post('/facebook-export/ads/bulk', adIds, {
      responseType: 'blob'
    })
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
  }
}