<template>
  <div :class="['page-wrapper', { 'sidebar-closed': !sidebarOpen }]">
    <AppSidebar :sidebarOpen="sidebarOpen" @toggle="toggleSidebar" @logout="handleLogout" />
    <div class="main-content-wrapper" :style="mainContentStyle">
      <!-- Mobile Header -->
      <div class="mobile-header lg:hidden">
        <button @click="toggleSidebar" class="btn btn-sm btn-ghost hover:bg-gray-100 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"></path>
          </svg>
        </button>
        <h1 class="text-lg font-semibold text-secondary-900">Create Campaign</h1>
      </div>

      <div class="content-wrapper">
        <div class="w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <!-- Header -->
          <div class="mb-8 animate-fade-in">
            <div class="flex items-center gap-4 mb-6">
              <router-link to="/campaigns" class="btn btn-sm btn-ghost hover:bg-gray-100 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                </svg>
                Back
              </router-link>
              <div class="flex-1">
                <h1 class="text-3xl font-bold text-secondary-900 mb-2">Create New Campaign</h1>
                <p class="text-secondary-600 text-lg">Set up your advertising campaign with objectives and budget</p>
              </div>
              <div class="flex gap-2">
                <button @click="showHelp = true" class="btn btn-ghost" aria-label="Help">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 8a3.001 3.001 0 01-2 2.83V11a1 1 0 11-2 0v-1a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- Form tạo campaign hiện đại, không chia bước -->
          <div class="card border-0 shadow-xl hover:shadow-2xl transition-all duration-300 animate-slide-in-up">
            <div class="card-header bg-gradient-to-r from-green-50 to-green-100 border-0">
              <div class="flex items-center gap-4 justify-start">
                <div class="text-left">
                  <h2 class="card-title text-blue-900 text-xl">Campaign Information</h2>
                  <p class="card-description text-blue-700">Configure your campaign settings and budget</p>
                </div>
              </div>
            </div>
            <div class="card-body p-8">
              <form @submit.prevent="handleSubmit" class="form-grid" @click="console.log('Form clicked')">
                <!-- Campaign Name -->
                <div class="form-field md:col-span-2">
                  <label class="field-label">
                    <span class="label-text">Campaign Name</span>
                    <span class="label-required">*</span>
                  </label>
                  <InputText 
                    v-model="form.name" 
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.name }" 
                    placeholder="Enter a memorable campaign name..." 
                    required 
                    @blur="validateForm()"
                  />
                  <div v-if="errors.name" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.name }}
                  </div>
                </div>

                <!-- Objective -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Campaign Objective</span>
                    <span class="label-required">*</span>
                  </label>
                  <Dropdown 
                    v-model="form.objective" 
                    :options="objectives" 
                    optionLabel="label" 
                    optionValue="value" 
                    placeholder="Select your campaign objective..." 
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.objective }" 
                  />
                  <div v-if="errors.objective" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.objective }}
                  </div>
                </div>

                <!-- Budget Type -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Budget Type</span>
                    <span class="label-required">*</span>
                  </label>
                  <Dropdown 
                    v-model="form.budgetType" 
                    :options="budgetTypes" 
                    optionLabel="label" 
                    optionValue="value" 
                    placeholder="Select budget type..." 
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.budgetType }" 
                  />
                  <div v-if="errors.budgetType" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.budgetType }}
                  </div>
                </div>

                <!-- Daily Budget -->
                <div v-if="form.budgetType === 'DAILY'" class="form-field">
                  <label class="field-label">
                    <span class="label-text">Daily Budget</span>
                    <span class="label-required">*</span>
                  </label>
                  <InputNumber 
                    v-model="form.dailyBudget" 
                    :min="1" 
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.dailyBudget }" 
                    placeholder="Enter daily budget..." 
                  />
                  <div v-if="errors.dailyBudget" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.dailyBudget }}
                  </div>
                </div>

                <!-- Total Budget -->
                <div v-if="form.budgetType === 'LIFETIME'" class="form-field">
                  <label class="field-label">
                    <span class="label-text">Total Budget</span>
                    <span class="label-required">*</span>
                  </label>
                  <InputNumber 
                    v-model="form.totalBudget" 
                    :min="1" 
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.totalBudget }" 
                    placeholder="Enter total budget..." 
                  />
                  <div v-if="errors.totalBudget" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.totalBudget }}
                  </div>
                </div>

                <!-- Start Date -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">Start Date & Time</span>
                    <span class="label-required">*</span>
                  </label>
                  <Calendar 
                    v-model="form.startDate" 
                    :minDate="new Date()"
                    :showTime="true"
                    :showSeconds="false"
                    :showIcon="true"
                    dateFormat="dd/mm/yy"
                    hourFormat="24"
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.startDate }" 
                    placeholder="Select start date and time..." 
                  />
                  <div v-if="errors.startDate" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.startDate }}
                  </div>
                </div>

                <!-- End Date -->
                <div class="form-field">
                  <label class="field-label">
                    <span class="label-text">End Date & Time</span>
                    <span class="label-required">*</span>
                  </label>
                  <Calendar 
                    v-model="form.endDate" 
                    :minDate="form.startDate || new Date()"
                    :showTime="true"
                    :showSeconds="false"
                    :showIcon="true"
                    dateFormat="dd/mm/yy"
                    hourFormat="24"
                    class="form-input w-full h-12 text-lg" 
                    :class="{ 'error': errors.endDate }" 
                    placeholder="Select end date and time..." 
                  />
                  <div v-if="errors.endDate" class="error-message flex items-center gap-2 mt-2">
                    <svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                    {{ errors.endDate }}
                  </div>
                </div>

                <!-- Submit -->
                <div class="flex justify-end mt-8">
                  <router-link to="/campaigns" class="btn btn-lg btn-secondary hover:bg-gray-100 transition-all duration-300 transform hover:-translate-y-1">
                    Cancel
                  </router-link>
                  <button type="submit" :disabled="isSubmitting" class="btn btn-lg btn-primary hover:scale-105 transition-all duration-300 transform hover:-translate-y-1 shadow-lg" @click="console.log('Submit button clicked')">
                    <svg v-if="isSubmitting" class="w-4 h-4 mr-2 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {{ isSubmitting ? 'Creating...' : 'Create Campaign' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Dialog v-model:visible="showHelp" header="Hướng dẫn tạo chiến dịch" :modal="true" :closable="true" class="help-dialog">
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">Bắt đầu</h3>
          <p class="help-text">
            Trình hướng dẫn này sẽ giúp bạn tạo chiến dịch quảng cáo Facebook chuyên nghiệp qua 3 bước đơn giản.
          </p>
        </div>
        <div class="help-section">
          <h3 class="help-title">Các bước thực hiện</h3>
          <div class="help-steps">
            <div class="help-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>Thông tin cơ bản</h4>
                <p>Nhập tên chiến dịch, chọn mục tiêu, loại ngân sách.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>Đối tượng mục tiêu & ngân sách</h4>
                <p>Chọn đối tượng khách hàng, thiết lập ngân sách và thời gian chạy.</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>Xem trước & Lưu</h4>
                <p>Xem lại thông tin chiến dịch và lưu lại.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/services/api'

import InputNumber from 'primevue/inputnumber';
import AppSidebar from '@/components/AppSidebar.vue'
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Dropdown from 'primevue/dropdown';
import Calendar from 'primevue/calendar';

export default {
  name: 'CampaignCreate',
  components: {
    InputNumber,
    AppSidebar,
    Dialog,
    InputText,
    Dropdown,
    Calendar
  },
  data() {
    return {
      sidebarOpen: true,
      isSubmitting: false,
      
      form: {
        name: '',
        objective: '',
        budgetType: 'DAILY',
        dailyBudget: null,
        totalBudget: null,
        targetAudience: '',
        startDate: '',
        endDate: ''
      },
      
      errors: {},
      
      objectives: [
        { value: 'BRAND_AWARENESS', label: 'Brand Awareness' },
        { value: 'REACH', label: 'Reach' },
        { value: 'TRAFFIC', label: 'Traffic' },
        { value: 'ENGAGEMENT', label: 'Engagement' },
        { value: 'APP_INSTALLS', label: 'App Installs' },
        { value: 'VIDEO_VIEWS', label: 'Video Views' },
        { value: 'LEAD_GENERATION', label: 'Lead Generation' },
        { value: 'CONVERSIONS', label: 'Conversions' },
        { value: 'CATALOG_SALES', label: 'Catalog Sales' },
        { value: 'STORE_TRAFFIC', label: 'Store Traffic' }
      ],
      
      budgetTypes: [
        {
          value: 'DAILY',
          label: 'Daily Budget',
          description: 'Set daily spending limit'
        },
        {
          value: 'LIFETIME',
          label: 'Lifetime Budget',
          description: 'Set total budget for entire campaign'
        }
      ],
      showHelp: false
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
    },
    mainContentStyle() {
      return this.sidebarOpen ? { marginLeft: '240px' } : { marginLeft: '0' }
    }
  },
  
  mounted() {
    console.log('CampaignCreate component mounted')
    console.log('Form data:', this.form)
    console.log('API object:', api)
  },
  
  methods: {
    ...mapActions('auth', ['logout']),
    ...mapActions('toast', ['showToast']),
    
    validateForm() {
      console.log('validateForm called')
      this.errors = {}
      
      if (!this.form.name) {
        this.errors.name = 'Campaign name is required'
      }
      
      if (!this.form.objective) {
        this.errors.objective = 'Campaign objective is required'
      }
      
      if (!this.form.budgetType) {
        this.errors.budgetType = 'Budget type is required'
      }

      // Validate budget based on type
      if (this.form.budgetType === 'DAILY') {
        if (!this.form.dailyBudget || this.form.dailyBudget <= 0) {
          this.errors.dailyBudget = 'Daily budget is required and must be greater than 0'
        }
      } else if (this.form.budgetType === 'LIFETIME') {
        if (!this.form.totalBudget || this.form.totalBudget <= 0) {
          this.errors.totalBudget = 'Total budget is required and must be greater than 0'
        }
      }

      // Validate start date
      if (!this.form.startDate) {
        this.errors.startDate = 'Start date is required'
      } else {
        const startDate = new Date(this.form.startDate)
        const now = new Date()
        if (startDate < now) {
          this.errors.startDate = 'Start date must be in the future'
        }
      }

      // Validate end date
      if (!this.form.endDate) {
        this.errors.endDate = 'End date is required'
      } else if (this.form.startDate) {
        const startDate = new Date(this.form.startDate)
        const endDate = new Date(this.form.endDate)
        const minEndDate = new Date(startDate.getTime() + 60 * 60 * 1000) // 1 hour later
        
        if (endDate <= startDate) {
          this.errors.endDate = 'End date must be after start date'
        } else if (endDate < minEndDate) {
          this.errors.endDate = 'End date must be at least 1 hour after start date'
        }
      }
      
      console.log('Validation errors:', this.errors)
      return Object.keys(this.errors).length === 0
    },
    
    async handleSubmit() {
      console.log('handleSubmit called')
      console.log('Form data:', this.form)
      console.log('Validation result:', this.validateForm())
      
      if (!this.validateForm()) {
        console.log('Form validation failed')
        return
      }
      console.log('Form validation passed, submitting...')
      this.isSubmitting = true
      try {
        console.log('Submitting campaign form:', this.form)
        console.log('API object:', api)
        console.log('API campaigns:', api.campaigns)
        const response = await api.campaigns.create(this.form)
        console.log('Campaign create response:', response)
        this.showToast({
          type: 'success',
          message: 'Campaign created successfully!'
        })
        await this.$store.dispatch('dashboard/fetchDashboardData', null, { root: true })
        this.$router.push('/dashboard')
      } catch (error) {
        console.error('Error creating campaign:', error)
        console.error('Error response:', error.response)
        console.error('Error data:', error.response?.data)
        this.showToast({
          type: 'error',
          message: error.response?.data?.message || 'Unable to create campaign. Please try again.'
        })
      } finally {
        this.isSubmitting = false
      }
    },
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    handleLogout() {
      this.$store.dispatch('auth/logout')
    }
  }
}
</script>

<style scoped>
.page-wrapper.sidebar-closed .main-content-wrapper {
  margin-left: 0 !important;
}
</style>

