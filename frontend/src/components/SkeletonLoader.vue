<template>
  <div :class="['skeleton-loader', variant, { animated }]">
    <!-- Text Skeleton -->
    <template v-if="variant === 'text'">
      <div 
        v-for="line in lines" 
        :key="line"
        class="skeleton-line"
        :style="{ 
          width: line === lines ? (lastLineWidth || '60%') : '100%',
          height: height || '1em'
        }"
      />
    </template>

    <!-- Avatar Skeleton -->
    <template v-else-if="variant === 'avatar'">
      <div 
        class="skeleton-avatar"
        :style="{ 
          width: size || '3rem',
          height: size || '3rem'
        }"
      />
    </template>

    <!-- Card Skeleton -->
    <template v-else-if="variant === 'card'">
      <div class="skeleton-card">
        <div v-if="showImage" class="skeleton-card-image" />
        <div class="skeleton-card-content">
          <div class="skeleton-line skeleton-title" />
          <div class="skeleton-line skeleton-subtitle" />
          <div v-for="n in (contentLines || 2)" :key="n" class="skeleton-line" />
        </div>
      </div>
    </template>

    <!-- Button Skeleton -->
    <template v-else-if="variant === 'button'">
      <div 
        class="skeleton-button"
        :style="{ 
          width: width || '120px',
          height: height || '2.5rem'
        }"
      />
    </template>

    <!-- Table Skeleton -->
    <template v-else-if="variant === 'table'">
      <div class="skeleton-table">
        <div class="skeleton-table-header">
          <div 
            v-for="col in (columns || 4)" 
            :key="col"
            class="skeleton-table-cell skeleton-table-header-cell"
          />
        </div>
        <div 
          v-for="row in (rows || 5)" 
          :key="row"
          class="skeleton-table-row"
        >
          <div 
            v-for="col in (columns || 4)" 
            :key="col"
            class="skeleton-table-cell"
          />
        </div>
      </div>
    </template>

    <!-- List Skeleton -->
    <template v-else-if="variant === 'list'">
      <div class="skeleton-list">
        <div 
          v-for="item in (items || 5)" 
          :key="item"
          class="skeleton-list-item"
        >
          <div v-if="showAvatar" class="skeleton-avatar skeleton-list-avatar" />
          <div class="skeleton-list-content">
            <div class="skeleton-line skeleton-list-title" />
            <div class="skeleton-line skeleton-list-subtitle" />
          </div>
          <div v-if="showActions" class="skeleton-list-actions">
            <div class="skeleton-button skeleton-action-btn" />
            <div class="skeleton-button skeleton-action-btn" />
          </div>
        </div>
      </div>
    </template>

    <!-- Custom Rectangle -->
    <template v-else>
      <div 
        class="skeleton-rectangle"
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
.skeleton-loader {
  --skeleton-color: var(--neutral-200, #e5e7eb);
  --skeleton-highlight: var(--neutral-100, #f3f4f6);
}

.skeleton-loader.animated .skeleton-line,
.skeleton-loader.animated .skeleton-avatar,
.skeleton-loader.animated .skeleton-button,
.skeleton-loader.animated .skeleton-rectangle,
.skeleton-loader.animated .skeleton-card-image,
.skeleton-loader.animated .skeleton-table-cell {
  background: linear-gradient(
    90deg,
    var(--skeleton-color) 25%,
    var(--skeleton-highlight) 50%,
    var(--skeleton-color) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

.skeleton-loader:not(.animated) .skeleton-line,
.skeleton-loader:not(.animated) .skeleton-avatar,
.skeleton-loader:not(.animated) .skeleton-button,
.skeleton-loader:not(.animated) .skeleton-rectangle,
.skeleton-loader:not(.animated) .skeleton-card-image,
.skeleton-loader:not(.animated) .skeleton-table-cell {
  background: var(--skeleton-color);
}

/* Base skeleton elements */
.skeleton-line {
  border-radius: var(--radius-sm, 0.25rem);
  margin-bottom: var(--space-2, 0.5rem);
}

.skeleton-line:last-child {
  margin-bottom: 0;
}

.skeleton-avatar {
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-button {
  border-radius: var(--radius-lg, 0.5rem);
}

.skeleton-rectangle {
  border-radius: var(--radius-md, 0.375rem);
}

/* Card Skeleton */
.skeleton-card {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-xl, 0.75rem);
  overflow: hidden;
  background: var(--color-bg-secondary, white);
}

.skeleton-card-image {
  width: 100%;
  height: 200px;
  border-radius: 0;
}

.skeleton-card-content {
  padding: var(--space-4, 1rem);
}

.skeleton-title {
  height: 1.25rem;
  margin-bottom: var(--space-3, 0.75rem);
}

.skeleton-subtitle {
  height: 1rem;
  width: 70%;
  margin-bottom: var(--space-4, 1rem);
}

/* Table Skeleton */
.skeleton-table {
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-lg, 0.5rem);
  overflow: hidden;
  background: var(--color-bg-secondary, white);
}

.skeleton-table-header {
  display: flex;
  background: var(--color-bg-tertiary, #f9fafb);
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.skeleton-table-row {
  display: flex;
  border-bottom: 1px solid var(--color-border-light, #f3f4f6);
}

.skeleton-table-row:last-child {
  border-bottom: none;
}

.skeleton-table-cell {
  flex: 1;
  height: 1rem;
  margin: var(--space-3, 0.75rem);
  border-radius: var(--radius-sm, 0.25rem);
}

.skeleton-table-header-cell {
  height: 0.875rem;
  margin: var(--space-4, 1rem) var(--space-3, 0.75rem);
}

/* List Skeleton */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
}

.skeleton-list-item {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-lg, 0.5rem);
  background: var(--color-bg-secondary, white);
}

.skeleton-list-avatar {
  width: 2.5rem;
  height: 2.5rem;
}

.skeleton-list-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.skeleton-list-title {
  height: 1rem;
  width: 60%;
}

.skeleton-list-subtitle {
  height: 0.875rem;
  width: 40%;
}

.skeleton-list-actions {
  display: flex;
  gap: var(--space-2, 0.5rem);
}

.skeleton-action-btn {
  width: 4rem;
  height: 2rem;
}

/* Animation */
@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .skeleton-card-content {
    padding: var(--space-3, 0.75rem);
  }
  
  .skeleton-list-item {
    padding: var(--space-3, 0.75rem);
  }
  
  .skeleton-list-actions {
    flex-direction: column;
  }
  
  .skeleton-action-btn {
    width: 3rem;
    height: 1.75rem;
  }
}

/* Dark mode support */
@media (prefers-color-scheme: dark) {
  .skeleton-loader {
    --skeleton-color: var(--neutral-700, #374151);
    --skeleton-highlight: var(--neutral-600, #4b5563);
  }
}
</style>
