<template>
  <a-button
    v-bind="$attrs"
    :class="buttonClass"
    :disabled="loading || disabled"
    @click="handleClick"
  >
    <template #icon v-if="loading">
      <loading-spinner
        size="small"
        :color="spinnerColor"
        :show-text="false"
      />
    </template>
    <template #icon v-else-if="icon">
      <component :is="icon" />
    </template>

    <span v-if="loading && loadingText" class="loading-text">
      {{ loadingText }}
    </span>
    <span v-else class="button-text">
      <slot>{{ text }}</slot>
    </span>
  </a-button>
</template>

<script>
import LoadingSpinner from './LoadingSpinner.vue'

export default {
  name: 'LoadingButton',
  components: {
    LoadingSpinner
  },
  inheritAttrs: false,
  emits: ['click'],
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    },
    text: {
      type: String,
      default: ''
    },
    loadingText: {
      type: String,
      default: ''
    },
    icon: {
      type: [String, Object],
      default: null
    },
    type: {
      type: String,
      default: 'default'
    },
    size: {
      type: String,
      default: 'middle'
    }
  },
  computed: {
    buttonClass() {
      return [
        'loading-button',
        {
          'is-loading': this.loading
        }
      ]
    },
    spinnerColor() {
      if (this.type === 'primary') return 'white'
      if (this.type === 'danger') return 'white'
      return 'primary'
    }
  },
  methods: {
    handleClick(event) {
      if (!this.loading && !this.disabled) {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style scoped>
.loading-button {
  position: relative;
  transition: all 0.2s ease;
}

.loading-button.is-loading {
  pointer-events: none;
}

.loading-text,
.button-text {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.loading-button :deep(.ant-btn-loading-icon) {
  display: none;
}

/* Ensure consistent height during loading state */
.loading-button .loading-text {
  min-height: 1.2em;
}

/* Animation for text change */
.loading-text,
.button-text {
  transition: opacity 0.2s ease;
}

/* Mobile optimizations */
@media (max-width: 640px) {
  .loading-button {
    min-height: 44px; /* Touch-friendly minimum */
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .loading-button.is-loading {
    border: 2px solid currentColor;
  }
}

/* Focus management */
.loading-button:focus-visible {
  outline: 2px solid theme('colors.primary.500');
  outline-offset: 2px;
}

.loading-button.is-loading:focus-visible {
  outline-color: theme('colors.gray.400');
}
</style>