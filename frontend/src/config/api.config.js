/**
 * Centralized API configuration
 * This ensures consistent API base URL across all components and services
 */

/**
 * Get the API base URL based on environment
 * Priority:
 * 1. VUE_APP_API_BASE_URL from environment variables (set during build)
 * 2. Relative path '/api' (works in both dev and production with proxy/nginx)
 *
 * IMPORTANT: This MUST be a constant, not a function, to ensure webpack
 * replaces it correctly during build with the environment variable value
 */
const API_BASE_URL = process.env.VUE_APP_API_BASE_URL || '/api'

export const getApiBaseUrl = () => API_BASE_URL

/**
 * Get the full API URL (including origin)
 * Used for OAuth redirects and external links
 */
export const getFullApiUrl = () => {
  const baseUrl = getApiBaseUrl()

  // If baseUrl is absolute (starts with http), return as is
  if (baseUrl.startsWith('http://') || baseUrl.startsWith('https://')) {
    return baseUrl
  }

  // Otherwise, prepend current origin
  return `${window.location.origin}${baseUrl}`
}

/**
 * Get OAuth login URL for Facebook
 */
export const getFacebookLoginUrl = () => {
  return `${getFullApiUrl()}/auth/oauth2/authorize/facebook`
}

/**
 * API endpoints configuration
 */
export const API_ENDPOINTS = {
  // Auth endpoints
  auth: {
    loginApp: '/auth/login-app',
    register: '/auth/register',
    forgotPassword: '/auth/forgot-password',
    resetPassword: '/auth/reset-password',
    facebook: '/auth/facebook',
    oauth2Authorize: '/auth/oauth2/authorize/facebook',
    oauth2Callback: '/auth/oauth2/callback/facebook',
    logout: '/auth/logout',
    user: '/auth/user',
    profile: '/auth/profile'
  },

  // Campaign endpoints
  campaigns: {
    base: '/campaigns'
  },

  // Ad endpoints
  ads: {
    base: '/ads',
    generate: '/ads/generate'
  },

  // AI Provider endpoints
  aiProviders: {
    text: '/ai-providers/text',
    image: '/ai-providers/image',
    all: '/ai-providers'
  },

  // Upload endpoints
  upload: {
    media: '/upload/media',
    image: '/upload/image'
  }
}

export default {
  getApiBaseUrl,
  getFullApiUrl,
  getFacebookLoginUrl,
  API_ENDPOINTS
}
