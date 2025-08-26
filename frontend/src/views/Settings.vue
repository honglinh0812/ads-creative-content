<template>
  <div class="settings-page">
    <!-- Header -->
    <div class="mobile-header">
      <h1>Settings</h1>
      <a-button type="text" @click="handleLogout">
        <template #icon><logout-outlined /></template>
      </a-button>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <div class="settings-container">
        <a-row :gutter="24">
          <!-- General Settings -->
          <a-col :xs="24" :lg="12">
            <a-card title="General Settings" class="settings-card">
              <a-form
                :model="generalSettings"
                layout="vertical"
                @finish="updateGeneralSettings"
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
                  <a-radio-group v-model:value="generalSettings.theme">
                    <a-radio value="light">Light</a-radio>
                    <a-radio value="dark">Dark</a-radio>
                    <a-radio value="auto">Auto</a-radio>
                  </a-radio-group>
                </a-form-item>

                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="updatingGeneral">
                    Save General Settings
                  </a-button>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>

          <!-- AI Provider Settings -->
          <a-col :xs="24" :lg="12">
            <a-card title="AI Provider Settings" class="settings-card">
              <a-form
                :model="aiSettings"
                layout="vertical"
                @finish="updateAISettings"
              >
                <a-form-item label="Default Text Provider">
                  <a-select v-model:value="aiSettings.defaultTextProvider" style="width: 100%">
                    <a-select-option value="openai">OpenAI GPT</a-select-option>
                    <a-select-option value="claude">Anthropic Claude</a-select-option>
                    <a-select-option value="gemini">Google Gemini</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item label="Default Image Provider">
                  <a-select v-model:value="aiSettings.defaultImageProvider" style="width: 100%">
                    <a-select-option value="dalle">DALL-E</a-select-option>
                    <a-select-option value="midjourney">Midjourney</a-select-option>
                    <a-select-option value="stable-diffusion">Stable Diffusion</a-select-option>
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

                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="updatingAI">
                    Save AI Settings
                  </a-button>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>

          <!-- Notification Settings -->
          <a-col :xs="24" :lg="12">
            <a-card title="Notification Settings" class="settings-card">
              <a-form
                :model="notificationSettings"
                layout="vertical"
                @finish="updateNotificationSettings"
              >
                <a-form-item label="Email Notifications">
                  <div class="notification-item">
                    <a-switch v-model:checked="notificationSettings.emailCampaignUpdates" />
                    <span>Campaign status updates</span>
                  </div>
                  <div class="notification-item">
                    <a-switch v-model:checked="notificationSettings.emailAdGeneration" />
                    <span>Ad generation completion</span>
                  </div>
                  <div class="notification-item">
                    <a-switch v-model:checked="notificationSettings.emailWeeklyReport" />
                    <span>Weekly performance reports</span>
                  </div>
                </a-form-item>

                <a-form-item label="Browser Notifications">
                  <div class="notification-item">
                    <a-switch v-model:checked="notificationSettings.browserNotifications" />
                    <span>Enable browser notifications</span>
                  </div>
                </a-form-item>

                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="updatingNotifications">
                    Save Notification Settings
                  </a-button>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>

          <!-- Export & Import Settings -->
          <a-col :xs="24" :lg="12">
            <a-card title="Data Management" class="settings-card">
              <div class="data-management">
                <div class="management-item">
                  <h4>Export Data</h4>
                  <p>Download all your campaigns and ads data</p>
                  <a-button @click="exportData" :loading="exporting">
                    <template #icon><download-outlined /></template>
                    Export Data
                  </a-button>
                </div>

                <a-divider />

                <div class="management-item">
                  <h4>Import Data</h4>
                  <p>Import campaigns and ads from a CSV file</p>
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
            </a-card>
          </a-col>
        </a-row>
      </div>
    </div>
  </div>
</template>

<script>
import {
  LogoutOutlined,
  DownloadOutlined,
  UploadOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'Settings',
  components: {
    LogoutOutlined,
    DownloadOutlined,
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
        defaultImageProvider: 'dalle',
        defaultVariations: 3,
        autoGenerateImages: true
      },
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
      loading: true
    }
  },
  async mounted() {
    await this.loadSettings()
  },
  methods: {
    async loadSettings() {
      this.loading = true
      try {
        const response = await api.get('/settings')
        if (response.data) {
          this.generalSettings = { ...this.generalSettings, ...response.data.general }
          this.aiSettings = { ...this.aiSettings, ...response.data.ai }
          this.notificationSettings = { ...this.notificationSettings, ...response.data.notifications }
        }
      } catch (error) {
        console.error('Error loading settings:', error)
        // Use default settings if API fails
      } finally {
        this.loading = false
      }
    },

    async updateGeneralSettings() {
      this.updatingGeneral = true
      try {
        await api.put('/settings/general', this.generalSettings)
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
        await api.put('/settings/ai', this.aiSettings)
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
        await api.put('/settings/notifications', this.notificationSettings)
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
        await api.post('/cache/clear')
        this.$message.success('Cache cleared successfully')
      } catch (error) {
        console.error('Error clearing cache:', error)
        this.$message.error('Failed to clear cache')
      } finally {
        this.clearingCache = false
      }
    },

    handleLogout() {
      this.$store.dispatch('auth/logout')
      this.$router.push('/login')
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  background: #f5f5f5;
  min-height: 100vh;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.mobile-header h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.main-content {
  padding: 2rem;
}

.settings-container {
  max-width: 1200px;
  margin: 0 auto;
}

.settings-card {
  margin-bottom: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.notification-item span {
  color: #374151;
}

.help-text {
  margin: 4px 0 0 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.data-management {
  .management-item {
    margin-bottom: 1rem;
  }

  .management-item h4 {
    margin: 0 0 0.5rem 0;
    font-weight: 600;
    color: #1f2937;
  }

  .management-item p {
    margin: 0 0 1rem 0;
    color: #6b7280;
    font-size: 0.875rem;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
}
</style>