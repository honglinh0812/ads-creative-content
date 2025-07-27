<template>
  <div :class="['skeleton', type]" :style="skeletonStyle">
    <template v-if="type === 'card'">
      <div class="skeleton-card"></div>
    </template>
    <template v-else-if="type === 'table'">
      <div v-for="i in rows" :key="i" class="skeleton-row">
        <div v-for="j in cols" :key="j" class="skeleton-bar"></div>
      </div>
    </template>
    <template v-else-if="type === 'text'">
      <div v-for="i in rows" :key="i" class="skeleton-bar-sm"></div>
    </template>
    <template v-else-if="type === 'avatar'">
      <div class="skeleton-avatar"></div>
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
.skeleton-container {
  display: flex;
  gap: var(--space-2);
  width: 100%;
  background: none;
}
.skeleton-card {
  width: 100%;
  height: 180px;
  border-radius: var(--radius-xl);
  background: linear-gradient(90deg, var(--color-bg), var(--color-bg-secondary), var(--color-bg));
  animation: skeleton-loading 1.2s infinite linear;
}
.skeleton-row {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.skeleton-bar {
  flex: 1;
  height: 24px;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-bg), var(--color-bg-secondary), var(--color-bg));
  animation: skeleton-loading 1.2s infinite linear;
}
.skeleton-bar-sm {
  width: 100%;
  height: 18px;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-bg), var(--color-bg-secondary), var(--color-bg));
  animation: skeleton-loading 1.2s infinite linear;
}
.skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-bg), var(--color-bg-secondary), var(--color-bg));
  animation: skeleton-loading 1.2s infinite linear;
}
@keyframes skeleton-loading {
  100% {
    background-position: 200% 0;
  }
  0% {
    background-position: -200% 0;
  }
}
</style> 