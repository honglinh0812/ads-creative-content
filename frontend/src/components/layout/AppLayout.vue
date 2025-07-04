
<template>
<div class="app-layout flex flex-col min-h-screen">
<AppHeader @togglesidebar="sidebarOpen = !sidebarOpen"></AppHeader>
<div class="flex flex-1">
<AppSidebar :open="sidebarOpen" :user="user" :logout="logout" @close="sidebarOpen = false" />
<main class="flex-1 p-6 bg-neutral-50">
<slot></slot>
</main>
</div>
</div>
</template>

<script>
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import { ref } from 'vue'

export default {
  props: {
    user: Object,
    logout: Function
  },
  components: { AppSidebar, AppHeader },
  setup() {
    const sidebarOpen = ref(false)
    return { sidebarOpen }
  }
}
</script>
