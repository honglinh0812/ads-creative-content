<template>
  <footer class="footer" aria-label="Footer" role="contentinfo" tabindex="0">
    <span>© {{ new Date().getFullYear() }} Ads Creative Content</span>
  </footer>
</template>

<script>
export default {
  name: 'AppFooter'
}
</script>

<style scoped>
.footer {
  width: 100%;
  text-align: center;
  padding: 1.25rem 0 1rem 0;
  font-size: 0.95rem;
  color: #8b949e;
  background: none;
  letter-spacing: 0.01em;
  outline: none;
}
@media (max-width: 600px) {
  .footer {
    font-size: 0.85rem;
    padding: 1rem 0 0.75rem 0;
  }
}
</style> 