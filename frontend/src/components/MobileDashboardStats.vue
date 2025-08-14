<template>
  <div class="mobile-dashboard-stats">
    <!-- Stats Grid -->
    <div class="stats-grid">
      <div 
        v-for="stat in stats" 
        :key="stat.key"
        class="stat-card"
        :class="`stat-card--${stat.variant || 'default'}`"
      >
        <div class="stat-icon">
          <i :class="stat.icon"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ formatValue(stat.value) }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div v-if="stat.change" class="stat-change" :class="getChangeClass(stat.change)">
            <i :class="getChangeIcon(stat.change)"></i>
            <span>{{ Math.abs(stat.change) }}%</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Quick Actions -->
    <div class="quick-actions">
      <h3 class="quick-actions-title">Quick Actions</h3>
      <div class="actions-grid">
        <button 
          v-for="action in quickActions"
          :key="action.key"
          class="action-button"
          @click="handleAction(action)"
        >
          <div class="action-icon">
            <i :class="action.icon"></i>
          </div>
          <span class="action-label">{{ action.label }}</span>
        </button>
      </div>
    </div>
    
    <!-- Recent Activity -->
    <div class="recent-activity">
      <div class="section-header">
        <h3 class="section-title">Recent Activity</h3>
        <button class="view-all-btn" @click="$emit('view-all-activity')">
          View All
        </button>
      </div>
      
      <div class="activity-list">
        <div 
          v-for="activity in recentActivity" 
          :key="activity.id"
          class="activity-item"
        >
          <div class="activity-icon" :class="`activity-icon--${activity.type}`">
            <i :class="getActivityIcon(activity.type)"></i>
          </div>
          <div class="activity-content">
            <div class="activity-title">{{ activity.title }}</div>
            <div class="activity-description">{{ activity.description }}</div>
            <div class="activity-time">{{ formatTime(activity.timestamp) }}</div>
          </div>
          <div v-if="activity.status" class="activity-status" :class="`status--${activity.status}`">
            {{ activity.status }}
          </div>
        </div>
        
        <div v-if="!recentActivity.length" class="empty-state">
          <i class="pi pi-clock"></i>
          <p>No recent activity</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MobileDashboardStats',
  props: {
    stats: {
      type: Array,
      default: () => [
        {
          key: 'campaigns',
          label: 'Total Campaigns',
          value: 0,
          icon: 'pi pi-briefcase',
          variant: 'primary',
          change: 0
        },
        {
          key: 'ads',
          label: 'Active Ads',
          value: 0,
          icon: 'pi pi-megaphone',
          variant: 'success',
          change: 0
        },
        {
          key: 'spent',
          label: 'Total Spent',
          value: 0,
          icon: 'pi pi-dollar',
          variant: 'warning',
          change: 0
        },
        {
          key: 'impressions',
          label: 'Impressions',
          value: 0,
          icon: 'pi pi-eye',
          variant: 'info',
          change: 0
        }
      ]
    },
    quickActions: {
      type: Array,
      default: () => [
        {
          key: 'create-ad',
          label: 'Create Ad',
          icon: 'pi pi-plus',
          action: 'create-ad'
        },
        {
          key: 'view-campaigns',
          label: 'Campaigns',
          icon: 'pi pi-briefcase',
          action: 'view-campaigns'
        },
        {
          key: 'analytics',
          label: 'Analytics',
          icon: 'pi pi-chart-line',
          action: 'view-analytics'
        },
        {
          key: 'settings',
          label: 'Settings',
          icon: 'pi pi-cog',
          action: 'view-settings'
        }
      ]
    },
    recentActivity: {
      type: Array,
      default: () => []
    }
  },
  
  emits: ['action', 'view-all-activity'],
  
  methods: {
    formatValue(value) {
      if (typeof value === 'number') {
        if (value >= 1000000) {
          return (value / 1000000).toFixed(1) + 'M'
        } else if (value >= 1000) {
          return (value / 1000).toFixed(1) + 'K'
        }
        return value.toLocaleString()
      }
      return value
    },
    
    getChangeClass(change) {
      if (change > 0) return 'stat-change--positive'
      if (change < 0) return 'stat-change--negative'
      return 'stat-change--neutral'
    },
    
    getChangeIcon(change) {
      if (change > 0) return 'pi pi-arrow-up'
      if (change < 0) return 'pi pi-arrow-down'
      return 'pi pi-minus'
    },
    
    getActivityIcon(type) {
      const icons = {
        'campaign': 'pi pi-briefcase',
        'ad': 'pi pi-megaphone',
        'payment': 'pi pi-dollar',
        'user': 'pi pi-user',
        'system': 'pi pi-cog',
        'default': 'pi pi-info-circle'
      }
      return icons[type] || icons.default
    },
    
    formatTime(timestamp) {
      const now = new Date()
      const time = new Date(timestamp)
      const diff = now - time
      
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return 'Just now'
      if (minutes < 60) return `${minutes}m ago`
      if (hours < 24) return `${hours}h ago`
      if (days < 7) return `${days}d ago`
      
      return time.toLocaleDateString()
    },
    
    handleAction(action) {
      this.$emit('action', action.action)
    }
  }
}
</script>

<style scoped>
.mobile-dashboard-stats {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  transition: var(--transition-all);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--gray-300);
  transition: var(--transition-colors);
}

.stat-card--primary::before { background: var(--brand-primary); }
.stat-card--success::before { background: var(--success-500); }
.stat-card--warning::before { background: var(--warning-500); }
.stat-card--info::before { background: var(--info-500); }

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--primary-50);
  color: var(--brand-primary);
  flex-shrink: 0;
}

.stat-card--success .stat-icon {
  background: var(--success-50);
  color: var(--success-600);
}

.stat-card--warning .stat-icon {
  background: var(--warning-50);
  color: var(--warning-600);
}

.stat-card--info .stat-icon {
  background: var(--info-50);
  color: var(--info-600);
}

.stat-icon i {
  font-size: 1.5rem;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  line-height: var(--leading-tight);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.stat-change {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
}

.stat-change--positive {
  color: var(--success-600);
}

.stat-change--negative {
  color: var(--error-600);
}

.stat-change--neutral {
  color: var(--color-text-muted);
}

/* Quick Actions */
.quick-actions {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
}

.quick-actions-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0 0 var(--space-4) 0;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(80px, 1fr));
  gap: var(--space-3);
}

.action-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  color: var(--color-text);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  transition: var(--transition-all);
  cursor: pointer;
  min-height: 80px;
}

.action-button:hover {
  background: var(--color-hover);
  border-color: var(--brand-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--primary-50);
  color: var(--brand-primary);
  border-radius: var(--radius-md);
}

.action-icon i {
  font-size: 1.125rem;
}

.action-label {
  text-align: center;
  line-height: var(--leading-tight);
}

/* Recent Activity */
.recent-activity {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin: 0;
}

.view-all-btn {
  background: none;
  border: none;
  color: var(--brand-primary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  cursor: pointer;
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: var(--transition-colors);
}

.view-all-btn:hover {
  background: var(--primary-50);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
  transition: var(--transition-colors);
}

.activity-item:hover {
  background: var(--color-hover);
}

.activity-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.activity-icon--campaign {
  background: var(--primary-50);
  color: var(--brand-primary);
}

.activity-icon--ad {
  background: var(--success-50);
  color: var(--success-600);
}

.activity-icon--payment {
  background: var(--warning-50);
  color: var(--warning-600);
}

.activity-icon--user {
  background: var(--info-50);
  color: var(--info-600);
}

.activity-icon--system {
  background: var(--gray-100);
  color: var(--gray-600);
}

.activity-icon i {
  font-size: 1rem;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-title {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.activity-description {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
  line-height: var(--leading-relaxed);
}

.activity-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.activity-status {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  flex-shrink: 0;
}

.status--active {
  background: var(--success-100);
  color: var(--success-700);
}

.status--pending {
  background: var(--warning-100);
  color: var(--warning-700);
}

.status--completed {
  background: var(--info-100);
  color: var(--info-700);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8);
  color: var(--color-text-muted);
  text-align: center;
}

.empty-state i {
  font-size: 2rem;
  opacity: 0.5;
}

/* Responsive Adjustments */
@media (width <= 640px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3);
  }
  
  .stat-card {
    padding: var(--space-3);
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
  }
  
  .stat-value {
    font-size: var(--text-xl);
  }
  
  .actions-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: var(--space-2);
  }
  
  .action-button {
    padding: var(--space-3);
    min-height: 70px;
  }
  
  .action-icon {
    width: 28px;
    height: 28px;
  }
  
  .action-label {
    font-size: var(--text-xs);
  }
}

@media (width <= 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-card {
    flex-direction: row;
    text-align: left;
  }
  
  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-2);
  }
  
  .view-all-btn {
    align-self: flex-end;
  }
}
</style>
