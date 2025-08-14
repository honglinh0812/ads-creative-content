<template>
  <div 
    :class="[
      'mobile-card',
      `mobile-card--${variant}`,
      { 'mobile-card--clickable': clickable },
      { 'mobile-card--selected': selected },
      { 'mobile-card--loading': loading }
    ]"
    @click="handleClick"
  >
    <!-- Loading Overlay -->
    <div v-if="loading" class="mobile-card-loading">
      <div class="loading-spinner"></div>
    </div>
    
    <!-- Card Header -->
    <div v-if="hasHeader" class="mobile-card-header">
      <div class="mobile-card-title-section">
        <div v-if="icon" class="mobile-card-icon">
          <i :class="icon"></i>
        </div>
        <div class="mobile-card-titles">
          <h3 v-if="title" class="mobile-card-title">{{ title }}</h3>
          <p v-if="subtitle" class="mobile-card-subtitle">{{ subtitle }}</p>
        </div>
      </div>
      
      <div v-if="hasHeaderActions" class="mobile-card-header-actions">
        <slot name="header-actions"></slot>
      </div>
    </div>
    
    <!-- Card Image -->
    <div v-if="image" class="mobile-card-image">
      <img 
        :src="image" 
        :alt="imageAlt || title || 'Card image'"
        @load="onImageLoad"
        @error="onImageError"
      />
    </div>
    
    <!-- Card Content -->
    <div class="mobile-card-content">
      <slot></slot>
    </div>
    
    <!-- Card Footer -->
    <div v-if="hasFooter" class="mobile-card-footer">
      <slot name="footer"></slot>
    </div>
    
    <!-- Selection Indicator -->
    <div v-if="selectable" class="mobile-card-selection">
      <div class="selection-indicator" :class="{ active: selected }">
        <i class="pi pi-check"></i>
      </div>
    </div>
    
    <!-- Badge -->
    <div v-if="badge" class="mobile-card-badge" :class="`badge--${badgeVariant}`">
      {{ badge }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'MobileCard',
  props: {
    title: {
      type: String,
      default: ''
    },
    subtitle: {
      type: String,
      default: ''
    },
    icon: {
      type: String,
      default: ''
    },
    image: {
      type: String,
      default: ''
    },
    imageAlt: {
      type: String,
      default: ''
    },
    variant: {
      type: String,
      default: 'default',
      validator: (value) => ['default', 'outlined', 'elevated', 'flat'].includes(value)
    },
    clickable: {
      type: Boolean,
      default: false
    },
    selectable: {
      type: Boolean,
      default: false
    },
    selected: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    badge: {
      type: String,
      default: ''
    },
    badgeVariant: {
      type: String,
      default: 'primary',
      validator: (value) => ['primary', 'secondary', 'success', 'warning', 'error'].includes(value)
    }
  },
  
  emits: ['click', 'select', 'image-load', 'image-error'],
  
  computed: {
    hasHeader() {
      return this.title || this.subtitle || this.icon || this.hasHeaderActions
    },
    
    hasHeaderActions() {
      return !!this.$slots['header-actions']
    },
    
    hasFooter() {
      return !!this.$slots.footer
    }
  },
  
  methods: {
    handleClick(event) {
      if (this.loading) return
      
      if (this.selectable) {
        this.$emit('select', !this.selected)
      }
      
      if (this.clickable) {
        this.$emit('click', event)
      }
    },
    
    onImageLoad(event) {
      this.$emit('image-load', event)
    },
    
    onImageError(event) {
      this.$emit('image-error', event)
    }
  }
}
</script>

<style scoped>
.mobile-card {
  position: relative;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: var(--transition-all);
  width: 100%;
}

/* Card Variants */
.mobile-card--default {
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.mobile-card--outlined {
  border: 2px solid var(--color-border);
  box-shadow: none;
}

.mobile-card--elevated {
  border: none;
  box-shadow: var(--shadow-lg);
}

.mobile-card--flat {
  border: none;
  box-shadow: none;
  background: transparent;
}

/* Interactive States */
.mobile-card--clickable {
  cursor: pointer;
}

.mobile-card--clickable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-xl);
}

.mobile-card--clickable:active {
  transform: translateY(0);
  box-shadow: var(--shadow-md);
}

.mobile-card--selected {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 2px rgb(24 119 242 / 20%);
}

.mobile-card--loading {
  pointer-events: none;
  opacity: 0.7;
}

/* Loading Overlay */
.mobile-card-loading {
  position: absolute;
  inset: 0;
  background: rgb(255 255 255 / 80%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  backdrop-filter: blur(2px);
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-top: 2px solid var(--brand-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Card Header */
.mobile-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--space-4);
  gap: var(--space-3);
}

.mobile-card-title-section {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  flex: 1;
  min-width: 0;
}

.mobile-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--primary-50);
  color: var(--brand-primary);
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}

.mobile-card-icon i {
  font-size: 1.25rem;
}

.mobile-card-titles {
  flex: 1;
  min-width: 0;
}

.mobile-card-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0 0 var(--space-1) 0;
  line-height: var(--leading-tight);
  overflow-wrap: break-word;
}

.mobile-card-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0;
  line-height: var(--leading-relaxed);
  overflow-wrap: break-word;
}

.mobile-card-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

/* Card Image */
.mobile-card-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.mobile-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: var(--transition-transform);
}

.mobile-card--clickable:hover .mobile-card-image img {
  transform: scale(1.05);
}

/* Card Content */
.mobile-card-content {
  padding: var(--space-4);
  color: var(--color-text);
  line-height: var(--leading-relaxed);
}

.mobile-card-content:empty {
  display: none;
}

/* Card Footer */
.mobile-card-footer {
  padding: var(--space-4);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg-tertiary);
}

/* Selection Indicator */
.mobile-card-selection {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  z-index: 5;
}

.selection-indicator {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-radius: 50%;
  background: var(--color-bg-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-all);
}

.selection-indicator.active {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: white;
}

.selection-indicator i {
  font-size: 0.75rem;
  opacity: 0;
  transition: var(--transition-opacity);
}

.selection-indicator.active i {
  opacity: 1;
}

/* Badge */
.mobile-card-badge {
  position: absolute;
  top: var(--space-3);
  left: var(--space-3);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  z-index: 5;
}

.badge--primary {
  background: var(--brand-primary);
  color: white;
}

.badge--secondary {
  background: var(--gray-500);
  color: white;
}

.badge--success {
  background: var(--success-500);
  color: white;
}

.badge--warning {
  background: var(--warning-500);
  color: white;
}

.badge--error {
  background: var(--error-500);
  color: white;
}

/* Responsive Adjustments */
@media (width <= 640px) {
  .mobile-card-header {
    padding: var(--space-3);
  }
  
  .mobile-card-content {
    padding: var(--space-3);
  }
  
  .mobile-card-footer {
    padding: var(--space-3);
  }
  
  .mobile-card-title {
    font-size: var(--text-base);
  }
  
  .mobile-card-subtitle {
    font-size: var(--text-xs);
  }
  
  .mobile-card-icon {
    width: 36px;
    height: 36px;
  }
  
  .mobile-card-icon i {
    font-size: 1.125rem;
  }
  
  .mobile-card-image {
    height: 160px;
  }
}

@media (width <= 480px) {
  .mobile-card-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-2);
  }
  
  .mobile-card-title-section {
    align-items: center;
  }
  
  .mobile-card-header-actions {
    justify-content: flex-end;
  }
  
  .mobile-card-image {
    height: 140px;
  }
}

/* Dark mode support */
.dark .mobile-card {
  background: var(--gray-800);
  border-color: var(--gray-700);
}

.dark .mobile-card-footer {
  background: var(--gray-900);
  border-color: var(--gray-700);
}

.dark .mobile-card-loading {
  background: rgb(0 0 0 / 80%);
}

.dark .selection-indicator {
  background: var(--gray-700);
  border-color: var(--gray-600);
}
</style>
