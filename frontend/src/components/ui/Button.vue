<template>
  <component
    :is="tag"
    :class="buttonClasses"
    :disabled="disabled || loading"
    :type="type"
    v-bind="$attrs"
    @click="handleClick"
  >
    <span v-if="loading" class="btn-loading">
      <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </span>
    
    <span v-if="icon && !loading" :class="iconClasses">
      <i :class="icon"></i>
    </span>
    
    <span v-if="$slots.default" :class="{ 'ml-2': icon && !loading }">
      <slot></slot>
    </span>
  </component>
</template>

<script>
export default {
  name: 'Button',
  inheritAttrs: false,
  props: {
    variant: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'secondary', 'success', 'warning', 'error', 'ghost', 'outline'].includes(value)
    },
    size: {
      type: String,
      default: 'md',
      validator: (value) => ['xs', 'sm', 'md', 'lg', 'xl'].includes(value)
    },
    tag: {
      type: String,
      default: 'button'
    },
    type: {
      type: String,
      default: 'button'
    },
    disabled: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    icon: {
      type: String,
      default: null
    },
    iconOnly: {
      type: Boolean,
      default: false
    },
    rounded: {
      type: Boolean,
      default: false
    },
    block: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    buttonClasses() {
      return [
        'btn',
        `btn-${this.variant}`,
        `btn-${this.size}`,
        {
          'btn-icon-only': this.iconOnly,
          'btn-rounded': this.rounded,
          'btn-block': this.block,
          'btn-loading': this.loading,
          'btn-disabled': this.disabled
        }
      ]
    },
    iconClasses() {
      return {
        'btn-icon': true,
        'text-sm': this.size === 'xs',
        'text-base': this.size === 'sm',
        'text-lg': this.size === 'md',
        'text-xl': this.size === 'lg',
        'text-2xl': this.size === 'xl'
      }
    }
  },
  methods: {
    handleClick(event) {
      if (!this.disabled && !this.loading) {
        this.$emit('click', event)
      }
    }
  }
}
</script>

<style scoped>
/* Base button styles */
.btn {
  @apply inline-flex items-center justify-center font-medium transition-all duration-200 ease-in-out;
  @apply focus:outline-none focus:ring-2 focus:ring-offset-2;
  @apply disabled:opacity-50 disabled:cursor-not-allowed;
  @apply hover:bg-opacity-90 active:bg-opacity-100;
  @apply border border-transparent;
}

/* Button variants */
.btn-primary {
  @apply bg-primary-500 text-white hover:bg-primary-600 focus:ring-primary-500;
  @apply border border-primary-600 hover:border-primary-700;
}

.btn-secondary {
  @apply bg-secondary-500 text-white hover:bg-secondary-600 focus:ring-secondary-500;
  @apply border border-primary-600 hover:border-primary-700;
}

.btn-success {
  @apply bg-success-500 text-white hover:bg-success-600 focus:ring-success-500;
  @apply border border-primary-600 hover:border-primary-700;
}

.btn-warning {
  @apply bg-warning-500 text-white hover:bg-warning-600 focus:ring-warning-500;
  @apply border border-primary-600 hover:border-primary-700;
}

.btn-error {
  @apply bg-error-500 text-white hover:bg-error-600 focus:ring-error-500;
  @apply border border-primary-600 hover:border-primary-700;
}

.btn-ghost {
  @apply bg-transparent text-neutral-700 hover:bg-neutral-100 focus:ring-neutral-500;
  @apply dark:text-neutral-300 dark:hover:bg-neutral-800;
}

.btn-outline {
  @apply bg-transparent border-neutral-300 text-neutral-700 hover:bg-neutral-50 focus:ring-neutral-500;
  @apply dark:border-neutral-600 dark:text-neutral-300 dark:hover:bg-neutral-800;
}

/* Button sizes */
.btn-xs {
  @apply px-2 py-1 text-xs rounded-md;
}

.btn-sm {
  @apply px-3 py-1.5 text-sm rounded-md;
}

.btn-md {
  @apply px-4 py-2 text-sm rounded-lg;
}

.btn-lg {
  @apply px-6 py-3 text-base rounded-lg;
}

.btn-xl {
  @apply px-8 py-4 text-lg rounded-xl;
}

/* Button modifiers */
.btn-icon-only {
  @apply p-2;
}

.btn-icon-only.btn-xs {
  @apply p-1;
}

.btn-icon-only.btn-sm {
  @apply p-1.5;
}

.btn-icon-only.btn-lg {
  @apply p-3;
}

.btn-icon-only.btn-xl {
  @apply p-4;
}

.btn-rounded {
  @apply rounded-full;
}

.btn-block {
  @apply w-full;
}

.btn-loading {
  @apply cursor-wait;
}

.btn-loading .btn-loading {
  @apply mr-2;
}

/* Dark mode adjustments */
.dark .btn-primary {
  @apply bg-primary-600 hover:bg-primary-700;
}

.dark .btn-secondary {
  @apply bg-secondary-600 hover:bg-secondary-700;
}

.dark .btn-success {
  @apply bg-success-600 hover:bg-success-700;
}

.dark .btn-warning {
  @apply bg-warning-600 hover:bg-warning-700;
}

.dark .btn-error {
  @apply bg-error-600 hover:bg-error-700;
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .btn {
    transition: none;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .btn {
    @apply border-2;
  }
}
</style>