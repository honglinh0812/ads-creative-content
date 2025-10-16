<template>
  <div class="login-container">
    <div class="login-background">
      <!-- Removed AI-generated floating orbs -->
    </div>
    <a-card class="login-card glass-effect">
      <template #title>
        <div class="login-header">
          <div class="logo-container">
            <img src="/logo.svg" alt="Ads Creative Logo" class="facebook-logo" />
          </div>
          <h1 class="login-title">Facebook Ads Automation</h1>
        </div>
      </template>
      <div class="login-content">
        <p class="login-description">
          Create and manage Facebook ads with AI-powered content generation
        </p>

        <div v-if="mode === 'login'" class="login-options">
          <div class="login-methods">
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
              Login with Facebook
            </a-button>

            <a-divider>Or</a-divider>

            <!-- Login Error Display -->
            <FieldError :error="loginError" />

            <form @submit.prevent="handleLoginApp" class="login-form">
              <a-input 
                v-model:value="loginForm.usernameOrEmail" 
                placeholder="Username or Email" 
                size="large"
                autocomplete="username"
              >
                <template #prefix>
                  <user-outlined />
                </template>
              </a-input>
              
              <a-input-password 
                v-model:value="loginForm.password" 
                placeholder="Password" 
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
                Login
              </a-button>
            </form>
            
            <div class="login-links">
              <a-space>
                <a-button type="link" @click="mode = 'register'">Register</a-button>
                <a-divider type="vertical" />
                <a-button type="link" @click="mode = 'forgot'">Forgot password?</a-button>
              </a-space>
            </div>
          </div>
        </div>

        <div v-else-if="mode === 'register'" class="register-form-wrapper">
          <!-- Register Error Display -->
          <FieldError :error="registerError" />

          <form @submit.prevent="handleRegister" class="register-form">
            <a-input 
              v-model:value="registerForm.username" 
              placeholder="Username" 
              size="large"
              autocomplete="username"
            >
              <template #prefix>
                <user-outlined />
              </template>
            </a-input>
            
            <a-input 
              v-model:value="registerForm.email" 
              placeholder="Email" 
              size="large"
              autocomplete="email"
            >
              <template #prefix>
                <mail-outlined />
              </template>
            </a-input>
            
            <a-input-password 
              v-model:value="registerForm.password" 
              placeholder="Password" 
              size="large"
              autocomplete="new-password"
            >
              <template #prefix>
                <lock-outlined />
              </template>
            </a-input-password>
            
            <a-input-password 
              v-model:value="registerForm.confirmPassword" 
              placeholder="Confirm password" 
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
              Register
            </a-button>
          </form>
          
          <div class="login-links">
            <a-button type="link" @click="mode = 'login'">Back to login</a-button>
          </div>
        </div>

        <div v-else-if="mode === 'forgot'" class="forgot-form-wrapper">
          <!-- Forgot Password Error Display -->
          <FieldError :error="forgotError" />

          <form @submit.prevent="handleForgotPassword" class="forgot-form">
            <a-input 
              v-model:value="forgotForm.email" 
              placeholder="Enter your email" 
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
              Send password reset request
            </a-button>
          </form>
          
          <div class="login-links">
            <a-button type="link" @click="mode = 'login'">Back to login</a-button>
          </div>
          
          <a-alert
            v-if="forgotSuccess"
            message="Email sent to reset password (if email exists)."
            type="success"
            show-icon
            style="margin-top: 16px;"
          />
        </div>

        <div class="login-features">
          <a-typography-title :level="3" class="features-title">Features</a-typography-title>
          <a-row :gutter="[16, 16]" class="features-grid">
            <a-col :span="12">
              <div class="feature-item">
                <div class="feature-icon">
                  <thunderbolt-outlined />
                </div>
                <span>AI-powered ad content generation</span>
              </div>
            </a-col>
            
            <a-col :span="12">
              <div class="feature-item">
                <div class="feature-icon">
                  <line-chart-outlined />
                </div>
                <span>Automated campaign management</span>
              </div>
            </a-col>
            
            <a-col :span="12">
              <div class="feature-item">
                <div class="feature-icon">
                  <picture-outlined />
                </div>
                <span>Multiple ad formats support</span>
              </div>
            </a-col>
            
            <a-col :span="12">
              <div class="feature-item">
                <div class="feature-icon">
                  <safety-outlined />
                </div>
                <span>Secure Facebook integration</span>
              </div>
            </a-col>
          </a-row>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script>
import { mapActions } from 'vuex'
import FieldError from '@/components/FieldError.vue'
import {
  FacebookOutlined,
  UserOutlined,
  LockOutlined,
  MailOutlined,
  ThunderboltOutlined,
  LineChartOutlined,
  PictureOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'

export default {
  name: 'Login',
  components: {
    FieldError,
    FacebookOutlined,
    UserOutlined,
    LockOutlined,
    MailOutlined,
    ThunderboltOutlined,
    LineChartOutlined,
    PictureOutlined,
    SafetyOutlined
  },
  data() {
    return {
      mode: 'login', // 'login' | 'register' | 'forgot'
      loadingLoginApp: false,
      loadingFacebook: false,
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
  methods: {
    ...mapActions('toast', ['showSuccess', 'showError', 'showInfo']),
    handleLoginFacebook() {
      this.loadingFacebook = true
      const redirectPath = this.$route.query.redirect || '/dashboard'
      sessionStorage.setItem('redirectAfterLogin', redirectPath)
      window.location.href = `${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/facebook`
    },
    async handleLoginApp() {
      this.error = ''
      this.loginError = null
      this.loadingLoginApp = true
      try {
        const res = await fetch(`${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/login-app`, {
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
          this.showError({ message: 'Đăng nhập thất bại: Không lấy được thông tin người dùng.' })
          throw e
        }
        console.log('Token set in localStorage:', localStorage.getItem('token'))
        this.showSuccess({ message: 'Login successful!' })

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
        const error = new Error('Please fill in all information.')
        error.fieldErrors = {
          username: !this.registerForm.username ? 'Username is required' : '',
          email: !this.registerForm.email ? 'Email is required' : '',
          password: !this.registerForm.password ? 'Password is required' : ''
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
        const error = new Error('The re-entered password does not match.')
        error.fieldErrors = {
          confirmPassword: 'Password confirmation does not match'
        }
        this.registerError = error
        this.showError({ message: error.message })
        this.loadingRegister = false
        return
      }
      try {
        const res = await fetch(`${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/register`, {
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
        this.showSuccess({ message: 'Register successful! Please login.' })
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
        const error = new Error('Please enter your email.')
        error.fieldErrors = {
          email: 'Email is required'
        }
        this.forgotError = error
        this.showError({ message: error.message })
        this.loadingForgot = false
        return
      }
      try {
        const res = await fetch(`${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/forgot-password`, {
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
        this.showSuccess({ message: 'Password reset email sent (if email exists).' })
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
  background: #fafafa;
  
  .login-background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
  }
  
  .login-card {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 500px;
    background: #ffffff;
    border: 1px solid #e0e6eb;
    box-shadow: 0 4px 16px rgba(45, 90, 160, 0.08);
    
    :deep(.ant-card-head) {
      border-bottom: none;
      padding: 24px 24px 0;
    }
    
    :deep(.ant-card-body) {
      padding: 0 24px 24px;
    }
    
    .login-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 2rem;
      
      .logo-container {
        width: 80px;
        height: 80px;
        background: #2d5aa0;
        border-radius: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1.5rem;
        box-shadow: 0 3px 12px rgba(45, 90, 160, 0.15);
        
        .facebook-logo {
          width: 50px;
          height: 50px;
          filter: brightness(0) invert(1);
        }
      }
      
      .login-title {
        margin: 0;
        color: #1877f2;
        font-size: 2rem;
        font-weight: 700;
        text-align: center;
        color: #2d5aa0;
      }
    }
    
    .login-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .login-description {
        text-align: center;
        margin-bottom: 2rem;
        color: #65676b;
        font-size: 1.1rem;
        line-height: 1.6;
      }
      
      .facebook-login-button {
        background: #2d5aa0;
        border: 1px solid #274d89;
        font-size: 1.1rem;
        height: 48px;
        border-radius: 12px;
        transition: all 0.2s ease;

        &:hover {
          background: #274d89;
          border-color: #1f3d6b;
        }

        &:focus {
          background: #274d89;
          border-color: #1f3d6b;
        }
      }
      
      .login-features {
        margin-top: 3rem;
        width: 100%;
        
        .features-title {
          font-size: 1.3rem;
          font-weight: 600;
          margin-bottom: 1.5rem;
          color: #1c1e21;
          text-align: center;
        }
        
        .features-grid {
          .feature-item {
            display: flex;
            align-items: center;
            padding: 1rem;
            background: #f7f9fc;
            border-radius: 8px;
            border: 1px solid #e8eff5;
            height: 100%;

            &:hover {
              background: #f0f4f7;
              border-color: #d6e2eb;
            }
            
            .feature-icon {
              width: 40px;
              height: 40px;
              background: #2d5aa0;
              border-radius: 10px;
              display: flex;
              align-items: center;
              justify-content: center;
              margin-right: 0.75rem;
              flex-shrink: 0;
              
              .anticon {
                color: white;
                font-size: 1.2rem;
              }
            }
            
            span {
              color: #1c1e21;
              font-size: 0.9rem;
              font-weight: 500;
            }
          }
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
  max-width: 350px;
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

/* Clean, authentic styling without AI-generated patterns */

@media (width <= 768px) {
  .login-container {
    padding: 1rem;
    
    .login-card {
      .login-header {
        .logo-container {
          width: 60px;
          height: 60px;
          
          .facebook-logo {
            width: 35px;
            height: 35px;
          }
        }
        
        .login-title {
          font-size: 1.5rem;
        }
      }
      
      .login-content {
        .login-features {
          .features-grid {
            :deep(.ant-col) {
              span: 24 !important;
            }
          }
        }
      }
    }
  }
}
</style>
