<template>
  <div class="creative-loading-screen" :class="{ 'full-screen': fullScreen }">
    <div class="loading-content">
      <!-- Animated Character -->
      <div class="loading-character">
        <div class="character-emoji" :class="{ 'bouncing': animationType === 'bounce', 'spinning': animationType === 'spin', 'floating': animationType === 'float' }">
          {{ currentEmoji }}
        </div>
        <div class="character-shadow"></div>
      </div>

      <!-- Context-Aware Loading Messages -->
      <div class="loading-messages">
        <h3 class="loading-title">{{ getContextTitle() }}</h3>
        <p class="loading-message">{{ currentMessage }}</p>
        <p class="loading-submessage">{{ currentSubmessage }}</p>
      </div>

      <!-- Creative Progress Indicator -->
      <div class="loading-progress">
        <div class="progress-container">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressWidth }"></div>
            <div class="progress-glow"></div>
          </div>
          <div class="progress-dots">
            <div v-for="n in 3" :key="n" class="progress-dot" :class="{ active: n <= Math.floor(fakeProgress / 33) + 1 }"></div>
          </div>
        </div>
        <div class="progress-text">{{ Math.floor(fakeProgress) }}% {{ getProgressMessage() }}</div>
      </div>

      <!-- Developer Personality Touch -->
      <div class="developer-touch">
        <div class="tech-speak">{{ getTechSpeak() }}</div>
        <div class="fun-fact">{{ getFunFact() }}</div>
      </div>

      <!-- Easter Egg Trigger -->
      <div class="easter-egg-trigger" @click="handleEasterEgg">
        <span class="trigger-text">{{ easterEggTrigger }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreativeLoadingScreen',
  props: {
    context: {
      type: String,
      default: 'general',
      validator: value => [
        'general', 'ai-generation', 'campaign-creation', 'ad-creation',
        'data-loading', 'export', 'optimization', 'upload'
      ].includes(value)
    },
    fullScreen: {
      type: Boolean,
      default: false
    },
    duration: {
      type: Number,
      default: 3000
    }
  },

  data() {
    return {
      currentEmoji: '🚀',
      currentMessage: '',
      currentSubmessage: '',
      fakeProgress: 0,
      animationType: 'bounce',
      easterEggTrigger: 'Click me if you\'re bored',
      easterEggClicks: 0,
      messageInterval: null,
      progressInterval: null,
      emojiInterval: null
    }
  },

  computed: {
    progressWidth() {
      return `${Math.min(this.fakeProgress, 100)}%`
    }
  },

  mounted() {
    this.startLoadingAnimation()
  },

  beforeUnmount() {
    this.clearIntervals()
  },

  methods: {
    startLoadingAnimation() {
      // Update messages every 2 seconds
      this.updateMessage()
      this.messageInterval = setInterval(() => {
        this.updateMessage()
      }, 2000)

      // Update progress
      this.progressInterval = setInterval(() => {
        if (this.fakeProgress < 95) {
          this.fakeProgress += Math.random() * 15 + 5
        }
      }, 300)

      // Change emoji every 3 seconds
      this.emojiInterval = setInterval(() => {
        this.updateEmoji()
      }, 3000)
    },

    clearIntervals() {
      if (this.messageInterval) clearInterval(this.messageInterval)
      if (this.progressInterval) clearInterval(this.progressInterval)
      if (this.emojiInterval) clearInterval(this.emojiInterval)
    },

    updateMessage() {
      this.currentMessage = this.getRandomMessage()
      this.currentSubmessage = this.getRandomSubmessage()
    },

    updateEmoji() {
      const emojis = this.getContextEmojis()
      this.currentEmoji = emojis[Math.floor(Math.random() * emojis.length)]

      // Change animation type occasionally
      const animations = ['bounce', 'spin', 'float']
      this.animationType = animations[Math.floor(Math.random() * animations.length)]
    },

    getContextTitle() {
      const titles = {
        'general': 'Working on it...',
        'ai-generation': 'AI is thinking hard...',
        'campaign-creation': 'Building your campaign...',
        'ad-creation': 'Crafting your ad...',
        'data-loading': 'Fetching your data...',
        'export': 'Preparing your export...',
        'optimization': 'Optimizing everything...',
        'upload': 'Uploading your files...'
      }
      return titles[this.context] || titles.general
    },

    getContextEmojis() {
      const emojiSets = {
        'general': ['⚡', '🚀', '✨', '💫', '🎯'],
        'ai-generation': ['🤖', '🧠', '💭', '⚙️', '🔮'],
        'campaign-creation': ['📊', '🎯', '📈', '💼', '🏗️'],
        'ad-creation': ['🎨', '🖼️', '✍️', '🎭', '📝'],
        'data-loading': ['📊', '📈', '📉', '💾', '🔍'],
        'export': ['📤', '📋', '📁', '💼', '🎁'],
        'optimization': ['⚡', '🔧', '⚙️', '📈', '💎'],
        'upload': ['⬆️', '☁️', '📤', '🌐', '💾']
      }
      return emojiSets[this.context] || emojiSets.general
    },

    getRandomMessage() {
      const messagesByContext = {
        'general': [
          'Making things happen behind the scenes...',
          'The magic is happening right now...',
          'Pixels are being arranged with care...',
          'Your request is our top priority...',
          'Building something awesome for you...'
        ],
        'ai-generation': [
          'Teaching AI to be more creative than humans...',
          'The neural networks are having a brainstorm...',
          'AI is consulting its digital imagination...',
          'Machine learning is earning its degree...',
          'Algorithms are writing poetry... almost...'
        ],
        'campaign-creation': [
          'Assembling your marketing masterpiece...',
          'Campaign ingredients: goals + creativity + data...',
          'Building a campaign that even Don Draper would approve...',
          'Your target audience is going to love this...',
          'Creating advertising magic, one click at a time...'
        ],
        'ad-creation': [
          'Crafting an ad that stops the scroll...',
          'Your ad is getting a personality makeover...',
          'Designing something that converts browsers to buyers...',
          'Adding that special sauce to your creative...',
          'This ad is going to be absolutely irresistible...'
        ]
      }

      const messages = messagesByContext[this.context] || messagesByContext.general
      return messages[Math.floor(Math.random() * messages.length)]
    },

    getRandomSubmessage() {
      const submessages = [
        'Fun fact: Loading screens used to be boring',
        'Pro tip: This is faster than making coffee',
        'Did you know? Great things take exactly this long',
        'Loading with 200% more personality than competitors',
        'Your patience is appreciated and rewarded',
        'Built by someone who hates waiting too',
        'Quality loading, crafted by a real human',
        'Servers are working overtime for you',
        'This beats staring at a blank screen, right?',
        'Almost there... definitely worth the wait!'
      ]
      return submessages[Math.floor(Math.random() * submessages.length)]
    },

    getProgressMessage() {
      if (this.fakeProgress < 30) return 'Getting started...'
      if (this.fakeProgress < 60) return 'Making progress...'
      if (this.fakeProgress < 85) return 'Almost there...'
      return 'Finishing touches...'
    },

    getTechSpeak() {
      const techTerms = [
        'Optimizing algorithms for maximum awesomeness...',
        'Compiling creativity at 3.2 GHz...',
        'Initializing user satisfaction protocols...',
        'Loading modules: happiness.js, success.css...',
        'Running quality assurance on your experience...',
        'Calibrating the awesome-ometer...',
        'Deploying pixels to production...',
        'Establishing secure connection to awesome...'
      ]
      return techTerms[Math.floor(Math.random() * techTerms.length)]
    },

    getFunFact() {
      const facts = [
        'Fun fact: This app was coded with 73% coffee',
        'Developer note: No AIs were harmed in this loading process',
        'Behind the scenes: Real humans built this with care',
        'Trivia: This loading screen has more personality than most apps',
        'Secret: The developer actually tests every loading state',
        'Fact: This loading animation was debugged at 2 AM',
        'Truth: Good software is worth the wait',
        'Reality: This app was built by someone who reads documentation'
      ]
      return facts[Math.floor(Math.random() * facts.length)]
    },

    handleEasterEgg() {
      this.easterEggClicks++

      const easterMessages = [
        'You clicked it! 🎉',
        'Curious, aren\'t you? 😄',
        'Achievement: Professional Button Clicker',
        'You found the secret loading button!',
        'Developers love users like you ❤️',
        'This doesn\'t make it load faster... but it\'s fun!',
        'You have excellent click instincts',
        'Secret unlocked: You have patience AND curiosity!'
      ]

      this.easterEggTrigger = easterMessages[Math.min(this.easterEggClicks - 1, easterMessages.length - 1)]

      // Add a fun animation
      this.animationType = 'spin'
      setTimeout(() => {
        this.animationType = 'bounce'
      }, 1000)
    }
  }
}
</script>

<style scoped>
.creative-loading-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: var(--space-8);
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;
}

.creative-loading-screen.full-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 100vh;
  background: #ffffff;
  z-index: 9999;
  border-radius: 0;
}

.loading-content {
  text-align: center;
  max-width: 500px;
  width: 100%;
}

/* Animated Character */
.loading-character {
  position: relative;
  margin-bottom: var(--space-8);
}

.character-emoji {
  font-size: 4rem;
  line-height: 1;
  display: inline-block;
  filter: drop-shadow(0 4px 8px rgba(45, 90, 160, 0.2));
}

.character-emoji.bouncing {
  animation: character-bounce 2s ease-in-out infinite;
}

.character-emoji.spinning {
  animation: character-spin 3s linear infinite;
}

.character-emoji.floating {
  animation: character-float 3s ease-in-out infinite;
}

.character-shadow {
  width: 60px;
  height: 20px;
  background: rgba(0, 0, 0, 0.12);
  border-radius: 50%;
  margin: var(--space-2) auto 0;
  animation: shadow-pulse 2s ease-in-out infinite;
}

/* Loading Messages */
.loading-messages {
  margin-bottom: var(--space-8);
}

.loading-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: #111827;
  margin: 0 0 var(--space-3) 0;
  text-shadow: none;
  animation: none;
}

.loading-message {
  font-size: var(--text-lg);
  color: #1f2937;
  margin: 0 0 var(--space-2) 0;
  font-weight: var(--font-medium);
}

.loading-submessage {
  font-size: var(--text-base);
  color: #4b5563;
  margin: 0;
  font-style: italic;
}

/* Creative Progress */
.loading-progress {
  margin-bottom: var(--space-6);
}

.progress-container {
  position: relative;
  margin-bottom: var(--space-4);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--primary-100);
  border-radius: var(--radius-full);
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(45, 90, 160, 0.1);
}

.progress-fill {
  height: 100%;
  background: #111827;
  border-radius: var(--radius-full);
  transition: width 0.5s ease;
  position: relative;
}

.progress-glow {
  position: absolute;
  top: -2px;
  right: -10px;
  width: 20px;
  height: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 50%;
  animation: progress-glow 2s ease-in-out infinite;
}

.progress-dots {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.progress-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--primary-200);
  transition: all 0.3s ease;
}

.progress-dot.active {
  background: #111827;
  transform: scale(1.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.progress-text {
  font-size: var(--text-sm);
  color: #111827;
  font-weight: var(--font-semibold);
}

/* Developer Personality */
.developer-touch {
  margin-bottom: var(--space-6);
}

.tech-speak {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  margin-bottom: var(--space-2);
  opacity: 0.8;
}

.fun-fact {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-style: italic;
}

/* Easter Egg */
.easter-egg-trigger {
  position: absolute;
  bottom: var(--space-4);
  right: var(--space-4);
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.3s ease;
}

.easter-egg-trigger:hover {
  opacity: 0.8;
}

.trigger-text {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-style: italic;
}

/* Animations */
@keyframes character-bounce {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-15px); }
}

@keyframes character-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes character-float {
  0%, 100% { transform: translateY(0px) rotate(-2deg); }
  33% { transform: translateY(-8px) rotate(1deg); }
  66% { transform: translateY(4px) rotate(-1deg); }
}

@keyframes shadow-pulse {
  0%, 100% { transform: scale(1); opacity: 0.2; }
  50% { transform: scale(1.1); opacity: 0.3; }
}

@keyframes title-glow {
  0%, 100% { text-shadow: 0 0 5px rgba(45, 90, 160, 0.3); }
  50% { text-shadow: 0 0 20px rgba(45, 90, 160, 0.5); }
}

@keyframes progress-glow {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-loading-screen {
    padding: var(--space-6);
    min-height: 300px;
  }

  .character-emoji {
    font-size: 3rem;
  }

  .loading-title {
    font-size: var(--text-xl);
  }

  .loading-message {
    font-size: var(--text-base);
  }

  .tech-speak {
    font-size: var(--text-xs);
  }

  .easter-egg-trigger {
    position: static;
    margin-top: var(--space-4);
    text-align: center;
  }
}
</style>
