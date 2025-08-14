<template>
  <div class="kpi-card" :class="`kpi-card--${color}`">
    <div class="kpi-header">
      <div class="kpi-icon" :class="`kpi-icon--${color}`">
        <i :class="`pi ${icon}`"></i>
      </div>
      <div class="kpi-change" :class="changeClass" v-if="change !== null && change !== undefined">
        <i :class="changeIcon"></i>
        <span>{{ Math.abs(change).toFixed(1) }}%</span>
      </div>
    </div>
    
    <div class="kpi-content">
      <div class="kpi-value">{{ formattedValue }}</div>
      <div class="kpi-title">{{ title }}</div>
    </div>
    
    <div class="kpi-trend" v-if="showTrend">
      <div class="trend-line" :class="trendClass"></div>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'KPICard',
  props: {
    title: {
      type: String,
      required: true
    },
    value: {
      type: [Number, String],
      required: true
    },
    change: {
      type: Number,
      default: null
    },
    icon: {
      type: String,
      required: true
    },
    color: {
      type: String,
      default: 'blue',
      validator: (value) => ['blue', 'green', 'purple', 'orange', 'teal', 'indigo', 'pink', 'cyan'].includes(value)
    },
    format: {
      type: String,
      default: 'number',
      validator: (value) => ['number', 'currency', 'percentage'].includes(value)
    },
    showTrend: {
      type: Boolean,
      default: false
    }
  },
  
  setup(props) {
    const formattedValue = computed(() => {
      const numValue = typeof props.value === 'string' ? parseFloat(props.value) : props.value
      
      if (isNaN(numValue)) return props.value
      
      switch (props.format) {
        case 'currency':
          return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
          }).format(numValue)
          
        case 'percentage':
          return `${numValue.toFixed(1)}%`
          
        case 'number':
        default:
          if (numValue >= 1000000) {
            return `${(numValue / 1000000).toFixed(1)}M`
          } else if (numValue >= 1000) {
            return `${(numValue / 1000).toFixed(1)}K`
          }
          return numValue.toLocaleString()
      }
    })
    
    const changeClass = computed(() => {
      if (props.change === null || props.change === undefined) return ''
      
      if (props.change > 0) return 'kpi-change--positive'
      if (props.change < 0) return 'kpi-change--negative'
      return 'kpi-change--neutral'
    })
    
    const changeIcon = computed(() => {
      if (props.change === null || props.change === undefined) return ''
      
      if (props.change > 0) return 'pi pi-arrow-up'
      if (props.change < 0) return 'pi pi-arrow-down'
      return 'pi pi-minus'
    })
    
    const trendClass = computed(() => {
      if (props.change === null || props.change === undefined) return ''
      
      if (props.change > 0) return 'trend-line--up'
      if (props.change < 0) return 'trend-line--down'
      return 'trend-line--flat'
    })
    
    return {
      formattedValue,
      changeClass,
      changeIcon,
      trendClass
    }
  }
}
</script>

<style scoped>
.kpi-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  position: relative;
  overflow: hidden;
  transition: var(--transition-all);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--gray-300);
  transition: var(--transition-colors);
}

.kpi-card--blue::before { background: var(--blue-500); }
.kpi-card--green::before { background: var(--green-500); }
.kpi-card--purple::before { background: var(--purple-500); }
.kpi-card--orange::before { background: var(--orange-500); }
.kpi-card--teal::before { background: var(--teal-500); }
.kpi-card--indigo::before { background: var(--indigo-500); }
.kpi-card--pink::before { background: var(--pink-500); }
.kpi-card--cyan::before { background: var(--cyan-500); }

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-4);
}

.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--gray-100);
  color: var(--gray-600);
  transition: var(--transition-colors);
}

.kpi-icon--blue { background: var(--blue-50); color: var(--blue-600); }
.kpi-icon--green { background: var(--green-50); color: var(--green-600); }
.kpi-icon--purple { background: var(--purple-50); color: var(--purple-600); }
.kpi-icon--orange { background: var(--orange-50); color: var(--orange-600); }
.kpi-icon--teal { background: var(--teal-50); color: var(--teal-600); }
.kpi-icon--indigo { background: var(--indigo-50); color: var(--indigo-600); }
.kpi-icon--pink { background: var(--pink-50); color: var(--pink-600); }
.kpi-icon--cyan { background: var(--cyan-50); color: var(--cyan-600); }

.kpi-icon i {
  font-size: 1.5rem;
}

.kpi-change {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.kpi-change--positive {
  background: var(--green-100);
  color: var(--green-700);
}

.kpi-change--negative {
  background: var(--red-100);
  color: var(--red-700);
}

.kpi-change--neutral {
  background: var(--gray-100);
  color: var(--gray-700);
}

.kpi-change i {
  font-size: 0.75rem;
}

.kpi-content {
  margin-bottom: var(--space-4);
}

.kpi-value {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  line-height: var(--leading-tight);
  margin-bottom: var(--space-2);
}

.kpi-title {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-medium);
  line-height: var(--leading-tight);
}

.kpi-trend {
  height: 4px;
  background: var(--gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
  position: relative;
}

.trend-line {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  width: 70%;
  border-radius: var(--radius-full);
  transition: var(--transition-all);
}

.trend-line--up {
  background: linear-gradient(90deg, var(--green-400), var(--green-600));
  animation: slideIn 1s ease-out;
}

.trend-line--down {
  background: linear-gradient(90deg, var(--red-400), var(--red-600));
  animation: slideIn 1s ease-out;
}

.trend-line--flat {
  background: linear-gradient(90deg, var(--gray-400), var(--gray-600));
  animation: slideIn 1s ease-out;
}

@keyframes slideIn {
  from {
    width: 0;
  }

  to {
    width: 70%;
  }
}

/* Responsive Design */
@media (width <= 640px) {
  .kpi-card {
    padding: var(--space-4);
  }
  
  .kpi-header {
    margin-bottom: var(--space-3);
  }
  
  .kpi-icon {
    width: 40px;
    height: 40px;
  }
  
  .kpi-icon i {
    font-size: 1.25rem;
  }
  
  .kpi-value {
    font-size: var(--text-2xl);
  }
  
  .kpi-content {
    margin-bottom: var(--space-3);
  }
}

/* Dark mode support */
.dark .kpi-card {
  background: var(--gray-800);
  border-color: var(--gray-700);
}

.dark .kpi-value {
  color: var(--gray-100);
}

.dark .kpi-title {
  color: var(--gray-300);
}

.dark .kpi-trend {
  background: var(--gray-700);
}
</style>
