<template>
  <div :class="containerClass">
    <!-- Spinner Loading -->
    <div v-if="type === 'spinner'" class="loading-container-standard">
      <div class="loading-spinner-standard"></div>
      <div class="loading-text-standard">{{ message }}</div>
    </div>

    <!-- Skeleton Loading -->
    <div v-else-if="type === 'skeleton'" class="skeleton-container">
      <div v-if="variant === 'card'" class="skeleton-card">
        <div class="skeleton-header">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-title">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line shorter"></div>
          </div>
        </div>
        <div class="skeleton-content">
          <div class="skeleton-line"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line medium"></div>
        </div>
      </div>

      <div v-else-if="variant === 'table'" class="skeleton-table">
        <div v-for="row in rows" :key="row" class="skeleton-row">
          <div v-for="col in columns" :key="col" class="skeleton-cell">
            <div class="skeleton-line"></div>
          </div>
        </div>
      </div>

      <div v-else-if="variant === 'list'" class="skeleton-list">
        <div v-for="item in rows" :key="item" class="skeleton-item">
          <div class="skeleton-avatar small"></div>
          <div class="skeleton-content">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line shorter"></div>
          </div>
        </div>
      </div>

      <div v-else class="skeleton-basic">
        <div v-for="line in rows" :key="line" class="skeleton-line" :class="getRandomWidth()"></div>
      </div>
    </div>

    <!-- Pulse Loading -->
    <div v-else-if="type === 'pulse'" class="pulse-container">
      <div class="pulse-dots">
        <div class="pulse-dot"></div>
        <div class="pulse-dot"></div>
        <div class="pulse-dot"></div>
      </div>
      <div v-if="message" class="loading-text-standard">{{ message }}</div>
    </div>

    <!-- Progress Loading -->
    <div v-else-if="type === 'progress'" class="progress-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>
      <div class="progress-text">{{ message }} {{ progress }}%</div>
    </div>

    <!-- Inline Loading -->
    <div v-else-if="type === 'inline'" class="inline-loading">
      <div class="inline-spinner"></div>
      <span v-if="message">{{ message }}</span>
    </div>

    <!-- Creative Student Loading Messages -->
    <div v-else-if="type === 'creative'" class="creative-loading">
      <div class="creative-character">{{ randomEmoji }}</div>
      <div class="creative-message">{{ creativeMessage }}</div>
      <div class="creative-submessage">{{ creativeSubmessage }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoadingState',
  props: {
    type: {
      type: String,
      default: 'spinner',
      validator: value => ['spinner', 'skeleton', 'pulse', 'progress', 'inline', 'creative'].includes(value)
    },
    variant: {
      type: String,
      default: 'basic',
      validator: value => ['basic', 'card', 'table', 'list'].includes(value)
    },
    message: {
      type: String,
      default: 'Loading...'
    },
    size: {
      type: String,
      default: 'medium',
      validator: value => ['small', 'medium', 'large'].includes(value)
    },
    rows: {
      type: Number,
      default: 3
    },
    columns: {
      type: Number,
      default: 4
    },
    progress: {
      type: Number,
      default: 0,
      validator: value => value >= 0 && value <= 100
    },
    fullPage: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    containerClass() {
      return {
        'loading-full-page': this.fullPage,
        [`loading-${this.size}`]: true
      }
    },

    randomEmoji() {
      const emojis = [
        '🚀', '✨', '🎯', '💎', '🔥', '⚡', '🤖', '🎨',
        '☕', '💻', '🎪', '🎭', '🧠', '⚙️', '🔮', '🎲',
        '🏗️', '🛠️', '⏰', '🌟', '💫', '🎊', '🎈', '🎁',
        '🔧', '⚗️', '🧪', '📡', '🛰️', '🎵', '🎶', '🕹️'
      ]
      return emojis[Math.floor(Math.random() * emojis.length)]
    },

    creativeMessage() {
      const messages = [
        'Crunching some numbers...',
        'Teaching AI to write ads...',
        'Consulting with the code gods...',
        'Brewing digital magic...',
        'Calibrating the awesome machine...',
        'Loading pixels with personality...',
        'Asking the servers nicely...',
        'Turning coffee into code...',
        'Waking up the algorithms...',
        'Polishing the ones and zeros...',
        'Debugging the matrix...',
        'Summoning data from the cloud...',
        'Optimizing your experience...',
        'Making the internet work harder...',
        'Convincing databases to cooperate...',
        'Translating wishes into reality...',
        'Running some background magic...',
        'Downloading more RAM... (kidding!)',
        'Compiling creativity...',
        'Orchestrating digital symphonies...'
      ]
      return messages[Math.floor(Math.random() * messages.length)]
    },

    creativeSubmessage() {
      const submessages = [
        'Pro tip: This is the perfect time for a coffee break ☕',
        'Fun fact: You\'re about to see something awesome!',
        'Patience is a virtue... and you\'ve got great timing!',
        'The best things come to those who wait (and reload)',
        'Loading speeds are directly proportional to excitement levels',
        'Your request is VIP in our queue right now',
        'Even the fastest computers need a moment to think',
        'Quality takes time, unlike instant noodles',
        'We promise this is faster than dial-up internet',
        'Loading... because good things don\'t happen instantly',
        'Taking our time to make it perfect',
        'Servers are doing their happy dance',
        'Almost there... probably... definitely maybe!',
        'Loading with 147% more personality than average',
        'This loading screen was handcrafted by a real human',
        'Built by someone who actually reads Stack Overflow',
        'Powered by determination and energy drinks',
        'Made with love, bugs, and several late-night commits',
        'Loading... like your patience, this won\'t last forever',
        'Fun loading fact: You\'re more patient than you think!'
      ]
      return submessages[Math.floor(Math.random() * submessages.length)]
    }
  },
  methods: {
    getRandomWidth() {
      const widths = ['', 'short', 'medium', 'shorter']
      return widths[Math.floor(Math.random() * widths.length)]
    }
  }
}
</script>

<style scoped>
/* Container classes */
.loading-full-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-small {
  padding: 12px;
}

.loading-medium {
  padding: 24px;
}

.loading-large {
  padding: 48px;
}

/* Skeleton Loading Styles */
.skeleton-container {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.skeleton-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f0f2f5;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 48px;
  height: 48px;
  background: #f0f2f5;
  border-radius: 50%;
}

.skeleton-avatar.small {
  width: 32px;
  height: 32px;
}

.skeleton-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skeleton-line {
  height: 12px;
  background: #f0f2f5;
  border-radius: 6px;
  margin-bottom: 8px;
}

.skeleton-line.short {
  width: 60%;
}

.skeleton-line.shorter {
  width: 40%;
}

.skeleton-line.medium {
  width: 80%;
}

.skeleton-table {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f2f5;
}

.skeleton-row {
  display: flex;
  padding: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.skeleton-row:last-child {
  border-bottom: none;
}

.skeleton-cell {
  flex: 1;
  margin-right: 16px;
}

.skeleton-cell:last-child {
  margin-right: 0;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #f0f2f5;
}

/* Pulse Loading Styles */
.pulse-container {
  text-align: center;
}

.pulse-dots {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.pulse-dot {
  width: 12px;
  height: 12px;
  background: #2d5aa0;
  border-radius: 50%;
  animation: pulse-dot 1.4s ease-in-out infinite both;
}

.pulse-dot:nth-child(1) { animation-delay: -0.32s; }
.pulse-dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes pulse-dot {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Progress Loading Styles */
.progress-container {
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2d5aa0 0%, #1890ff 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #8c8c8c;
  font-weight: 500;
}

/* Inline Loading Styles */
.inline-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #8c8c8c;
  font-size: 14px;
}

.inline-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f0f2f5;
  border-left: 2px solid #2d5aa0;
  border-radius: 50%;
  animation: spin-standard 1s linear infinite;
}

/* Creative Loading Styles */
.creative-loading {
  text-align: center;
  padding: 32px;
}

.creative-character {
  font-size: 48px;
  margin-bottom: 16px;
  animation: character-bounce 2s ease-in-out infinite;
}

@keyframes character-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.creative-message {
  font-size: 18px;
  font-weight: 600;
  color: #2d5aa0;
  margin-bottom: 8px;
}

.creative-submessage {
  font-size: 14px;
  color: #8c8c8c;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .loading-container-standard {
    padding: 20px;
  }

  .creative-character {
    font-size: 36px;
  }

  .creative-message {
    font-size: 16px;
  }

  .skeleton-card {
    padding: 16px;
  }

  .skeleton-row {
    padding: 12px;
  }
}
</style>