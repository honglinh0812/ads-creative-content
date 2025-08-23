<template>
  <div class="home">
    <div class="redirect-message">
      <h2>Redirecting...</h2>
      <a-spin size="large" />
    </div>
  </div>
</template>

<script>
import { Spin } from 'ant-design-vue'

export default {
  name: 'Home',
  components: {
    ASpin: Spin
  },
  mounted() {
    // Check if user is authenticated
    if (this.$store.getters['auth/isAuthenticated']) {
      this.$router.push('/dashboard');
    } else {
      this.$router.push('/login');
    }
  }
}
</script>

<style scoped>
.home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 70vh;
  text-align: center;
}
</style>
