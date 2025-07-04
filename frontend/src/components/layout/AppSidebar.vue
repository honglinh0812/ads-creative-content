<template>
  <div>
    <!-- Overlay for mobile -->
    <div 
      v-if="open" 
      @click="$emit('close')" 
      class="fixed inset-0 bg-black bg-opacity-30 z-30 lg:hidden">
    </div>

    <!-- Sidebar -->
    <aside
      :class="{ '-translate-x-full': !open }"
      class="app-sidebar fixed z-40 bg-white w-64 h-full transition-transform transform lg:static lg:translate-x-0 border-r"
    >
      <!-- Header -->
      <div class="sidebar-header p-4 border-b">
        <router-link class="text-xl font-bold text-primary-600" to="/">YourApp</router-link>
      </div>

      <!-- Navigation -->
      <nav class="nav flex flex-col p-4 gap-2">
        <router-link active-class="active" class="nav-item" to="/dashboard">Dashboard</router-link>
        <router-link active-class="active" class="nav-item" to="/campaigns">Campaigns</router-link>
        <router-link active-class="active" class="nav-item" to="/ads">Ads</router-link>

        <!-- User Info -->
        <div class="mt-6 border-t pt-4 text-sm text-gray-600">
          <div v-if="user?.name" class="mb-1">
            Logged in as: <strong>{{ user.name }}</strong>
          </div>
          <button @click="logout" class="text-red-500 hover:underline text-sm">Logout</button>
        </div>
      </nav>
    </aside>
  </div>
</template>

<script>
export default {
  name: 'AppSidebar',
  props: {
    open: {
      type: Boolean,
      default: false
    },
    user: {
      type: Object,
      required: true
    },
    logout: {
      type: Function,
      required: true
    }
  }
}
</script>
