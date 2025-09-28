<template>
  <div class="page-container">
    <!-- Standardized Page Header -->
    <div class="page-header-standard">
      <div class="page-header-content">
        <h1 class="page-title-standard">Settings</h1>
        <p class="page-subtitle-standard">
          Configure your account preferences and application settings
        </p>
      </div>

      <div class="page-actions-standard">
        <button @click="handleLogout" class="btn-danger-standard">
          <logout-outlined />
          Logout
        </button>
      </div>
    </div>

    <!-- Creative Settings Layout -->
    <div class="creative-settings-container">
      <!-- Settings Navigation -->
      <div class="settings-navigation">
        <div class="nav-header">
          <h3 class="nav-title">Settings Menu</h3>
          <p class="nav-subtitle">Customize your experience</p>
        </div>
        <div class="nav-items">
          <div
            v-for="section in settingSections"
            :key="section.key"
            class="nav-item"
            :class="{ active: activeSection === section.key }"
            @click="activeSection = section.key"
          >
            <div class="nav-icon">{{ section.emoji }}</div>
            <div class="nav-content">
              <div class="nav-name">{{ section.name }}</div>
              <div class="nav-desc">{{ section.description }}</div>
            </div>
            <div class="nav-indicator">
              <div class="indicator-dot"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Settings Content -->
      <div class="settings-content">
        <!-- General Settings Section -->
        <div v-if="activeSection === 'general'" class="settings-section">
          <div class="section-header-creative">
            <div class="section-icon-large">⚙️</div>
            <div class="section-info">
              <h2 class="section-title-creative">General Preferences</h2>
              <p class="section-subtitle-creative">Basic settings for your account</p>
            </div>
            <div class="section-decoration">{{ getRandomTip() }}</div>
          </div>

          <div class="creative-form-container">
            <a-form
              :model="generalSettings"
              layout="vertical"
              @finish="updateGeneralSettings"
              class="creative-form"
            >
                <a-form-item label="Default Language">
                  <a-select v-model:value="generalSettings.defaultLanguage" style="width: 100%">
                    <a-select-option value="en">English</a-select-option>
                    <a-select-option value="vi">Tiếng Việt</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Default Currency">
                  <a-select v-model:value="generalSettings.defaultCurrency" style="width: 100%">
                    <a-select-option value="USD">USD - US Dollar</a-select-option>
                    <a-select-option value="VND">VND - Vietnamese Dong</a-select-option>
                    <a-select-option value="EUR">EUR - Euro</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Date Format">
                  <a-select v-model:value="generalSettings.dateFormat" style="width: 100%">
                    <a-select-option value="MM/DD/YYYY">MM/DD/YYYY</a-select-option>
                    <a-select-option value="DD/MM/YYYY">DD/MM/YYYY</a-select-option>
                    <a-select-option value="YYYY-MM-DD">YYYY-MM-DD</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Theme">
                  <a-radio-group v-model:value="generalSettings.theme" @change="onThemeChange">
                    <a-radio value="light">Light</a-radio>
                    <a-radio value="dark">Dark</a-radio>
                    <a-radio value="auto">Auto</a-radio>
                  </a-radio-group>
                  <p class="help-text">Choose your preferred theme. Auto will follow your system preference.</p>
                </a-form-item>

                <div class="form-actions-creative">
                  <button type="submit" class="btn-primary-creative" :disabled="updatingGeneral">
                    <span v-if="updatingGeneral">Saving...</span>
                    <span v-else>Save Preferences 💾</span>
                  </button>
                </div>
              </a-form>
            </div>
          </div>
        </div>

        <!-- AI Provider Settings Section -->
        <div v-if="activeSection === 'ai'" class="settings-section">
          <div class="section-header-creative">
            <div class="section-icon-large">🤖</div>
            <div class="section-info">
              <h2 class="section-title-creative">AI Configuration</h2>
              <p class="section-subtitle-creative">Set your AI provider preferences</p>
            </div>
            <div class="section-decoration">🔮</div>
          </div>

          <div class="creative-form-container">
            <div class="ai-provider-showcase">
              <div class="provider-stats">
                <div class="stat-item">
                  <div class="stat-emoji">✨</div>
                  <div class="stat-number">{{ aiSettings.defaultVariations || 3 }}</div>
                  <div class="stat-label">Variations</div>
                </div>
                <div class="stat-item">
                  <div class="stat-emoji">🎨</div>
                  <div class="stat-number">{{ aiSettings.autoGenerateImages ? 'ON' : 'OFF' }}</div>
                  <div class="stat-label">Auto Images</div>
                </div>
              </div>
            </div>

            <a-form
              :model="aiSettings"
              layout="vertical"
              @finish="updateAISettings"
              class="creative-form"
            >
                <a-form-item label="Default Text Provider">
                  <a-select v-model:value="aiSettings.defaultTextProvider" style="width: 100%">
                    <a-select-option 
                      v-for="provider in textProviderOptions" 
                      :key="provider.value" 
                      :value="provider.value"
                    >
                      {{ provider.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Default Image Provider">
                  <a-select v-model:value="aiSettings.defaultImageProvider" style="width: 100%">
                    <a-select-option 
                      v-for="provider in imageProviderOptions" 
                      :key="provider.value" 
                      :value="provider.value"
                    >
                      {{ provider.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Default Number of Variations">
                  <a-input-number 
                    v-model:value="aiSettings.defaultVariations" 
                    :min="1" 
                    :max="10" 
                    style="width: 100%"
                  />
                </a-form-item>

                <a-form-item label="Auto-generate Images">
                  <a-switch v-model:checked="aiSettings.autoGenerateImages" />
                  <p class="help-text">Automatically generate images for new ads</p>
                </a-form-item>

                <div class="form-actions-creative">
                  <button type="submit" class="btn-primary-creative" :disabled="updatingAI">
                    <span v-if="updatingAI">Updating AI...</span>
                    <span v-else>Save AI Settings 🤖</span>
                  </button>
                </div>
              </a-form>
            </div>
          </div>
        </div>

        <!-- Notification Settings Section -->
        <div v-if="activeSection === 'notifications'" class="settings-section">
          <div class="section-header-creative">
            <div class="section-icon-large">🔔</div>
            <div class="section-info">
              <h2 class="section-title-creative">Notifications</h2>
              <p class="section-subtitle-creative">Stay updated on important events</p>
            </div>
            <div class="section-decoration">📬</div>
          </div>

          <div class="creative-form-container">
            <div class="notification-showcase">
              <div class="notification-summary">
                <div class="summary-item">
                  <div class="summary-icon">📧</div>
                  <div class="summary-text">
                    <span class="summary-count">{{ countEnabledNotifications('email') }}</span>
                    <span class="summary-label">Email alerts</span>
                  </div>
                </div>
                <div class="summary-item">
                  <div class="summary-icon">🖥️</div>
                  <div class="summary-text">
                    <span class="summary-count">{{ notificationSettings.browserNotifications ? 'ON' : 'OFF' }}</span>
                    <span class="summary-label">Browser alerts</span>
                  </div>
                </div>
              </div>
            </div>

            <a-form
              :model="notificationSettings"
              layout="vertical"
              @finish="updateNotificationSettings"
              class="creative-form"
            >
              <div class="creative-switches">
                <div class="switch-group">
                  <h4 class="switch-title">📧 Email Notifications</h4>
                  <div class="creative-switch-item">
                    <a-switch v-model:checked="notificationSettings.emailCampaignUpdates" />
                    <div class="switch-info">
                      <span class="switch-name">Campaign Updates</span>
                      <span class="switch-desc">Get notified when campaigns change status</span>
                    </div>
                  </div>
                  <div class="creative-switch-item">
                    <a-switch v-model:checked="notificationSettings.emailAdGeneration" />
                    <div class="switch-info">
                      <span class="switch-name">Ad Generation Complete</span>
                      <span class="switch-desc">Know when your AI ad creation is finished</span>
                    </div>
                  </div>
                  <div class="creative-switch-item">
                    <a-switch v-model:checked="notificationSettings.emailWeeklyReport" />
                    <div class="switch-info">
                      <span class="switch-name">Weekly Reports</span>
                      <span class="switch-desc">Receive performance summaries every week</span>
                    </div>
                  </div>
                </div>

                <div class="switch-group">
                  <h4 class="switch-title">🖥️ Browser Notifications</h4>
                  <div class="creative-switch-item">
                    <a-switch v-model:checked="notificationSettings.browserNotifications" />
                    <div class="switch-info">
                      <span class="switch-name">Browser Alerts</span>
                      <span class="switch-desc">Real-time notifications in your browser</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-actions-creative">
                <button type="submit" class="btn-primary-creative" :disabled="updatingNotifications">
                  <span v-if="updatingNotifications">Saving...</span>
                  <span v-else>Save Notifications 🔔</span>
                </button>
              </div>
            </a-form>
          </div>
        </div>

        <!-- Data Management Section -->
        <div v-if="activeSection === 'data'" class="settings-section">
          <div class="section-header-creative">
            <div class="section-icon-large">💾</div>
            <div class="section-info">
              <h2 class="section-title-creative">Data Management</h2>
              <p class="section-subtitle-creative">Import, export, and manage your data</p>
            </div>
            <div class="section-decoration">📊</div>
          </div>

          <div class="creative-form-container">
            <div class="data-actions-grid">
              <div class="data-action-card export-card">
                <div class="action-visual">
                  <div class="action-icon">📤</div>
                  <div class="action-bg-pattern"></div>
                </div>
                <div class="action-content">
                  <h4 class="action-title">Export Data</h4>
                  <p class="action-desc">Download all your campaigns and ads data as CSV or JSON</p>
                  <button @click="exportData" class="btn-action-card" :disabled="exporting">
                    <span v-if="exporting">Exporting...</span>
                    <span v-else>Export Now 📤</span>
                  </button>
                </div>
              </div>

              <div class="data-action-card import-card">
                <div class="action-visual">
                  <div class="action-icon">📥</div>
                  <div class="action-bg-pattern"></div>
                </div>
                <div class="action-content">
                  <h4 class="action-title">Import Data</h4>
                  <p class="action-desc">Import campaigns and ads from CSV files</p>
                  <a-upload
                    :before-upload="handleImport"
                    accept=".csv"
                    :show-upload-list="false"
                  >
                    <a-button :loading="importing">
                      <template #icon><upload-outlined /></template>
                      Import Data
                    </a-button>
                  </a-upload>
                </div>
              </div>

              <a-divider />

              <div class="management-item">
                <h4>Clear Cache</h4>
                <p>Clear all cached data and temporary files</p>
                <a-button @click="clearCache" :loading="clearingCache">
                  <template #icon><clear-outlined /></template>
                  Clear Cache
                </a-button>
              </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  LogoutOutlined,
  UploadOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'AppSettings',
  components: {
    LogoutOutlined,
    UploadOutlined,
    ClearOutlined
  },
  data() {
    return {
      generalSettings: {
        defaultLanguage: 'en',
        defaultCurrency: 'USD',
        dateFormat: 'MM/DD/YYYY',
        theme: 'light'
      },
      aiSettings: {
        defaultTextProvider: 'openai',
        defaultImageProvider: 'openai',
        defaultVariations: 3,
        autoGenerateImages: true
      },
      textProviderOptions: [
        { value: 'openai', label: 'OpenAI GPT' },
        { value: 'gemini', label: 'Google Gemini' },
        { value: 'anthropic', label: 'Anthropic Claude' },
        { value: 'huggingface', label: 'Hugging Face' }
      ],
      imageProviderOptions: [
        { value: 'openai', label: 'OpenAI DALL-E' },
        { value: 'fal-ai', label: 'FAL AI' },
        { value: 'stable-diffusion', label: 'Stable Diffusion' }
      ],
      notificationSettings: {
        emailCampaignUpdates: true,
        emailAdGeneration: true,
        emailWeeklyReport: false,
        browserNotifications: true
      },
      updatingGeneral: false,
      updatingAI: false,
      updatingNotifications: false,
      exporting: false,
      importing: false,
      clearingCache: false,
      loading: true,

      // Phase 3: Creative Settings Implementation
      activeSection: 'general',
      settingSections: [
        {
          key: 'general',
          name: 'General',
          description: 'Basic preferences',
          emoji: '⚙️'
        },
        {
          key: 'ai',
          name: 'AI Providers',
          description: 'AI configuration',
          emoji: '🤖'
        },
        {
          key: 'notifications',
          name: 'Notifications',
          description: 'Alert preferences',
          emoji: '🔔'
        },
        {
          key: 'data',
          name: 'Data',
          description: 'Import & export',
          emoji: '💾'
        }
      ]
    }
  },
  async mounted() {
    await Promise.all([
      this.loadSettings(),
      this.loadProviders()
    ])
  },
  methods: {
    async loadSettings() {
      this.loading = true
      try {
        const response = await api.settings.getSettings()
        if (response.data) {
          this.generalSettings = { ...this.generalSettings, ...response.data.general }
          this.aiSettings = { ...this.aiSettings, ...response.data.ai }
          this.notificationSettings = { ...this.notificationSettings, ...response.data.notifications }
        }
        
        // Load theme from localStorage
        const savedTheme = localStorage.getItem('theme')
        if (savedTheme) {
          this.generalSettings.theme = savedTheme
        }
      } catch (error) {
        console.error('Error loading settings:', error)
        this.$message.error('Failed to load settings')
      } finally {
        this.loading = false
      }
    },

    async loadProviders() {
      try {
        // Load text providers for dropdown options
        const textResponse = await api.providers.getTextProviders()
        this.textProviderOptions = textResponse.data.map(provider => ({
          value: provider.id,
          label: provider.name
        }))
        
        // Load image providers for dropdown options
        const imageResponse = await api.providers.getImageProviders()
        this.imageProviderOptions = imageResponse.data.map(provider => ({
          value: provider.id,
          label: provider.name
        }))
        
      } catch (error) {
        console.error('Error loading providers:', error)
        // Keep default options as fallback
      }
    },

    async updateGeneralSettings() {
      this.updatingGeneral = true
      try {
        await api.settings.updateGeneralSettings(this.generalSettings)
        this.$message.success('General settings updated successfully')
      } catch (error) {
        console.error('Error updating general settings:', error)
        this.$message.error('Failed to update general settings')
      } finally {
        this.updatingGeneral = false
      }
    },

    async updateAISettings() {
      this.updatingAI = true
      try {
        await api.settings.updateAISettings(this.aiSettings)
        this.$message.success('AI settings updated successfully')
      } catch (error) {
        console.error('Error updating AI settings:', error)
        this.$message.error('Failed to update AI settings')
      } finally {
        this.updatingAI = false
      }
    },

    async updateNotificationSettings() {
      this.updatingNotifications = true
      try {
        await api.settings.updateNotificationSettings(this.notificationSettings)
        this.$message.success('Notification settings updated successfully')
      } catch (error) {
        console.error('Error updating notification settings:', error)
        this.$message.error('Failed to update notification settings')
      } finally {
        this.updatingNotifications = false
      }
    },

    async exportData() {
      this.exporting = true
      try {
        const response = await api.get('/export/data', { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `ads-data-${new Date().toISOString().split('T')[0]}.csv`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        this.$message.success('Data exported successfully')
      } catch (error) {
        console.error('Error exporting data:', error)
        this.$message.error('Failed to export data')
      } finally {
        this.exporting = false
      }
    },

    async handleImport(file) {
      this.importing = true
      try {
        const formData = new FormData()
        formData.append('file', file)
        await api.post('/import/data', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        this.$message.success('Data imported successfully')
      } catch (error) {
        console.error('Error importing data:', error)
        this.$message.error('Failed to import data')
      } finally {
        this.importing = false
      }
      return false // Prevent default upload
    },

    async clearCache() {
      this.clearingCache = true
      try {
        await api.settings.clearCache()
        this.$message.success('Cache cleared successfully')
      } catch (error) {
        console.error('Error clearing cache:', error)
        this.$message.error('Failed to clear cache')
      } finally {
        this.clearingCache = false
      }
    },

    onThemeChange() {
      // Apply theme immediately when changed
      const theme = this.generalSettings.theme
      localStorage.setItem('theme', theme)
      
      if (theme === 'dark') {
        document.documentElement.classList.add('dark')
      } else if (theme === 'light') {
        document.documentElement.classList.remove('dark')
      } else if (theme === 'auto') {
        // Auto mode - follow system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        if (prefersDark) {
          document.documentElement.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
        }
      }
    },

    handleLogout() {
      this.$store.dispatch('auth/logout')
      this.$router.push('/login')
    },

    // Phase 3: Creative Settings Methods
    getRandomTip() {
      const tips = [
        "💡 Pro tip from a student developer",
        "🚀 Making your experience better",
        "✨ Built with attention to detail",
        "🎯 Optimized for your workflow",
        "💎 Crafted with care",
        "🔧 Student-built, enterprise-ready"
      ]
      return tips[Math.floor(Math.random() * tips.length)]
    },

    countEnabledNotifications(type) {
      if (type === 'email') {
        let count = 0
        if (this.notificationSettings.emailCampaignUpdates) count++
        if (this.notificationSettings.emailAdGeneration) count++
        if (this.notificationSettings.emailWeeklyReport) count++
        return count
      }
      return 0
    }
  }
}
</script>

<style lang="scss" scoped>
/* Creative Settings Styles - Phase 3 Implementation */

.creative-settings-container {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  margin-top: 24px;
}

/* Settings Navigation */
.settings-navigation {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(45, 90, 160, 0.06);
  border: 1px solid #f0f2f5;
  height: fit-content;
  position: sticky;
  top: 24px;
}

.nav-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 4px 0;
}

.nav-subtitle {
  font-size: 12px;
  color: #8c8c8c;
  margin: 0;
}

.nav-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  background: transparent;
}

.nav-item:hover {
  background: #f0f9ff;
  transform: translateX(2px);
}

.nav-item.active {
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.nav-icon {
  font-size: 20px;
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}

.nav-content {
  flex: 1;
}

.nav-name {
  font-size: 14px;
  font-weight: 600;
  color: inherit;
  margin-bottom: 2px;
}

.nav-desc {
  font-size: 11px;
  opacity: 0.8;
  color: inherit;
}

.nav-indicator {
  width: 16px;
  display: flex;
  justify-content: center;
}

.indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-item.active .indicator-dot {
  opacity: 1;
}

/* Settings Content */
.settings-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(45, 90, 160, 0.06);
  border: 1px solid #f0f2f5;
  overflow: hidden;
}

.settings-section {
  padding: 32px;
}

/* Creative Section Headers */
.section-header-creative {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid #f0f2f5;
}

.section-icon-large {
  font-size: 48px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-radius: 20px;
  border: 2px solid #91d5ff;
  flex-shrink: 0;
}

.section-info {
  flex: 1;
}

.section-title-creative {
  font-size: 28px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 8px 0;
  line-height: 1.2;
}

.section-subtitle-creative {
  font-size: 16px;
  color: #8c8c8c;
  margin: 0;
  line-height: 1.4;
}

.section-decoration {
  font-size: 18px;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #ffd6cc;
  color: #d46b08;
  font-weight: 600;
}

/* Creative Form Container */
.creative-form-container {
  max-width: 600px;
}

.creative-form .ant-form-item {
  margin-bottom: 24px;
}

.creative-form .ant-form-item-label > label {
  font-weight: 600;
  color: #262626;
  font-size: 14px;
}

.help-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
  font-style: italic;
}

/* AI Provider Showcase */
.ai-provider-showcase {
  margin-bottom: 32px;
  padding: 20px;
  background: linear-gradient(135deg, #fff8e1 0%, #fff1b0 100%);
  border-radius: 12px;
  border: 1px solid #ffd666;
}

.provider-stats {
  display: flex;
  gap: 24px;
  justify-content: center;
}

.stat-item {
  text-align: center;
}

.stat-emoji {
  font-size: 24px;
  margin-bottom: 8px;
  display: block;
}

.stat-number {
  font-size: 20px;
  font-weight: 700;
  color: #d46b08;
  margin-bottom: 4px;
  display: block;
}

.stat-label {
  font-size: 12px;
  color: #a8071a;
  font-weight: 600;
}

/* Notification Showcase */
.notification-showcase {
  margin-bottom: 24px;
  padding: 20px;
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border-radius: 12px;
  border: 1px solid #b7eb8f;
}

.notification-summary {
  display: flex;
  gap: 32px;
  justify-content: center;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-icon {
  font-size: 24px;
}

.summary-text {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.summary-count {
  font-size: 18px;
  font-weight: 700;
  color: #389e0d;
}

.summary-label {
  font-size: 12px;
  color: #52c41a;
  font-weight: 600;
}

/* Creative Switches */
.creative-switches {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.switch-group {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #f0f2f5;
  background: #fafafa;
}

.switch-title {
  font-size: 16px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 16px 0;
}

.creative-switch-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.creative-switch-item:last-child {
  margin-bottom: 0;
}

.switch-info {
  flex: 1;
}

.switch-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.switch-desc {
  display: block;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

/* Data Actions Grid */
.data-actions-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 32px;
}

.data-action-card {
  background: white;
  border: 2px solid #f0f2f5;
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.data-action-card:hover {
  border-color: #2d5aa0;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(45, 90, 160, 0.15);
}

.export-card {
  border-left: 4px solid #16a085;
}

.import-card {
  border-left: 4px solid #e76f51;
}

.action-visual {
  position: relative;
  margin-bottom: 16px;
}

.action-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

.action-bg-pattern {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 40px;
  height: 40px;
  background: rgba(24, 144, 255, 0.1);
  border-radius: 50%;
  opacity: 0.5;
}

.action-title {
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  margin: 0 0 8px 0;
}

.action-desc {
  font-size: 14px;
  color: #8c8c8c;
  line-height: 1.4;
  margin-bottom: 16px;
}

/* Creative Buttons */
.btn-primary-creative {
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  border: none;
  border-radius: 12px;
  color: white;
  font-weight: 600;
  font-size: 14px;
  padding: 12px 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
}

.btn-primary-creative:hover:not(:disabled) {
  background: linear-gradient(135deg, #1e3a6f 0%, #0d7cc0 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
}

.btn-primary-creative:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-action-card {
  background: #2d5aa0;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 600;
  font-size: 13px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 100%;
}

.btn-action-card:hover:not(:disabled) {
  background: #1e3a6f;
  transform: translateY(-1px);
}

.form-actions-creative {
  display: flex;
  justify-content: flex-end;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f2f5;
}

/* Mobile Responsiveness - Phase 2 Complete */
@media (max-width: 1024px) {
  .creative-settings-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .settings-navigation {
    position: static;
    margin-bottom: 16px;
  }

  .nav-items {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    gap: 8px;
    padding-bottom: 8px;
  }

  .nav-item {
    flex-shrink: 0;
    min-width: 140px;
  }
}

@media (max-width: 768px) {
  .section-header-creative {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .section-icon-large {
    width: 60px;
    height: 60px;
    font-size: 32px;
    margin: 0 auto;
  }

  .section-title-creative {
    font-size: 24px;
  }

  .provider-stats,
  .notification-summary {
    flex-direction: column;
    gap: 16px;
  }

  .data-actions-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .creative-form-container {
    max-width: none;
  }

  .settings-section {
    padding: 24px;
  }
}

@media (max-width: 480px) {
  .nav-items {
    flex-direction: column;
    overflow-x: visible;
  }

  .nav-item {
    min-width: auto;
  }

  .section-title-creative {
    font-size: 20px;
  }

  .settings-section {
    padding: 20px;
  }

  .creative-switches {
    gap: 16px;
  }

  .switch-group {
    padding: 16px;
  }
}
</style>