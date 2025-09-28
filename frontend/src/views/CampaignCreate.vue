<template>
  <div class="dashboard-container">
    <!-- Page Header -->
    <div class="page-header">
      <a-page-header
        title="Create New Campaign"
        sub-title="Set up your advertising campaign with objectives and budget"
      >
        <template #extra>
          <a-space>
            <a-button @click="showHelp = true" type="default">
              <template #icon>
                <question-circle-outlined />
              </template>
              Help
            </a-button>
            <router-link to="/campaigns">
              <a-button type="default">
                <template #icon>
                  <arrow-left-outlined />
                </template>
                Back to Campaigns
              </a-button>
            </router-link>
          </a-space>
        </template>
      </a-page-header>
    </div>

    <!-- Campaign Form -->
    <a-card class="campaign-form-card" title="Campaign Information" style="margin-bottom: 24px;">
      <template #extra>
        <a-typography-text type="secondary">Configure your campaign settings and budget</a-typography-text>
      </template>
      
      <form @submit.prevent="handleSubmit">
        <a-row :gutter="[24, 24]">
          <!-- Campaign Name -->
          <a-col :span="24">
            <a-form-item 
              label="Campaign Name" 
              :validate-status="errors.name ? 'error' : ''"
              :help="errors.name"
              required
            >
              <a-input 
                v-model:value="form.name" 
                size="large"
                placeholder="Enter a memorable campaign name..." 
                @blur="validateForm()"
              />
            </a-form-item>
          </a-col>

          <!-- Objective -->
          <a-col :xs="24" :md="12">
            <a-form-item 
              label="Campaign Objective" 
              :validate-status="errors.objective ? 'error' : ''"
              :help="errors.objective"
              required
            >
              <a-select 
                v-model:value="form.objective" 
                size="large"
                placeholder="Select your campaign objective..." 
              >
                <a-select-option v-for="option in objectives" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- Budget Type -->
          <a-col :xs="24" :md="12">
            <a-form-item 
              label="Budget Type" 
              :validate-status="errors.budgetType ? 'error' : ''"
              :help="errors.budgetType"
              required
            >
              <a-select 
                v-model:value="form.budgetType" 
                size="large"
                placeholder="Select budget type..." 
              >
                <a-select-option v-for="option in budgetTypes" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- Daily Budget -->
          <a-col v-if="form.budgetType === 'DAILY'" :xs="24" :md="12">
            <a-form-item 
              label="Daily Budget" 
              :validate-status="errors.dailyBudget ? 'error' : ''"
              :help="errors.dailyBudget"
              required
            >
              <a-input-number 
                v-model:value="form.dailyBudget" 
                :min="1" 
                size="large"
                style="width: 100%"
                placeholder="Enter daily budget..." 
                :formatter="value => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="value => value.replace(/\$\s?|(,*)/g, '')"
              />
            </a-form-item>
          </a-col>

          <!-- Total Budget -->
          <a-col v-if="form.budgetType === 'LIFETIME'" :xs="24" :md="12">
            <a-form-item 
              label="Total Budget" 
              :validate-status="errors.totalBudget ? 'error' : ''"
              :help="errors.totalBudget"
              required
            >
              <a-input-number 
                v-model:value="form.totalBudget" 
                :min="1" 
                size="large"
                style="width: 100%"
                placeholder="Enter total budget..." 
                :formatter="value => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="value => value.replace(/\$\s?|(,*)/g, '')"
              />
            </a-form-item>
          </a-col>

          <!-- Start Date -->
          <a-col :xs="24" :md="12">
            <a-form-item 
              label="Start Date & Time" 
              :validate-status="errors.startDate ? 'error' : ''"
              :help="errors.startDate"
              required
            >
              <a-date-picker 
                v-model:value="form.startDate" 
                :disabled-date="(current) => current && current < new Date().setHours(0,0,0,0)"
                show-time
                format="DD/MM/YYYY HH:mm"
                size="large"
                style="width: 100%"
                placeholder="Select start date and time..." 
              />
            </a-form-item>
          </a-col>

          <!-- End Date -->
          <a-col :xs="24" :md="12">
            <a-form-item 
              label="End Date & Time" 
              :validate-status="errors.endDate ? 'error' : ''"
              :help="errors.endDate"
              required
            >
              <a-date-picker 
                v-model:value="form.endDate" 
                :disabled-date="(current) => current && current < (form.startDate || new Date().setHours(0,0,0,0))"
                show-time
                format="DD/MM/YYYY HH:mm"
                size="large"
                style="width: 100%"
                placeholder="Select end date and time..." 
              />
            </a-form-item>
          </a-col>

          <!-- Submit -->
          <a-col :span="24">
            <a-form-item style="margin-top: 24px;">
              <a-space style="width: 100%; justify-content: flex-end;">
                <router-link to="/campaigns">
                  <a-button size="large">
                    Cancel
                  </a-button>
                </router-link>
                <a-button 
                  type="primary" 
                  size="large"
                  html-type="submit" 
                  :loading="isSubmitting"
                >
                  {{ isSubmitting ? 'Creating...' : 'Create Campaign' }}
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </form>
    </a-card>
    <a-modal v-model:open="showHelp" title="Hướng dẫn tạo chiến dịch" :footer="null" class="help-dialog">
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
    </a-modal>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import api from '@/services/api'

import { 
  InputNumber, 
  Modal, 
  Input, 
  Select, 
  DatePicker,
  Card,
  PageHeader,
  Space,
  Button,
  Row,
  Col,
  FormItem,
  Typography,
  SelectOption
} from 'ant-design-vue';

import { QuestionCircleOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue';

export default {
  name: 'CampaignCreate',
  components: {
    AInputNumber: InputNumber,
    AModal: Modal,
    AInput: Input,
    ASelect: Select,
    ASelectOption: SelectOption,
    ADatePicker: DatePicker,
    ACard: Card,
    APageHeader: PageHeader,
    ASpace: Space,
    AButton: Button,
    ARow: Row,
    ACol: Col,
    AFormItem: FormItem,
    ATypographyText: Typography.Text,
    QuestionCircleOutlined,
    ArrowLeftOutlined
  },
  data() {
    return {
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

  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  background: white;
  margin-bottom: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.campaign-form-card {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.help-dialog .help-content {
  padding: 16px 0;
}

.help-section {
  margin-bottom: 24px;
}

.help-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #262626;
}

.help-text {
  color: #595959;
  line-height: 1.6;
}

.help-steps {
  margin-top: 16px;
}

.help-step {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.step-number {
  width: 24px;
  height: 24px;
  background: #1890ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-right: 12px;
  flex-shrink: 0;
}

.step-content h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.step-content p {
  margin: 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.5;
}

/* Mobile Responsiveness - Phase 2 Implementation */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 12px;
  }

  .page-header {
    margin-bottom: 16px;
    padding: 12px;
  }

  .campaign-form-card {
    margin-bottom: 16px;
  }

  .help-dialog .ant-modal {
    margin: 0;
    max-width: 100vw;
    height: 100vh;
  }

  .help-dialog .ant-modal-content {
    height: 100%;
    border-radius: 0;
  }

  .help-section {
    margin-bottom: 16px;
  }

  .help-step {
    margin-bottom: 12px;
  }
}

@media (max-width: 480px) {
  .dashboard-container {
    padding: 8px;
  }

  .page-header {
    padding: 8px;
    margin-bottom: 12px;
  }

  .help-title {
    font-size: 14px;
  }

  .help-text {
    font-size: 13px;
  }

  .step-number {
    width: 20px;
    height: 20px;
    font-size: 11px;
  }

  .step-content h4 {
    font-size: 13px;
  }

  .step-content p {
    font-size: 12px;
  }
}
</style>

