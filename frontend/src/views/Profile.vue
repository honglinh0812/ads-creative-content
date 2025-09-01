<template>
  <div class="profile-page">
    <!-- Header -->
    <div class="mobile-header">
      <h1>Profile</h1>
      <a-button type="text" @click="handleLogout">
        <template #icon><logout-outlined /></template>
      </a-button>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <div class="profile-container">
        <a-row :gutter="24">
          <!-- Profile Information -->
          <a-col :xs="24" :lg="16">
            <a-card title="Profile Information" class="profile-card">
              <a-form
                :model="profileForm"
                layout="vertical"
                @finish="updateProfile"
              >
                <a-row :gutter="16">
                  <a-col :span="12">
                    <a-form-item
                      label="First Name"
                      name="firstName"
                      :rules="[{ required: true, message: 'Please enter your first name' }]"
                    >
                      <a-input v-model:value="profileForm.firstName" placeholder="Enter first name" />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item
                      label="Last Name"
                      name="lastName"
                      :rules="[{ required: true, message: 'Please enter your last name' }]"
                    >
                      <a-input v-model:value="profileForm.lastName" placeholder="Enter last name" />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-form-item
                  label="Email"
                  name="email"
                  :rules="[{ required: true, type: 'email', message: 'Please enter a valid email' }]"
                >
                  <a-input v-model:value="profileForm.email" placeholder="Enter email" disabled />
                </a-form-item>

                <a-form-item
                  label="Phone Number"
                  name="phoneNumber"
                >
                  <a-input v-model:value="profileForm.phoneNumber" placeholder="Enter phone number" />
                </a-form-item>

                <a-form-item
                  label="Company"
                  name="company"
                >
                  <a-input v-model:value="profileForm.company" placeholder="Enter company name" />
                </a-form-item>

                <a-form-item
                  label="Job Title"
                  name="jobTitle"
                >
                  <a-input v-model:value="profileForm.jobTitle" placeholder="Enter job title" />
                </a-form-item>

                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="updating">
                    Update Profile
                  </a-button>
                </a-form-item>
              </a-form>
            </a-card>
          </a-col>

          <!-- Account Settings -->
          <a-col :xs="24" :lg="8">
            <a-card title="Account Settings" class="settings-card">
              <div class="setting-item">
                <h4>Change Password</h4>
                <p>Update your password to keep your account secure</p>
                <a-button @click="showPasswordModal = true">Change Password</a-button>
              </div>

              <a-divider />

              <div class="setting-item">
                <h4>Language</h4>
                <p>Choose your preferred language</p>
                <a-select v-model:value="profileForm.language" style="width: 100%">
                  <a-select-option value="en">English</a-select-option>
                  <a-select-option value="vi">Tiếng Việt</a-select-option>
                </a-select>
              </div>

              <a-divider />

              <div class="setting-item">
                <h4>Timezone</h4>
                <p>Set your local timezone</p>
                <a-select v-model:value="profileForm.timezone" style="width: 100%">
                  <a-select-option value="Asia/Ho_Chi_Minh">Ho Chi Minh City (GMT+7)</a-select-option>
                  <a-select-option value="America/New_York">New York (GMT-5)</a-select-option>
                  <a-select-option value="Europe/London">London (GMT+0)</a-select-option>
                  <a-select-option value="Asia/Tokyo">Tokyo (GMT+9)</a-select-option>
                </a-select>
              </div>

              <a-divider />

              <div class="setting-item danger">
                <h4>Delete Account</h4>
                <p>Permanently delete your account and all data</p>
                <a-button danger @click="showDeleteModal = true">Delete Account</a-button>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- Change Password Modal -->
    <a-modal
      v-model:open="showPasswordModal"
      title="Change Password"
      @ok="changePassword"
      :confirm-loading="changingPassword"
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

    <!-- Delete Account Modal -->
    <a-modal
      v-model:open="showDeleteModal"
      title="Delete Account"
      @ok="deleteAccount"
      :confirm-loading="deleting"
      ok-text="Delete"
      ok-type="danger"
    >
      <a-alert
        message="Warning"
        description="This action cannot be undone. All your data will be permanently deleted."
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
import {
  LogoutOutlined
} from '@ant-design/icons-vue'
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
      loading: true
    }
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
          this.profileForm = {
            ...this.profileForm,
            ...response.data
          }
        }
      } catch (error) {
        console.error('Error loading profile:', error)
        this.$message.error('Failed to load profile information')
      } finally {
        this.loading = false
      }
    },

    async updateProfile() {
      this.updating = true
      try {
        await api.auth.updateProfile(this.profileForm)
        this.$message.success('Profile updated successfully')
      } catch (error) {
        console.error('Error updating profile:', error)
        this.$message.error('Failed to update profile')
      } finally {
        this.updating = false
      }
    },

    async changePassword() {
      if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
        this.$message.error('New passwords do not match')
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
        this.passwordForm = {
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
      } catch (error) {
        console.error('Error changing password:', error)
        this.$message.error('Failed to change password')
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
        this.$router.push('/login')
      } catch (error) {
        console.error('Error deleting account:', error)
        this.$message.error('Failed to delete account')
      } finally {
        this.deleting = false
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

.profile-container {
  max-width: 1200px;
  margin: 0 auto;
}

.profile-card,
.settings-card {
  margin-bottom: 2rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.setting-item {
  margin-bottom: 1rem;
}

.setting-item h4 {
  margin: 0 0 0.5rem 0;
  font-weight: 600;
  color: #1f2937;
}

.setting-item p {
  margin: 0 0 1rem 0;
  color: #6b7280;
  font-size: 0.875rem;
}

.setting-item.danger h4 {
  color: #dc2626;
}

.setting-item.danger p {
  color: #dc2626;
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }
}
</style>