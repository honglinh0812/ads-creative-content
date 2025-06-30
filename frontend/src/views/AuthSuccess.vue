<template>
  <div class="auth-success-container">
    <div v-if="loading" class="loading-container">
      <ProgressSpinner />
      <h2>Đang xử lý đăng nhập...</h2>
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
        // Lấy token từ URL fragment
        const hash = window.location.hash.substring(1); // Bỏ dấu # ở đầu
        const params = new URLSearchParams(hash);
        const token = params.get('token');
        
        if (!token) {
          console.error('Không tìm thấy token trong URL');
          this.$router.push('/login?error=no_token');
          return;
        }
        
        // Lưu token vào store và localStorage
        this.SET_TOKEN(token);
        
        // Tải thông tin người dùng
        await this.fetchUser();
        
        // Chuyển hướng đến trang dashboard
        this.$router.push('/dashboard');
      } catch (error) {
        console.error('Lỗi xử lý đăng nhập:', error);
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
