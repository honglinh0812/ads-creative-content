<template>
  <div v-if="visible" class="modal-overlay z-50">
    <div class="modal max-w-md w-full">
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-semibold">{{ title }}</h3>
        <button @click="$emit('update:visible', false)" class="btn btn-sm btn-ghost">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
      <div class="mb-6">
        <p class="text-base text-secondary-700 dark:text-gray-300">{{ message }}</p>
      </div>
      <div class="flex justify-end space-x-3">
        <button @click="$emit('update:visible', false)" class="btn btn-secondary">
          {{ cancelText || 'Cancel' }}
        </button>
        <button @click="handleConfirm" class="btn btn-primary">
          {{ confirmText || 'Confirm' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConfirmModal',
  props: {
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    cancelText: String
  },
  emits: ['update:visible', 'confirm'],
  methods: {
    handleConfirm() {
      this.$emit('update:visible', false)
      this.$emit('confirm')
    }
  }
}</script>

<style scoped>
/* Modal style used globally in the system */
</style>