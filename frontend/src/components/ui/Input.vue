<template>
  <div class="input-wrapper" :class="wrapperClasses">
    <label v-if="label" :for="inputId" class="input-label">
      {{ label }}
      <span v-if="required" class="text-error-500">*</span>
    </label>
    
    <div class="input-container" :class="containerClasses">
      <span v-if="prefixIcon" class="input-prefix-icon">
        <i :class="prefixIcon"></i>
      </span>
      
      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :required="required"
        :class="inputClasses"
        v-bind="$attrs"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />
      
      <span v-if="suffixIcon || clearable" class="input-suffix">
        <button
          v-if="clearable && modelValue && !disabled && !readonly"
          type="button"
          class="input-clear-btn"
          @click="clearInput"
        >
          <i class="pi pi-times"></i>
        </button>
        <span v-else-if="suffixIcon" class="input-suffix-icon">
          <i :class="suffixIcon"></i>
        </span>
      </span>
    </div>
    
    <div v-if="error || hint" class="input-message">
      <span v-if="error" class="input-error">
        <i class="pi pi-exclamation-triangle mr-1"></i>
        {{ error }}
      </span>
      <span v-else-if="hint" class="input-hint">
        {{ hint }}
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Input',
  inheritAttrs: false,
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    type: {
      type: String,
      default: 'text'
    },
    label: {
      type: String,
      default: null
    },
    placeholder: {
      type: String,
      default: null
    },
    size: {
      type: String,
      default: 'md',
      validator: (value) => ['sm', 'md', 'lg'].includes(value)
    },
    variant: {
      type: String,
      default: 'default',
      validator: (value) => ['default', 'filled', 'outlined'].includes(value)
    },
    disabled: {
      type: Boolean,
      default: false
    },
    readonly: {
      type: Boolean,
      default: false
    },
    required: {
      type: Boolean,
      default: false
    },
    error: {
      type: String,
      default: null
    },
    hint: {
      type: String,
      default: null
    },
    prefixIcon: {
      type: String,
      default: null
    },
    suffixIcon: {
      type: String,
      default: null
    },
    clearable: {
      type: Boolean,
      default: false
    },
    rounded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      focused: false,
      inputId: `input-${Math.random().toString(36).substr(2, 9)}`
    }
  },
  computed: {
    wrapperClasses() {
      return {
        'input-wrapper-disabled': this.disabled,
        'input-wrapper-error': this.error,
        'input-wrapper-focused': this.focused
      }
    },
    containerClasses() {
      return [
        'input-container',
        `input-${this.variant}`,
        `input-${this.size}`,
        {
          'input-rounded': this.rounded,
          'input-with-prefix': this.prefixIcon,
          'input-with-suffix': this.suffixIcon || this.clearable,
          'input-error': this.error,
          'input-disabled': this.disabled,
          'input-focused': this.focused
        }
      ]
    },
    inputClasses() {
      return [
        'input-field',
        {
          'pl-10': this.prefixIcon,
          'pr-10': this.suffixIcon || this.clearable
        }
      ]
    }
  },
  methods: {
    handleInput(event) {
      this.$emit('update:modelValue', event.target.value)
      this.$emit('input', event)
    },
    handleFocus(event) {
      this.focused = true
      this.$emit('focus', event)
    },
    handleBlur(event) {
      this.focused = false
      this.$emit('blur', event)
    },
    handleKeydown(event) {
      this.$emit('keydown', event)
    },
    clearInput() {
      this.$emit('update:modelValue', '')
      this.$emit('clear')
    }
  }
}
</script>

<style scoped>
/* Input wrapper */
.input-wrapper {
  @apply w-full;
}

.input-label {
  @apply block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-2;
}

/* Input container */
.input-container {
  @apply relative;
}

/* Input variants */
.input-default {
  @apply bg-white dark:bg-neutral-800 border border-neutral-300 dark:border-neutral-600;
  @apply focus-within:border-primary-500 focus-within:ring-1 focus-within:ring-primary-500;
}

.input-filled {
  @apply bg-neutral-50 dark:bg-neutral-700 border border-transparent;
  @apply focus-within:bg-white dark:focus-within:bg-neutral-800;
  @apply focus-within:border-primary-500 focus-within:ring-1 focus-within:ring-primary-500;
}

.input-outlined {
  @apply bg-transparent border-2 border-neutral-300 dark:border-neutral-600;
  @apply focus-within:border-primary-500;
}

/* Input sizes */
.input-sm {
  @apply rounded-md;
}

.input-md {
  @apply rounded-lg;
}

.input-lg {
  @apply rounded-xl;
}

.input-rounded {
  @apply rounded-full;
}

/* Input field */
.input-field {
  @apply w-full bg-transparent border-none outline-none;
  @apply text-neutral-900 dark:text-neutral-100;
  @apply placeholder-neutral-500 dark:placeholder-neutral-400;
  @apply transition-all duration-200;
}

.input-sm .input-field {
  @apply px-3 py-2 text-sm;
}

.input-md .input-field {
  @apply px-4 py-2.5 text-sm;
}

.input-lg .input-field {
  @apply px-5 py-3 text-base;
}

/* Icons */
.input-prefix-icon,
.input-suffix-icon {
  @apply absolute top-1/2 transform -translate-y-1/2;
  @apply text-neutral-500 dark:text-neutral-400;
  @apply pointer-events-none;
}

.input-prefix-icon {
  @apply left-3;
}

.input-suffix {
  @apply absolute right-3 top-1/2 transform -translate-y-1/2;
  @apply flex items-center space-x-1;
}

.input-suffix-icon {
  @apply relative top-auto right-auto transform-none;
}

.input-clear-btn {
  @apply p-1 rounded-full text-neutral-400 hover:text-neutral-600;
  @apply dark:text-neutral-500 dark:hover:text-neutral-300;
  @apply hover:bg-neutral-100 dark:hover:bg-neutral-700;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-1;
}

/* States */
.input-error {
  @apply border-error-500 focus-within:border-error-500 focus-within:ring-error-500;
}

.input-disabled {
  @apply opacity-50 cursor-not-allowed;
}

.input-disabled .input-field {
  @apply cursor-not-allowed;
}

.input-focused {
  @apply shadow-sm;
}

/* Messages */
.input-message {
  @apply mt-1.5 text-sm;
}

.input-error {
  @apply text-error-600 dark:text-error-400 flex items-center;
}

.input-hint {
  @apply text-neutral-500 dark:text-neutral-400;
}

/* Animations */
.input-container {
  @apply transition-all duration-200 ease-in-out;
}

.input-focused {
  @apply transform scale-[1.01];
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .input-container,
  .input-field,
  .input-clear-btn {
    @apply transition-none;
  }
  
  .input-focused {
    @apply transform-none scale-100;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .input-container {
    @apply border-2;
  }
}
</style>