<template>
  <div class="login-container">
    <div class="login-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
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

        <a-alert
          v-if="error"
          :message="error"
          type="error"
          show-icon
          closable
          style="margin-bottom: 24px;"
          @close="error = ''"
        />

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
      this.loadingLoginApp = true
      try {
        const res = await fetch(`${process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080/api'}/auth/login-app`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(this.loginForm)
        })
        if (!res.ok) {
          const data = await res.json().catch(() => ({}))
          this.showError({ message: data.message || 'Login failed' })
          throw new Error(data.message || 'Login failed')
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
      this.loadingRegister = true
      if (!this.registerForm.username || !this.registerForm.email || !this.registerForm.password) {
        this.error = 'Please fill in all information.'
        this.showError({ message: 'Please fill in all information.' })
        this.loadingRegister = false
        return
      }
      if (this.registerForm.password !== this.registerForm.confirmPassword) {
        this.error = 'The re-entered password does not match.'
        this.showError({ message: 'The re-entered password does not match.' })
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
          this.showError({ message: data.message || 'Register failed' })
          throw new Error(data.message || 'Register failed')
        }
        // Đăng ký thành công, chuyển về login
        this.mode = 'login'
        this.error = ''
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
      this.loadingForgot = true
      this.forgotSuccess = false
      if (!this.forgotForm.email) {
        this.error = 'Please enter your email.'
        this.showError({ message: 'Please enter your email.' })
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
          this.showError({ message: data.message || 'Failed to send email' })
          throw new Error(data.message || 'Failed to send email')
        }
        this.forgotSuccess = true
        this.error = ''
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .login-background {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
    
    .gradient-orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(40px);
      opacity: 0.3;
      animation: float 6s ease-in-out infinite;
      
      &.orb-1 {
        width: 300px;
        height: 300px;
        background: linear-gradient(45deg, #ff6b6b, #feca57);
        top: -150px;
        left: -150px;
        animation-delay: 0s;
      }
      
      &.orb-2 {
        width: 200px;
        height: 200px;
        background: linear-gradient(45deg, #48dbfb, #0abde3);
        top: 50%;
        right: -100px;
        animation-delay: 2s;
      }
      
      &.orb-3 {
        width: 250px;
        height: 250px;
        background: linear-gradient(45deg, #ff9ff3, #f368e0);
        bottom: -125px;
        left: 50%;
        animation-delay: 4s;
      }
    }
  }
  
  .login-card {
    position: relative;
    z-index: 2;
    width: 100%;
    max-width: 500px;
    backdrop-filter: blur(20px);
    background: rgb(255 255 255 / 95%);
    border: 1px solid rgb(255 255 255 / 20%);
    box-shadow: 0 25px 50px rgb(0 0 0 / 10%);
    
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
        background: linear-gradient(135deg, #1877f2, #42a5f5);
        border-radius: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1.5rem;
        box-shadow: 0 10px 30px rgb(24 119 242 / 30%);
        animation: pulse 2s ease-in-out infinite;
        
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
        background: linear-gradient(135deg, #1877f2, #42a5f5);
        background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
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
        background: linear-gradient(135deg, #1877f2, #42a5f5);
        border: none;
        font-size: 1.1rem;
        height: 48px;
        border-radius: 12px;
        box-shadow: 0 10px 30px rgb(24 119 242 / 30%);
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 15px 40px rgb(24 119 242 / 40%);
          background: linear-gradient(135deg, #1877f2, #42a5f5);
        }
        
        &:focus {
          background: linear-gradient(135deg, #1877f2, #42a5f5);
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
            background: rgb(24 119 242 / 5%);
            border-radius: 12px;
            transition: all 0.3s ease;
            height: 100%;
            
            &:hover {
              background: rgb(24 119 242 / 10%);
              transform: translateY(-2px);
            }
            
            .feature-icon {
              width: 40px;
              height: 40px;
              background: linear-gradient(135deg, #1877f2, #42a5f5);
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

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }

  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.05);
  }
}

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
