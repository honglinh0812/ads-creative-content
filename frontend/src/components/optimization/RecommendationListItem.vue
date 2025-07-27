<template>
  <div class="recommendation-list-item" :class="[`priority-${recommendation.priority.toLowerCase()}`, `status-${recommendation.status.toLowerCase()}`]">
    <div class="item-content">
      <!-- Left Section: Type and Priority -->
      <div class="item-left">
        <div class="type-icon" :class="`priority-${recommendation.priority.toLowerCase()}`">
          <i :class="getTypeIcon(recommendation.type)"></i>
        </div>
        <div class="item-info">
          <h4 class="item-title">{{ recommendation.title }}</h4>
          <div class="item-meta">
            <span class="item-type">{{ formatRecommendationType(recommendation.type) }}</span>
            <span class="item-separator">•</span>
            <span class="item-category">{{ recommendation.category }}</span>
            <span v-if="recommendation.targetEntityName" class="item-separator">•</span>
            <span v-if="recommendation.targetEntityName" class="item-target">{{ recommendation.targetEntityName }}</span>
          </div>
        </div>
      </div>

      <!-- Center Section: Description and Impact -->
      <div class="item-center">
        <p class="item-description">{{ recommendation.description }}</p>
        <div v-if="recommendation.expectedImpact" class="impact-summary">
          <span class="impact-metric">{{ recommendation.expectedImpact.metric }}</span>
          <span class="impact-change" :class="getChangeClass(recommendation.expectedImpact.changeType)">
            <i :class="getChangeIcon(recommendation.expectedImpact.changeType)"></i>
            {{ formatChange(recommendation.expectedImpact.expectedChange, recommendation.expectedImpact.changeType) }}
          </span>
          <span class="impact-confidence">{{ (recommendation.expectedImpact.confidence * 100).toFixed(0) }}% confidence</span>
        </div>
      </div>

      <!-- Right Section: Actions and Status -->
      <div class="item-right">
        <div class="priority-badge" :class="`priority-${recommendation.priority.toLowerCase()}`">
          {{ recommendation.priority }}
        </div>
        
        <div class="item-actions">
          <button 
            @click="$emit('view-details', recommendation)"
            class="action-btn view-btn"
            title="View Details"
          >
            <i class="pi pi-eye"></i>
          </button>
          
          <button 
            v-if="recommendation.status === 'PENDING'"
            @click="$emit('accept', recommendation)"
            class="action-btn accept-btn"
            title="Accept Recommendation"
          >
            <i class="pi pi-check"></i>
          </button>
          
          <button 
            v-if="recommendation.status === 'PENDING'"
            @click="$emit('schedule', recommendation)"
            class="action-btn schedule-btn"
            title="Schedule Implementation"
          >
            <i class="pi pi-calendar"></i>
          </button>
          
          <button 
            v-if="recommendation.status === 'PENDING'"
            @click="$emit('dismiss', recommendation)"
            class="action-btn dismiss-btn"
            title="Dismiss Recommendation"
          >
            <i class="pi pi-times"></i>
          </button>
        </div>
        
        <div class="status-indicator" :class="`status-${recommendation.status.toLowerCase()}`">
          <i :class="getStatusIcon(recommendation.status)"></i>
          <span>{{ formatStatus(recommendation.status) }}</span>
        </div>
      </div>
    </div>

    <!-- Expiration Warning -->
    <div v-if="isExpiringSoon" class="expiration-warning">
      <i class="pi pi-exclamation-triangle"></i>
      <span>Expires {{ formatExpirationTime(recommendation.expiresAt) }}</span>
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'RecommendationListItem',
  props: {
    recommendation: {
      type: Object,
      required: true
    }
  },
  emits: ['accept', 'dismiss', 'schedule', 'view-details'],
  
  setup(props) {
    const isExpiringSoon = computed(() => {
      if (!props.recommendation.expiresAt) return false
      
      const expirationDate = new Date(props.recommendation.expiresAt)
      const now = new Date()
      const hoursUntilExpiration = (expirationDate - now) / (1000 * 60 * 60)
      
      return hoursUntilExpiration <= 48 && hoursUntilExpiration > 0
    })
    
    // Methods
    const getTypeIcon = (type) => {
      const icons = {
        'BUDGET_REALLOCATION': 'pi pi-dollar',
        'AI_PROVIDER_SWITCH': 'pi pi-cog',
        'CAMPAIGN_OBJECTIVE_OPTIMIZATION': 'pi pi-target',
        'AD_SCHEDULING': 'pi pi-clock',
        'CONTENT_TYPE_OPTIMIZATION': 'pi pi-file-edit',
        'AD_CREATIVE_REFRESH': 'pi pi-refresh',
        'CAMPAIGN_PAUSE': 'pi pi-pause',
        'CAMPAIGN_SCALE': 'pi pi-arrow-up'
      }
      return icons[type] || 'pi pi-lightbulb'
    }
    
    const formatRecommendationType = (type) => {
      return type.replace(/_/g, ' ').toLowerCase()
        .replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const getChangeClass = (changeType) => {
      const classes = {
        'increase': 'change-positive',
        'decrease': 'change-negative',
        'optimize': 'change-neutral'
      }
      return classes[changeType] || 'change-neutral'
    }
    
    const getChangeIcon = (changeType) => {
      const icons = {
        'increase': 'pi pi-arrow-up',
        'decrease': 'pi pi-arrow-down',
        'optimize': 'pi pi-arrows-h'
      }
      return icons[changeType] || 'pi pi-arrows-h'
    }
    
    const formatChange = (change, changeType) => {
      const prefix = changeType === 'increase' ? '+' : changeType === 'decrease' ? '-' : ''
      return `${prefix}${Math.abs(change).toFixed(1)}%`
    }
    
    const getStatusIcon = (status) => {
      const icons = {
        'PENDING': 'pi pi-clock',
        'ACCEPTED': 'pi pi-check',
        'DISMISSED': 'pi pi-times',
        'IMPLEMENTED': 'pi pi-check-circle',
        'SCHEDULED': 'pi pi-calendar',
        'EXPIRED': 'pi pi-exclamation-triangle'
      }
      return icons[status] || 'pi pi-question'
    }
    
    const formatStatus = (status) => {
      return status.toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
    }
    
    const formatExpirationTime = (expiresAt) => {
      if (!expiresAt) return ''
      
      const expirationDate = new Date(expiresAt)
      const now = new Date()
      const diffInHours = (expirationDate - now) / (1000 * 60 * 60)
      
      if (diffInHours < 24) {
        return `in ${Math.round(diffInHours)} hours`
      } else {
        return `in ${Math.round(diffInHours / 24)} days`
      }
    }
    
    return {
      isExpiringSoon,
      getTypeIcon,
      formatRecommendationType,
      getChangeClass,
      getChangeIcon,
      formatChange,
      getStatusIcon,
      formatStatus,
      formatExpirationTime
    }
  }
}
</script>

<style scoped>
.recommendation-list-item {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.recommendation-list-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.recommendation-list-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 4px;
  background: #d1d5db;
}

.priority-high::before {
  background: #dc2626;
}

.priority-medium::before {
  background: #f59e0b;
}

.priority-low::before {
  background: #10b981;
}

.item-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.item-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-shrink: 0;
}

.type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 18px;
}

.type-icon.priority-high {
  background: #fee2e2;
  color: #dc2626;
}

.type-icon.priority-medium {
  background: #fef3c7;
  color: #f59e0b;
}

.type-icon.priority-low {
  background: #dcfce7;
  color: #10b981;
}

.item-info {
  min-width: 200px;
}

.item-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
  flex-wrap: wrap;
}

.item-separator {
  color: #d1d5db;
}

.item-center {
  flex: 1;
  min-width: 0;
}

.item-description {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin: 0 0 8px 0;
}

.impact-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  flex-wrap: wrap;
}

.impact-metric {
  color: #374151;
  font-weight: 500;
}

.impact-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.change-positive {
  color: #059669;
}

.change-negative {
  color: #dc2626;
}

.change-neutral {
  color: #6b7280;
}

.impact-confidence {
  color: #9ca3af;
}

.item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.priority-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.priority-badge.priority-high {
  background: #fee2e2;
  color: #991b1b;
}

.priority-badge.priority-medium {
  background: #fef3c7;
  color: #92400e;
}

.priority-badge.priority-low {
  background: #dcfce7;
  color: #166534;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: #f9fafb;
}

.view-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.accept-btn:hover {
  border-color: #10b981;
  color: #10b981;
  background: #f0fdf4;
}

.schedule-btn:hover {
  border-color: #f59e0b;
  color: #f59e0b;
  background: #fffbeb;
}

.dismiss-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  color: #6b7280;
}

.status-accepted {
  color: #059669;
}

.status-dismissed {
  color: #dc2626;
}

.status-implemented {
  color: #059669;
}

.status-scheduled {
  color: #f59e0b;
}

.expiration-warning {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #fef3c7;
  color: #92400e;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

/* Responsive Design */
@media (max-width: 768px) {
  .item-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .item-left {
    align-items: center;
  }
  
  .item-info {
    min-width: auto;
    flex: 1;
  }
  
  .item-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .item-separator {
    display: none;
  }
  
  .item-right {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }
  
  .impact-summary {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}

@media (max-width: 480px) {
  .recommendation-list-item {
    padding: 16px;
  }
  
  .item-actions {
    flex-wrap: wrap;
  }
  
  .action-btn {
    width: 28px;
    height: 28px;
  }
}
</style>
