<template>
  <div class="login-container">
    <div class="login-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>
    <Card class="login-card glass-effect">
      <template #title>
        <div class="login-header">
          <div class="logo-container">
            <img src="/logo.svg" alt="Ads Creative Logo" class="facebook-logo" />
          </div>
          <h1 class="login-title">Facebook Ads Automation</h1>
        </div>
      </template>
      <template #content>
        <div class="login-content">
          <p class="login-description">
            Create and manage Facebook ads with AI-powered content generation
          </p>

          <div v-if="error" class="login-error">
            <i class="pi pi-exclamation-circle"></i>
            <span>{{ error }}</span>
          </div>

          <div v-if="mode === 'login'" class="login-options">
            <div class="login-methods">
              <Button 
                label="Login with Facebook" 
                icon="pi pi-facebook" 
                class="facebook-login-button"
                @click="handleLoginFacebook"
                :loading="loadingFacebook"
              />
              <div class="or-divider">Or</div>
              <form @submit.prevent="handleLoginApp" class="login-form">
                <InputText v-model="loginForm.usernameOrEmail" placeholder="Username or Email" class="login-input" autocomplete="username" />
                <InputText v-model="loginForm.password" type="password" placeholder="Password" class="login-input" autocomplete="current-password" />
                <Button label="Login" type="submit" class="btn-login" :loading="loadingLoginApp" />
              </form>
              <div class="login-links">
                <a href="#" @click.prevent="mode = 'register'">Register</a>
                <span>|</span>
                <a href="#" @click.prevent="mode = 'forgot'">Forgot password?</a>
              </div>
            </div>
          </div>

          <div v-else-if="mode === 'register'" class="register-form-wrapper">
            <form @submit.prevent="handleRegister" class="register-form">
              <InputText v-model="registerForm.username" placeholder="Username" class="login-input" autocomplete="username" />
              <InputText v-model="registerForm.email" placeholder="Email" class="login-input" autocomplete="email" />
              <InputText v-model="registerForm.password" type="password" placeholder="Password" class="login-input" autocomplete="new-password" />
              <InputText v-model="registerForm.confirmPassword" type="password" placeholder="Confirm password" class="login-input" autocomplete="new-password" />
              <Button label="Register" type="submit" class="btn-login" :loading="loadingRegister" />
            </form>
            <div class="login-links">
              <a href="#" @click.prevent="mode = 'login'">Back to login</a>
            </div>
          </div>

          <div v-else-if="mode === 'forgot'" class="forgot-form-wrapper">
            <form @submit.prevent="handleForgotPassword" class="forgot-form">
              <InputText v-model="forgotForm.email" placeholder="Enter your email" class="login-input" autocomplete="email" />
              <Button label="Send password reset request" type="submit" class="btn-login" :loading="loadingForgot" />
            </form>
            <div class="login-links">
              <a href="#" @click.prevent="mode = 'login'">Back to login</a>
            </div>
            <div v-if="forgotSuccess" class="forgot-success">Email sent to reset password (if email exists).</div>
          </div>

          <div class="login-features">
            <h3 class="features-title">Features</h3>
            <div class="features-grid">
              <div class="feature-item">
                <div class="feature-icon">
                  <i class="pi pi-bolt"></i>
                </div>
                <span>AI-powered ad content generation</span>
              </div>
              <div class="feature-item">
                <div class="feature-icon">
                  <i class="pi pi-chart-line"></i>
                </div>
                <span>Automated campaign management</span>
              </div>
              <div class="feature-item">
                <div class="feature-icon">
                  <i class="pi pi-images"></i>
                </div>
                <span>Multiple ad formats support</span>
              </div>
              <div class="feature-item">
                <div class="feature-icon">
                  <i class="pi pi-shield"></i>
                </div>
                <span>Secure Facebook integration</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script>
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Card from 'primevue/card'
import { mapActions } from 'vuex'

export default {
  name: 'Login',
  components: { InputText, Button, Card },
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
    background: rgba(255, 255, 255, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 25px 50px rgba(0, 0, 0, 0.1);
    
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
        box-shadow: 0 10px 30px rgba(24, 119, 242, 0.3);
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
        -webkit-background-clip: text;
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
      
      .login-error {
        display: flex;
        align-items: center;
        background: linear-gradient(135deg, #ff6b6b, #ee5a52);
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 12px;
        margin-bottom: 1.5rem;
        width: 100%;
        box-shadow: 0 5px 15px rgba(255, 107, 107, 0.3);
        
        i {
          margin-right: 0.75rem;
          font-size: 1.2rem;
        }
      }
      
      .facebook-login-button {
        background: linear-gradient(135deg, #1877f2, #42a5f5);
        border: none;
        width: 100%;
        font-size: 1.1rem;
        padding: 1rem;
        border-radius: 12px;
        box-shadow: 0 10px 30px rgba(24, 119, 242, 0.3);
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 15px 40px rgba(24, 119, 242, 0.4);
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
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 1rem;
          
          .feature-item {
            display: flex;
            align-items: center;
            padding: 1rem;
            background: rgba(24, 119, 242, 0.05);
            border-radius: 12px;
            transition: all 0.3s ease;
            
            &:hover {
              background: rgba(24, 119, 242, 0.1);
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
              
              i {
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
.or-divider {
  margin: 1.5rem 0 1rem 0;
  color: #888;
  font-weight: 600;
  text-align: center;
  width: 100%;
  position: relative;
}
.login-form, .register-form, .forgot-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1rem;
}
.login-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  font-size: 1rem;
}
.btn-login {
  width: 100%;
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 1.1rem;
}
.login-links {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  font-size: 0.95rem;
}
.forgot-success {
  color: #2563eb;
  margin-top: 1rem;
  text-align: center;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
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

@media (max-width: 768px) {
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
            grid-template-columns: 1fr;
          }
        }
      }
    }
  }
}
</style>
