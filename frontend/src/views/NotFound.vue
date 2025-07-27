<template>
  <div class="not-found">
    <div class="not-found__container">
      <h1>404</h1>
      <h2>Page Not Found</h2>
      <p>The page you are looking for does not exist or has been moved.</p>
      <router-link to="/" class="btn btn--primary">Return to Home</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound'
}
</script>

<style scoped>
.notfound-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 70vh;
  text-align: center;
}

.notfound-content {
  max-width: 500px;
  margin: 0 auto;
}

.notfound-title {
  font-size: var(--text-6xl);
  margin-bottom: 0;
  color: var(--color-primary);
  font-weight: var(--font-bold);
}

.notfound-message {
  margin-bottom: var(--space-4);
  color: var(--color-text-secondary);
  font-size: var(--text-lg);
}

.notfound-description {
  margin-bottom: var(--space-6);
  color: var(--color-text-secondary);
}

.notfound-btn {
  display: inline-block;
  padding: var(--space-3) var(--space-6);
  background-color: var(--color-primary);
  color: white;
  border-radius: var(--radius-base);
  font-weight: var(--font-medium);
  font-size: var(--text-base);
  transition: background-color 0.3s;
  border: none;
  cursor: pointer;
}
.notfound-btn:hover {
  background-color: var(--color-info);
}
</style>
