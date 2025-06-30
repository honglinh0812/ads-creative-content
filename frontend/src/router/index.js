import { createRouter, createWebHistory } from 'vue-router'
import store from '../store'

// Views
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Dashboard from '../views/Dashboard.vue'
import CampaignPage from '../views/CampaignPage.vue'
import Ads from '../views/Ads.vue'
import CampaignCreate from '../views/CampaignCreate.vue'
import CampaignDetail from '../views/CampaignDetail.vue'
import AdCreate from '../views/AdCreate.vue'
import AdDetail from '../views/AdDetail.vue'
import NotFound from '../views/NotFound.vue'
import PrivacyPolicy from '../views/legal/PrivacyPolicy.vue'
import TermsOfService from '../views/legal/TermsOfService.vue'
import DataDeletion from '../views/legal/DataDeletion.vue'
import AuthSuccess from '../views/AuthSuccess.vue'

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
    path: '/campaigns',
    name: 'Campaigns',
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
    path: '/campaigns/:id/edit',
    name: 'CampaignEdit',
    component: CampaignCreate, // Reuse create component for editing
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
    component: AdCreate, // Reuse create component for editing
    props: true,
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
    next()
    return
  }

  // If route requires auth and user is not authenticated
  if (requiresAuth && !isAuthenticated) {
    next({ 
      name: 'Login', 
      query: { redirect: to.fullPath } 
    })
    return
  }

  // If user is authenticated and trying to access login page, redirect to dashboard
  if (isAuthenticated && to.name === 'Login') {
    next({ name: 'Dashboard' })
    return
  }

  // Default redirect for authenticated users accessing root
  if (isAuthenticated && to.path === '/') {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router