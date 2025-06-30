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
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 70vh;
  text-align: center;
}

.not-found__container {
  max-width: 500px;
}

h1 {
  font-size: 6rem;
  margin-bottom: 0;
  color: #4267B2;
}

h2 {
  margin-bottom: 1rem;
}

p {
  margin-bottom: 2rem;
  color: #666;
}

.btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  background-color: #4267B2;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color: #365899;
}
</style>
