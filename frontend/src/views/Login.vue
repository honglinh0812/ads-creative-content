<template>
  <div class="login-container">
    <div class="login-background">
      <div class="background-gradient"></div>
      <div class="background-blur background-blur--one"></div>
      <div class="background-blur background-blur--two"></div>
    </div>
    <a-card class="login-card glass-effect">
      <template #title>
        <div class="login-header">
          <div class="logo-container">
            <img src="/logo.svg" :alt="$t('auth.page.logoAlt')" class="facebook-logo" />
          </div>
          <h1 class="login-title">{{ $t('auth.page.title') }}</h1>
        </div>
      </template>
      <div class="login-content">
        <p class="login-description">
          {{ $t('auth.page.description') }}
        </p>

        <div v-if="mode === 'login'" class="login-options">
          <div class="login-methods">
            <div class="social-login-buttons">
              <a-button
                type="primary"
                size="large"
                class="facebook-login-button"
                @click="handleLoginFacebook"
                :loading="loadingFacebook"
                block
              >
                <template #icon>
                  <facebook-outlined />
                </template>
                {{ $t('auth.loginForm.facebookButton') }}
              </a-button>

              <a-button
                size="large"
                class="google-login-button"
                @click="handleLoginGoogle"
                :loading="loadingGoogle"
                block
              >
                <template #icon>
                  <google-outlined />
                </template>
                {{ $t('auth.loginForm.googleButton') }}
              </a-button>
            </div>

            <a-divider>{{ $t('auth.loginForm.or') }}</a-divider>

            <FieldError :error="loginError" />

            <form @submit.prevent="handleLoginApp" class="login-form">
              <a-input
                v-model:value="loginForm.usernameOrEmail"
                :placeholder="$t('auth.loginForm.usernamePlaceholder')"
                size="large"
                autocomplete="username"
              >
                <template #prefix>
                  <user-outlined />
                </template>
              </a-input>

              <a-input-password
                v-model:value="loginForm.password"
                :placeholder="$t('auth.loginForm.passwordPlaceholder')"
                size="large"
                autocomplete="current-password"
              >
                <template #prefix>
                  <lock-outlined />
                </template>
              </a-input-password>

              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loadingLoginApp"
                block
              >
                {{ $t('auth.loginForm.submitButton') }}
              </a-button>
            </form>

            <div class="login-links">
              <a-space>
                <a-button type="link" @click="mode = 'register'">{{ $t('auth.loginForm.registerLink') }}</a-button>
                <a-divider type="vertical" />
                <a-button type="link" @click="mode = 'forgot'">{{ $t('auth.loginForm.forgotLink') }}</a-button>
              </a-space>
            </div>
          </div>
        </div>

        <div v-else-if="mode === 'register'" class="register-form-wrapper">
          <FieldError :error="registerError" />

          <form @submit.prevent="handleRegister" class="register-form">
            <a-input
              v-model:value="registerForm.username"
              :placeholder="$t('auth.registerForm.usernamePlaceholder')"
              size="large"
              autocomplete="username"
            >
              <template #prefix>
                <user-outlined />
              </template>
            </a-input>

            <a-input
              v-model:value="registerForm.email"
              :placeholder="$t('auth.registerForm.emailPlaceholder')"
              size="large"
              autocomplete="email"
            >
              <template #prefix>
                <mail-outlined />
              </template>
            </a-input>

            <a-input-password
              v-model:value="registerForm.password"
              :placeholder="$t('auth.registerForm.passwordPlaceholder')"
              size="large"
              autocomplete="new-password"
            >
              <template #prefix>
                <lock-outlined />
              </template>
            </a-input-password>

            <a-input-password
              v-model:value="registerForm.confirmPassword"
              :placeholder="$t('auth.registerForm.confirmPasswordPlaceholder')"
              size="large"
              autocomplete="new-password"
            >
              <template #prefix>
                <lock-outlined />
              </template>
            </a-input-password>

            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loadingRegister"
              block
            >
              {{ $t('auth.registerForm.submitButton') }}
            </a-button>
          </form>

          <div class="login-links">
            <a-button type="link" @click="mode = 'login'">{{ $t('auth.registerForm.backToLogin') }}</a-button>
          </div>
        </div>

        <div v-else-if="mode === 'forgot'" class="forgot-form-wrapper">
          <FieldError :error="forgotError" />

          <form @submit.prevent="handleForgotPassword" class="forgot-form">
            <a-input
              v-model:value="forgotForm.email"
              :placeholder="$t('auth.forgotForm.emailPlaceholder')"
              size="large"
              autocomplete="email"
            >
              <template #prefix>
                <mail-outlined />
              </template>
            </a-input>

            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loadingForgot"
              block
            >
              {{ $t('auth.forgotForm.submitButton') }}
            </a-button>
          </form>

          <div class="login-links">
            <a-button type="link" @click="mode = 'login'">{{ $t('auth.forgotForm.backToLogin') }}</a-button>
          </div>

          <a-alert
            v-if="forgotSuccess"
            :message="$t('auth.forgotForm.successAlert')"
            type="success"
            show-icon
            style="margin-top: 16px;"
          />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script>
import { mapActions } from 'vuex'
import FieldError from '@/components/FieldError.vue'
import { getApiBaseUrl, getOAuthLoginUrl } from '@/config/api.config'
import {
  FacebookOutlined,
  GoogleOutlined,
  UserOutlined,
  LockOutlined,
  MailOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'Login',
  components: {
    FieldError,
    FacebookOutlined,
    GoogleOutlined,
    UserOutlined,
    LockOutlined,
    MailOutlined
  },
  data() {
    return {
      mode: 'login', // 'login' | 'register' | 'forgot'
      loadingLoginApp: false,
      loadingFacebook: false,
      loadingGoogle: false,
      loadingRegister: false,
      loadingForgot: false,
      error: '',
      loginError: null,
      registerError: null,
      forgotError: null,
      forgotSuccess: false,
      loginForm: {
        usernameOrEmail: '',
        password: ''
      },
      registerForm: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      forgotForm: {
        email: ''
      }
    }
  },
  mounted() {
    this.resetSocialLoginLoading()
    window.addEventListener('pageshow', this.handlePageShow)
    document.addEventListener('visibilitychange', this.handleVisibilityChange)
  },
  beforeUnmount() {
    window.removeEventListener('pageshow', this.handlePageShow)
    document.removeEventListener('visibilitychange', this.handleVisibilityChange)
  },
  methods: {
    ...mapActions('toast', ['showSuccess', 'showError', 'showInfo']),
    resetSocialLoginLoading() {
      this.loadingFacebook = false
      this.loadingGoogle = false
    },
    handlePageShow() {
      this.resetSocialLoginLoading()
    },
    handleVisibilityChange() {
      if (document.visibilityState === 'visible') {
        this.resetSocialLoginLoading()
      }
    },
    startOAuthLogin(provider, loadingKey) {
      this[loadingKey] = true
      const redirectPath = this.$route.query.redirect || '/dashboard'
      sessionStorage.setItem('redirectAfterLogin', redirectPath)
      window.location.href = getOAuthLoginUrl(provider)
    },
    handleLoginFacebook() {
      this.startOAuthLogin('facebook', 'loadingFacebook')
    },
    handleLoginGoogle() {
      this.startOAuthLogin('google', 'loadingGoogle')
    },
    async handleLoginApp() {
      this.error = ''
      this.loginError = null
      this.loadingLoginApp = true
      try {
        const res = await fetch(`${getApiBaseUrl()}/auth/login-app`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.loginForm)
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))

          // Create error object with fieldErrors support
          const error = new Error(data.message || 'Login failed')
          error.fieldErrors = data.fieldErrors || {}
          error.errorCode = data.error || 'Login Error'
          error.requestId = data.requestId || null

          this.loginError = error
          this.showError({ message: error.message })
          throw error
        }
        const data = await res.json()
        console.log('Login response:', data)
        localStorage.setItem('token', data.token)
        this.$store.commit('auth/SET_TOKEN', data.token)
        // Gọi fetchUser để lấy thông tin user sau khi lưu token
        try {
          await this.$store.dispatch('auth/fetchUser')
        } catch (e) {
          // Nếu lỗi khi fetch user, hiển thị thông báo và không redirect
          this.showError({ message: this.$t('auth.loginFailed') })
          throw e
        }

        this.showSuccess({ message: this.$t('auth.loginForm.success') })

        // Redirect to intended page or dashboard
        const redirectPath = this.$route.query.redirect || '/dashboard'
        this.$router.push(redirectPath)
      } catch (e) {
        this.error = e.message
      } finally {
        this.loadingLoginApp = false
      }
    },
    async handleRegister() {
      this.error = ''
      this.registerError = null
      this.loadingRegister = true
      if (!this.registerForm.username || !this.registerForm.email || !this.registerForm.password) {
        const error = new Error(this.$t('auth.registerForm.validation.allRequired'))
        error.fieldErrors = {
          username: !this.registerForm.username ? this.$t('auth.registerForm.validation.usernameRequired') : '',
          email: !this.registerForm.email ? this.$t('auth.registerForm.validation.emailRequired') : '',
          password: !this.registerForm.password ? this.$t('auth.registerForm.validation.passwordRequired') : ''
        }
        // Remove empty field errors
        Object.keys(error.fieldErrors).forEach(key => {
          if (!error.fieldErrors[key]) delete error.fieldErrors[key]
        })
        this.registerError = error
        this.showError({ message: error.message })
        this.loadingRegister = false
        return
      }
      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        const error = new Error(this.$t('auth.registerForm.validation.passwordMismatch'))
        error.fieldErrors = {
          confirmPassword: this.$t('auth.registerForm.validation.confirmPasswordMismatch')
        }
        this.registerError = error
        this.showError({ message: error.message })
        this.loadingRegister = false
        return
      }
      try {
        const res = await fetch(`${getApiBaseUrl()}/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: this.registerForm.username,
            email: this.registerForm.email,
            password: this.registerForm.password
          })
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))

          // Create error object with fieldErrors support
          const error = new Error(data.message || 'Register failed')
          error.fieldErrors = data.fieldErrors || {}
          error.errorCode = data.error || 'Registration Error'
          error.requestId = data.requestId || null

          this.registerError = error
          this.showError({ message: error.message })
          throw error
        }
        // Đăng ký thành công, chuyển về login
        this.mode = 'login'
        this.error = ''
        this.registerError = null
        this.registerForm = { username: '', email: '', password: '', confirmPassword: '' }
        this.showSuccess({ message: this.$t('auth.registerForm.success') })
      } catch (e) {
        this.error = e.message
      } finally {
        this.loadingRegister = false
      }
    },
    async handleForgotPassword() {
      this.error = ''
      this.forgotError = null
      this.loadingForgot = true
      this.forgotSuccess = false
      if (!this.forgotForm.email) {
        const error = new Error(this.$t('auth.forgotForm.validation.emailRequired'))
        error.fieldErrors = {
          email: this.$t('auth.forgotForm.validation.emailFieldRequired')
        }
        this.forgotError = error
        this.showError({ message: error.message })
        this.loadingForgot = false
        return
      }
      try {
        const res = await fetch(`${getApiBaseUrl()}/auth/forgot-password`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: this.forgotForm.email })
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))

          // Create error object with fieldErrors support
          const error = new Error(data.message || 'Failed to send email')
          error.fieldErrors = data.fieldErrors || {}
          error.errorCode = data.error || 'Forgot Password Error'
          error.requestId = data.requestId || null

          this.forgotError = error
          this.showError({ message: error.message })
          throw error
        }
        this.forgotSuccess = true
        this.error = ''
        this.forgotError = null
        this.showSuccess({ message: this.$t('auth.forgotForm.success') })
      } catch (e) {
        this.error = e.message
      } finally {
        this.loadingForgot = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 48px 16px;
  background: #0a1d37;

  .login-background {
    position: absolute;
    inset: 0;
    z-index: 1;
    pointer-events: none;
    background: url('/img/background.jpg') center/cover no-repeat, #0a1d37;
    filter: blur(4px);
    transform: scale(1.05);
    opacity: 0.95;
  }

  .login-background::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(11, 25, 46, 0.15) 0%, rgba(10, 20, 38, 0.6) 85%);
  }
  
  .login-card {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 520px;
    background: #ffffff;
    border: 1px solid #e0e6eb;
    border-radius: 28px;
    box-shadow:
      0 30px 80px rgba(15, 23, 42, 0.15),
      0 10px 25px rgba(59, 130, 246, 0.08);
    
    :deep(.ant-card-head) {
      border-bottom: none;
      padding: 32px 32px 0;
    }
    
    :deep(.ant-card-body) {
      padding: 0 32px 32px 32px;
    }
    
    .login-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 2rem;
      
      .logo-container {
        width: 80px;
        height: 80px;
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        border-radius: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1.5rem;
        box-shadow: 0 15px 30px rgba(37, 99, 235, 0.25);
        
        .facebook-logo {
          width: 48px;
          height: 48px;
          filter: brightness(0) invert(1);
        }
      }
      
      .login-title {
        margin: 0;
        color: #1d4ed8;
        font-size: 2rem;
        font-weight: 700;
        text-align: center;
      }
    }
    
    .login-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .login-description {
        text-align: center;
        margin-bottom: 2rem;
        color: #475569;
        font-size: 1.1rem;
        line-height: 1.6;
      }

      .social-login-buttons {
        width: 100%;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
        margin-bottom: 1rem;
      }
      
      .facebook-login-button {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        border: none;
        font-size: 1.05rem;
        height: 48px;
        border-radius: 14px;
        box-shadow: 0 12px 24px rgba(37, 99, 235, 0.25);
        transition: transform 0.2s ease, box-shadow 0.2s ease;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 16px 32px rgba(37, 99, 235, 0.3);
        }
      }

      .google-login-button {
        background: #fff;
        color: #1f2937;
        border: 1px solid #e2e8f0;
        font-size: 1.05rem;
        height: 48px;
        border-radius: 14px;
        transition: box-shadow 0.2s ease;

        &:hover,
        &:focus {
          border-color: #cbd5f5;
          box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
        }
      }
    }
  }
}

.login-options {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-methods {
  width: 100%;
  max-width: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-form, .register-form, .forgot-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}

.login-links {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

@media (width <= 768px) {
  .login-container {
    padding: 20px;
    .login-background {
      filter: blur(6px);
    }
    
    .login-card {
      border-radius: 22px;
      
      :deep(.ant-card-head) {
        padding: 24px 24px 0;
      }
      
      :deep(.ant-card-body) {
        padding: 0 24px 24px 24px;
      }
      
      .login-header {
        .logo-container {
          width: 64px;
          height: 64px;
          
          .facebook-logo {
            width: 36px;
            height: 36px;
          }
        }
        
        .login-title {
          font-size: 1.6rem;
        }
      }
    }
  }
}
</style>
