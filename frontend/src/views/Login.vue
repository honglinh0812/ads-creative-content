<template>
  <div class="login-container">
    <Card class="login-card">
      <template #title>
        <div class="login-header">
          <img src="../assets/facebook-logo.png" alt="Facebook Logo" class="facebook-logo" />
          <h1>Facebook Ads Automation</h1>
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
          
          <Button 
            label="Login with Facebook" 
            icon="pi pi-facebook" 
            class="facebook-login-button"
            @click="login"
            :loading="loading"
          />
          
          <div class="login-features">
            <h3>Features</h3>
            <ul>
              <li>
                <i class="pi pi-bolt"></i>
                <span>AI-powered ad content generation</span>
              </li>
              <li>
                <i class="pi pi-chart-line"></i>
                <span>Automated campaign management</span>
              </li>
              <li>
                <i class="pi pi-images"></i>
                <span>Multiple ad formats support</span>
              </li>
              <li>
                <i class="pi pi-shield"></i>
                <span>Secure Facebook integration</span>
              </li>
            </ul>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script>
import { mapActions, mapGetters } from 'vuex'

export default {
  name: 'Login',
  computed: {
    ...mapGetters('auth', ['loading', 'error'])
  },
  methods: {
    ...mapActions('auth', ['login'])
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  
  .login-card {
    width: 100%;
    max-width: 500px;
    
    .login-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 1rem;
      
      .facebook-logo {
        width: 60px;
        margin-bottom: 1rem;
      }
      
      h1 {
        margin: 0;
        color: #1877f2;
        font-size: 1.8rem;
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
        color: #65676b;
        font-size: 1.1rem;
      }
      
      .login-error {
        display: flex;
        align-items: center;
        background-color: #ffebee;
        color: #c62828;
        padding: 0.75rem 1rem;
        border-radius: 4px;
        margin-bottom: 1rem;
        width: 100%;
        
        i {
          margin-right: 0.5rem;
        }
      }
      
      .facebook-login-button {
        background-color: #1877f2;
        border-color: #1877f2;
        width: 100%;
        font-size: 1.1rem;
        padding: 0.75rem;
        
        &:hover {
          background-color: #166fe5;
          border-color: #166fe5;
        }
      }
      
      .login-features {
        margin-top: 3rem;
        width: 100%;
        
        h3 {
          font-size: 1.2rem;
          margin-bottom: 1rem;
          color: #1c1e21;
        }
        
        ul {
          list-style: none;
          padding: 0;
          margin: 0;
          
          li {
            display: flex;
            align-items: center;
            margin-bottom: 1rem;
            
            i {
              color: #1877f2;
              margin-right: 0.75rem;
              font-size: 1.2rem;
            }
            
            span {
              color: #1c1e21;
            }
          }
        }
      }
    }
  }
}
</style>
