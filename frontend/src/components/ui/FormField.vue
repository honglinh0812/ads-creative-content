<template>
  <div :class="['form-group-standard', { 'form-group-focused': isFocused, 'form-group-error': hasError }]">
    <label v-if="label" class="form-label-standard" :for="fieldId">
      {{ label }}
      <span v-if="required" class="text-danger-standard" aria-label="Required field">*</span>
      <a-button
        v-if="helpTooltip"
        type="text"
        size="small"
        class="help-button"
        @click="showHelp = true"
        :aria-label="`Help for ${label}`"
      >
        <template #icon><question-circle-outlined /></template>
      </a-button>
    </label>

    <!-- Input Field -->
    <input
      v-if="type === 'text' || type === 'email' || type === 'password' || type === 'url'"
      :id="fieldId"
      :type="type"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus"
      :class="[
        'form-input-standard',
        { error: hasError }
      ]"
      :placeholder="placeholder"
      :disabled="disabled"
      :maxlength="maxlength"
    />

    <!-- Textarea Field -->
    <textarea
      v-else-if="type === 'textarea'"
      :id="fieldId"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus"
      :class="[
        'form-input-standard',
        { error: hasError }
      ]"
      :placeholder="placeholder"
      :disabled="disabled"
      :rows="rows || 3"
      :maxlength="maxlength"
    />

    <!-- Select Field -->
    <select
      v-else-if="type === 'select'"
      :id="fieldId"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus"
      :class="[
        'form-input-standard',
        { error: hasError }
      ]"
      :disabled="disabled"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <option
        v-for="option in options"
        :key="option.value"
        :value="option.value"
      >
        {{ option.label }}
      </option>
    </select>

    <!-- Number Input -->
    <input
      v-else-if="type === 'number'"
      :id="fieldId"
      type="number"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @blur="handleBlur"
      @focus="handleFocus"
      :class="[
        'form-input-standard',
        { error: hasError }
      ]"
      :placeholder="placeholder"
      :disabled="disabled"
      :min="min"
      :max="max"
      :step="step"
    />

    <!-- Character Count -->
    <div v-if="maxlength && showCount" class="character-count">
      {{ (modelValue || '').length }}/{{ maxlength }}
    </div>

    <!-- Validation Error -->
    <div v-if="hasError" class="form-error-standard">
      ⚠️ {{ errorMessage }}
    </div>

    <!-- Help Text -->
    <div v-if="helpText && !hasError" class="form-help-standard">
      {{ helpText }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'FormField',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    },
    type: {
      type: String,
      default: 'text',
      validator: value => ['text', 'email', 'password', 'url', 'textarea', 'select', 'number'].includes(value)
    },
    label: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    required: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    },
    maxlength: {
      type: Number,
      default: null
    },
    showCount: {
      type: Boolean,
      default: false
    },
    rows: {
      type: Number,
      default: 3
    },
    min: {
      type: Number,
      default: null
    },
    max: {
      type: Number,
      default: null
    },
    step: {
      type: Number,
      default: 1
    },
    options: {
      type: Array,
      default: () => []
    },
    helpText: {
      type: String,
      default: ''
    },
    validationRules: {
      type: Array,
      default: () => []
    },
    validateOnBlur: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue', 'validation-change'],
  data() {
    return {
      errorMessage: '',
      hasError: false,
      fieldId: `field-${Math.random().toString(36).substr(2, 9)}`
    }
  },
  watch: {
    modelValue() {
      if (this.hasError) {
        // Re-validate when user starts typing after an error
        this.validateField()
      }
    }
  },
  methods: {
    handleFocus() {
      this.isFocused = true
    },

    handleBlur() {
      this.isFocused = false
      this.validateField()
    },

    validateField() {
      if (!this.validateOnBlur) return

      this.errorMessage = ''
      this.hasError = false

      // Required validation
      if (this.required && (!this.modelValue || this.modelValue.toString().trim() === '')) {
        this.errorMessage = `${this.label || 'This field'} is required`
        this.hasError = true
        this.emitValidation()
        return
      }

      // Skip further validation if field is empty and not required
      if (!this.modelValue || this.modelValue.toString().trim() === '') {
        this.emitValidation()
        return
      }

      // Type-specific validation
      if (this.type === 'email') {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(this.modelValue)) {
          this.errorMessage = 'Please enter a valid email address'
          this.hasError = true
        }
      }

      if (this.type === 'url') {
        try {
          new URL(this.modelValue)
        } catch {
          this.errorMessage = 'Please enter a valid URL'
          this.hasError = true
        }
      }

      // Length validation
      if (this.maxlength && this.modelValue.length > this.maxlength) {
        this.errorMessage = `Maximum ${this.maxlength} characters allowed`
        this.hasError = true
      }

      // Number validation
      if (this.type === 'number') {
        const num = parseFloat(this.modelValue)
        if (isNaN(num)) {
          this.errorMessage = 'Please enter a valid number'
          this.hasError = true
        } else {
          if (this.min !== null && num < this.min) {
            this.errorMessage = `Value must be at least ${this.min}`
            this.hasError = true
          }
          if (this.max !== null && num > this.max) {
            this.errorMessage = `Value must be no more than ${this.max}`
            this.hasError = true
          }
        }
      }

      // Custom validation rules
      for (const rule of this.validationRules) {
        const result = rule.validator(this.modelValue)
        if (result !== true) {
          this.errorMessage = result || rule.message || 'Validation failed'
          this.hasError = true
          break
        }
      }

      this.emitValidation()
    },

    emitValidation() {
      this.$emit('validation-change', {
        valid: !this.hasError,
        message: this.errorMessage
      })
    },

    // Public method to trigger validation
    validate() {
      this.validateField()
      return !this.hasError
    }
  }
}
</script>

<style scoped>
.character-count {
  font-size: 12px;
  color: #8c8c8c;
  text-align: right;
  margin-top: 4px;
}

.text-danger-standard {
  color: #e76f51;
  margin-left: 2px;
}

/* Focus styles for better accessibility */
.form-input-standard:focus {
  outline: none;
  border-color: #2d5aa0;
  box-shadow: 0 0 0 3px rgba(45, 90, 160, 0.1);
}

.form-input-standard.error:focus {
  border-color: #e76f51;
  box-shadow: 0 0 0 3px rgba(231, 111, 81, 0.1);
}

/* Disabled state */
.form-input-standard:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  cursor: not-allowed;
  opacity: 0.7;
}

/* Placeholder styling */
.form-input-standard::placeholder {
  color: #bfbfbf;
  font-style: italic;
}

/* Select arrow styling */
select.form-input-standard {
  background-image: url("data:image/svg+xml;charset=US-ASCII,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 4 5'><path fill='%23666' d='m0,1 2,2 2-2z'/></svg>");
  background-repeat: no-repeat;
  background-position: right 12px center;
  background-size: 12px;
  padding-right: 32px;
  appearance: none;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .form-input-standard {
    padding: 14px 16px;
    font-size: 16px; /* Prevent zoom on iOS */
  }
}
</style>