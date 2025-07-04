
<template>
  <header class="flex items-center justify-between px-6 py-4 border-b bg-white">
    <button class="lg:hidden text-gray-600" @click="$emit('toggleSidebar')">
      <i class="pi pi-bars text-xl"></i>
    </button>
    <div class="text-lg font-medium text-gray-800">Welcome back!</div>
  </header>
</template>

<script>
export default {}
</script>
