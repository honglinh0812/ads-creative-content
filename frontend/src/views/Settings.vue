<template>
  <div class="settings-page">
    <header class="page-header">
      <div>
        <h1>Settings</h1>
        <p>Review and update the preferences that power your workspace.</p>
      </div>
      <div class="header-actions">
        <a-button @click="loadSettings" :loading="loading">
          Reload
        </a-button>
        <a-button type="primary" danger @click="handleLogout">
          <template #icon><logout-outlined /></template>
          Logout
        </a-button>
      </div>
    </header>

    <a-spin :spinning="loading">
      <div class="settings-layout">
        <div class="panels-column">
          <section id="general" class="settings-panel">
            <div class="panel-header">
              <h2>General Preferences</h2>
              <p>Language, timezone, and theme are synced across the application.</p>
            </div>
            <a-form
              :model="generalSettings"
              layout="vertical"
              @finish="updateGeneralSettings"
              :disabled="loading"
            >
              <div class="form-grid">
                <a-form-item label="Language" name="language">
                  <a-select v-model:value="generalSettings.language">
                    <a-select-option
                      v-for="lang in languageOptions"
                      :key="lang.value"
                      :value="lang.value"
                    >
                      {{ lang.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="Timezone" name="timezone">
                  <a-select
                    v-model:value="generalSettings.timezone"
                    show-search
                    option-filter-prop="children"
                  >
                    <a-select-option
                      v-for="tz in timezoneOptions"
                      :key="tz.value"
                      :value="tz.value"
                    >
                      {{ tz.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </div>

              <div class="form-grid">
                <a-form-item label="Theme" name="theme">
                  <a-radio-group v-model:value="generalSettings.theme" @change="onThemeChange">
                    <a-radio value="light">Light</a-radio>
                    <a-radio value="dark">Dark</a-radio>
                    <a-radio value="auto">Auto</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="Auto save changes" name="autoSave">
                  <div class="switch-row">
                    <a-switch v-model:checked="generalSettings.autoSave" />
                    <span>When enabled, drafts are saved automatically.</span>
                  </div>
                </a-form-item>
              </div>

              <div class="panel-actions">
                <a-button html-type="submit" type="primary" :loading="updatingGeneral" :disabled="loading">
                  Save Preferences
                </a-button>
              </div>
            </a-form>
          </section>

          <section id="ai" class="settings-panel">
            <div class="panel-header">
              <h2>AI Automation</h2>
              <p>Pick the default provider and guardrails for creative generation.</p>
            </div>
            <div class="panel-highlight">
              <div>
                <span class="highlight-label">Active provider</span>
                <strong>{{ resolvedProviderName }}</strong>
              </div>
              <div>
                <span class="highlight-label">Creativity</span>
                <strong>{{ Math.round(aiSettings.creativity * 100) }}%</strong>
              </div>
            </div>
            <a-form
              :model="aiSettings"
              layout="vertical"
              @finish="updateAISettings"
              :disabled="loading"
            >
              <div class="form-grid">
                <a-form-item label="Default provider" name="defaultProvider">
                  <a-select v-model:value="aiSettings.defaultProvider" :loading="providersLoading">
                    <a-select-option
                      v-for="provider in providerOptions"
                      :key="provider.value"
                      :value="provider.value"
                    >
                      {{ provider.label }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="Output quality" name="quality">
                  <a-radio-group v-model:value="aiSettings.quality">
                    <a-radio value="low">Fast (low)</a-radio>
                    <a-radio value="medium">Balanced</a-radio>
                    <a-radio value="high">Best (high)</a-radio>
                  </a-radio-group>
                </a-form-item>
              </div>

              <a-form-item label="Creativity" name="creativity">
                <div class="slider-row">
                  <a-slider v-model:value="aiSettings.creativity" :min="0" :max="1" :step="0.05" />
                  <a-input-number
                    v-model:value="aiSettings.creativity"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    style="margin-left: 16px; width: 90px"
                  />
                </div>
                <p class="help-text">0 keeps copy close to inputs, 1 allows more experimentation.</p>
              </a-form-item>

              <a-form-item label="Auto optimization" name="autoOptimize">
                <div class="switch-row">
                  <a-switch v-model:checked="aiSettings.autoOptimize" />
                  <span>Re-rank generated variants before saving.</span>
                </div>
              </a-form-item>

              <div class="panel-actions">
                <a-button html-type="submit" type="primary" :loading="updatingAI" :disabled="loading">
                  Save AI Settings
                </a-button>
              </div>
            </a-form>
          </section>

          <section id="notifications" class="settings-panel">
            <div class="panel-header">
              <h2>Notifications</h2>
              <p>Decide when we should ping you about campaign activity.</p>
            </div>
            <div class="panel-highlight secondary">
              <div>
                <span class="highlight-label">Email channels</span>
                <strong>{{ emailNotificationCount }} enabled</strong>
              </div>
              <div>
                <span class="highlight-label">Browser alerts</span>
                <strong>{{ notificationSettings.browserNotifications ? 'On' : 'Off' }}</strong>
              </div>
            </div>
            <a-form
              :model="notificationSettings"
              layout="vertical"
              @finish="updateNotificationSettings"
              :disabled="loading"
            >
              <div class="notification-grid">
                <div class="toggle-card">
                  <h4>Email notifications</h4>
                  <p>Keep an eye on campaign health and reports.</p>
                  <div class="toggle-item">
                    <div>
                      <strong>Campaign updates</strong>
                      <p>Delivery, budget, and approval status changes.</p>
                    </div>
                    <a-switch v-model:checked="notificationSettings.campaignUpdates" />
                  </div>
                  <div class="toggle-item">
                    <div>
                      <strong>Weekly report</strong>
                      <p>Performance overview every Monday.</p>
                    </div>
                    <a-switch v-model:checked="notificationSettings.weeklyReports" />
                  </div>
                  <div class="toggle-item">
                    <div>
                      <strong>General email alerts</strong>
                      <p>Reminders that are not tied to a single campaign.</p>
                    </div>
                    <a-switch v-model:checked="notificationSettings.emailNotifications" />
                  </div>
                </div>
                <div class="toggle-card">
                  <h4>Browser & push</h4>
                  <p>Surface urgent issues while you are online.</p>
                  <div class="toggle-item">
                    <div>
                      <strong>Browser alerts</strong>
                      <p>Requires granting permission to this site.</p>
                    </div>
                    <a-switch v-model:checked="notificationSettings.browserNotifications" />
                  </div>
                  <div class="toggle-item">
                    <div>
                      <strong>Push notifications</strong>
                      <p>Use push services when they are available.</p>
                    </div>
                    <a-switch v-model:checked="notificationSettings.pushNotifications" />
                  </div>
                </div>
              </div>

              <div class="panel-actions">
                <a-button html-type="submit" type="primary" :loading="updatingNotifications" :disabled="loading">
                  Save Notification Settings
                </a-button>
              </div>
            </a-form>
          </section>
        </div>

        <aside class="aside-column">
          <div class="aside-card">
            <h3>Account summary</h3>
            <p>Signed in as <strong>{{ user?.email || '—' }}</strong></p>
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="Name">
                {{ user?.name || 'Not provided' }}
              </a-descriptions-item>
              <a-descriptions-item label="Preferred language">
                {{ resolveLanguageLabel(generalSettings.language) }}
              </a-descriptions-item>
              <a-descriptions-item label="Timezone">
                {{ resolveTimezoneLabel(generalSettings.timezone) }}
              </a-descriptions-item>
            </a-descriptions>
            <a-button block style="margin-top: 16px" @click="$router.push('/profile')">
              Open Profile
            </a-button>
          </div>

          <div class="aside-card muted">
            <h3>Data workspace</h3>
            <p>Data import/export tools will reappear when the backend wires up dedicated endpoints.</p>
            <p class="todo-note">
              TODO: expose /settings/export, /settings/import, and /settings/cache/clear on the backend to enable data tools here.
            </p>
          </div>
        </aside>
      </div>
    </a-spin>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { LogoutOutlined } from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'AppSettings',
  components: {
    LogoutOutlined
  },
  data() {
    return {
      loading: true,
      updatingGeneral: false,
      updatingAI: false,
      updatingNotifications: false,
      providersLoading: false,
      generalSettings: {
        language: 'en',
        timezone: 'Asia/Ho_Chi_Minh',
        theme: 'light',
        autoSave: true
      },
      aiSettings: {
        defaultProvider: 'openai',
        creativity: 0.7,
        quality: 'high',
        autoOptimize: true
      },
      notificationSettings: {
        emailNotifications: true,
        pushNotifications: true,
        campaignUpdates: true,
        weeklyReports: true,
        browserNotifications: true
      },
      providerOptions: [
        { value: 'openai', label: 'OpenAI GPT' },
        { value: 'gemini', label: 'Google Gemini' }
      ],
      languageOptions: [
        { value: 'en', label: 'English' },
        { value: 'vi', label: 'Tiếng Việt' }
      ],
      timezoneOptions: [
        { value: 'Asia/Ho_Chi_Minh', label: 'Ho Chi Minh City (GMT+7)' },
        { value: 'America/New_York', label: 'New York (GMT-5)' },
        { value: 'Europe/London', label: 'London (GMT+0)' },
        { value: 'Asia/Tokyo', label: 'Tokyo (GMT+9)' },
        { value: 'UTC', label: 'UTC' }
      ],
      // TODO: Data import/export widgets should return once /settings/export, /settings/import, and /settings/cache/clear exist.
    }
  },
  computed: {
    ...mapGetters('auth', ['user']),
    resolvedProviderName() {
      const match = this.providerOptions.find(option => option.value === this.aiSettings.defaultProvider)
      return match ? match.label : this.aiSettings.defaultProvider
    },
    emailNotificationCount() {
      const checks = ['emailNotifications', 'campaignUpdates', 'weeklyReports']
      return checks.filter(key => this.notificationSettings[key]).length
    }
  },
  async mounted() {
    await Promise.all([this.loadSettings(), this.loadProviderOptions()])
  },
  methods: {
    async loadSettings() {
      this.loading = true
      try {
        const response = await api.settings.getSettings()
        const data = response.data || {}
        const general = data.general || {}
        const ai = data.ai || {}
        const notifications = data.notifications || {}

        this.generalSettings = {
          language: general.language || 'en',
          timezone: general.timezone || 'Asia/Ho_Chi_Minh',
          theme: general.theme || localStorage.getItem('theme') || 'light',
          autoSave: general.autoSave !== undefined ? general.autoSave : true
        }

        this.aiSettings = {
          defaultProvider: ai.defaultProvider || 'openai',
          creativity: typeof ai.creativity === 'number' ? ai.creativity : 0.7,
          quality: ai.quality || 'high',
          autoOptimize: ai.autoOptimize !== undefined ? ai.autoOptimize : true
        }

        this.notificationSettings = {
          emailNotifications: notifications.emailNotifications !== undefined ? notifications.emailNotifications : true,
          pushNotifications: notifications.pushNotifications !== undefined ? notifications.pushNotifications : true,
          campaignUpdates: notifications.campaignUpdates !== undefined ? notifications.campaignUpdates : true,
          weeklyReports: notifications.weeklyReports !== undefined ? notifications.weeklyReports : true,
          browserNotifications: notifications.browserNotifications !== undefined ? notifications.browserNotifications : true
        }

        this.applyTheme(this.generalSettings.theme)
      } catch (error) {
        console.error('Error loading settings:', error)
        this.$message.error('Failed to load settings')
      } finally {
        this.loading = false
      }
    },

    async loadProviderOptions() {
      this.providersLoading = true
      try {
        const textResponse = await api.providers.getTextProviders()
        if (Array.isArray(textResponse.data) && textResponse.data.length > 0) {
          this.providerOptions = textResponse.data.map(provider => ({
            value: provider.id,
            label: provider.name
          }))
        }
      } catch (error) {
        console.error('Error loading provider options:', error)
        // keep defaults
      } finally {
        this.providersLoading = false
      }
    },

    async updateGeneralSettings() {
      this.updatingGeneral = true
      try {
        await api.settings.updateGeneralSettings({
          language: this.generalSettings.language,
          timezone: this.generalSettings.timezone,
          theme: this.generalSettings.theme,
          autoSave: this.generalSettings.autoSave
        })
        this.applyTheme(this.generalSettings.theme)
        this.$message.success('General settings updated')
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
        await api.settings.updateAISettings({
          defaultProvider: this.aiSettings.defaultProvider,
          creativity: this.aiSettings.creativity,
          quality: this.aiSettings.quality,
          autoOptimize: this.aiSettings.autoOptimize
        })
        this.$message.success('AI settings updated')
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
        await api.settings.updateNotificationSettings({
          emailNotifications: this.notificationSettings.emailNotifications,
          pushNotifications: this.notificationSettings.pushNotifications,
          campaignUpdates: this.notificationSettings.campaignUpdates,
          weeklyReports: this.notificationSettings.weeklyReports,
          browserNotifications: this.notificationSettings.browserNotifications
        })
        this.$message.success('Notification settings updated')
      } catch (error) {
        console.error('Error updating notification settings:', error)
        this.$message.error('Failed to update notification settings')
      } finally {
        this.updatingNotifications = false
      }
    },

    onThemeChange() {
      this.applyTheme(this.generalSettings.theme)
    },

    applyTheme(theme) {
      localStorage.setItem('theme', theme)
      if (theme === 'dark') {
        document.documentElement.classList.add('dark')
      } else if (theme === 'light') {
        document.documentElement.classList.remove('dark')
      } else {
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        if (prefersDark) {
          document.documentElement.classList.add('dark')
        } else {
          document.documentElement.classList.remove('dark')
        }
      }
    },

    resolveLanguageLabel(value) {
      const match = this.languageOptions.find(option => option.value === value)
      return match ? match.label : value
    },

    resolveTimezoneLabel(value) {
      const match = this.timezoneOptions.find(option => option.value === value)
      return match ? match.label : value
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
  padding: 24px;
  background: #f5f6fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.page-header p {
  margin: 4px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 24px;
}

.panels-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-panel {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #edf2f7;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
}

.panel-header h2 {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.panel-header p {
  margin: 0;
  color: #64748b;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #475569;
}

.slider-row {
  display: flex;
  align-items: center;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  border-top: 1px solid #edf2f7;
  padding-top: 16px;
}

.panel-highlight {
  display: flex;
  justify-content: space-between;
  background: #f0f7ff;
  border: 1px solid #bee3f8;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  color: #1e3a8a;
}

.panel-highlight.secondary {
  background: #f6ffed;
  border-color: #c6f6d5;
  color: #276749;
}

.highlight-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: block;
}

.notification-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.toggle-card {
  border: 1px solid #edf2f7;
  border-radius: 12px;
  padding: 16px;
  background: #f8fafc;
}

.toggle-card h4 {
  margin: 0 0 8px;
  font-size: 16px;
}

.toggle-card p {
  margin: 0 0 16px;
  color: #64748b;
}

.toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #e2e8f0;
}

.toggle-item:first-of-type {
  border-top: none;
}

.toggle-item strong {
  display: block;
  margin-bottom: 4px;
}

.aside-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.aside-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #edf2f7;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
}

.aside-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.aside-card p {
  margin: 0 0 8px;
  color: #475569;
}

.aside-card.muted {
  background: #f8fafc;
}

.todo-note {
  font-size: 12px;
  color: #b45309;
  margin: 8px 0 0;
}

@media (max-width: 1024px) {
  .settings-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .panel-highlight {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
