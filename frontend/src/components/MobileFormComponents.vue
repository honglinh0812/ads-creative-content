<template>
  <div class="mobile-form-components">
    <!-- Mobile-Optimized Input Field -->
    <div class="mobile-input-field" v-if="type === 'input'">
      <label v-if="label" class="mobile-label" :for="fieldId">
        {{ label }}
        <span v-if="required" class="required-indicator">*</span>
      </label>
      <div class="mobile-input-container">
        <input
          :id="fieldId"
          :type="inputType"
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          :readonly="readonly"
          :required="required"
          :aria-invalid="hasError"
          :aria-describedby="hasError ? `${fieldId}-error` : helpText ? `${fieldId}-help` : undefined"
          :class="[
            'mobile-input',
            { 'has-error': hasError },
            { 'has-icon': hasIcon }
          ]"
          @input="$emit('update:modelValue', $event.target.value)"
          @focus="$emit('focus', $event)"
          @blur="$emit('blur', $event)"
        />
        <div v-if="hasIcon" class="mobile-input-icon">
          <i :class="iconClass"></i>
        </div>
      </div>
      <div v-if="hasError && errorMessage" class="mobile-error-message" :id="`${fieldId}-error`" role="alert" aria-live="polite">
        <i class="pi pi-exclamation-triangle" aria-hidden="true"></i>
        {{ errorMessage }}
      </div>
      <div v-if="helpText" class="mobile-help-text" :id="`${fieldId}-help`">
        {{ helpText }}
      </div>
    </div>

    <!-- Mobile-Optimized Textarea -->
    <div class="mobile-textarea-field" v-else-if="type === 'textarea'">
      <label v-if="label" class="mobile-label" :for="fieldId">
        {{ label }}
        <span v-if="required" class="required-indicator">*</span>
      </label>
      <div class="mobile-textarea-container">
        <textarea
          :id="fieldId"
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          :readonly="readonly"
          :rows="rows || 4"
          :required="required"
          :aria-invalid="hasError"
          :aria-describedby="hasError ? `${fieldId}-error` : helpText ? `${fieldId}-help` : undefined"
          :maxlength="maxLength"
          :class="[
            'mobile-textarea',
            { 'has-error': hasError }
          ]"
          @input="$emit('update:modelValue', $event.target.value)"
          @focus="$emit('focus', $event)"
          @blur="$emit('blur', $event)"
        ></textarea>
        <div class="mobile-textarea-counter" v-if="maxLength">
          {{ (modelValue || '').length }} / {{ maxLength }}
        </div>
      </div>
      <div v-if="hasError && errorMessage" class="mobile-error-message" :id="`${fieldId}-error`" role="alert" aria-live="polite">
        <i class="pi pi-exclamation-triangle" aria-hidden="true"></i>
        {{ errorMessage }}
      </div>
      <div v-if="helpText" class="mobile-help-text" :id="`${fieldId}-help`">
        {{ helpText }}
      </div>
    </div>

    <!-- Mobile-Optimized Select -->
    <div class="mobile-select-field" v-else-if="type === 'select'">
      <label v-if="label" class="mobile-label" :for="fieldId">
        {{ label }}
        <span v-if="required" class="required-indicator">*</span>
      </label>
      <div class="mobile-select-container">
        <select
          :id="fieldId"
          :value="modelValue"
          :disabled="disabled"
          :required="required"
          :aria-invalid="hasError"
          :aria-describedby="hasError ? `${fieldId}-error` : helpText ? `${fieldId}-help` : undefined"
          :class="[
            'mobile-select',
            { 'has-error': hasError }
          ]"
          @change="$emit('update:modelValue', $event.target.value)"
          @focus="$emit('focus', $event)"
          @blur="$emit('blur', $event)"
        >
          <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
          <option
            v-for="option in options"
            :key="getOptionValue(option)"
            :value="getOptionValue(option)"
          >
            {{ getOptionLabel(option) }}
          </option>
        </select>
        <div class="mobile-select-arrow">
          <i class="pi pi-chevron-down"></i>
        </div>
      </div>
      <div v-if="hasError && errorMessage" class="mobile-error-message">
        <i class="pi pi-exclamation-triangle"></i>
        {{ errorMessage }}
      </div>
      <div v-if="helpText" class="mobile-help-text">
        {{ helpText }}
      </div>
    </div>

    <!-- Mobile-Optimized Checkbox -->
    <div class="mobile-checkbox-field" v-else-if="type === 'checkbox'">
      <div class="mobile-checkbox-container">
        <input
          :id="fieldId"
          type="checkbox"
          :checked="modelValue"
          :disabled="disabled"
          class="mobile-checkbox"
          @change="$emit('update:modelValue', $event.target.checked)"
        />
        <label :for="fieldId" class="mobile-checkbox-label">
          <span class="checkbox-indicator"></span>
          <span class="checkbox-text">{{ label }}</span>
        </label>
      </div>
      <div v-if="hasError && errorMessage" class="mobile-error-message">
        <i class="pi pi-exclamation-triangle"></i>
        {{ errorMessage }}
      </div>
      <div v-if="helpText" class="mobile-help-text">
        {{ helpText }}
      </div>
    </div>

    <!-- Mobile-Optimized Radio Group -->
    <div class="mobile-radio-field" v-else-if="type === 'radio'">
      <div v-if="label" class="mobile-label">
        {{ label }}
        <span v-if="required" class="required-indicator">*</span>
      </div>
      <div class="mobile-radio-group">
        <div
          v-for="option in options"
          :key="getOptionValue(option)"
          class="mobile-radio-item"
        >
          <input
            :id="`${fieldId}_${getOptionValue(option)}`"
            type="radio"
            :name="fieldId"
            :value="getOptionValue(option)"
            :checked="modelValue === getOptionValue(option)"
            :disabled="disabled"
            class="mobile-radio"
            @change="$emit('update:modelValue', $event.target.value)"
          />
          <label
            :for="`${fieldId}_${getOptionValue(option)}`"
            class="mobile-radio-label"
          >
            <span class="radio-indicator"></span>
            <span class="radio-text">{{ getOptionLabel(option) }}</span>
          </label>
        </div>
      </div>
      <div v-if="hasError && errorMessage" class="mobile-error-message">
        <i class="pi pi-exclamation-triangle"></i>
        {{ errorMessage }}
      </div>
      <div v-if="helpText" class="mobile-help-text">
        {{ helpText }}
      </div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'MobileFormComponents',
  props: {
    type: {
      type: String,
      required: true,
      validator: (value) => ['input', 'textarea', 'select', 'checkbox', 'radio'].includes(value)
    },
    modelValue: {
      type: [String, Number, Boolean, Array],
      default: ''
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
    readonly: {
      type: Boolean,
      default: false
    },
    hasError: {
      type: Boolean,
      default: false
    },
    errorMessage: {
      type: String,
      default: ''
    },
    helpText: {
      type: String,
      default: ''
    },
    inputType: {
      type: String,
      default: 'text'
    },
    rows: {
      type: Number,
      default: 4
    },
    maxLength: {
      type: Number,
      default: null
    },
    options: {
      type: Array,
      default: () => []
    },
    optionLabel: {
      type: String,
      default: 'label'
    },
    optionValue: {
      type: String,
      default: 'value'
    },
    hasIcon: {
      type: Boolean,
      default: false
    },
    iconClass: {
      type: String,
      default: ''
    }
  },
  
  emits: ['update:modelValue', 'focus', 'blur'],
  
  setup(props) {
    const fieldId = computed(() => {
      return `mobile-field-${Math.random().toString(36).substr(2, 9)}`
    })
    
    const getOptionLabel = (option) => {
      if (typeof option === 'string') return option
      return option[props.optionLabel] || option.label || option.name || option
    }
    
    const getOptionValue = (option) => {
      if (typeof option === 'string') return option
      return option[props.optionValue] || option.value || option.id || option
    }
    
    return {
      fieldId,
      getOptionLabel,
      getOptionValue
    }
  }
}
</script>

<style scoped>
.mobile-form-components {
  width: 100%;
}

/* Common Label Styles */
.mobile-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-2);
  line-height: var(--leading-tight);
}

.required-indicator {
  color: var(--error-500);
  margin-left: var(--space-1);
}

/* Input Field Styles */
.mobile-input-field {
  margin-bottom: var(--space-4);
}

.mobile-input-container {
  position: relative;
}

.mobile-input {
  width: 100%;
  padding: var(--space-4);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  background: var(--color-bg-secondary);
  color: var(--color-text);
  transition: var(--transition-all);
  min-height: 48px; /* iOS touch target minimum */
  appearance: none; /* Remove iOS styling */
  appearance: none;
}

.mobile-input:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgb(24 119 242 / 10%);
  transform: none; /* Prevent zoom on iOS */
}

.mobile-input.has-error {
  border-color: var(--error-500);
  box-shadow: 0 0 0 3px rgb(239 68 68 / 10%);
}

.mobile-input.has-icon {
  padding-right: var(--space-12);
}

.mobile-input-icon {
  position: absolute;
  right: var(--space-4);
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

/* Textarea Styles */
.mobile-textarea-field {
  margin-bottom: var(--space-4);
}

.mobile-textarea-container {
  position: relative;
}

.mobile-textarea {
  width: 100%;
  padding: var(--space-4);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  background: var(--color-bg-secondary);
  color: var(--color-text);
  transition: var(--transition-all);
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
  appearance: none;
  appearance: none;
}

.mobile-textarea:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgb(24 119 242 / 10%);
}

.mobile-textarea.has-error {
  border-color: var(--error-500);
  box-shadow: 0 0 0 3px rgb(239 68 68 / 10%);
}

.mobile-textarea-counter {
  position: absolute;
  bottom: var(--space-2);
  right: var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-bg-secondary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

/* Select Styles */
.mobile-select-field {
  margin-bottom: var(--space-4);
}

.mobile-select-container {
  position: relative;
}

.mobile-select {
  width: 100%;
  padding: var(--space-4);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  background: var(--color-bg-secondary);
  color: var(--color-text);
  transition: var(--transition-all);
  min-height: 48px;
  appearance: none;
  appearance: none;
  cursor: pointer;
}

.mobile-select:focus {
  outline: none;
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px rgb(24 119 242 / 10%);
}

.mobile-select.has-error {
  border-color: var(--error-500);
  box-shadow: 0 0 0 3px rgb(239 68 68 / 10%);
}

.mobile-select-arrow {
  position: absolute;
  right: var(--space-4);
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

/* Checkbox Styles */
.mobile-checkbox-field {
  margin-bottom: var(--space-4);
}

.mobile-checkbox-container {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.mobile-checkbox {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.mobile-checkbox-label {
  display: flex;
  gap: var(--space-3);
  cursor: pointer;
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  min-height: 44px;
  align-items: center;
}

.checkbox-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-secondary);
  transition: var(--transition-all);
  position: relative;
  flex-shrink: 0;
}

.mobile-checkbox:checked + .mobile-checkbox-label .checkbox-indicator {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
}

.mobile-checkbox:checked + .mobile-checkbox-label .checkbox-indicator::after {
  content: '';
  position: absolute;
  left: 6px;
  top: 2px;
  width: 6px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.checkbox-text {
  color: var(--color-text);
}

/* Radio Styles */
.mobile-radio-field {
  margin-bottom: var(--space-4);
}

.mobile-radio-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.mobile-radio-item {
  display: flex;
  align-items: center;
}

.mobile-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.mobile-radio-label {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  cursor: pointer;
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  min-height: 44px;
}

.radio-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-secondary);
  transition: var(--transition-all);
  position: relative;
  flex-shrink: 0;
}

.mobile-radio:checked + .mobile-radio-label .radio-indicator {
  border-color: var(--brand-primary);
}

.mobile-radio:checked + .mobile-radio-label .radio-indicator::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 8px;
  height: 8px;
  background: var(--brand-primary);
  border-radius: 50%;
}

.radio-text {
  color: var(--color-text);
}

/* Error and Help Text */
.mobile-error-message {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--error-600);
  line-height: var(--leading-tight);
}

.mobile-error-message i {
  font-size: var(--text-xs);
  flex-shrink: 0;
}

.mobile-help-text {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
}

/* Responsive adjustments */
@media (width <= 480px) {
  .mobile-input,
  .mobile-textarea,
  .mobile-select {
    font-size: 16px; /* Prevent zoom on iOS */
  }
  
  .mobile-label {
    font-size: var(--text-base);
  }
  
  .mobile-checkbox-label,
  .mobile-radio-label {
    font-size: var(--text-sm);
  }
}
</style>
