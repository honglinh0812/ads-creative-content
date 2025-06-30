<template>
  <div class="home">
    <div class="redirect-message">
      <h2>Redirecting to Create Ad page...</h2>
      <ProgressSpinner />
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  mounted() {
    // Redirect to Create Ad page as per user flow requirements
    this.$router.push('/ad/create');
  }
}
</script>

<style scoped>
.home {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 70vh;
}

.redirect-message {
  text-align: center;
}
</style>
