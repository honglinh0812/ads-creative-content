<template>
  <div class="recommendation-card" :class="[`priority-${recommendation.priority.toLowerCase()}`, `status-${recommendation.status.toLowerCase()}`]">
    <!-- Card Header -->
    <div class="card-header">
      <div class="recommendation-type">
        <i :class="getTypeIcon(recommendation.type)"></i>
        <span>{{ formatRecommendationType(recommendation.type) }}</span>
      </div>
      <div class="priority-badge" :class="`priority-${recommendation.priority.toLowerCase()}`">
        {{ recommendation.priority }}
      </div>
    </div>

    <!-- Card Content -->
    <div class="card-content">
      <h3 class="recommendation-title">{{ recommendation.title }}</h3>
      <p class="recommendation-description">{{ recommendation.description }}</p>
      
      <!-- Target Entity -->
      <div v-if="recommendation.targetEntityName" class="target-entity">
        <i class="pi pi-tag"></i>
        <span>{{ recommendation.targetEntityName }}</span>
      </div>

      <!-- Expected Impact -->
      <div v-if="recommendation.expectedImpact" class="expected-impact">
        <div class="impact-header">
          <i class="pi pi-chart-line"></i>
          <span>Expected Impact</span>
        </div>
        <div class="impact-details">
          <div class="impact-metric">
            <span class="metric-name">{{ recommendation.expectedImpact.metric }}</span>
            <span class="metric-change" :class="getChangeClass(recommendation.expectedImpact.changeType)">
              <i :class="getChangeIcon(recommendation.expectedImpact.changeType)"></i>
              {{ formatChange(recommendation.expectedImpact.expectedChange, recommendation.expectedImpact.changeType) }}
            </span>
          </div>
          <div class="impact-timeframe">
            <i class="pi pi-clock"></i>
            <span>{{ recommendation.expectedImpact.timeframe }}</span>
          </div>
          <div class="impact-confidence">
            <i class="pi pi-shield"></i>
            <span>{{ (recommendation.expectedImpact.confidence * 100).toFixed(0) }}% confidence</span>
          </div>
        </div>
      </div>

      <!-- Category and Tags -->
      <div class="metadata">
        <div class="category">
          <i class="pi pi-folder"></i>
          <span>{{ recommendation.category }}</span>
        </div>
        <div v-if="recommendation.tags && recommendation.tags.length" class="tags">
          <span 
            v-for="tag in recommendation.tags.slice(0, 3)" 
            :key="tag" 
            class="tag"
          >
            {{ tag }}
          </span>
        </div>
      </div>
    </div>

    <!-- Card Actions -->
    <div class="card-actions">
      <div class="action-buttons">
        <button 
          @click="$emit('view-details', recommendation)"
          class="action-btn view-btn"
          title="View Details"
        >
          <i class="pi pi-eye"></i>
          Details
        </button>
        
        <button 
          v-if="recommendation.status === 'PENDING'"
          @click="$emit('accept', recommendation)"
          class="action-btn accept-btn"
          title="Accept Recommendation"
        >
          <i class="pi pi-check"></i>
          Accept
        </button>
        
        <button 
          v-if="recommendation.status === 'PENDING'"
          @click="$emit('schedule', recommendation)"
          class="action-btn schedule-btn"
          title="Schedule Implementation"
        >
          <i class="pi pi-calendar"></i>
          Schedule
        </button>
        
        <button 
          v-if="recommendation.status === 'PENDING'"
          @click="showDismissModal = true"
          class="action-btn dismiss-btn"
          title="Dismiss Recommendation"
        >
          <i class="pi pi-times"></i>
          Dismiss
        </button>
      </div>
      
      <!-- Status Indicator -->
      <div class="status-indicator" :class="`status-${recommendation.status.toLowerCase()}`">
        <i :class="getStatusIcon(recommendation.status)"></i>
        <span>{{ formatStatus(recommendation.status) }}</span>
      </div>
    </div>

    <!-- Expiration Warning -->
    <div v-if="isExpiringSoon" class="expiration-warning">
      <i class="pi pi-exclamation-triangle"></i>
      <span>Expires {{ formatExpirationTime(recommendation.expiresAt) }}</span>
    </div>

    <!-- Dismiss Modal -->
    <div v-if="showDismissModal" class="modal-overlay" @click="showDismissModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Dismiss Recommendation</h3>
          <button @click="showDismissModal = false" class="modal-close">
            <i class="pi pi-times"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <p>Why are you dismissing this recommendation?</p>
          <div class="dismiss-reasons">
            <label v-for="reason in dismissReasons" :key="reason.value" class="reason-option">
              <input 
                type="radio" 
                :value="reason.value" 
                v-model="selectedDismissReason"
                name="dismissReason"
              >
              <span>{{ reason.label }}</span>
            </label>
          </div>
          <textarea 
            v-model="customDismissReason" 
            placeholder="Additional comments (optional)"
            class="custom-reason"
          ></textarea>
        </div>
        
        <div class="modal-footer">
          <button @click="showDismissModal = false" class="btn btn-secondary">
            Cancel
          </button>
          <button @click="confirmDismiss" class="btn btn-danger">
            Dismiss
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'

export default {
  name: 'RecommendationCard',
  props: {
    recommendation: {
      type: Object,
      required: true
    }
  },
  emits: ['accept', 'dismiss', 'schedule', 'view-details'],
  
  setup(props, { emit }) {
    const showDismissModal = ref(false)
    const selectedDismissReason = ref('')
    const customDismissReason = ref('')
    
    const dismissReasons = [
      { value: 'not_relevant', label: 'Not relevant to my goals' },
      { value: 'already_implemented', label: 'Already implemented' },
      { value: 'insufficient_data', label: 'Based on insufficient data' },
      { value: 'too_risky', label: 'Too risky for my business' },
      { value: 'other', label: 'Other reason' }
    ]
    
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
    
    const confirmDismiss = () => {
      const reason = selectedDismissReason.value === 'other' ? customDismissReason.value : selectedDismissReason.value
      emit('dismiss', props.recommendation, reason)
      showDismissModal.value = false
      selectedDismissReason.value = ''
      customDismissReason.value = ''
    }
    
    return {
      showDismissModal,
      selectedDismissReason,
      customDismissReason,
      dismissReasons,
      isExpiringSoon,
      getTypeIcon,
      formatRecommendationType,
      getChangeClass,
      getChangeIcon,
      formatChange,
      getStatusIcon,
      formatStatus,
      formatExpirationTime,
      confirmDismiss
    }
  }
}
</script>

<style scoped>
.recommendation-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.recommendation-card:hover {
  box-shadow: 0 8px 25px -3px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.recommendation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.recommendation-type {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
}

.recommendation-type i {
  font-size: 16px;
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

.card-content {
  margin-bottom: 20px;
}

.recommendation-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.recommendation-description {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin: 0 0 16px 0;
}

.target-entity {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #9ca3af;
  margin-bottom: 16px;
}

.expected-impact {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.impact-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.impact-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.impact-metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-name {
  font-size: 13px;
  color: #6b7280;
}

.metric-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
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

.impact-timeframe,
.impact-confidence {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.metadata {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.category {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  padding: 2px 6px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  color: #6b7280;
  font-size: 12px;
  font-weight: 500;
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

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.modal-close {
  padding: 8px;
  border: none;
  background: none;
  color: #6b7280;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 24px;
}

.modal-body p {
  margin: 0 0 16px 0;
  color: #6b7280;
}

.dismiss-reasons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.reason-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.reason-option input[type="radio"] {
  margin: 0;
}

.custom-reason {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  min-height: 80px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid #e5e7eb;
}

.btn {
  padding: 10px 20px;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-danger {
  background: #dc2626;
  color: white;
}

.btn-danger:hover {
  background: #b91c1c;
}

/* Responsive Design */
@media (max-width: 640px) {
  .recommendation-card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .metadata {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  
  .card-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .action-buttons {
    justify-content: space-between;
  }
  
  .action-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>
