<template>
  <div class="next-steps-card">
    <div class="next-steps-header">
      <div class="header-content">
        <h3 class="next-steps-title">
          <span class="title-icon"></span>
          {{ $t('dashboard.whatNext') }}
        </h3>
        <p class="next-steps-subtitle">{{ $t('dashboard.nextSteps.title') }}</p>
      </div>
    </div>

    <div class="next-steps-list">
      <div
        v-for="(step, index) in suggestedSteps"
        :key="index"
        class="next-step-item"
        :class="{ completed: step.completed }"
      >
        <div class="step-indicator">
          <check-circle-outlined v-if="step.completed" class="step-icon completed" />
          <span v-else class="step-number">{{ index + 1 }}</span>
        </div>
        <div class="step-content">
          <p class="step-text">{{ step.text }}</p>
          <router-link v-if="step.action && !step.completed" :to="step.action.link" class="step-action">
            {{ step.action.text }}
            <arrow-right-outlined class="action-arrow" />
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { CheckCircleOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'

export default {
  name: 'DashboardNextSteps',
  components: {
    CheckCircleOutlined,
    ArrowRightOutlined
  },
  props: {
    stats: {
      type: Object,
      required: true
    }
  },
  computed: {
    suggestedSteps() {
      const steps = []

      // Step 1: Create first campaign
      if (this.stats.totalCampaigns === 0) {
        steps.push({
          text: this.$t('dashboard.nextSteps.firstCampaign'),
          completed: false,
          action: {
            text: this.$t('navigation.createCampaign'),
            link: '/campaign/create'
          }
        })
      } else {
        steps.push({
          text: this.$t('dashboard.nextSteps.firstCampaign'),
          completed: true
        })
      }

      // Step 2: Create first ad
      if (this.stats.totalAds === 0) {
        steps.push({
          text: this.$t('dashboard.nextSteps.firstAd'),
          completed: false,
          action: {
            text: this.$t('navigation.createAd'),
            link: '/ad/create'
          }
        })
      } else {
        steps.push({
          text: this.$t('dashboard.nextSteps.firstAd'),
          completed: true
        })
      }

      // Step 3: Expand campaigns (if at least 1 campaign exists)
      if (this.stats.totalCampaigns > 0 && this.stats.totalCampaigns < 3) {
        steps.push({
          text: this.$t('dashboard.nextSteps.moreCampaigns'),
          completed: false,
          action: {
            text: this.$t('navigation.createCampaign'),
            link: '/campaign/create'
          }
        })
      }

      // Step 4: Add more ads (if at least 1 campaign exists)
      if (this.stats.totalCampaigns > 0 && this.stats.totalAds < 5) {
        steps.push({
          text: this.$t('dashboard.nextSteps.moreAds'),
          completed: false,
          action: {
            text: this.$t('navigation.createAd'),
            link: '/ad/create'
          }
        })
      }

      // Step 5: Review analytics (if have campaigns)
      if (this.stats.totalCampaigns > 0) {
        steps.push({
          text: this.$t('dashboard.nextSteps.reviewPerformance'),
          completed: false,
          action: {
            text: this.$t('navigation.analytics'),
            link: '/analytics'
          }
        })
      }

      // Step 6: Optimize campaigns (if have active campaigns)
      if (this.stats.activeCampaigns > 0) {
        steps.push({
          text: this.$t('dashboard.nextSteps.optimizeCampaigns'),
          completed: false,
          action: {
            text: this.$t('navigation.optimization'),
            link: '/optimization'
          }
        })
      }

      // Return maximum 4 steps to avoid overwhelming
      return steps.slice(0, 4)
    }
  }
}
</script>

<style scoped>
.next-steps-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 32px;
}

.next-steps-header {
  margin-bottom: 20px;
}

.next-steps-title {
  font-size: 20px;
  font-weight: 700;
  color: #0c4a6e;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 24px;
}

.next-steps-subtitle {
  font-size: 14px;
  color: #0369a1;
  margin: 0;
}

.next-steps-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.next-step-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #e0f2fe;
  transition: all 0.3s ease;
}

.next-step-item:hover {
  border-color: #38bdf8;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.1);
  transform: translateY(-1px);
}

.next-step-item.completed {
  opacity: 0.6;
  background: #f8fafc;
}

.next-step-item.completed:hover {
  transform: none;
  box-shadow: none;
}

.step-indicator {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
  color: white;
  font-weight: 700;
  font-size: 14px;
}

.next-step-item.completed .step-indicator {
  background: #10b981;
}

.step-icon.completed {
  font-size: 20px;
}

.step-number {
  line-height: 1;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-text {
  font-size: 15px;
  color: #1e293b;
  margin: 0 0 8px 0;
  line-height: 1.5;
}

.next-step-item.completed .step-text {
  color: #64748b;
  text-decoration: line-through;
}

.step-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #0284c7;
  text-decoration: none;
  transition: all 0.2s ease;
}

.step-action:hover {
  color: #0369a1;
  gap: 8px;
}

.action-arrow {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.step-action:hover .action-arrow {
  transform: translateX(2px);
}

/* Mobile responsive */
@media (max-width: 768px) {
  .next-steps-card {
    padding: 20px;
  }

  .next-steps-title {
    font-size: 18px;
  }

  .next-step-item {
    padding: 14px;
    gap: 12px;
  }

  .step-indicator {
    width: 28px;
    height: 28px;
    font-size: 13px;
  }

  .step-text {
    font-size: 14px;
  }
}
</style>
