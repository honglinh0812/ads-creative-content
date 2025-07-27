<template>
  <div class="analytics-view">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">Analytics Dashboard</h1>
        <p class="page-description">
          Comprehensive insights and performance metrics for your Facebook advertising campaigns
        </p>
      </div>
      
      <!-- Quick Actions -->
      <div class="header-actions">
        <button @click="refreshData" class="action-btn refresh-btn" :disabled="loading">
          <i class="pi pi-refresh" :class="{ 'pi-spin': loading }"></i>
          Refresh
        </button>
        <button @click="exportData" class="action-btn export-btn">
          <i class="pi pi-download"></i>
          Export
        </button>
        <button @click="openSettings" class="action-btn settings-btn">
          <i class="pi pi-cog"></i>
          Settings
        </button>
      </div>
    </div>

    <!-- Analytics Dashboard Component -->
    <AnalyticsDashboard />

    <!-- Export Modal -->
    <div v-if="showExportModal" class="modal-overlay" @click="closeExportModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Export Analytics Data</h3>
          <button @click="closeExportModal" class="modal-close">
            <i class="pi pi-times"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <div class="export-options">
            <div class="option-group">
              <label class="option-label">Data Type</label>
              <div class="option-buttons">
                <button 
                  v-for="type in exportTypes"
                  :key="type.value"
                  @click="selectedExportType = type.value"
                  :class="['option-btn', { active: selectedExportType === type.value }]"
                >
                  <i :class="type.icon"></i>
                  {{ type.label }}
                </button>
              </div>
            </div>
            
            <div class="option-group">
              <label class="option-label">Format</label>
              <div class="option-buttons">
                <button 
                  v-for="format in exportFormats"
                  :key="format.value"
                  @click="selectedExportFormat = format.value"
                  :class="['option-btn', { active: selectedExportFormat === format.value }]"
                >
                  <i :class="format.icon"></i>
                  {{ format.label }}
                </button>
              </div>
            </div>
            
            <div class="option-group">
              <label class="option-label">Time Range</label>
              <select v-model="selectedExportTimeRange" class="time-range-select">
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
                <option value="90d">Last 90 days</option>
                <option value="1y">Last year</option>
              </select>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeExportModal" class="btn btn-secondary">
            Cancel
          </button>
          <button @click="performExport" class="btn btn-primary" :disabled="exporting">
            <i v-if="exporting" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-download"></i>
            {{ exporting ? 'Exporting...' : 'Export' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Settings Modal -->
    <div v-if="showSettingsModal" class="modal-overlay" @click="closeSettingsModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Analytics Settings</h3>
          <button @click="closeSettingsModal" class="modal-close">
            <i class="pi pi-times"></i>
          </button>
        </div>
        
        <div class="modal-body">
          <div class="settings-options">
            <div class="setting-group">
              <label class="setting-label">Default Time Range</label>
              <select v-model="defaultTimeRange" class="setting-select">
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
                <option value="90d">Last 90 days</option>
                <option value="1y">Last year</option>
              </select>
            </div>
            
            <div class="setting-group">
              <label class="setting-label">Auto Refresh</label>
              <div class="setting-toggle">
                <input 
                  type="checkbox" 
                  id="autoRefresh" 
                  v-model="autoRefresh"
                  class="toggle-input"
                >
                <label for="autoRefresh" class="toggle-label">
                  <span class="toggle-slider"></span>
                </label>
                <span class="toggle-text">{{ autoRefresh ? 'Enabled' : 'Disabled' }}</span>
              </div>
            </div>
            
            <div class="setting-group" v-if="autoRefresh">
              <label class="setting-label">Refresh Interval</label>
              <select v-model="refreshInterval" class="setting-select">
                <option value="30">30 seconds</option>
                <option value="60">1 minute</option>
                <option value="300">5 minutes</option>
                <option value="600">10 minutes</option>
              </select>
            </div>
            
            <div class="setting-group">
              <label class="setting-label">Email Reports</label>
              <div class="setting-toggle">
                <input 
                  type="checkbox" 
                  id="emailReports" 
                  v-model="emailReports"
                  class="toggle-input"
                >
                <label for="emailReports" class="toggle-label">
                  <span class="toggle-slider"></span>
                </label>
                <span class="toggle-text">{{ emailReports ? 'Enabled' : 'Disabled' }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button @click="closeSettingsModal" class="btn btn-secondary">
            Cancel
          </button>
          <button @click="saveSettings" class="btn btn-primary">
            <i class="pi pi-save"></i>
            Save Settings
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import AnalyticsDashboard from '@/components/AnalyticsDashboard.vue'
import { analyticsAPI } from '@/services/api'

export default {
  name: 'AnalyticsView',
  components: {
    AnalyticsDashboard
  },
  
  setup() {
    // Reactive data
    const loading = ref(false)
    const showExportModal = ref(false)
    const showSettingsModal = ref(false)
    const exporting = ref(false)
    
    // Export options
    const selectedExportType = ref('campaigns')
    const selectedExportFormat = ref('csv')
    const selectedExportTimeRange = ref('30d')
    
    // Settings
    const defaultTimeRange = ref('30d')
    const autoRefresh = ref(false)
    const refreshInterval = ref(300) // 5 minutes
    const emailReports = ref(false)
    
    let refreshTimer = null
    
    // Export configuration
    const exportTypes = [
      { value: 'campaigns', label: 'Campaigns', icon: 'pi pi-briefcase' },
      { value: 'ads', label: 'Ads', icon: 'pi pi-megaphone' },
      { value: 'content', label: 'Content', icon: 'pi pi-file-edit' },
      { value: 'all', label: 'All Data', icon: 'pi pi-database' }
    ]
    
    const exportFormats = [
      { value: 'csv', label: 'CSV', icon: 'pi pi-file' },
      { value: 'xlsx', label: 'Excel', icon: 'pi pi-file-excel' },
      { value: 'pdf', label: 'PDF', icon: 'pi pi-file-pdf' }
    ]
    
    // Methods
    const refreshData = async () => {
      loading.value = true
      try {
        // The AnalyticsDashboard component will handle its own refresh
        // This is just for the loading state
        await new Promise(resolve => setTimeout(resolve, 1000))
      } catch (error) {
        console.error('Error refreshing data:', error)
      } finally {
        loading.value = false
      }
    }
    
    const exportData = () => {
      showExportModal.value = true
    }
    
    const closeExportModal = () => {
      showExportModal.value = false
    }
    
    const performExport = async () => {
      exporting.value = true
      try {
        const response = await analyticsAPI.exportData(
          selectedExportType.value,
          { timeRange: selectedExportTimeRange.value },
          selectedExportFormat.value
        )
        
        // Create download link
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `analytics-${selectedExportType.value}-${selectedExportTimeRange.value}.${selectedExportFormat.value}`)
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        
        closeExportModal()
      } catch (error) {
        console.error('Export failed:', error)
        // You could show a toast notification here
      } finally {
        exporting.value = false
      }
    }
    
    const openSettings = () => {
      showSettingsModal.value = true
    }
    
    const closeSettingsModal = () => {
      showSettingsModal.value = false
    }
    
    const saveSettings = () => {
      // Save settings to localStorage or API
      const settings = {
        defaultTimeRange: defaultTimeRange.value,
        autoRefresh: autoRefresh.value,
        refreshInterval: refreshInterval.value,
        emailReports: emailReports.value
      }
      
      localStorage.setItem('analyticsSettings', JSON.stringify(settings))
      
      // Update auto-refresh timer
      setupAutoRefresh()
      
      closeSettingsModal()
    }
    
    const loadSettings = () => {
      const saved = localStorage.getItem('analyticsSettings')
      if (saved) {
        const settings = JSON.parse(saved)
        defaultTimeRange.value = settings.defaultTimeRange || '30d'
        autoRefresh.value = settings.autoRefresh || false
        refreshInterval.value = settings.refreshInterval || 300
        emailReports.value = settings.emailReports || false
      }
    }
    
    const setupAutoRefresh = () => {
      if (refreshTimer) {
        clearInterval(refreshTimer)
        refreshTimer = null
      }
      
      if (autoRefresh.value) {
        refreshTimer = setInterval(() => {
          refreshData()
        }, refreshInterval.value * 1000)
      }
    }
    
    // Lifecycle
    onMounted(() => {
      loadSettings()
      setupAutoRefresh()
    })
    
    onUnmounted(() => {
      if (refreshTimer) {
        clearInterval(refreshTimer)
      }
    })
    
    return {
      loading,
      showExportModal,
      showSettingsModal,
      exporting,
      selectedExportType,
      selectedExportFormat,
      selectedExportTimeRange,
      defaultTimeRange,
      autoRefresh,
      refreshInterval,
      emailReports,
      exportTypes,
      exportFormats,
      refreshData,
      exportData,
      closeExportModal,
      performExport,
      openSettings,
      closeSettingsModal,
      saveSettings
    }
  }
}
</script>

<style scoped>
.analytics-view {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 24px;
}

.header-content h1 {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.header-content p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  max-width: 600px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  color: #374151;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  background: #f9fafb;
  border-color: #9ca3af;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.refresh-btn:hover:not(:disabled) {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #3b82f6;
}

.export-btn:hover {
  background: #f0fdf4;
  border-color: #16a34a;
  color: #16a34a;
}

.settings-btn:hover {
  background: #fef3c7;
  border-color: #d97706;
  color: #d97706;
}

.pi-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
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

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid #e5e7eb;
}

/* Export Options */
.export-options {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.option-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.option-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.option-btn.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.time-range-select {
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
}

/* Settings */
.settings-options {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.setting-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.setting-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.setting-select {
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  background: white;
}

.setting-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-input {
  display: none;
}

.toggle-label {
  position: relative;
  width: 48px;
  height: 24px;
  background: #d1d5db;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.toggle-slider {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: white;
  border-radius: 50%;
  transition: transform 0.2s ease;
}

.toggle-input:checked + .toggle-label {
  background: #3b82f6;
}

.toggle-input:checked + .toggle-label .toggle-slider {
  transform: translateX(24px);
}

.toggle-text {
  font-size: 14px;
  color: #6b7280;
}

/* Buttons */
.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 8px;
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

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Responsive Design */
@media (max-width: 768px) {
  .analytics-view {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .header-actions {
    justify-content: space-between;
  }
  
  .action-btn {
    flex: 1;
    justify-content: center;
    padding: 10px 16px;
  }
  
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 16px;
  }
  
  .option-buttons {
    flex-direction: column;
  }
  
  .option-btn {
    justify-content: center;
  }
}
</style>
