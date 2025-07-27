<template>
  <div class="notification-page">
    <h1 class="noti-title-main">All notifications</h1>
    <div v-if="notifications.length > 0" class="noti-list">
      <div v-for="noti in notifications" :key="noti.id" class="noti-row">
        <span class="noti-icon" :class="noti.type">
          <i v-if="noti.type==='success'" class="pi pi-check-circle"></i>
          <i v-else-if="noti.type==='error'" class="pi pi-times-circle"></i>
          <i v-else-if="noti.type==='warning'" class="pi pi-exclamation-triangle"></i>
          <i v-else class="pi pi-info-circle"></i>
        </span>
        <div class="noti-content">
          <div class="noti-title">{{ noti.title || noti.type }}</div>
          <div class="noti-message">{{ noti.message }}</div>
          <div class="noti-time">{{ formatTime(noti.timestamp) }}</div>
        </div>
      </div>
    </div>
    <div v-else class="noti-empty">You don't have any notifications.</div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
export default {
  name: 'NotificationPage',
  computed: {
    ...mapGetters('toast', ['toasts']),
    notifications() {
      // Sắp xếp mới nhất lên trên
      return [...this.toasts].sort((a, b) => b.timestamp - a.timestamp)
    }
  },
  methods: {
    formatTime(ts) {
      if (!ts) return ''
      const d = new Date(ts)
      return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
    }
  }
}
</script>

<style scoped>
.notification-page {
  max-width: 600px;
  margin: 2.5rem auto 0 auto;
  padding: 1.5rem;
  background: #fff;
  border-radius: 1.2rem;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10), 0 1.5px 6px rgba(0,0,0,0.08);
}
.noti-title-main {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 2rem;
  text-align: center;
}
.noti-list {
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}
.noti-row {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.1rem 1.2rem;
  border-radius: 0.8rem;
  background: #f8fafc;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.noti-icon {
  font-size: 1.7rem;
  margin-top: 0.15rem;
}
.noti-icon.success { color: #10b981; }
.noti-icon.error { color: #ef4444; }
.noti-icon.warning { color: #f59e0b; }
.noti-icon.info { color: #3b82f6; }
.noti-content {
  flex: 1;
  min-width: 0;
}
.noti-title {
  font-weight: 700;
  color: #1e293b;
  font-size: 1.08rem;
  margin-bottom: 0.1rem;
}
.noti-message {
  color: #334155;
  font-size: 1.01rem;
  line-height: 1.5;
}
.noti-time {
  color: #64748b;
  font-size: 0.92rem;
  margin-top: 0.1rem;
}
.noti-empty {
  text-align: center;
  color: #64748b;
  padding: 2.5rem 0 2.5rem 0;
  font-size: 1.1rem;
}
@media (max-width: 700px) {
  .notification-page {
    max-width: 98vw;
    padding: 0.5rem;
  }
  .noti-row {
    padding: 0.7rem 0.5rem;
  }
}
</style> 