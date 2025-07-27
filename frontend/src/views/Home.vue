<template>
  <div class="home">
    <div class="redirect-message">
      <h2>Redirecting...</h2>
      <ProgressSpinner />
    </div>
  </div>
</template>

<script>
import ProgressSpinner from 'primevue/progressspinner'

export default {
  name: 'Home',
  components: {
    ProgressSpinner
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
