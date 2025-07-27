<template>
  <div :class="['ai-provider-card', isSelected ? 'selected' : '']" @click="$emit('select', provider.id)">
    <div class="flex items-start space-x-4">
      <div :class="['w-5 h-5 rounded-full border-2 mt-1 flex items-center justify-center', 
                    isSelected ? 'border-purple-500 bg-purple-500' : 'border-gray-300']">
        <div v-if="isSelected" class="w-2.5 h-2.5 bg-white rounded-full"></div>
      </div>
      <div class="flex-1">
        <h3 class="font-semibold text-gray-900 text-lg mb-2">{{ provider.name }}</h3>
        <p class="text-gray-600">{{ provider.description }}</p>
        <div class="flex flex-wrap gap-1 mt-2">
          <span v-for="capability in provider.capabilities" :key="capability" 
                class="px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-800">
            {{ capability }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AIProviderCard',
  props: {
    provider: {
      type: Object,
      required: true
    },
    isSelected: {
      type: Boolean,
      default: false
    }
  },
  emits: ['select']
}
</script>

<style scoped>
.ai-provider-card {
  @apply p-4 border border-gray-200 rounded-xl cursor-pointer transition-all duration-300 hover:border-purple-300 hover:shadow-md;
}

.ai-provider-card.selected {
  @apply border-purple-500 bg-purple-50 shadow-md;
}
</style> 