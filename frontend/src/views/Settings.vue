<template>
  <div class="settings-page">
    <header class="page-header">
      <div>
        <h1>{{ $t('settingsPage.title') }}</h1>
        <p>{{ $t('settingsPage.subtitle') }}</p>
      </div>
    </header>

    <a-spin :spinning="loading">
      <div class="settings-grid">
        <!-- Account Information -->
        <section class="settings-panel">
          <div class="panel-header">
            <h2>{{ $t('profilePage.sections.information') }}</h2>
            <p>{{ accountSummary }}</p>
          </div>
          <a-form :model="profileForm" layout="vertical" @finish="saveProfile">
            <div class="form-grid">
              <a-form-item
                :label="$t('profilePage.fields.fullName')"
                name="name"
                :rules="[{ required: true, message: $t('profilePage.validation.nameRequired') }]"
              >
                <a-input
                  v-model:value="profileForm.name"
                  :placeholder="$t('profilePage.fields.fullName')"
                />
              </a-form-item>
              <a-form-item :label="$t('profilePage.fields.phone')" name="phoneNumber">
                <a-input
                  v-model:value="profileForm.phoneNumber"
                  :placeholder="$t('profilePage.placeholders.phone')"
                />
              </a-form-item>
            </div>

            <div class="static-fields">
              <div>
                <span class="static-label">{{ $t('profilePage.fields.email') }}</span>
                <p>{{ profileMeta.email }}</p>
              </div>
              <div>
                <span class="static-label">{{ $t('profilePage.fields.memberSince', { date: memberSince }) }}</span>
                <p>{{ memberSince }}</p>
              </div>
            </div>

            <div class="panel-actions">
              <a-space>
                <a-button @click="resetProfileForm" :disabled="savingProfile">
                  {{ $t('profilePage.actions.reset') }}
                </a-button>
                <a-button type="primary" html-type="submit" :loading="savingProfile">
                  {{ $t('profilePage.actions.save') }}
                </a-button>
              </a-space>
            </div>
          </a-form>
        </section>

        <!-- Workspace Stats -->
        <section class="settings-panel stats-panel">
          <div class="stats-grid">
            <div class="stat-card">
              <p class="stat-label">{{ $t('profilePage.stats.campaigns') }}</p>
              <p class="stat-value">{{ stats.totalCampaigns }}</p>
              <span>{{ $t('profilePage.stats.campaignsLabel') }}</span>
            </div>
            <div class="stat-card">
              <p class="stat-label">{{ $t('profilePage.stats.ads') }}</p>
              <p class="stat-value">{{ stats.totalAds }}</p>
              <span>{{ $t('profilePage.stats.adsLabel') }}</span>
            </div>
          </div>

          <div class="recent-ads">
            <div class="panel-header compact">
              <h3>{{ $t('profilePage.stats.recentTitle') }}</h3>
              <p>{{ $t('profilePage.stats.recentSubtitle') }}</p>
            </div>
            <div v-if="stats.recentAds.length" class="recent-list">
              <div
                v-for="item in stats.recentAds"
                :key="item.id"
                class="recent-item"
              >
                <div>
                  <strong>{{ item.name || $t('profilePage.stats.untitledAd') }}</strong>
                  <p>{{ formatAdDate(item.createdDate) }}</p>
                </div>
                <span class="status-pill">{{ item.status }}</span>
              </div>
            </div>
            <a-empty v-else :description="$t('profilePage.stats.empty')" />
          </div>
        </section>

        <!-- Security -->
        <section class="settings-panel">
          <div class="panel-header">
            <h2>{{ $t('settingsPage.security.title') }}</h2>
            <p>{{ $t('settingsPage.security.description') }}</p>
          </div>
          <a-form :model="passwordForm" layout="vertical" @finish="handleChangePassword">
            <a-form-item :label="$t('settingsPage.security.current')" name="currentPassword">
              <a-input-password v-model:value="passwordForm.currentPassword" />
            </a-form-item>

            <div class="form-grid">
              <a-form-item
                :label="$t('settingsPage.security.new')"
                name="newPassword"
                :rules="[{ required: true, message: $t('settingsPage.security.validation.newRequired') }]"
              >
                <a-input-password v-model:value="passwordForm.newPassword" />
              </a-form-item>
              <a-form-item
                :label="$t('settingsPage.security.confirm')"
                name="confirmPassword"
                :rules="[{ required: true, message: $t('settingsPage.security.validation.confirmRequired') }]"
              >
                <a-input-password v-model:value="passwordForm.confirmPassword" />
              </a-form-item>
            </div>

            <div class="panel-actions">
              <a-button type="primary" html-type="submit" :loading="changingPassword">
                {{ $t('settingsPage.security.actions.save') }}
              </a-button>
            </div>
          </a-form>
        </section>

        <!-- Danger Zone -->
        <section class="settings-panel danger">
          <div class="panel-header">
            <h2>{{ $t('settingsPage.danger.title') }}</h2>
            <p>{{ $t('settingsPage.danger.description') }}</p>
          </div>
          <a-button danger block @click="showDeleteModal = true">
            {{ $t('settingsPage.danger.cta') }}
          </a-button>
        </section>
      </div>
    </a-spin>

    <a-modal
      v-model:open="showDeleteModal"
      :title="$t('settingsPage.modals.deleteTitle')"
      :ok-text="$t('settingsPage.modals.deleteAction')"
      ok-type="danger"
      @ok="handleDeleteAccount"
      :confirm-loading="deleting"
      @cancel="resetDeleteState"
    >
      <p>{{ $t('settingsPage.modals.deleteDescription') }}</p>
      <p class="confirm-instruction">{{ $t('settingsPage.danger.confirmLabel') }}</p>
      <a-input
        v-model:value="deleteConfirmation"
        :placeholder="$t('settingsPage.danger.confirmPlaceholder')"
      />
    </a-modal>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { mapActions } from 'vuex'
import api from '@/services/api'

export default {
  name: 'AppSettings',
  data() {
    return {
      loading: true,
      savingProfile: false,
      profileForm: {
        name: '',
        phoneNumber: ''
      },
      initialProfile: null,
      profileMeta: {
        email: '',
        createdDate: null
      },
      stats: {
        totalCampaigns: 0,
        totalAds: 0,
        recentAds: []
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      changingPassword: false,
      deleting: false,
      showDeleteModal: false,
      deleteConfirmation: ''
    }
  },
  computed: {
    memberSince() {
      if (!this.profileMeta.createdDate) return '—'
      const formatted = dayjs(this.profileMeta.createdDate)
      return formatted.isValid() ? formatted.format('MMM DD, YYYY') : '—'
    },
    accountSummary() {
      if (!this.profileMeta.email) {
        return this.$t('profilePage.summary.empty')
      }
      return this.$t('profilePage.summary.text', { email: this.profileMeta.email })
    }
  },
  async mounted() {
    await this.loadProfile()
  },
  methods: {
    ...mapActions('auth', ['fetchUser']),
    async loadProfile() {
      this.loading = true
      try {
        const response = await api.auth.getProfile()
        const data = response.data || {}
        this.profileForm = {
          name: data.name || '',
          phoneNumber: data.phoneNumber || ''
        }
        this.initialProfile = { ...this.profileForm }
        this.profileMeta = {
          email: data.email || '',
          createdDate: data.createdDate
        }
        this.stats = {
          totalCampaigns: data.stats?.totalCampaigns || 0,
          totalAds: data.stats?.totalAds || 0,
          recentAds: data.stats?.recentAds || []
        }
      } catch (error) {
        console.error('Error loading profile:', error)
        this.$message.error(this.$t('profilePage.messages.loadError'))
      } finally {
        this.loading = false
      }
    },
    resetProfileForm() {
      if (this.initialProfile) {
        this.profileForm = { ...this.initialProfile }
      }
    },
    formatAdDate(date) {
      if (!date) return ''
      const parsed = dayjs(date)
      return parsed.isValid() ? parsed.format('MMM DD, YYYY') : ''
    },
    async saveProfile() {
      this.savingProfile = true
      try {
        await api.auth.updateProfile({
          name: this.profileForm.name,
          phoneNumber: this.profileForm.phoneNumber
        })
        this.initialProfile = { ...this.profileForm }
        await this.fetchUser()
        this.$message.success(this.$t('profilePage.messages.updateSuccess'))
        await this.loadProfile()
      } catch (error) {
        console.error('Error updating profile:', error)
        this.$message.error(this.$t('profilePage.messages.updateError'))
      } finally {
        this.savingProfile = false
      }
    },
    resetPasswordForm() {
      this.passwordForm = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    },
    async handleChangePassword() {
      if (this.passwordForm.newPassword.length < 6) {
        this.$message.error(this.$t('settingsPage.security.messages.tooShort'))
        return
      }
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.error(this.$t('settingsPage.security.messages.mismatch'))
        return
      }
      this.changingPassword = true
      try {
        await api.auth.changePassword({
          currentPassword: this.passwordForm.currentPassword,
          newPassword: this.passwordForm.newPassword
        })
        this.$message.success(this.$t('settingsPage.security.messages.success'))
        this.resetPasswordForm()
      } catch (error) {
        console.error('Error updating password:', error)
        const msg = error.response?.data?.message || this.$t('settingsPage.security.messages.error')
        this.$message.error(msg)
      } finally {
        this.changingPassword = false
      }
    },
    resetDeleteState() {
      this.deleteConfirmation = ''
      this.showDeleteModal = false
    },
    async handleDeleteAccount() {
      if (this.deleteConfirmation !== 'DELETE') {
        this.$message.error(this.$t('settingsPage.danger.validation'))
        return
      }
      this.deleting = true
      try {
        await api.auth.deleteAccount()
        this.$message.success(this.$t('settingsPage.danger.messages.success'))
        this.$store.dispatch('auth/logout')
      } catch (error) {
        console.error('Error deleting account:', error)
        const msg = error.response?.data?.message || this.$t('settingsPage.danger.messages.error')
        this.$message.error(msg)
      } finally {
        this.deleting = false
        this.resetDeleteState()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.settings-page {
  padding: 24px;
  min-height: 100vh;
  background: #f5f6fa;
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

.settings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
}

.settings-panel {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #edf2f7;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
}

.panel-header {
  margin-bottom: 16px;
}

.panel-header h2,
.panel-header h3 {
  margin: 0 0 4px;
  font-size: 20px;
  color: #1f2937;
}

.panel-header p {
  margin: 0;
  color: #64748b;
}

.panel-header.compact {
  margin-bottom: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.static-fields {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.static-label {
  text-transform: uppercase;
  font-size: 12px;
  color: #94a3b8;
}

.panel-actions {
  border-top: 1px solid #edf2f7;
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.stats-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  background: #f8fafc;
}

.stat-label {
  margin: 0 0 4px;
  text-transform: uppercase;
  font-size: 12px;
  color: #94a3b8;
}

.stat-value {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: #2563eb;
}

.recent-ads {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.recent-item strong {
  display: block;
  margin-bottom: 2px;
}

.status-pill {
  padding: 4px 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 600;
}

.settings-panel.danger {
  border-color: #fecdd3;
}

.confirm-instruction {
  margin: 12px 0 8px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .settings-panel {
    padding: 20px;
  }
}
</style>
