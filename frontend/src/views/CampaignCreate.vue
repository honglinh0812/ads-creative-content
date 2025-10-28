<template>
  <div class="dashboard-container">
    <!-- Page Header -->
    <div class="page-header">
      <a-page-header
        :title="$t('campaign.create.page.title')"
        :sub-title="$t('campaign.create.page.subtitle')"
      >
        <template #extra>
          <a-space>
            <a-button @click="showHelp = true" type="default">
              <template #icon>
                <question-circle-outlined />
              </template>
              {{ $t('campaign.create.page.help') }}
            </a-button>
            <router-link to="/campaigns">
              <a-button type="default">
                <template #icon>
                  <arrow-left-outlined />
                </template>
                {{ $t('campaign.create.page.backToCampaigns') }}
              </a-button>
            </router-link>
          </a-space>
        </template>
      </a-page-header>
    </div>

    <!-- Campaign Form -->
    <a-card class="campaign-form-card" :title="$t('campaign.create.card.title')" style="margin-bottom: 24px;">
      <template #extra>
        <a-typography-text type="secondary">{{ $t('campaign.create.card.subtitle') }}</a-typography-text>
      </template>
      
      <form @submit.prevent="handleSubmit">
        <a-row :gutter="[24, 24]">
          <!-- Campaign Name -->
          <a-col :span="24">
            <a-form-item
              :label="$t('campaign.create.form.label.campaignName')"
              :validate-status="errors.name ? 'error' : ''"
              :help="errors.name"
              required
            >
              <a-input
                v-model:value="form.name"
                size="large"
                :placeholder="$t('campaign.create.form.placeholder.campaignName')"
                @blur="validateForm()"
              />
            </a-form-item>
          </a-col>

          <!-- Objective -->
          <a-col :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.objective')"
              :validate-status="errors.objective ? 'error' : ''"
              :help="errors.objective"
              required
            >
              <a-select
                v-model:value="form.objective"
                size="large"
                :placeholder="$t('campaign.create.form.placeholder.objective')"
              >
                <a-select-option v-for="option in objectiveOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- Budget Type -->
          <a-col :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.budgetType')"
              :validate-status="errors.budgetType ? 'error' : ''"
              :help="errors.budgetType"
              required
            >
              <a-select
                v-model:value="form.budgetType"
                size="large"
                :placeholder="$t('campaign.create.form.placeholder.budgetType')"
              >
                <a-select-option v-for="option in budgetTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <!-- Daily Budget -->
          <a-col v-if="form.budgetType === 'DAILY'" :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.dailyBudget')"
              :validate-status="errors.dailyBudget ? 'error' : ''"
              :help="errors.dailyBudget"
              required
            >
              <a-input-number
                v-model:value="form.dailyBudget"
                :min="1"
                size="large"
                style="width: 100%"
                :placeholder="$t('campaign.create.form.placeholder.dailyBudget')"
                :formatter="value => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="value => value.replace(/\$\s?|(,*)/g, '')"
              />
            </a-form-item>
          </a-col>

          <!-- Total Budget -->
          <a-col v-if="form.budgetType === 'LIFETIME'" :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.totalBudget')"
              :validate-status="errors.totalBudget ? 'error' : ''"
              :help="errors.totalBudget"
              required
            >
              <a-input-number
                v-model:value="form.totalBudget"
                :min="1"
                size="large"
                style="width: 100%"
                :placeholder="$t('campaign.create.form.placeholder.totalBudget')"
                :formatter="value => `$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                :parser="value => value.replace(/\$\s?|(,*)/g, '')"
              />
            </a-form-item>
          </a-col>

          <!-- Start Date -->
          <a-col :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.startDate')"
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
                :placeholder="$t('campaign.create.form.placeholder.startDate')"
              />
            </a-form-item>
          </a-col>

          <!-- End Date -->
          <a-col :xs="24" :md="12">
            <a-form-item
              :label="$t('campaign.create.form.label.endDate')"
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
                :placeholder="$t('campaign.create.form.placeholder.endDate')"
              />
            </a-form-item>
          </a-col>

          <!-- Issue #9: Target Audience (Campaign Level) -->
          <a-col :span="24">
            <AudienceSegmentForm v-model="form.audienceSegment" />
          </a-col>

          <!-- Error Display -->
          <a-col :span="24">
            <FieldError :error="submitError" />
          </a-col>

          <!-- Submit -->
          <a-col :span="24">
            <a-form-item style="margin-top: 24px;">
              <a-space style="width: 100%; justify-content: flex-end;">
                <router-link to="/campaigns">
                  <a-button size="large">
                    {{ $t('campaign.create.form.action.cancel') }}
                  </a-button>
                </router-link>
                <a-button
                  type="primary"
                  size="large"
                  html-type="submit"
                  :loading="isSubmitting"
                >
                  {{ isSubmitting ? $t('campaign.create.form.action.creating') : $t('campaign.create.form.action.create') }}
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </form>
    </a-card>
    <a-modal v-model:open="showHelp" :title="$t('campaign.create.help.title')" :footer="null" class="help-dialog">
      <div class="help-content">
        <div class="help-section">
          <h3 class="help-title">{{ $t('common.action.start') }}</h3>
          <p class="help-text">
            {{ $t('campaign.create.help.intro') }}
          </p>
        </div>
        <div class="help-section">
          <h3 class="help-title">{{ $t('campaign.create.help.steps.title') }}</h3>
          <div class="help-steps">
            <div class="help-step">
              <div class="step-number">1</div>
              <div class="step-content">
                <h4>{{ $t('campaign.create.help.steps.step1.title') }}</h4>
                <p>{{ $t('campaign.create.help.steps.step1.desc') }}</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">2</div>
              <div class="step-content">
                <h4>{{ $t('campaign.create.help.steps.step2.title') }}</h4>
                <p>{{ $t('campaign.create.help.steps.step2.desc') }}</p>
              </div>
            </div>
            <div class="help-step">
              <div class="step-number">3</div>
              <div class="step-content">
                <h4>{{ $t('campaign.create.help.steps.step3.title') }}</h4>
                <p>{{ $t('campaign.create.help.steps.step3.desc') }}</p>
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
import FieldError from '@/components/FieldError.vue'
import AudienceSegmentForm from '@/components/AudienceSegmentForm.vue'

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
    ArrowLeftOutlined,
    FieldError,
    AudienceSegmentForm
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
        // Issue #9: Target audience at campaign level
        audienceSegment: {
          gender: 'ALL',
          minAge: 18,
          maxAge: 65,
          location: '',
          interests: ''
        },
        startDate: '',
        endDate: ''
      },

      errors: {},
      submitError: null,
      showHelp: false
    }
  },

  computed: {
    ...mapState('auth', ['user']),

    objectiveOptions() {
      return [
        { value: 'BRAND_AWARENESS', label: this.$t('campaign.objective.brandAwareness') },
        { value: 'REACH', label: this.$t('campaign.objective.reach') },
        { value: 'TRAFFIC', label: this.$t('campaign.objective.traffic') },
        { value: 'ENGAGEMENT', label: this.$t('campaign.objective.engagement') },
        { value: 'APP_INSTALLS', label: this.$t('campaign.objective.appInstalls') },
        { value: 'VIDEO_VIEWS', label: this.$t('campaign.objective.videoViews') },
        { value: 'LEAD_GENERATION', label: this.$t('campaign.objective.leadGeneration') },
        { value: 'CONVERSIONS', label: this.$t('campaign.objective.conversions') },
        { value: 'CATALOG_SALES', label: this.$t('campaign.objective.catalogSales') },
        { value: 'STORE_TRAFFIC', label: this.$t('campaign.objective.storeTraffic') }
      ]
    },

    budgetTypeOptions() {
      return [
        {
          value: 'DAILY',
          label: this.$t('campaign.create.form.budgetType.daily'),
          description: this.$t('campaign.create.form.budgetType.dailyDesc')
        },
        {
          value: 'LIFETIME',
          label: this.$t('campaign.create.form.budgetType.lifetime'),
          description: this.$t('campaign.create.form.budgetType.lifetimeDesc')
        }
      ]
    },
    
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

    // Issue #9: Convert audienceSegment object to targetAudience string
    formatTargetAudienceString(segment) {
      if (!segment) return '';

      const parts = [];

      // Gender
      if (segment.gender && segment.gender !== 'ALL') {
        parts.push(`Gender: ${segment.gender}`);
      }

      // Age range
      if (segment.minAge && segment.maxAge) {
        parts.push(`Age: ${segment.minAge}-${segment.maxAge}`);
      } else if (segment.minAge) {
        parts.push(`Age: ${segment.minAge}+`);
      }

      // Location
      if (segment.location && segment.location.trim()) {
        parts.push(`Location: ${segment.location.trim()}`);
      }

      // Interests
      if (segment.interests && segment.interests.trim()) {
        parts.push(`Interests: ${segment.interests.trim()}`);
      }

      return parts.join(', ');
    },

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
      this.submitError = null
      try {
        // Issue #9: Convert audienceSegment to targetAudience string
        const campaignData = {
          ...this.form,
          targetAudience: this.formatTargetAudienceString(this.form.audienceSegment)
        };
        // Remove audienceSegment from payload (it's converted to targetAudience)
        delete campaignData.audienceSegment;

        console.log('Submitting campaign form:', campaignData)
        console.log('API object:', api)
        console.log('API campaigns:', api.campaigns)
        const response = await api.campaigns.create(campaignData)
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

        // Store entire error object for FieldError component
        this.submitError = error

        this.showToast({
          type: 'error',
          message: error.message || 'Unable to create campaign. Please try again.'
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

