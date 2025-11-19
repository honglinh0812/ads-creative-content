<template>
  <div class="profile-page">
    <header class="page-header">
      <div>
        <h1>Profile</h1>
        <p>Manage your personal information and account security.</p>
      </div>
      <div class="header-actions">
        <a-button @click="loadProfile" :loading="loading">
          Reload
        </a-button>
        <a-button danger @click="handleLogout">
          <template #icon><logout-outlined /></template>
          Logout
        </a-button>
      </div>
    </header>

    <a-spin :spinning="loading">
      <div class="profile-layout">
        <section class="profile-panel">
          <div class="panel-header">
            <h2>Personal Information</h2>
            <p>These details show up inside approvals, exports, and notifications.</p>
          </div>
          <a-form
            :model="profileForm"
            layout="vertical"
            @finish="updateProfile"
          >
            <div class="form-section">
              <h3>Contact</h3>
              <div class="form-grid">
                <a-form-item
                  label="First Name"
                  name="firstName"
                  :rules="[{ required: true, message: 'Please enter your first name' }]"
                >
                  <a-input v-model:value="profileForm.firstName" placeholder="Enter first name" />
                </a-form-item>
                <a-form-item
                  label="Last Name"
                  name="lastName"
                  :rules="[{ required: true, message: 'Please enter your last name' }]"
                >
                  <a-input v-model:value="profileForm.lastName" placeholder="Enter last name" />
                </a-form-item>
              </div>
              <div class="form-grid">
                <a-form-item
                  label="Email"
                  name="email"
                  :rules="[{ required: true, type: 'email', message: 'Please enter a valid email' }]"
                >
                  <a-input v-model:value="profileForm.email" disabled />
                </a-form-item>
                <a-form-item label="Phone Number" name="phoneNumber">
                  <a-input v-model:value="profileForm.phoneNumber" placeholder="Enter phone number" />
                </a-form-item>
              </div>
            </div>

            <div class="form-section">
              <h3>Work Details</h3>
              <div class="form-grid">
                <a-form-item label="Company" name="company">
                  <a-input v-model:value="profileForm.company" placeholder="Enter company name" />
                </a-form-item>
                <a-form-item label="Job Title" name="jobTitle">
                  <a-input v-model:value="profileForm.jobTitle" placeholder="Enter job title" />
                </a-form-item>
              </div>
            </div>

            <div class="form-section">
              <h3>Localization</h3>
              <div class="form-grid">
                <a-form-item label="Language" name="language">
                  <a-select v-model:value="profileForm.language">
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
                    v-model:value="profileForm.timezone"
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
            </div>

            <div class="panel-actions">
              <a-space>
                <a-button @click="resetForm" :disabled="updating">
                  Reset
                </a-button>
                <a-button type="primary" html-type="submit" :loading="updating">
                  Save Changes
                </a-button>
              </a-space>
            </div>
          </a-form>
        </section>

        <aside class="profile-aside">
          <section class="aside-card">
            <h3>Security</h3>
            <p>Change your password or end the current session.</p>
            <a-button block @click="showPasswordModal = true">
              Change Password
            </a-button>
            <a-divider />
            <h4>Current session</h4>
            <p>{{ user?.email || profileForm.email }}</p>
            <a-button block type="dashed" @click="handleLogout">
              Sign Out
            </a-button>
          </section>

          <section class="aside-card danger">
            <h3>Danger Zone</h3>
            <p>Permanently delete your account and all underlying data.</p>
            <a-button block danger @click="showDeleteModal = true">
              Delete Account
            </a-button>
          </section>
        </aside>
      </div>
    </a-spin>

    <a-modal
      v-model:open="showPasswordModal"
      title="Change Password"
      @ok="changePassword"
      :confirm-loading="changingPassword"
      @cancel="resetPasswordForm"
    >
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item
          label="Current Password"
          name="currentPassword"
          :rules="[{ required: true, message: 'Please enter your current password' }]"
        >
          <a-input-password v-model:value="passwordForm.currentPassword" placeholder="Enter current password" />
        </a-form-item>

        <a-form-item
          label="New Password"
          name="newPassword"
          :rules="[{ required: true, min: 6, message: 'Password must be at least 6 characters' }]"
        >
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="Enter new password" />
        </a-form-item>

        <a-form-item
          label="Confirm New Password"
          name="confirmPassword"
          :rules="[{ required: true, message: 'Please confirm your new password' }]"
        >
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="Confirm new password" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showDeleteModal"
      title="Delete Account"
      @ok="deleteAccount"
      :confirm-loading="deleting"
      ok-text="Delete"
      ok-type="danger"
      @cancel="deleteConfirmation = ''"
    >
      <a-alert
        message="Warning"
        description="This action cannot be undone. All data will be permanently deleted."
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <p>Please type <strong>DELETE</strong> to confirm:</p>
      <a-input v-model:value="deleteConfirmation" placeholder="Type DELETE to confirm" />
    </a-modal>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { LogoutOutlined } from '@ant-design/icons-vue'
import api from '@/services/api'

export default {
  name: 'UserProfile',
  components: {
    LogoutOutlined
  },
  data() {
    return {
      profileForm: {
        firstName: '',
        lastName: '',
        email: '',
        phoneNumber: '',
        company: '',
        jobTitle: '',
        language: 'en',
        timezone: 'Asia/Ho_Chi_Minh'
      },
      passwordForm: {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      deleteConfirmation: '',
      showPasswordModal: false,
      showDeleteModal: false,
      updating: false,
      changingPassword: false,
      deleting: false,
      loading: true,
      initialProfile: null,
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
      ]
    }
  },
  computed: {
    ...mapGetters('auth', ['user'])
  },
  async mounted() {
    await this.loadProfile()
  },
  methods: {
    async loadProfile() {
      this.loading = true
      try {
        const response = await api.auth.getProfile()
        if (response.data) {
          const profile = {
            firstName: response.data.firstName || '',
            lastName: response.data.lastName || '',
            email: response.data.email || '',
            phoneNumber: response.data.phoneNumber || '',
            company: response.data.company || '',
            jobTitle: response.data.jobTitle || '',
            language: response.data.language || 'en',
            timezone: response.data.timezone || 'Asia/Ho_Chi_Minh'
          }
          this.profileForm = { ...profile }
          this.initialProfile = { ...profile }
        }
      } catch (error) {
        console.error('Error loading profile:', error)
        this.$message.error('Failed to load profile information')
      } finally {
        this.loading = false
      }
    },

    resetForm() {
      if (this.initialProfile) {
        this.profileForm = { ...this.initialProfile }
      }
    },

    async updateProfile() {
      this.updating = true
      try {
        const profileData = {
          firstName: this.profileForm.firstName,
          lastName: this.profileForm.lastName,
          phoneNumber: this.profileForm.phoneNumber,
          company: this.profileForm.company,
          jobTitle: this.profileForm.jobTitle,
          language: this.profileForm.language,
          timezone: this.profileForm.timezone
        }
        await api.auth.updateProfile(profileData)
        this.initialProfile = { ...this.profileForm }
        this.$message.success('Profile updated successfully')
      } catch (error) {
        console.error('Error updating profile:', error)
        this.$message.error('Failed to update profile')
      } finally {
        this.updating = false
      }
    },

    resetPasswordForm() {
      this.passwordForm = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
      }
    },

    async changePassword() {
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.error('New passwords do not match')
        return
      }

      if (this.passwordForm.newPassword.length < 6) {
        this.$message.error('Password must be at least 6 characters long')
        return
      }

      this.changingPassword = true
      try {
        await api.auth.changePassword({
          currentPassword: this.passwordForm.currentPassword,
          newPassword: this.passwordForm.newPassword
        })
        this.$message.success('Password changed successfully')
        this.showPasswordModal = false
        this.resetPasswordForm()
      } catch (error) {
        console.error('Error changing password:', error)
        const errorMessage = error.response?.data?.message || 'Failed to change password'
        this.$message.error(errorMessage)
      } finally {
        this.changingPassword = false
      }
    },

    async deleteAccount() {
      if (this.deleteConfirmation !== 'DELETE') {
        this.$message.error('Please type DELETE to confirm')
        return
      }

      this.deleting = true
      try {
        await api.auth.deleteAccount()
        this.$message.success('Account deleted successfully')
        this.$store.dispatch('auth/logout')
        this.$router.push('/login')
      } catch (error) {
        console.error('Error deleting account:', error)
        const errorMessage = error.response?.data?.message || 'Failed to delete account'
        this.$message.error(errorMessage)
      } finally {
        this.deleting = false
        this.deleteConfirmation = ''
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
.profile-page {
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

.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 24px;
}

.profile-panel {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #edf2f7;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
}

.panel-header h2 {
  margin: 0 0 4px;
  font-size: 22px;
}

.panel-header p {
  margin: 0 0 16px;
  color: #64748b;
}

.form-section {
  margin-bottom: 24px;
}

.form-section h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #1e293b;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #edf2f7;
  padding-top: 16px;
}

.profile-aside {
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
  margin: 0 0 12px;
  color: #475569;
}

.aside-card.danger {
  border-color: #fecdd3;
  background: #fff1f2;
}

@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
