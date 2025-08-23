<template>
  <div class="auth-success-container">
    <div v-if="loading" class="loading-container">
      <div class="spinner spinner-lg"></div>
      <h2>Processing login...</h2>
    </div>
  </div>
</template>

<script>
import { mapMutations, mapActions } from 'vuex';

export default {
  name: 'AuthSuccess',
  data() {
    return {
      loading: true,
      error: null
    };
  },
  created() {
    this.processAuthSuccess();
  },
  methods: {
    ...mapMutations('auth', ['SET_TOKEN']),
    ...mapActions('auth', ['fetchUser']),
    
    async processAuthSuccess() {
      try {
        // Get token from URL fragment
        const hash = window.location.hash.substring(1); // Remove # from start
        const params = new URLSearchParams(hash);
        const token = params.get('token');
        
        if (!token) {
          console.error('Token not found in URL');
          this.$router.push('/login?error=no_token');
          return;
        }
        
        // Save token to store and localStorage
        this.SET_TOKEN(token);
        
        // Load user info
        await this.fetchUser();
        
        // Get redirect path from sessionStorage or default to dashboard
        const redirectPath = sessionStorage.getItem('redirectAfterLogin') || '/dashboard'
        sessionStorage.removeItem('redirectAfterLogin') // Clear after use
        
        // Redirect to target page
        this.$router.push(redirectPath);
      } catch (error) {
        console.error('Login handling error:', error);
        this.$router.push('/login?error=auth_process_failed');
      }
    }
  }
};
</script>

<style scoped>
.auth-success-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  text-align: center;
  padding: 2rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}
</style>
