<template>
  <div class="reset-container">
    <a-card class="reset-card" title="Reset Password">
      <form @submit.prevent="handleReset" class="reset-form">
        <div v-if="!tokenFromUrl" class="form-group">
          <a-input v-model:value="token" placeholder="Token from email" class="reset-input" />
        </div>
        <div class="form-group">
          <a-input-password v-model:value="newPassword" placeholder="New password" class="reset-input" />
        </div>
        <div class="form-group">
          <a-input-password v-model:value="confirmPassword" placeholder="Confirm new password" class="reset-input" />
        </div>
        <a-button type="primary" html-type="submit" class="btn-reset" :loading="loading" block>
          Reset Password
        </a-button>
      </form>
      <div v-if="error" class="reset-error">{{ error }}</div>
      <div v-if="success" class="reset-success">Reset password successfully! Redirecting to login page...</div>
    </a-card>
  </div>
</template>

<script>
import { Input, Button, Card } from 'ant-design-vue'
import { getApiBaseUrl } from '@/config/api.config'

export default {
  name: 'ResetPassword',
  components: {
    AInput: Input,
    AInputPassword: Input.Password,
    AButton: Button,
    ACard: Card
  },
  data() {
    return {
      token: '',
      newPassword: '',
      confirmPassword: '',
      loading: false,
      error: '',
      success: false
    }
  },
  computed: {
    tokenFromUrl() {
      return this.$route.query.token || ''
    }
  },
  mounted() {
    if (this.tokenFromUrl) {
      this.token = this.tokenFromUrl
    }
  },
  methods: {
    async handleReset() {
      this.error = ''
      this.success = false
      if (!this.token) {
        this.error = 'Missing verification token.'
        return
      }
      if (!this.newPassword || !this.confirmPassword) {
        this.error = 'Please fill in all information.'
        return
      }
      if (this.newPassword !== this.confirmPassword) {
        this.error = 'The re-entered password does not match.'
        return
      }
      this.loading = true
      try {
        const res = await fetch(`${getApiBaseUrl()}/auth/reset-password`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token: this.token, newPassword: this.newPassword })
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))
          throw new Error(data.message || 'Reset password failed')
        }
        this.success = true
        setTimeout(() => {
          this.$router.push('/login')
        }, 2000)
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.reset-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.reset-card {
  width: 100%;
  max-width: 400px;
  padding: 2rem 1.5rem;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 32px rgb(24 119 242 / 8%);
}

.reset-header {
  text-align: center;
  margin-bottom: 1.5rem;
}

.reset-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.reset-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  font-size: 1rem;
}

.btn-reset {
  width: 100%;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 1.1rem;
}

.reset-error {
  color: #e53e3e;
  margin-top: 1rem;
  text-align: center;
}

.reset-success {
  color: #2563eb;
  margin-top: 1rem;
  text-align: center;
}
</style>