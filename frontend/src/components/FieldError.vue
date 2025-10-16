<template>
  <div v-if="hasErrors" class="field-error-container">
    <!-- Field-specific errors -->
    <div
      v-for="(message, field) in fieldErrors"
      :key="field"
      class="field-error-item"
    >
      <span class="field-name">{{ formatFieldName(field) }}:</span>
      <span class="error-message">{{ message }}</span>
    </div>

    <!-- Generic error message if no field errors -->
    <div v-if="!hasFieldErrors && genericMessage" class="generic-error">
      {{ genericMessage }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'FieldError',
  props: {
    error: {
      type: Object,
      default: null
    },
    fieldErrors: {
      type: Object,
      default: () => ({})
    },
    message: {
      type: String,
      default: ''
    }
  },
  computed: {
    computedFieldErrors() {
      // Priority: explicit fieldErrors prop > error.fieldErrors > empty
      if (Object.keys(this.fieldErrors).length > 0) {
        return this.fieldErrors
      }
      return this.error?.fieldErrors || {}
    },
    hasFieldErrors() {
      return Object.keys(this.computedFieldErrors).length > 0
    },
    genericMessage() {
      return this.message || this.error?.message || ''
    },
    hasErrors() {
      return this.hasFieldErrors || this.genericMessage
    }
  },
  methods: {
    formatFieldName(field) {
      // Convert camelCase/snake_case to readable format
      // campaignId -> Campaign ID
      // website_url -> Website URL
      return field
        .replace(/([A-Z])/g, ' $1')
        .replace(/_/g, ' ')
        .replace(/^\w/, c => c.toUpperCase())
        .trim()
    }
  }
}
</script>

<style scoped>
.field-error-container {
  background-color: #FEF2F2;
  border: 1px solid #FCA5A5;
  border-radius: 6px;
  padding: 12px;
  margin-top: 8px;
  margin-bottom: 16px;
}

.field-error-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 6px;
  font-size: 14px;
}

.field-error-item:last-child {
  margin-bottom: 0;
}

.field-name {
  color: #991B1B;
  font-weight: 600;
  margin-right: 6px;
  min-width: 120px;
}

.error-message {
  color: #DC2626;
  flex: 1;
}

.generic-error {
  color: #DC2626;
  font-size: 14px;
}
</style>
