<template>
  <div :class="['skeleton', 'animate-pulse', type]" :style="skeletonStyle">
    <template v-if="type === 'card'">
      <div class="skeleton-card bg-gray-200 dark:bg-gray-700 w-full h-45 rounded-xl transition-all duration-300"></div>
    </template>
    <template v-else-if="type === 'table'">
      <div v-for="i in rows" :key="i" class="skeleton-row flex gap-2 mb-2">
        <div v-for="j in cols" :key="j" class="skeleton-bar bg-gray-200 dark:bg-gray-700 flex-1 h-6 rounded-md transition-all duration-300"></div>
      </div>
    </template>
    <template v-else-if="type === 'text'">
      <div v-for="i in rows" :key="i" class="skeleton-bar-sm bg-gray-200 dark:bg-gray-700 w-full h-4.5 rounded-md mb-2 last:mb-0 transition-all duration-300"></div>
    </template>
    <template v-else-if="type === 'avatar'">
      <div class="skeleton-avatar bg-gray-200 dark:bg-gray-700 w-12 h-12 rounded-full transition-all duration-300"></div>
    </template>
  </div>
</template>

<script>
export default {
  name: 'LoadingSkeleton',
  props: {
    type: {
      type: String,
      default: 'card'
    },
    rows: {
      type: Number,
      default: 3
    },
    cols: {
      type: Number,
      default: 3
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: 'auto'
    }
  },
  computed: {
    skeletonStyle() {
      return {
        width: this.width,
        height: this.height
      }
    }
  }
}
</script>

<style scoped>
/* Custom shimmer animation for enhanced visual effect */
.skeleton.animate-pulse .skeleton-card,
.skeleton.animate-pulse .skeleton-bar,
.skeleton.animate-pulse .skeleton-bar-sm,
.skeleton.animate-pulse .skeleton-avatar {
  background: linear-gradient(
    90deg,
    theme('colors.gray.200') 25%,
    theme('colors.gray.100') 50%,
    theme('colors.gray.200') 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

@media (prefers-color-scheme: dark) {
  .skeleton.animate-pulse .skeleton-card,
  .skeleton.animate-pulse .skeleton-bar,
  .skeleton.animate-pulse .skeleton-bar-sm,
  .skeleton.animate-pulse .skeleton-avatar {
    background: linear-gradient(
      90deg,
      theme('colors.gray.700') 25%,
      theme('colors.gray.600') 50%,
      theme('colors.gray.700') 75%
    );
  }
}

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
  .skeleton-card {
    @apply h-32;
  }
  
  .skeleton-bar {
    @apply h-5;
  }
  
  .skeleton-bar-sm {
    @apply h-4;
  }
  
  .skeleton-avatar {
    @apply w-10 h-10;
  }
}

/* Accessibility improvements */
@media (prefers-reduced-motion: reduce) {
  .skeleton.animate-pulse .skeleton-card,
  .skeleton.animate-pulse .skeleton-bar,
  .skeleton.animate-pulse .skeleton-bar-sm,
  .skeleton.animate-pulse .skeleton-avatar {
    animation: none;

    @apply opacity-60;
  }
}

/* High contrast mode support */
@media (prefers-contrast: high) {
  .skeleton .skeleton-card,
  .skeleton .skeleton-bar,
  .skeleton .skeleton-bar-sm,
  .skeleton .skeleton-avatar {
    @apply border border-gray-400 dark:border-gray-500;
  }
}

/* Focus management */
.skeleton {
  @apply select-none;
}
</style>