import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

// Lazy-loaded
const Home = () => import('../views/Home.vue')
const Login = () => import('../views/Login.vue')
const Dashboard = () => import('../views/Dashboard.vue')
const Analytics = () => import('../views/Analytics.vue')
const Optimization = () => import('../views/Optimization.vue')
const CampaignPage = () => import('../views/CampaignPage.vue')
const CampaignCreate = () => import('../views/CampaignCreate.vue')
const CampaignDetail = () => import('../views/CampaignDetail.vue')
const Ads = () => import('../views/Ads.vue')
const AdCreate = () => import('../views/AdCreate.vue')
const AdLearn = () => import('../views/AdLearn.vue')
const AdDetail = () => import('../views/AdDetail.vue')
const Personas = () => import('../views/Personas.vue')
const Competitors = () => import('../views/Competitors.vue')
const NotFound = () => import('../views/NotFound.vue')
const PrivacyPolicy = () => import('../views/legal/PrivacyPolicy.vue')
const TermsOfService = () => import('../views/legal/TermsOfService.vue')
const DataDeletion = () => import('../views/legal/DataDeletion.vue')
const AuthSuccess = () => import('../views/AuthSuccess.vue')
const ResetPassword = () => import('../views/ResetPassword.vue')
const Settings = () => import('../views/Settings.vue')

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home,
    meta: { public: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/auth-success',
    name: 'AuthSuccess',
    component: AuthSuccess,
    meta: { public: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/analytics',
    name: 'Analytics',
    component: Analytics,
    meta: { requiresAuth: true }
  },
  {
    path: '/optimization',
    name: 'Optimization',
    component: Optimization,
    meta: { requiresAuth: true }
  },
  {
    path: '/campaigns',
    name: 'CampaignPage',
    component: CampaignPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/campaign/create',
    name: 'CampaignCreate',
    component: CampaignCreate,
    meta: { requiresAuth: true }
  },
  {
    path: '/campaigns/:id',
    name: 'CampaignDetail',
    component: CampaignDetail,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/ads',
    name: 'Ads',
    component: Ads,
    meta: { requiresAuth: true }
  },
  {
    path: '/ads/learn',
    name: 'AdLearn',
    component: AdLearn,
    meta: { requiresAuth: true }
  },
  {
    path: '/ad/create',
    name: 'AdCreate',
    component: AdCreate,
    meta: { requiresAuth: true }
  },
  {
    path: '/ads/:id',
    name: 'AdDetail',
    component: AdDetail,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/ads/:id/edit',
    name: 'AdEdit',
    component: AdCreate, 
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/personas',
    name: 'Personas',
    component: Personas,
    meta: { requiresAuth: true }
  },
  {
    path: '/competitors',
    name: 'Competitors',
    component: Competitors,
    meta: { requiresAuth: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: ResetPassword,
    meta: { public: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  },
  // Legacy routes for backward compatibility
  {
    path: '/campaigns/create',
    redirect: '/campaign/create'
  },
  {
    path: '/campaigns/:campaignId/ads/create',
    redirect: '/ad/create'
  },
  {
    path: '/campaigns/:campaignId/ads/:adId',
    redirect: to => `/ads/${to.params.adId}`
  },
  // Legal pages
  {
    path: '/privacy-policy',
    name: 'PrivacyPolicy',
    component: PrivacyPolicy,
    meta: { public: true }
  },
  {
    path: '/terms-of-service',
    name: 'TermsOfService',
    component: TermsOfService,
    meta: { public: true }
  },
  {
    path: '/data-deletion',
    name: 'DataDeletion',
    component: DataDeletion,
    meta: { public: true }
  },
  // 404 page
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  const isAuthenticated = store.getters['auth/isAuthenticated']
  const isPublic = to.matched.some(record => record.meta.public)
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)

  // If route is public, allow access
  if (isPublic && !requiresAuth) {
    // If user is authenticated and trying to access public pages (except legal), redirect to dashboard
    if (isAuthenticated && to.name !== 'PrivacyPolicy' && to.name !== 'TermsOfService' && to.name !== 'DataDeletion' && to.name !== 'AuthSuccess') {
      next({ name: 'Dashboard' })
      return
    }
    next()
    return
  }

  // If route requires auth and user is not authenticated
  if (requiresAuth && !isAuthenticated) {
    // Clear any invalid token
    store.dispatch('auth/clearAuth')
    next({ 
      name: 'Login', 
      query: { redirect: to.fullPath } 
    })
    return
  }

  // If user is not authenticated and trying to access home page, redirect to login
  if (!isAuthenticated && to.path === '/') {
    next({ 
      name: 'Login', 
      query: { redirect: '/dashboard' } 
    })
    return
  }

  // If user is authenticated and trying to access login page, redirect to dashboard
  if (isAuthenticated && to.name === 'Login') {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
