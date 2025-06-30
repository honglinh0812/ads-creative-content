<template>
  <div id="app">
    <router-view />
    <ToastNotifications />
  </div>
</template>

<script>
import ToastNotifications from './components/ToastNotifications.vue'

export default {
  name: 'App',
  components: {
    ToastNotifications
  }
}
</script>

<style>
/* Global styles are handled by the CSS framework */
#app {
  min-height: 100vh;
}
</style>

