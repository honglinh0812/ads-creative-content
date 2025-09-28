<template>
  <teleport to="body">
    <transition name="modal" appear>
      <div v-if="modelValue" class="modal-overlay" @click="handleOverlayClick">
        <div 
          :class="modalClasses" 
          @click.stop
          role="dialog"
          :aria-labelledby="titleId"
          :aria-describedby="contentId"
          aria-modal="true"
        >
          <!-- Modal Header -->
          <div v-if="$slots.header || title || closable" class="modal-header">
            <div class="modal-header-content">
              <slot name="header">
                <h2 v-if="title" :id="titleId" class="modal-title">{{ title }}</h2>
              </slot>
            </div>
            
            <button 
              v-if="closable"
              type="button"
              class="modal-close-btn"
              @click="closeModal"
              aria-label="Đóng modal"
            >
              <i class="pi pi-times"></i>
            </button>
          </div>
          
          <!-- Modal Body -->
          <div :id="contentId" class="modal-body" :class="bodyClasses">
            <slot></slot>
          </div>
          
          <!-- Modal Footer -->
          <div v-if="$slots.footer" class="modal-footer">
            <slot name="footer"></slot>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script>
export default {
  name: 'Modal',
  props: {
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: null
    },
    size: {
      type: String,
      default: 'md',
      validator: (value) => ['xs', 'sm', 'md', 'lg', 'xl', 'full'].includes(value)
    },
    closable: {
      type: Boolean,
      default: true
    },
    closeOnOverlay: {
      type: Boolean,
      default: true
    },
    closeOnEscape: {
      type: Boolean,
      default: true
    },
    persistent: {
      type: Boolean,
      default: false
    },
    centered: {
      type: Boolean,
      default: true
    },
    scrollable: {
      type: Boolean,
      default: false
    },
    padding: {
      type: String,
      default: 'md',
      validator: (value) => ['none', 'sm', 'md', 'lg'].includes(value)
    }
  },
  data() {
    return {
      titleId: `modal-title-${Math.random().toString(36).substr(2, 9)}`,
      contentId: `modal-content-${Math.random().toString(36).substr(2, 9)}`
    }
  },
  computed: {
    modalClasses() {
      return [
        'modal',
        `modal-${this.size}`,
        {
          'modal-centered': this.centered,
          'modal-scrollable': this.scrollable
        }
      ]
    },
    bodyClasses() {
      return [
        `modal-padding-${this.padding}`
      ]
    }
  },
  watch: {
    modelValue(newVal) {
      if (newVal) {
        this.addEventListeners()
        this.lockBodyScroll()
      } else {
        this.removeEventListeners()
        this.unlockBodyScroll()
      }
    }
  },
  beforeUnmount() {
    this.removeEventListeners()
    this.unlockBodyScroll()
  },
  methods: {
    closeModal() {
      if (!this.persistent) {
        this.$emit('update:modelValue', false)
        this.$emit('close')
      }
    },
    handleOverlayClick() {
      if (this.closeOnOverlay && !this.persistent) {
        this.closeModal()
      }
    },
    handleEscapeKey(event) {
      if (event.key === 'Escape' && this.closeOnEscape && !this.persistent) {
        this.closeModal()
      }
    },
    addEventListeners() {
      document.addEventListener('keydown', this.handleEscapeKey)
    },
    removeEventListeners() {
      document.removeEventListener('keydown', this.handleEscapeKey)
    },
    lockBodyScroll() {
      document.body.style.overflow = 'hidden'
    },
    unlockBodyScroll() {
      document.body.style.overflow = ''
    }
  }
}
</script>

<style scoped>
/* Modal overlay */
.modal-overlay {
  @apply fixed inset-0 z-50 flex items-center justify-center;
  @apply bg-black bg-opacity-50 backdrop-blur-sm;
  @apply p-4;
}

/* Modal container */
.modal {
  @apply bg-white dark:bg-neutral-800 rounded-xl shadow-2xl;
  @apply border border-neutral-200 dark:border-neutral-700;
  @apply max-h-full overflow-hidden;
  @apply transform transition-all duration-300 ease-out;
}

/* Modal sizes */
.modal-xs {
  @apply w-full max-w-xs;
}

.modal-sm {
  @apply w-full max-w-sm;
}

.modal-md {
  @apply w-full max-w-md;
}

.modal-lg {
  @apply w-full max-w-lg;
}

.modal-xl {
  @apply w-full max-w-2xl;
}

.modal-full {
  @apply w-full h-full max-w-none max-h-none rounded-none;
}

/* Modal positioning */
.modal-centered {
  @apply my-auto;
}

.modal-scrollable {
  @apply overflow-y-auto;
}

/* Modal header */
.modal-header {
  @apply flex items-center justify-between;
  @apply px-6 py-4 border-b border-neutral-200 dark:border-neutral-700;
}

.modal-header-content {
  @apply flex-1 min-w-0;
}

.modal-title {
  @apply text-lg font-semibold text-neutral-900 dark:text-neutral-100;
  @apply leading-tight;
}

.modal-close-btn {
  @apply p-2 ml-4 rounded-lg;
  @apply text-neutral-500 hover:text-neutral-700;
  @apply dark:text-neutral-400 dark:hover:text-neutral-200;
  @apply hover:bg-neutral-100 dark:hover:bg-neutral-700;
  @apply transition-all duration-200;
  @apply focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2;
  @apply dark:focus:ring-offset-neutral-800;
}

/* Modal body */
.modal-body {
  @apply text-neutral-700 dark:text-neutral-300;
  @apply overflow-y-auto;
}

.modal-padding-none {
  @apply p-0;
}

.modal-padding-sm {
  @apply p-4;
}

.modal-padding-md {
  @apply p-6;
}

.modal-padding-lg {
  @apply p-8;
}

/* Modal footer */
.modal-footer {
  @apply px-6 py-4;
  @apply border-t border-neutral-200 dark:border-neutral-700;
  @apply bg-neutral-50 dark:bg-neutral-700;
  @apply flex items-center justify-end space-x-3;
}

/* Modal transitions */
.modal-enter-active {
  @apply transition-all duration-300 ease-out;
}

.modal-leave-active {
  @apply transition-all duration-200 ease-in;
}

.modal-enter-from {
  @apply opacity-0;
}

.modal-enter-from .modal {
  @apply transform scale-95 translate-y-4;
}

.modal-leave-to {
  @apply opacity-0;
}

.modal-leave-to .modal {
  @apply transform scale-95 translate-y-4;
}

/* Responsive adjustments */
@media (width <= 640px) {
  .modal-overlay {
    @apply p-2;
  }
  
  .modal {
    @apply rounded-lg;
  }
  
  .modal-full {
    @apply rounded-none;
  }
  
  .modal-header {
    @apply px-4 py-3;
  }
  
  .modal-padding-md {
    @apply p-4;
  }
  
  .modal-padding-lg {
    @apply p-6;
  }
  
  .modal-footer {
    @apply px-4 py-3;
  }
  
  .modal-title {
    @apply text-base;
  }
}

/* Reduced motion support */
@media (prefers-reduced-motion: reduce) {
  .modal-enter-active,
  .modal-leave-active {
    @apply transition-none;
  }
  
  .modal-enter-from .modal,
  .modal-leave-to .modal {
    @apply transform-none scale-100 translate-y-0;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .modal {
    @apply border-2;
  }
  
  .modal-close-btn {
    @apply border border-neutral-400;
  }
}

/* Focus management */
.modal {
  @apply focus:outline-none;
}

/* Clean overlay without AI effects */
</style>