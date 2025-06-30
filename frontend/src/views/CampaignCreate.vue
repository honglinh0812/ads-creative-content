<template>
  <div class="app-layout">
    <div class="flex flex-1">
      <!-- Sidebar -->
      <aside class="app-sidebar" :class="{ open: sidebarOpen }">
        <div class="sidebar-header">
          <router-link to="/dashboard" class="flex items-center gap-3 px-6 py-4">
            <div class="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
            </div>
            <h1 class="text-lg font-bold text-secondary-900">AI Ads Creator</h1>
          </router-link>
        </div>

        <nav class="nav">
          <router-link to="/dashboard" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"></path>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5a2 2 0 012-2h4a2 2 0 012 2v6H8V5z"></path>
            </svg>
            <span class="nav-text">Dashboard</span>
          </router-link>
          
          <router-link to="/campaigns" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path>
            </svg>
            <span class="nav-text">Campaigns</span>
          </router-link>
          
          <router-link to="/ads" class="nav-item" active-class="active">
            <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 0h10m-10 0a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V6a2 2 0 00-2-2"></path>
            </svg>
            <span class="nav-text">Ads</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="px-6 py-4">
            <div class="flex items-center gap-3 mb-3">
              <div class="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                <span class="text-primary-700 font-medium text-sm">{{ userInitials }}</span>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-secondary-900 truncate">{{ userName }}</p>
                <p class="text-xs text-secondary-500 truncate">{{ userEmail }}</p>
              </div>
            </div>
            <button @click="logout" class="btn btn-sm btn-ghost w-full">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              Logout
            </button>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main-content">
        <!-- Mobile Header -->
        <div class="mobile-header lg:hidden">
          <button @click="sidebarOpen = !sidebarOpen" class="btn btn-sm btn-ghost">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
          <h1 class="text-lg font-semibold text-secondary-900">Tạo Campaign</h1>
        </div>

        <div class="content-wrapper">
          <div class="max-w-2xl mx-auto">
            <!-- Header -->
            <div class="mb-8">
              <div class="flex items-center gap-4 mb-4">
                <router-link to="/campaigns" class="btn btn-sm btn-ghost">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                  </svg>
                  Quay lại
                </router-link>
                <div>
                  <h1 class="text-2xl font-bold text-secondary-900">Tạo Campaign Mới</h1>
                  <p class="text-secondary-600">Thiết lập campaign quảng cáo với mục tiêu và ngân sách của bạn</p>
                </div>
              </div>
            </div>

            <!-- Form -->
            <form @submit.prevent="submitForm" class="card">
              <div class="card-header">
                <h2 class="card-title">Chi tiết Campaign</h2>
                <p class="card-description">Cấu hình cài đặt và mục tiêu cho campaign của bạn</p>
              </div>
              
              <div class="card-body space-y-6">
                <!-- Campaign Name -->
                <div>
                  <label class="form-label">Tên Campaign <span class="text-red-500">*</span></label>
                  <input
                    v-model="form.name"
                    type="text"
                    class="form-input"
                    :class="{ 'border-red-500': errors.name }"
                    placeholder="Nhập tên campaign..."
                    required
                  />
                  <p v-if="errors.name" class="form-error">{{ errors.name }}</p>
                </div>

                <!-- Campaign Objective -->
                <div>
                  <label class="form-label">Mục tiêu Campaign <span class="text-red-500">*</span></label>
                  <select v-model="form.objective" class="form-select" :class="{ 'border-red-500': errors.objective }" required>
                    <option value="">Chọn mục tiêu...</option>
                    <option v-for="objective in objectives" :key="objective.value" :value="objective.value">
                      {{ objective.label }}
                    </option>
                  </select>
                  <p v-if="errors.objective" class="form-error">{{ errors.objective }}</p>
                </div>

                <!-- Budget Type -->
                <div>
                  <label class="form-label">Loại ngân sách <span class="text-red-500">*</span></label>
                  <div class="grid grid-cols-2 gap-4">
                    <div v-for="type in budgetTypes" :key="type.value" 
                         :class="['border-2 rounded-lg p-4 cursor-pointer transition-all', 
                                  form.budgetType === type.value ? 'border-primary-500 bg-primary-50' : 'border-neutral-200 hover:border-neutral-300']"
                         @click="form.budgetType = type.value">
                      <div class="flex items-center space-x-3">
                        <div :class="['w-4 h-4 rounded-full border-2', 
                                      form.budgetType === type.value ? 'border-primary-500 bg-primary-500' : 'border-neutral-300']">
                          <div v-if="form.budgetType === type.value" class="w-2 h-2 bg-white rounded-full mx-auto mt-0.5"></div>
                        </div>
                        <div>
                          <h3 class="font-medium text-secondary-900">{{ type.label }}</h3>
                          <p class="text-sm text-secondary-600">{{ type.description }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                  <p v-if="errors.budgetType" class="form-error">{{ errors.budgetType }}</p>
                </div>

                <!-- Budget Amount -->
                <div v-if="form.budgetType === 'DAILY'">
                  <label class="form-label">Ngân sách hàng ngày <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <input
                      v-model.number="form.dailyBudget"
                      type="number"
                      class="form-input pl-8"
                      :class="{ 'border-red-500': errors.dailyBudget }"
                      placeholder="0"
                      min="1"
                      step="0.01"
                      required
                    />
                    <span class="absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-500">$</span>
                  </div>
                  <p v-if="errors.dailyBudget" class="form-error">{{ errors.dailyBudget }}</p>
                </div>

                <div v-if="form.budgetType === 'LIFETIME'">
                  <label class="form-label">Tổng ngân sách <span class="text-red-500">*</span></label>
                  <div class="relative">
                    <input
                      v-model.number="form.totalBudget"
                      type="number"
                      class="form-input pl-8"
                      :class="{ 'border-red-500': errors.totalBudget }"
                      placeholder="0"
                      min="1"
                      step="0.01"
                      required
                    />
                    <span class="absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-500">$</span>
                  </div>
                  <p v-if="errors.totalBudget" class="form-error">{{ errors.totalBudget }}</p>
                </div>

                <!-- Target Audience -->
                <div>
                  <label class="form-label">Đối tượng mục tiêu</label>
                  <textarea
                    v-model="form.targetAudience"
                    class="form-textarea"
                    rows="3"
                    placeholder="Mô tả đối tượng mục tiêu của bạn..."
                  ></textarea>
                </div>

                <!-- Date Range -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label class="form-label">Ngày bắt đầu</label>
                    <input
                      v-model="form.startDate"
                      type="date"
                      class="form-input"
                      :min="today"
                    />
                  </div>
                  <div>
                    <label class="form-label">Ngày kết thúc</label>
                    <input
                      v-model="form.endDate"
                      type="date"
                      class="form-input"
                      :min="form.startDate || today"
                    />
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex justify-end space-x-3 pt-6 border-t border-neutral-200">
                  <router-link to="/campaigns" class="btn btn-secondary">
                    Hủy
                  </router-link>
                  <button type="submit" :disabled="isSubmitting" class="btn btn-primary">
                    <svg v-if="isSubmitting" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {{ isSubmitting ? 'Đang tạo...' : 'Tạo Campaign' }}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/services/api'

export default {
  name: 'CampaignCreate',
  data() {
    return {
      sidebarOpen: false,
      isSubmitting: false,
      
      form: {
        name: '',
        objective: '',
        budgetType: '',
        dailyBudget: null,
        totalBudget: null,
        targetAudience: '',
        startDate: '',
        endDate: ''
      },
      
      errors: {},
      
      objectives: [
        { value: 'BRAND_AWARENESS', label: 'Nhận diện thương hiệu' },
        { value: 'REACH', label: 'Tiếp cận' },
        { value: 'TRAFFIC', label: 'Lưu lượng truy cập' },
        { value: 'ENGAGEMENT', label: 'Tương tác' },
        { value: 'APP_INSTALLS', label: 'Cài đặt ứng dụng' },
        { value: 'VIDEO_VIEWS', label: 'Lượt xem video' },
        { value: 'LEAD_GENERATION', label: 'Thu thập khách hàng tiềm năng' },
        { value: 'CONVERSIONS', label: 'Chuyển đổi' },
        { value: 'CATALOG_SALES', label: 'Bán hàng từ catalog' },
        { value: 'STORE_TRAFFIC', label: 'Lưu lượng cửa hàng' }
      ],
      
      budgetTypes: [
        {
          value: 'DAILY',
          label: 'Ngân sách hàng ngày',
          description: 'Đặt giới hạn chi tiêu mỗi ngày'
        },
        {
          value: 'LIFETIME',
          label: 'Ngân sách trọn đời',
          description: 'Đặt tổng ngân sách cho toàn bộ campaign'
        }
      ]
    }
  },
  
  computed: {
    ...mapState('auth', ['user']),
    
    userInitials() {
      if (!this.user?.name) return 'U'
      return this.user.name.split(' ').map(n => n[0]).join('').toUpperCase()
    },
    
    userName() {
      return this.user?.name || 'User'
    },
    
    userEmail() {
      return this.user?.email || ''
    },
    
    today() {
      return new Date().toISOString().split('T')[0]
    }
  },
  
  methods: {
    ...mapActions('auth', ['logout']),
    ...mapActions('toast', ['showToast']),
    
    validateForm() {
      this.errors = {}
      
      if (!this.form.name) {
        this.errors.name = 'Tên campaign là bắt buộc'
      }
      
      if (!this.form.objective) {
        this.errors.objective = 'Mục tiêu campaign là bắt buộc'
      }
      
      if (!this.form.budgetType) {
        this.errors.budgetType = 'Loại ngân sách là bắt buộc'
      }
      
      if (this.form.budgetType === 'DAILY' && (!this.form.dailyBudget || this.form.dailyBudget <= 0)) {
        this.errors.dailyBudget = 'Ngân sách hàng ngày phải lớn hơn 0'
      }
      
      if (this.form.budgetType === 'LIFETIME' && (!this.form.totalBudget || this.form.totalBudget <= 0)) {
        this.errors.totalBudget = 'Tổng ngân sách phải lớn hơn 0'
      }
      
      return Object.keys(this.errors).length === 0
    },
    
    async submitForm() {
      if (!this.validateForm()) {
        return
      }
      
      this.isSubmitting = true
      
      try {
        await api.campaigns.create(this.form)
        
        this.showToast({
          type: 'success',
          message: 'Tạo campaign thành công!'
        })
        
        this.$router.push('/dashboard')
      } catch (error) {
        console.error('Error creating campaign:', error)
        this.showToast({
          type: 'error',
          message: error.response?.data?.message || 'Không thể tạo campaign. Vui lòng thử lại.'
        })
      } finally {
        this.isSubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.mobile-header {
  @apply flex items-center gap-4 p-4 border-b border-neutral-200 bg-white;
}

.sidebar-header {
  @apply border-b border-neutral-200;
}

.sidebar-footer {
  @apply mt-auto border-t border-neutral-200;
}

@media (max-width: 1023px) {
  .app-sidebar {
    @apply fixed inset-y-0 left-0 z-50 transform -translate-x-full transition-transform duration-300 ease-in-out;
  }
  
  .app-sidebar.open {
    @apply translate-x-0;
  }
}
</style>

