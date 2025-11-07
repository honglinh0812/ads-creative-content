<template>
  <a-modal
    :open="visible"
    :title="$t('components.facebookInstructions.modal.title')"
    width="700px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="instructions-content">
      <!-- Success Message -->
      <a-result
        status="success"
        :title="$t('components.facebookInstructions.result.title')"
        :sub-title="$t('components.facebookInstructions.result.subtitle')"
      >
        <template #icon>
          <check-circle-outlined style="color: #52c41a" />
        </template>
      </a-result>

      <!-- Step-by-Step Instructions -->
      <div class="steps-container">
        <a-steps direction="vertical" :current="currentStep">
          <a-step>
            <template #title>
              <span class="text-base font-medium">{{ $t('components.facebookInstructions.steps.locate.title') }}</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>{{ $t('components.facebookInstructions.steps.locate.description') }}</p>
                <div class="file-info mt-2 p-3 bg-gray-50 rounded">
                  <file-text-outlined class="mr-2" />
                  <span class="font-mono text-sm">facebook_ads_{{ timestamp }}.{{ format }}</span>
                </div>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">{{ $t('components.facebookInstructions.steps.navigate.title') }}</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>{{ $t('components.facebookInstructions.steps.navigate.description') }}</p>
                <p class="mt-2">{{ $t('components.facebookInstructions.steps.navigate.ifNot') }}</p>
                <a-button type="link" @click="openFacebookAdsManager" class="mt-2">
                  <global-outlined class="mr-1" />
                  {{ $t('components.facebookInstructions.steps.navigate.button') }}
                </a-button>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">{{ $t('components.facebookInstructions.steps.upload.title') }}</span>
            </template>
            <template #description>
              <div class="step-desc">
                <ol class="list-decimal list-inside space-y-2">
                  <li v-html="$t('components.facebookInstructions.steps.upload.step1')"></li>
                  <li v-html="$t('components.facebookInstructions.steps.upload.step2')"></li>
                  <li v-html="$t('components.facebookInstructions.steps.upload.step3')"></li>
                  <li>{{ $t('components.facebookInstructions.steps.upload.step4', { format: format.toUpperCase() }) }}</li>
                </ol>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">{{ $t('components.facebookInstructions.steps.review.title') }}</span>
            </template>
            <template #description>
              <div class="step-desc">
                <ol class="list-decimal list-inside space-y-2">
                  <li>{{ $t('components.facebookInstructions.steps.review.step1') }}</li>
                  <li>{{ $t('components.facebookInstructions.steps.review.step2') }}</li>
                  <li v-html="$t('components.facebookInstructions.steps.review.step3')"></li>
                </ol>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">{{ $t('components.facebookInstructions.steps.monitor.title') }}</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>{{ $t('components.facebookInstructions.steps.monitor.description') }}</p>
                <ul class="list-disc list-inside space-y-1 mt-2">
                  <li>{{ $t('components.facebookInstructions.steps.monitor.step1') }}</li>
                  <li>{{ $t('components.facebookInstructions.steps.monitor.step2') }}</li>
                  <li>{{ $t('components.facebookInstructions.steps.monitor.step3') }}</li>
                </ul>
              </div>
            </template>
          </a-step>
        </a-steps>
      </div>

      <!-- Help Links -->
      <div class="help-links mt-6 p-4 bg-blue-50 rounded">
        <h4 class="font-medium text-blue-900 mb-2">{{ $t('components.facebookInstructions.help.title') }}</h4>
        <div class="space-y-2 text-sm">
          <a
            href="https://www.facebook.com/business/help/152745875104997"
            target="_blank"
            rel="noopener noreferrer"
            class="block text-blue-600 hover:text-blue-800"
          >
            <question-circle-outlined class="mr-1" />
            {{ $t('components.facebookInstructions.help.bulkGuide') }}
          </a>
          <a
            href="https://www.facebook.com/business/help/268591773177064"
            target="_blank"
            rel="noopener noreferrer"
            class="block text-blue-600 hover:text-blue-800"
          >
            <book-outlined class="mr-1" />
            {{ $t('components.facebookInstructions.help.documentation') }}
          </a>
        </div>
      </div>

      <!-- Don't Show Again Option -->
      <div class="dont-show-again mt-4">
        <a-checkbox v-model:checked="dontShowAgain">
          {{ $t('components.facebookInstructions.dontShowAgain') }}
        </a-checkbox>
      </div>

      <!-- Footer Actions -->
      <div class="footer-actions mt-6 flex justify-end space-x-2">
        <a-button @click="handleClose">{{ $t('common.actions.close') }}</a-button>
        <a-button type="primary" @click="handleUnderstood">
          {{ $t('components.facebookInstructions.understand') }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script>
import {
  CheckCircleOutlined,
  FileTextOutlined,
  GlobalOutlined,
  QuestionCircleOutlined,
  BookOutlined
} from '@ant-design/icons-vue'
import { mapActions } from 'vuex'

export default {
  name: 'FacebookInstructionsModal',

  components: {
    CheckCircleOutlined,
    FileTextOutlined,
    GlobalOutlined,
    QuestionCircleOutlined,
    BookOutlined
  },

  props: {
    visible: {
      type: Boolean,
      required: true
    },
    format: {
      type: String,
      default: 'csv'
    },
    timestamp: {
      type: Number,
      default: () => Date.now()
    }
  },

  emits: ['update:visible', 'close'],

  data() {
    return {
      currentStep: -1,
      dontShowAgain: false
    }
  },

  watch: {
    visible(newVal) {
      if (newVal) {
        this.currentStep = -1
        this.loadPreference()
      }
    }
  },

  methods: {
    ...mapActions('fbExport', ['showInstructions']),

    openFacebookAdsManager() {
      const url = 'https://business.facebook.com/adsmanager/manage/ads'
      window.open(url, '_blank', 'noopener,noreferrer')
    },

    handleUnderstood() {
      if (this.dontShowAgain) {
        this.savePreference()
      }
      this.handleClose()
    },

    handleClose() {
      this.$emit('update:visible', false)
      this.$emit('close')
      this.showInstructions(false)
    },

    savePreference() {
      localStorage.setItem('fb-export-hide-instructions', 'true')
    },

    loadPreference() {
      const hideInstructions = localStorage.getItem('fb-export-hide-instructions')
      this.dontShowAgain = hideInstructions === 'true'
    },

    shouldShowInstructions() {
      return localStorage.getItem('fb-export-hide-instructions') !== 'true'
    }
  }
}
</script>

<style scoped>
.instructions-content {
  padding: 8px;
}

.steps-container {
  margin-top: 24px;
}

.step-desc {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.6;
}

.file-info {
  display: inline-flex;
  align-items: center;
}

.help-links {
  border-left: 4px solid #3b82f6;
}

.dont-show-again {
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.footer-actions {
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .step-desc {
    color: #9ca3af;
  }

  .file-info {
    background: #1f2937 !important;
  }

  .help-links {
    background: #1e3a5f !important;
  }

  .dont-show-again,
  .footer-actions {
    border-color: #374151;
  }
}

/* Animation for steps */
:deep(.ant-steps-item-process .ant-steps-item-icon) {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
