<template>
  <div class="creative-progress-container">
    <div class="progress-header">
      <h2 class="progress-title">{{ $t('ad.creation.title') || "Let's create your ad" }}</h2>
      <div class="progress-subtitle">Step {{ currentStep }} of 3</div>
    </div>

    <div class="creative-progress-tracker">
      <div class="progress-line">
        <div class="progress-fill" :style="{ width: (currentStep / 3 * 100) + '%' }"></div>
      </div>

      <div class="progress-steps">
        <div
          v-for="(step, index) in progressSteps"
          :key="index"
          class="progress-step"
          :class="{
            active: currentStep === index + 1,
            completed: currentStep > index + 1,
            current: currentStep === index + 1
          }"
        >
          <div class="step-circle">
            <div v-if="currentStep > index + 1" class="step-check">✓</div>
            <div v-else class="step-number">{{ index + 1 }}</div>
          </div>
          <div class="step-content">
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.description }}</div>
            <div v-if="currentStep === index + 1" class="step-emoji">{{ step.emoji }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Fun Progress Indicator -->
    <div class="fun-progress">
      <div class="progress-character">
        <span v-if="currentStep === 1">🎯</span>
        <span v-else-if="currentStep === 2">🤖</span>
        <span v-else>🎉</span>
      </div>
      <div class="progress-message">
        <span v-if="currentStep === 1">Tell us about your ad</span>
        <span v-else-if="currentStep === 2">Pick your AI superpowers</span>
        <span v-else>Almost there!</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdCreateProgress',
  props: {
    currentStep: {
      type: Number,
      required: true,
      default: 1
    }
  },
  computed: {
    progressSteps() {
      return [
        {
          title: 'Basic Info',
          description: 'Tell us about your ad',
          emoji: '📝'
        },
        {
          title: 'AI Magic',
          description: 'Choose your AI tools',
          emoji: '🤖'
        },
        {
          title: 'Preview',
          description: 'See your creation',
          emoji: '✨'
        }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.creative-progress-container {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 32px;
  box-shadow: 0 4px 20px rgba(45, 90, 160, 0.08);
  border: 1px solid #f0f2f5;
}

.progress-header {
  text-align: center;
  margin-bottom: 28px;
}

.progress-title {
  font-size: 28px;
  font-weight: 700;
  color: #2d5aa0;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #2d5aa0 0%, #1890ff 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.progress-subtitle {
  font-size: 16px;
  color: #8c8c8c;
  font-weight: 500;
}

.creative-progress-tracker {
  position: relative;
  margin-bottom: 24px;
}

.progress-line {
  position: absolute;
  top: 28px;
  left: 32px;
  right: 32px;
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  z-index: 1;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2d5aa0 0%, #1890ff 100%);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.progress-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 3px solid #f0f2f5;
  margin-bottom: 12px;
  transition: all 0.4s ease;
  position: relative;
  z-index: 3;
}

.progress-step.completed .step-circle {
  background: #2d5aa0;
  border-color: #2d5aa0;
  color: white;
}

.progress-step.current .step-circle {
  background: #1890ff;
  border-color: #1890ff;
  color: white;
  animation: pulse-step 2s ease-in-out infinite;
}

@keyframes pulse-step {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.step-check,
.step-number {
  font-size: 18px;
  font-weight: 700;
}

.step-content {
  text-align: center;
  max-width: 120px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.step-desc {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.3;
  margin-bottom: 8px;
}

.step-emoji {
  font-size: 20px;
  animation: bounce-emoji 1.5s ease-in-out infinite;
}

@keyframes bounce-emoji {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.fun-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff1f0 0%, #fff7e6 100%);
  border-radius: 12px;
  border: 1px solid #ffd6cc;
}

.progress-character {
  font-size: 24px;
  animation: character-float 2s ease-in-out infinite;
}

@keyframes character-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.progress-message {
  font-size: 16px;
  font-weight: 600;
  color: #d46b08;
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-progress-container {
    padding: 20px;
  }

  .progress-title {
    font-size: 24px;
  }

  .progress-steps {
    gap: 8px;
  }

  .step-circle {
    width: 44px;
    height: 44px;
  }

  .step-content {
    max-width: 90px;
  }

  .step-title {
    font-size: 12px;
  }

  .step-desc {
    font-size: 10px;
  }
}

@media (max-width: 480px) {
  .progress-line {
    left: 22px;
    right: 22px;
  }

  .step-circle {
    width: 36px;
    height: 36px;
  }

  .step-check,
  .step-number {
    font-size: 14px;
  }
}
</style>
