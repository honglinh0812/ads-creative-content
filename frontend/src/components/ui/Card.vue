<template>
  <div :class="cardClasses" v-bind="$attrs">
    <!-- Card Header -->
    <div v-if="$slots.header || title || subtitle" class="card-header">
      <slot name="header">
        <div v-if="title || subtitle" class="card-header-content">
          <h3 v-if="title" class="card-title">{{ title }}</h3>
          <p v-if="subtitle" class="card-subtitle">{{ subtitle }}</p>
        </div>
      </slot>
      
      <div v-if="$slots.actions" class="card-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <!-- Card Body -->
    <div v-if="$slots.default" class="card-body" :class="bodyClasses">
      <slot></slot>
    </div>
    
    <!-- Card Footer -->
    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Card',
  inheritAttrs: false,
  props: {
    title: {
      type: String,
      default: null
    },
    subtitle: {
      type: String,
      default: null
    },
    variant: {
      type: String,
      default: 'default',
      validator: (value) => ['default', 'elevated', 'outlined', 'filled'].includes(value)
    },
    size: {
      type: String,
      default: 'md',
      validator: (value) => ['sm', 'md', 'lg'].includes(value)
    },
    rounded: {
      type: String,
      default: 'md',
      validator: (value) => ['none', 'sm', 'md', 'lg', 'xl', 'full'].includes(value)
    },
    padding: {
      type: String,
      default: 'md',
      validator: (value) => ['none', 'sm', 'md', 'lg'].includes(value)
    },
    hoverable: {
      type: Boolean,
      default: false
    },
    clickable: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    cardClasses() {
      return [
        'card',
        `card-${this.variant}`,
        `card-${this.size}`,
        `card-rounded-${this.rounded}`,
        {
          'card-hoverable': this.hoverable,
          'card-clickable': this.clickable,
          'card-loading': this.loading,
          'card-disabled': this.disabled
        }
      ]
    },
    bodyClasses() {
      return [
        `card-padding-${this.padding}`
      ]
    }
  }
}
</script>

<style scoped>
/* Base card styles */
.card {
  @apply bg-white dark:bg-neutral-800 transition-all duration-200 ease-in-out;
  @apply relative overflow-hidden;
}

/* Card variants */
.card-default {
  @apply border border-neutral-200 dark:border-neutral-700;
}

.card-elevated {
  @apply shadow-soft hover:shadow-medium;
}

.card-outlined {
  @apply border-2 border-neutral-300 dark:border-neutral-600;
}

.card-filled {
  @apply bg-neutral-50 dark:bg-neutral-700 border border-neutral-200 dark:border-neutral-600;
}

/* Card sizes */
.card-sm {
  @apply max-w-sm;
}

.card-md {
  @apply max-w-md;
}

.card-lg {
  @apply max-w-lg;
}

/* Card rounded variants */
.card-rounded-none {
  @apply rounded-none;
}

.card-rounded-sm {
  @apply rounded-sm;
}

.card-rounded-md {
  @apply rounded-lg;
}

.card-rounded-lg {
  @apply rounded-xl;
}

.card-rounded-xl {
  @apply rounded-2xl;
}

.card-rounded-full {
  @apply rounded-3xl;
}

/* Card states */
.card-hoverable:hover {
  @apply shadow-medium border-primary-200;
}

.card-clickable {
  @apply cursor-pointer;
}

.card-clickable:hover {
  @apply shadow-soft border-primary-200;
}

.card-clickable:active {
  @apply shadow-soft border-primary-300;
}

.card-loading {
  @apply opacity-75 pointer-events-none;
}

.card-loading::before {
  content: '';

  @apply absolute inset-0 bg-white dark:bg-neutral-800 bg-opacity-50 dark:bg-opacity-50;
  @apply flex items-center justify-center z-10;

  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24'%3E%3Ccircle cx='12' cy='12' r='10' stroke='currentColor' stroke-width='4' class='opacity-25'%3E%3C/circle%3E%3Cpath fill='currentColor' d='m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z' class='opacity-75'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  background-size: 2rem;
  animation: spin 1s linear infinite;
}

.card-disabled {
  @apply opacity-50 pointer-events-none cursor-not-allowed;
}

/* Card header */
.card-header {
  @apply flex items-start justify-between p-6 pb-4;
  @apply border-b border-neutral-200 dark:border-neutral-700;
}

.card-header-content {
  @apply flex-1 min-w-0;
}

.card-title {
  @apply text-lg font-semibold text-neutral-900 dark:text-neutral-100;
  @apply mb-1 leading-tight;
}

.card-subtitle {
  @apply text-sm text-neutral-600 dark:text-neutral-400;
  @apply leading-relaxed;
}

.card-actions {
  @apply flex items-center space-x-2 ml-4;
}

/* Card body */
.card-body {
  @apply text-neutral-700 dark:text-neutral-300;
}

.card-padding-none {
  @apply p-0;
}

.card-padding-sm {
  @apply p-4;
}

.card-padding-md {
  @apply p-6;
}

.card-padding-lg {
  @apply p-8;
}

/* Card footer */
.card-footer {
  @apply px-6 py-4 bg-neutral-50 dark:bg-neutral-700;
  @apply border-t border-neutral-200 dark:border-neutral-600;
}

/* Dark mode adjustments */
.dark .card-elevated {
  @apply shadow-neutral-900/25;
}

.dark .card-hoverable:hover {
  @apply shadow-neutral-900/40;
}

.dark .card-clickable:hover {
  @apply shadow-neutral-900/30;
}

/* Animations */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .card {
    @apply transition-none;
  }
  
  .card {
    transition: none;
  }
  
  .card-loading::before {
    animation: none;
  }
}

/* Responsive adjustments */
@media (width <= 640px) {
  .card-header {
    @apply p-4 pb-3;
  }
  
  .card-padding-md {
    @apply p-4;
  }
  
  .card-padding-lg {
    @apply p-6;
  }
  
  .card-footer {
    @apply px-4 py-3;
  }
  
  .card-title {
    @apply text-base;
  }
  
  .card-subtitle {
    @apply text-xs;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .card-default,
  .card-filled {
    @apply border-2;
  }
}
</style>