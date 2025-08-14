<template>
  <div :class="[
    'skeleton-loader',
    variant,
    { 
      'animate-pulse': animated,
      'opacity-75': !animated
    }
  ]">
    <!-- Text Skeleton -->
    <template v-if="variant === 'text'">
      <div 
        v-for="line in lines" 
        :key="line"
        class="skeleton-line bg-gray-200 dark:bg-gray-700 rounded-md mb-2 last:mb-0 transition-all duration-300"
        :style="{ 
          width: line === lines ? (lastLineWidth || '60%') : '100%',
          height: height || '1em'
        }"
      />
    </template>

    <!-- Avatar Skeleton -->
    <template v-else-if="variant === 'avatar'">
      <div 
        class="skeleton-avatar bg-gray-200 dark:bg-gray-700 rounded-full flex-shrink-0 transition-all duration-300"
        :style="{ 
          width: size || '3rem',
          height: size || '3rem'
        }"
      />
    </template>

    <!-- Card Skeleton -->
    <template v-else-if="variant === 'card'">
      <div class="skeleton-card bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-all duration-300">
        <div v-if="showImage" class="skeleton-card-image bg-gray-200 dark:bg-gray-700 w-full h-48 transition-all duration-300" />
        <div class="skeleton-card-content p-4">
          <div class="skeleton-line skeleton-title bg-gray-200 dark:bg-gray-700 h-5 rounded-md mb-3 transition-all duration-300" />
          <div class="skeleton-line skeleton-subtitle bg-gray-200 dark:bg-gray-700 h-4 w-3/4 rounded-md mb-4 transition-all duration-300" />
          <div v-for="n in (contentLines || 2)" :key="n" class="skeleton-line bg-gray-200 dark:bg-gray-700 h-4 rounded-md mb-2 last:mb-0 transition-all duration-300" />
        </div>
      </div>
    </template>

    <!-- Button Skeleton -->
    <template v-else-if="variant === 'button'">
      <div 
        class="skeleton-button bg-gray-200 dark:bg-gray-700 rounded-lg transition-all duration-300"
        :style="{ 
          width: width || '120px',
          height: height || '2.5rem'
        }"
      />
    </template>

    <!-- Table Skeleton -->
    <template v-else-if="variant === 'table'">
      <div class="skeleton-table bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden shadow-sm transition-all duration-300">
        <div class="skeleton-table-header bg-gray-50 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600 flex">
          <div 
            v-for="col in (columns || 4)" 
            :key="col"
            class="skeleton-table-cell skeleton-table-header-cell bg-gray-200 dark:bg-gray-600 h-3.5 rounded-sm mx-3 my-4 flex-1 transition-all duration-300"
          />
        </div>
        <div 
          v-for="row in (rows || 5)" 
          :key="row"
          class="skeleton-table-row flex border-b border-gray-100 dark:border-gray-700 last:border-b-0"
        >
          <div 
            v-for="col in (columns || 4)" 
            :key="col"
            class="skeleton-table-cell bg-gray-200 dark:bg-gray-700 h-4 rounded-sm mx-3 my-3 flex-1 transition-all duration-300"
          />
        </div>
      </div>
    </template>

    <!-- List Skeleton -->
    <template v-else-if="variant === 'list'">
      <div class="skeleton-list flex flex-col gap-3">
        <div 
          v-for="item in (items || 5)" 
          :key="item"
          class="skeleton-list-item flex items-center gap-3 p-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-sm hover:shadow-md transition-all duration-300"
        >
          <div v-if="showAvatar" class="skeleton-avatar skeleton-list-avatar bg-gray-200 dark:bg-gray-700 w-10 h-10 rounded-full flex-shrink-0 transition-all duration-300" />
          <div class="skeleton-list-content flex-1 flex flex-col gap-2">
            <div class="skeleton-line skeleton-list-title bg-gray-200 dark:bg-gray-700 h-4 w-3/5 rounded-md transition-all duration-300" />
            <div class="skeleton-line skeleton-list-subtitle bg-gray-200 dark:bg-gray-700 h-3.5 w-2/5 rounded-md transition-all duration-300" />
          </div>
          <div v-if="showActions" class="skeleton-list-actions flex gap-2">
            <div class="skeleton-button skeleton-action-btn bg-gray-200 dark:bg-gray-700 w-16 h-8 rounded-lg transition-all duration-300" />
            <div class="skeleton-button skeleton-action-btn bg-gray-200 dark:bg-gray-700 w-16 h-8 rounded-lg transition-all duration-300" />
          </div>
        </div>
      </div>
    </template>

    <!-- Custom Rectangle -->
    <template v-else>
      <div 
        class="skeleton-rectangle bg-gray-200 dark:bg-gray-700 rounded-md transition-all duration-300"
        :style="{ 
          width: width || '100%',
          height: height || '1rem'
        }"
      />
    </template>
  </div>
</template>

<script>
export default {
  name: 'SkeletonLoader',
  props: {
    variant: {
      type: String,
      default: 'rectangle',
      validator: (value) => [
        'text', 'avatar', 'card', 'button', 'table', 'list', 'rectangle'
      ].includes(value)
    },
    animated: {
      type: Boolean,
      default: true
    },
    width: {
      type: String,
      default: null
    },
    height: {
      type: String,
      default: null
    },
    size: {
      type: String,
      default: null
    },
    lines: {
      type: Number,
      default: 3
    },
    lastLineWidth: {
      type: String,
      default: '60%'
    },
    showImage: {
      type: Boolean,
      default: true
    },
    showAvatar: {
      type: Boolean,
      default: true
    },
    showActions: {
      type: Boolean,
      default: false
    },
    contentLines: {
      type: Number,
      default: 2
    },
    columns: {
      type: Number,
      default: 4
    },
    rows: {
      type: Number,
      default: 5
    },
    items: {
      type: Number,
      default: 5
    }
  }
}
</script>

<style scoped>
/* Custom shimmer animation for better visual effect */
.skeleton-loader.animate-pulse .skeleton-line,
.skeleton-loader.animate-pulse .skeleton-avatar,
.skeleton-loader.animate-pulse .skeleton-button,
.skeleton-loader.animate-pulse .skeleton-rectangle,
.skeleton-loader.animate-pulse .skeleton-card-image,
.skeleton-loader.animate-pulse .skeleton-table-cell {
  background: linear-gradient(
    90deg,
    theme('colors.gray.200') 25%,
    theme('colors.gray.100') 50%,
    theme('colors.gray.200') 75%
  );
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .skeleton-loader.animate-pulse .skeleton-line,
  .skeleton-loader.animate-pulse .skeleton-avatar,
  .skeleton-loader.animate-pulse .skeleton-button,
  .skeleton-loader.animate-pulse .skeleton-rectangle,
  .skeleton-loader.animate-pulse .skeleton-card-image,
  .skeleton-loader.animate-pulse .skeleton-table-cell {
    background: linear-gradient(
      90deg,
      theme('colors.gray.700') 25%,
      theme('colors.gray.600') 50%,
      theme('colors.gray.700') 75%
    );
  }
}

/* Custom shimmer keyframes */
@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

/* Responsive adjustments */
@media (width <= 640px) {
  .skeleton-card-content {
    @apply p-3;
  }
  
  .skeleton-list-item {
    @apply p-3;
  }
  
  .skeleton-list-actions {
    @apply flex-col gap-1;
  }
  
  .skeleton-action-btn {
    @apply w-12 h-7;
  }
  
  .skeleton-card-image {
    @apply h-32;
  }
}

/* Accessibility improvements */
@media (prefers-reduced-motion: reduce) {
  .skeleton-loader.animate-pulse .skeleton-line,
  .skeleton-loader.animate-pulse .skeleton-avatar,
  .skeleton-loader.animate-pulse .skeleton-button,
  .skeleton-loader.animate-pulse .skeleton-rectangle,
  .skeleton-loader.animate-pulse .skeleton-card-image,
  .skeleton-loader.animate-pulse .skeleton-table-cell {
    animation: none;

    @apply opacity-60;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .skeleton-loader .skeleton-line,
  .skeleton-loader .skeleton-avatar,
  .skeleton-loader .skeleton-button,
  .skeleton-loader .skeleton-rectangle,
  .skeleton-loader .skeleton-card-image,
  .skeleton-loader .skeleton-table-cell {
    @apply border border-gray-400 dark:border-gray-500;
  }
}

/* Focus management for accessibility */
.skeleton-loader {
  @apply select-none;
}

/* Improved loading states */
.skeleton-loader:not(.animate-pulse) {
  @apply opacity-60;
}

/* Enhanced visual hierarchy */
.skeleton-title {
  @apply w-4/5;
}

.skeleton-subtitle {
  @apply w-3/5;
}
</style>
