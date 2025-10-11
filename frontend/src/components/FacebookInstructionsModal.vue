<template>
  <a-modal
    :open="visible"
    title="Upload to Facebook Ads Manager"
    width="700px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="instructions-content">
      <!-- Success Message -->
      <a-result
        status="success"
        title="File Downloaded Successfully!"
        sub-title="Follow these steps to upload your ads to Facebook"
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
              <span class="text-base font-medium">Locate Your Downloaded File</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>Your file has been downloaded to your default downloads folder.</p>
                <div class="file-info mt-2 p-3 bg-gray-50 rounded">
                  <file-text-outlined class="mr-2" />
                  <span class="font-mono text-sm">facebook_ads_{{ timestamp }}.{{ format }}</span>
                </div>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">Navigate to Ads Manager</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>Facebook Ads Manager should have opened in a new tab.</p>
                <p class="mt-2">If not, click the button below:</p>
                <a-button type="link" @click="openFacebookAdsManager" class="mt-2">
                  <global-outlined class="mr-1" />
                  Open Facebook Ads Manager
                </a-button>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">Upload Your Ads</span>
            </template>
            <template #description>
              <div class="step-desc">
                <ol class="list-decimal list-inside space-y-2">
                  <li>Click the <strong>"Create"</strong> button in Ads Manager</li>
                  <li>Select <strong>"Bulk Create"</strong> or <strong>"Import"</strong></li>
                  <li>Choose <strong>"Upload File"</strong></li>
                  <li>Select your downloaded {{ format.toUpperCase() }} file</li>
                </ol>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">Review and Publish</span>
            </template>
            <template #description>
              <div class="step-desc">
                <ol class="list-decimal list-inside space-y-2">
                  <li>Review your ads in the Facebook preview</li>
                  <li>Configure any additional targeting if needed</li>
                  <li>Click <strong>"Publish"</strong> to launch your campaign</li>
                </ol>
              </div>
            </template>
          </a-step>

          <a-step>
            <template #title>
              <span class="text-base font-medium">Monitor Performance</span>
            </template>
            <template #description>
              <div class="step-desc">
                <p>After your ads are running:</p>
                <ul class="list-disc list-inside space-y-1 mt-2">
                  <li>Download performance reports from Facebook</li>
                  <li>Import them back here for analytics</li>
                  <li>Track ROI and optimize your campaigns</li>
                </ul>
              </div>
            </template>
          </a-step>
        </a-steps>
      </div>

      <!-- Help Links -->
      <div class="help-links mt-6 p-4 bg-blue-50 rounded">
        <h4 class="font-medium text-blue-900 mb-2">Need Help?</h4>
        <div class="space-y-2 text-sm">
          <a
            href="https://www.facebook.com/business/help/152745875104997"
            target="_blank"
            rel="noopener noreferrer"
            class="block text-blue-600 hover:text-blue-800"
          >
            <question-circle-outlined class="mr-1" />
            Facebook Bulk Upload Guide
          </a>
          <a
            href="https://www.facebook.com/business/help/268591773177064"
            target="_blank"
            rel="noopener noreferrer"
            class="block text-blue-600 hover:text-blue-800"
          >
            <book-outlined class="mr-1" />
            Ads Manager Documentation
          </a>
        </div>
      </div>

      <!-- Don't Show Again Option -->
      <div class="dont-show-again mt-4">
        <a-checkbox v-model:checked="dontShowAgain">
          Don't show this again
        </a-checkbox>
      </div>

      <!-- Footer Actions -->
      <div class="footer-actions mt-6 flex justify-end space-x-2">
        <a-button @click="handleClose">Close</a-button>
        <a-button type="primary" @click="handleUnderstood">
          I Understand
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
