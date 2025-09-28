<template>
  <div
    v-if="shouldShowOnboarding"
    class="creative-onboarding-overlay"
    @click="handleOverlayClick"
  >
    <div class="onboarding-content" @click.stop>
      <!-- Creative Onboarding Header -->
      <div class="onboarding-header">
        <div class="onboarding-character">{{ getCurrentCharacter() }}</div>
        <div class="onboarding-title">
          <h3>{{ getCurrentTitle() }}</h3>
          <div class="onboarding-progress">
            <span class="progress-text">{{ currentStep + 1 }} of {{ totalSteps }}</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
            </div>
          </div>
        </div>
        <button class="onboarding-close" @click="closeOnboarding">
          <span class="close-text">Skip Tour</span>
          <span class="close-icon">✕</span>
        </button>
      </div>

      <!-- Onboarding Message -->
      <div class="onboarding-message">
        <p v-html="getCurrentMessage()"></p>

        <!-- Interactive Elements for Some Steps -->
        <div v-if="currentStepData.interactive" class="onboarding-interactive">
          <div v-if="currentStepData.type === 'tip'" class="tip-box">
            <div class="tip-icon">💡</div>
            <div class="tip-content">
              <strong>Pro Tip:</strong> {{ currentStepData.tip }}
            </div>
          </div>

          <div v-if="currentStepData.type === 'feature-highlight'" class="feature-highlight">
            <div class="feature-list">
              <div
                v-for="feature in currentStepData.features"
                :key="feature.name"
                class="feature-item"
              >
                <span class="feature-emoji">{{ feature.emoji }}</span>
                <span class="feature-name">{{ feature.name }}</span>
                <span class="feature-description">{{ feature.description }}</span>
              </div>
            </div>
          </div>

          <div v-if="currentStepData.type === 'developer-note'" class="developer-note">
            <div class="note-header">
              <span class="note-emoji">👨‍💻</span>
              <span class="note-title">Developer's Quick Note:</span>
            </div>
            <p class="note-text">{{ currentStepData.note }}</p>
          </div>
        </div>
      </div>

      <!-- Navigation Controls -->
      <div class="onboarding-controls">
        <button
          v-if="currentStep > 0"
          @click="previousStep"
          class="onboarding-btn secondary"
        >
          ← Previous
        </button>

        <div class="step-indicators">
          <div
            v-for="(step, index) in onboardingSteps"
            :key="index"
            class="step-indicator"
            :class="{ 'active': index === currentStep, 'completed': index < currentStep }"
          ></div>
        </div>

        <button
          v-if="currentStep < totalSteps - 1"
          @click="nextStep"
          class="onboarding-btn primary"
        >
          {{ getNextButtonText() }} →
        </button>

        <button
          v-else
          @click="finishOnboarding"
          class="onboarding-btn primary finish"
        >
          {{ getFinishButtonText() }} 🚀
        </button>
      </div>

      <!-- Developer Personality Touch -->
      <div class="onboarding-footer">
        <small class="developer-signature">{{ getDeveloperFooter() }}</small>
      </div>
    </div>

    <!-- Highlighting Target Element (if any) -->
    <div
      v-if="currentStepData.targetElement"
      class="highlight-overlay"
      :style="getHighlightStyle()"
    ></div>
  </div>
</template>

<script>
export default {
  name: 'CreativeOnboarding',
  props: {
    onboardingType: {
      type: String,
      default: 'dashboard',
      validator: value => [
        'dashboard', 'ad-creation', 'campaign-creation', 'analytics'
      ].includes(value)
    },
    autoStart: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      currentStep: 0,
      isVisible: false,
      onboardingSteps: []
    }
  },

  computed: {
    shouldShowOnboarding() {
      return this.isVisible && this.onboardingSteps.length > 0
    },

    totalSteps() {
      return this.onboardingSteps.length
    },

    progressPercentage() {
      return ((this.currentStep + 1) / this.totalSteps) * 100
    },

    currentStepData() {
      return this.onboardingSteps[this.currentStep] || {}
    }
  },

  created() {
    this.initializeOnboarding()
    if (this.autoStart) {
      this.startOnboarding()
    }
  },

  methods: {
    initializeOnboarding() {
      const onboardingData = {
        'dashboard': [
          {
            character: '👋',
            title: 'Welcome to Your Creative Hub!',
            message: `Hey there! Ready to create some amazing ads? I'm going to show you around - think of this as a friendly tour, not a boring tutorial. <br><br>This dashboard is your command center for all things creative advertising.`,
            interactive: true,
            type: 'developer-note',
            note: `I built this to be intuitive, but a quick tour never hurts. Skip anytime if you're feeling adventurous! 😄`
          },
          {
            character: '🎯',
            title: 'Your Stats at a Glance',
            message: `These cards show your campaign performance. Don't worry if they're empty - everyone starts somewhere! <br><br>Fun fact: Even the best advertisers had zero campaigns once.`,
            targetElement: '.stats-grid',
            interactive: true,
            type: 'feature-highlight',
            features: [
              { emoji: '📊', name: 'Campaigns', description: 'Track your active campaigns' },
              { emoji: '📢', name: 'Ads', description: 'Monitor your ad performance' },
              { emoji: '💰', name: 'Spend', description: 'Keep an eye on your budget' },
              { emoji: '👀', name: 'Impressions', description: 'See how many people saw your ads' }
            ]
          },
          {
            character: '🚀',
            title: 'Ready to Create?',
            message: `The "Create New Ad" button is your best friend. Click it whenever inspiration strikes! <br><br>Pro tip: The best ads come from genuine ideas, not perfect templates.`,
            interactive: true,
            type: 'tip',
            tip: `Don't overthink your first ad. Sometimes the simplest ideas perform the best. Just start creating!`
          },
          {
            character: '🎉',
            title: 'You\'re All Set!',
            message: `That's it! You're ready to create some advertising magic. Remember, every expert was once a beginner. <br><br>Have fun, experiment, and don't be afraid to try new things!`,
            interactive: true,
            type: 'developer-note',
            note: `I'm genuinely excited to see what you'll create. If something breaks or seems weird, it's probably my fault, not yours! 😅`
          }
        ],
        'ad-creation': [
          {
            character: '🎨',
            title: 'Time to Get Creative!',
            message: `Welcome to the ad creation studio! This is where ideas become reality. <br><br>Don't worry about making it perfect - focus on making it authentic.`,
            interactive: true,
            type: 'developer-note',
            note: `I spent way too much time making this form user-friendly. Enjoy the smooth experience! 😎`
          },
          {
            character: '🤖',
            title: 'AI is Your Creative Partner',
            message: `Choose your AI provider to help generate content. Think of AI as your creative writing assistant, not a replacement for your ideas. <br><br>Each provider has its own personality - experiment to find your favorite!`,
            interactive: true,
            type: 'feature-highlight',
            features: [
              { emoji: '🧠', name: 'OpenAI', description: 'Creative and versatile' },
              { emoji: '💎', name: 'Claude', description: 'Thoughtful and detailed' },
              { emoji: '🎭', name: 'Gemini', description: 'Innovative and dynamic' }
            ]
          },
          {
            character: '✨',
            title: 'Your Ad, Your Style',
            message: `Fill in the details, but remember - authenticity beats perfection. Your unique voice is what will make your ad stand out. <br><br>The AI will help, but the creative vision is all yours!`,
            interactive: true,
            type: 'tip',
            tip: `Write like you're talking to a friend. The most engaging ads feel like conversations, not advertisements.`
          }
        ]
      }

      this.onboardingSteps = onboardingData[this.onboardingType] || []
    },

    startOnboarding() {
      // Check if user has seen this onboarding before
      const hasSeenOnboarding = localStorage.getItem(`onboarding-${this.onboardingType}-seen`)
      if (!hasSeenOnboarding) {
        this.isVisible = true
        this.currentStep = 0
      }
    },

    getCurrentCharacter() {
      return this.currentStepData.character || '🎯'
    },

    getCurrentTitle() {
      return this.currentStepData.title || 'Welcome!'
    },

    getCurrentMessage() {
      return this.currentStepData.message || 'Let me show you around!'
    },

    getNextButtonText() {
      const nextTexts = ['Got it!', 'Show me more', 'Continue', 'Next', 'Cool!']
      return nextTexts[this.currentStep % nextTexts.length]
    },

    getFinishButtonText() {
      const finishTexts = ['Let\'s do this!', 'I\'m ready!', 'Start creating!', 'Awesome!']
      return finishTexts[Math.floor(Math.random() * finishTexts.length)]
    },

    getDeveloperFooter() {
      const footers = [
        'Onboarding crafted by a real human (me!) 👨‍💻',
        'No AI was used to write this tour 🙋‍♂️',
        'Made with care (and lots of coffee) ☕',
        'Built by someone who actually tests user flows 🧪',
        'Tour designed by a developer who hates boring tutorials 😴'
      ]
      return footers[this.currentStep % footers.length]
    },

    nextStep() {
      if (this.currentStep < this.totalSteps - 1) {
        this.currentStep++
      }
    },

    previousStep() {
      if (this.currentStep > 0) {
        this.currentStep--
      }
    },

    finishOnboarding() {
      this.completeOnboarding()
    },

    closeOnboarding() {
      this.completeOnboarding()
    },

    completeOnboarding() {
      // Mark as seen
      localStorage.setItem(`onboarding-${this.onboardingType}-seen`, 'true')

      this.isVisible = false
      this.$emit('onboarding-complete')
    },

    handleOverlayClick() {
      // Allow closing by clicking overlay
      this.closeOnboarding()
    },

    getHighlightStyle() {
      // This would calculate position based on target element
      // For now, return empty object - can be enhanced later
      return {}
    },

    // Public methods for parent components
    show() {
      this.isVisible = true
      this.currentStep = 0
    },

    hide() {
      this.isVisible = false
    },

    reset() {
      localStorage.removeItem(`onboarding-${this.onboardingType}-seen`)
      this.currentStep = 0
    }
  }
}
</script>

<style scoped>
.creative-onboarding-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  animation: overlay-appear 0.3s ease-out;
}

.onboarding-content {
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  max-width: 500px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  animation: content-appear 0.4s ease-out;
}

/* Header */
.onboarding-header {
  display: flex;
  align-items: flex-start;
  padding: var(--space-6);
  border-bottom: 1px solid var(--primary-100);
  background: linear-gradient(135deg, #f8fafc 0%, #f0f4f7 100%);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.onboarding-character {
  font-size: 2.5rem;
  margin-right: var(--space-4);
  animation: character-bounce 2s ease-in-out infinite;
}

.onboarding-title {
  flex: 1;
}

.onboarding-title h3 {
  margin: 0 0 var(--space-2) 0;
  color: var(--color-text);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
}

.onboarding-progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.progress-text {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-medium);
  min-width: 60px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--primary-100);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--brand-primary) 0%, var(--accent-orange) 100%);
  border-radius: var(--radius-full);
  transition: width 0.3s ease;
}

.onboarding-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-2);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  transition: all 0.2s ease;
}

.onboarding-close:hover {
  background: var(--primary-100);
  color: var(--brand-primary);
}

/* Message */
.onboarding-message {
  padding: var(--space-6);
}

.onboarding-message p {
  color: var(--color-text);
  line-height: var(--leading-relaxed);
  margin: 0 0 var(--space-4) 0;
}

/* Interactive Elements */
.onboarding-interactive {
  margin-top: var(--space-4);
}

.tip-box {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  background: var(--warning-50);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border-left: 4px solid var(--warning-400);
}

.tip-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.tip-content {
  color: var(--warning-800);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
}

.feature-highlight {
  background: var(--primary-50);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--primary-200);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.feature-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: var(--space-3);
  align-items: center;
  background: white;
  padding: var(--space-3);
  border-radius: var(--radius-md);
}

.feature-emoji {
  font-size: 1.2rem;
}

.feature-name {
  font-weight: var(--font-semibold);
  color: var(--color-text);
}

.feature-description {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  text-align: right;
}

.developer-note {
  background: var(--success-50);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--success-200);
}

.note-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.note-emoji {
  font-size: 1.2rem;
}

.note-title {
  font-weight: var(--font-semibold);
  color: var(--success-800);
}

.note-text {
  color: var(--success-700);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  margin: 0;
  font-style: italic;
}

/* Controls */
.onboarding-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  border-top: 1px solid var(--primary-100);
  background: #fafafa;
}

.step-indicators {
  display: flex;
  gap: var(--space-2);
}

.step-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-200);
  transition: all 0.3s ease;
}

.step-indicator.active {
  background: var(--brand-primary);
  transform: scale(1.2);
}

.step-indicator.completed {
  background: var(--success-500);
}

.onboarding-btn {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  font-size: var(--text-sm);
  min-width: 100px;
}

.onboarding-btn.primary {
  background: var(--brand-primary);
  color: white;
  box-shadow: 0 2px 8px rgba(45, 90, 160, 0.3);
}

.onboarding-btn.primary:hover {
  background: #274d89;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(45, 90, 160, 0.4);
}

.onboarding-btn.primary.finish {
  background: linear-gradient(45deg, var(--brand-primary), var(--accent-orange));
  animation: finish-glow 2s ease-in-out infinite;
}

.onboarding-btn.secondary {
  background: transparent;
  border: 1px solid var(--primary-300);
  color: var(--brand-primary);
}

.onboarding-btn.secondary:hover {
  background: var(--primary-50);
  border-color: var(--brand-primary);
}

/* Footer */
.onboarding-footer {
  padding: var(--space-4) var(--space-6);
  text-align: center;
  background: var(--primary-25);
  border-radius: 0 0 var(--radius-xl) var(--radius-xl);
}

.developer-signature {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Animations */
@keyframes overlay-appear {
  from {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(4px);
  }
}

@keyframes content-appear {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes character-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

@keyframes finish-glow {
  0%, 100% { box-shadow: 0 2px 8px rgba(45, 90, 160, 0.3); }
  50% { box-shadow: 0 4px 20px rgba(45, 90, 160, 0.6); }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-onboarding-overlay {
    padding: var(--space-2);
  }

  .onboarding-content {
    max-width: 100%;
  }

  .onboarding-header {
    padding: var(--space-4);
  }

  .onboarding-character {
    font-size: 2rem;
    margin-right: var(--space-3);
  }

  .onboarding-message {
    padding: var(--space-4);
  }

  .onboarding-controls {
    padding: var(--space-4);
    flex-direction: column;
    gap: var(--space-3);
  }

  .step-indicators {
    order: -1;
  }

  .feature-item {
    grid-template-columns: auto 1fr;
    grid-template-rows: auto auto;
    gap: var(--space-2);
  }

  .feature-description {
    grid-column: 1 / -1;
    text-align: left;
    font-size: var(--text-xs);
  }
}
</style>