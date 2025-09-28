<template>
  <div class="creative-empty-state" :class="`empty-state--${variant}`">
    <!-- Creative ASCII Art or Emoji Illustrations -->
    <div class="empty-illustration">
      <div v-if="variant === 'no-campaigns'" class="ascii-art">
        <div class="emoji-stack">📋</div>
        <div class="ascii-text">¯\_(ツ)_/¯</div>
      </div>
      <div v-else-if="variant === 'no-ads'" class="ascii-art">
        <div class="emoji-stack">📢</div>
        <div class="creative-dots">• • •</div>
      </div>
      <div v-else-if="variant === 'no-activity'" class="ascii-art">
        <div class="emoji-stack">🦗</div>
        <div class="creative-animation">*cricket sounds*</div>
      </div>
      <div v-else-if="variant === 'no-results'" class="ascii-art">
        <div class="emoji-stack">🔍</div>
        <div class="ascii-text">¯\_(👀)_/¯</div>
      </div>
      <div v-else-if="variant === 'no-data'" class="ascii-art">
        <div class="emoji-stack">📊</div>
        <div class="creative-pattern">∿ ∿ ∿</div>
      </div>
      <div v-else-if="variant === 'loading-failed'" class="ascii-art">
        <div class="emoji-stack">🤖</div>
        <div class="ascii-text">( ͡° ͜ʖ ͡°)</div>
      </div>
      <div v-else-if="variant === 'empty-dashboard'" class="ascii-art">
        <div class="emoji-stack">✨</div>
        <div class="creative-message">fresh start!</div>
      </div>
      <div v-else class="ascii-art">
        <div class="emoji-stack">{{ getRandomEmoji() }}</div>
        <div class="ascii-text">{{ getRandomFace() }}</div>
      </div>
    </div>

    <!-- Creative Title with Student Developer Personality -->
    <h3 class="empty-title">{{ getTitle() }}</h3>

    <!-- Helpful but fun message -->
    <p class="empty-message">{{ getMessage() }}</p>

    <!-- Fun Call-to-Action (if provided) -->
    <div v-if="actionText && actionHandler" class="empty-action">
      <button
        class="empty-action-btn"
        @click="actionHandler"
        :class="{ 'btn-rainbow': isSpecialAction }"
      >
        {{ actionText }} {{ getActionEmoji() }}
      </button>
    </div>

    <!-- Student Developer Easter Egg -->
    <div class="developer-signature" @click="showEasterEgg">
      <span class="signature-text">{{ getSignature() }}</span>
      <div v-if="easterEggVisible" class="easter-egg">
        {{ getEasterEggMessage() }} 🎮
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreativeEmptyState',
  props: {
    variant: {
      type: String,
      default: 'default',
      validator: (value) => [
        'default', 'no-campaigns', 'no-ads', 'no-activity',
        'no-results', 'no-data', 'loading-failed', 'empty-dashboard'
      ].includes(value)
    },
    customTitle: String,
    customMessage: String,
    actionText: String,
    actionHandler: Function,
    showSignature: {
      type: Boolean,
      default: true
    }
  },

  data() {
    return {
      easterEggVisible: false,
      clickCount: 0
    }
  },

  computed: {
    isSpecialAction() {
      return ['Create Your First Campaign', 'Start Creating', 'Begin Journey'].includes(this.actionText)
    }
  },

  methods: {
    getTitle() {
      if (this.customTitle) return this.customTitle

      const titles = {
        'no-campaigns': this.getRandomTitle([
          "No campaigns yet!",
          "Campaign list looking lonely",
          "Time to create some magic ✨",
          "Empty canvas awaits",
          "Ready to launch something epic?"
        ]),
        'no-ads': this.getRandomTitle([
          "No ads to show",
          "Ad gallery is empty",
          "Your creative space awaits",
          "Time to make some noise 📢",
          "Ready to create something awesome?"
        ]),
        'no-activity': this.getRandomTitle([
          "All quiet here...",
          "Nothing happening yet",
          "Peaceful emptiness",
          "Waiting for action...",
          "The calm before the storm"
        ]),
        'no-results': this.getRandomTitle([
          "No results found",
          "Search came up empty",
          "Nothing matches that search",
          "Try different keywords?",
          "The void stares back"
        ]),
        'no-data': this.getRandomTitle([
          "No data available",
          "Charts feeling lonely",
          "Numbers are hiding",
          "Data is taking a break",
          "Analytics on vacation"
        ]),
        'loading-failed': this.getRandomTitle([
          "Oops, something went wrong",
          "The hamsters stopped running",
          "404: Motivation not found",
          "Server needs more coffee ☕",
          "Technology had a moment"
        ]),
        'empty-dashboard': this.getRandomTitle([
          "Welcome to your dashboard!",
          "Your creative journey starts here",
          "Fresh start, infinite possibilities",
          "Time to build something amazing",
          "Your blank canvas awaits"
        ])
      }

      return titles[this.variant] || "Nothing to see here"
    },

    getMessage() {
      if (this.customMessage) return this.customMessage

      const messages = {
        'no-campaigns': this.getRandomMessage([
          "Every great advertiser started with zero campaigns. You've got this! 🚀",
          "Your first campaign is just one click away from changing everything.",
          "Fun fact: Empty spaces have infinite potential. Let's fill this one!",
          "Time to turn those brilliant ideas into actual Facebook ads.",
          "Pro tip: The best campaigns start with taking the first step."
        ]),
        'no-ads': this.getRandomMessage([
          "Your next viral ad could be the first one you create. No pressure! 😉",
          "Remember: every creative genius had an empty portfolio once.",
          "This space is reserved for your upcoming masterpieces.",
          "Fun challenge: Can you create an ad that makes people stop scrolling?",
          "Your audience is waiting for content only you can create."
        ]),
        'no-activity': this.getRandomMessage([
          "Sometimes the best activity is preparing for the next big thing.",
          "Enjoy the peace - things are about to get exciting!",
          "This quietness is perfect for planning your next move.",
          "Every productivity guru knows: rest comes before the sprint.",
          "The dashboard is charged up and ready for action."
        ]),
        'no-results': this.getRandomMessage([
          "Even Sherlock Holmes had days when the clues didn't add up.",
          "Maybe what you're looking for hasn't been created yet? 🤔",
          "Try a different search term, or create what you're looking for!",
          "Sometimes the best discoveries happen by accident. Keep exploring!",
          "Plot twist: The perfect result might be something you haven't thought of yet."
        ]),
        'no-data': this.getRandomMessage([
          "Data is like a shy cat - sometimes you have to wait for it to appear.",
          "Your future analytics will thank you for starting somewhere.",
          "Every data scientist's favorite chart: the one that's about to be created.",
          "Numbers are just stories waiting to be told. Start writing yours!",
          "Fun fact: Zero can be the most important number in your journey."
        ]),
        'loading-failed': this.getRandomMessage([
          "Even the best developers have 'it was working 5 minutes ago' moments.",
          "Technology is like a moody teenager - give it a minute to cooperate.",
          "Error 418: I'm a teapot... wait, that's not right. Let's try again!",
          "The server might be taking a coffee break. Refresh and see what happens!",
          "Plot twist: This might actually be a feature, not a bug. (It's definitely a bug.)"
        ]),
        'empty-dashboard': this.getRandomMessage([
          "Your dashboard is like a blank notebook - full of endless possibilities!",
          "Every successful campaign starts with someone brave enough to click 'Create'.",
          "Fun fact: You're about to become the main character of your own success story.",
          "This clean slate is perfect for making your first (or next) breakthrough.",
          "Ready to turn this empty space into your personal command center?"
        ])
      }

      return messages[this.variant] || "Nothing here yet, but that's about to change!"
    },

    getRandomTitle(titles) {
      return titles[Math.floor(Math.random() * titles.length)]
    },

    getRandomMessage(messages) {
      return messages[Math.floor(Math.random() * messages.length)]
    },

    getRandomEmoji() {
      const emojis = ['🎨', '✨', '🚀', '💡', '🎯', '🌟', '🎪', '🎭', '🎬', '📱', '💫', '🎉']
      return emojis[Math.floor(Math.random() * emojis.length)]
    },

    getRandomFace() {
      const faces = [
        '¯\\_(ツ)_/¯',
        '(◕‿◕)',
        'ʘ‿ʘ',
        '(｡◕‿◕｡)',
        '◉_◉',
        '(⌐■_■)',
        'ಠ_ಠ',
        '(╯°□°)╯',
        '¯\\(°_o)/¯',
        '(☆▽☆)'
      ]
      return faces[Math.floor(Math.random() * faces.length)]
    },

    getActionEmoji() {
      const emojis = ['🚀', '✨', '🎯', '💫', '⚡', '🎪', '🎨', '💡']
      return emojis[Math.floor(Math.random() * emojis.length)]
    },

    getSignature() {
      const signatures = [
        'built with ❤️ and too much coffee',
        'crafted by a student who googles everything',
        'made with passion and Stack Overflow',
        '100% authentic code, 0% AI templates',
        'handcrafted pixels & dreams',
        'powered by determination and energy drinks',
        'built by someone who actually reads documentation',
        'made with love, bugs, and late-night commits'
      ]
      return signatures[Math.floor(Math.random() * signatures.length)]
    },

    getEasterEggMessage() {
      const messages = [
        'You found the secret message!',
        'Achievement unlocked: Curious Developer',
        'Hello there, fellow explorer',
        'You clicked it, didn\'t you?',
        'Secret developer handshake activated',
        'Konami code vibes detected',
        'You have good taste in empty states',
        'This was worth the click, right?'
      ]
      return messages[Math.floor(Math.random() * messages.length)]
    },

    showEasterEgg() {
      this.clickCount++
      if (this.clickCount >= 3) {
        this.easterEggVisible = !this.easterEggVisible
        // Hide after 3 seconds
        if (this.easterEggVisible) {
          setTimeout(() => {
            this.easterEggVisible = false
            this.clickCount = 0
          }, 3000)
        }
      }
    }
  }
}
</script>

<style scoped>
.creative-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) var(--space-6);
  text-align: center;
  min-height: 280px;
  background: linear-gradient(145deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: var(--radius-xl);
  border: 2px dashed #e0e0e0;
  margin: var(--space-4) 0;
  position: relative;
  overflow: hidden;
}

/* Creative Illustration Styles */
.empty-illustration {
  margin-bottom: var(--space-6);
  position: relative;
}

.ascii-art {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.emoji-stack {
  font-size: 4rem;
  line-height: 1;
  filter: drop-shadow(0 4px 8px rgba(45, 90, 160, 0.1));
  animation: gentle-float 3s ease-in-out infinite;
}

.ascii-text {
  font-family: 'Courier New', monospace;
  font-size: 1.5rem;
  color: var(--color-text-secondary);
  font-weight: bold;
  letter-spacing: 2px;
  animation: subtle-pulse 2s ease-in-out infinite;
}

.creative-dots {
  font-size: 2rem;
  color: var(--brand-primary);
  letter-spacing: 8px;
  animation: dots-wave 1.5s ease-in-out infinite;
}

.creative-animation {
  font-style: italic;
  color: var(--color-text-muted);
  font-size: 1rem;
  animation: fade-in-out 2.5s ease-in-out infinite;
}

.creative-message {
  background: linear-gradient(45deg, var(--brand-primary), var(--accent-orange));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: bold;
  font-size: 1.2rem;
  animation: shimmer 2s ease-in-out infinite;
}

.creative-pattern {
  font-size: 1.8rem;
  color: var(--accent-orange);
  letter-spacing: 4px;
  animation: wave-pattern 2s ease-in-out infinite;
}

/* Title and Message Styling */
.empty-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  margin: 0 0 var(--space-3) 0;
  line-height: var(--leading-tight);
}

.empty-message {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  max-width: 400px;
  margin: 0 0 var(--space-6) 0;
}

/* Creative Action Button */
.empty-action {
  margin-bottom: var(--space-4);
}

.empty-action-btn {
  background: var(--brand-primary);
  color: white;
  border: none;
  border-radius: var(--radius-xl);
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(45, 90, 160, 0.2);
}

.empty-action-btn:hover {
  background: #274d89;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(45, 90, 160, 0.3);
}

.btn-rainbow {
  background: linear-gradient(45deg, var(--brand-primary), var(--accent-orange), var(--brand-primary));
  background-size: 200% 200%;
  animation: rainbow-shift 3s ease-in-out infinite;
}

/* Developer Signature */
.developer-signature {
  position: absolute;
  bottom: var(--space-3);
  right: var(--space-4);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.3s ease;
  font-style: italic;
}

.developer-signature:hover {
  opacity: 1;
}

.signature-text {
  user-select: none;
}

.easter-egg {
  position: absolute;
  bottom: 100%;
  right: 0;
  background: var(--brand-primary);
  color: white;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  white-space: nowrap;
  animation: bounce-in 0.5s ease-out;
  z-index: 10;
}

.easter-egg::after {
  content: '';
  position: absolute;
  top: 100%;
  right: var(--space-3);
  border: 6px solid transparent;
  border-top-color: var(--brand-primary);
}

/* Variant-specific styling */
.empty-state--no-campaigns {
  border-color: var(--brand-primary);
  background: linear-gradient(145deg, #f0f4f7 0%, #e8f1f5 100%);
}

.empty-state--no-ads {
  border-color: var(--success-400);
  background: linear-gradient(145deg, #f0f9f4 0%, #e8f5ea 100%);
}

.empty-state--loading-failed {
  border-color: var(--error-400);
  background: linear-gradient(145deg, #fef2f2 0%, #fce8e8 100%);
}

.empty-state--empty-dashboard {
  border-color: var(--accent-orange);
  background: linear-gradient(145deg, #fffaf0 0%, #fef5e7 100%);
}

/* Animations */
@keyframes gentle-float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

@keyframes subtle-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes dots-wave {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

@keyframes fade-in-out {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

@keyframes shimmer {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes wave-pattern {
  0%, 100% { transform: scaleX(1); }
  50% { transform: scaleX(1.1); }
}

@keyframes rainbow-shift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes bounce-in {
  0% {
    transform: scale(0) translateY(20px);
    opacity: 0;
  }
  50% {
    transform: scale(1.1) translateY(-5px);
    opacity: 1;
  }
  100% {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-empty-state {
    padding: var(--space-8) var(--space-4);
    min-height: 240px;
  }

  .emoji-stack {
    font-size: 3rem;
  }

  .ascii-text {
    font-size: 1.2rem;
  }

  .empty-title {
    font-size: var(--text-xl);
  }

  .empty-message {
    font-size: var(--text-sm);
    max-width: 300px;
  }

  .developer-signature {
    position: static;
    margin-top: var(--space-4);
    text-align: center;
  }
}

@media (max-width: 480px) {
  .creative-empty-state {
    padding: var(--space-6) var(--space-3);
    min-height: 200px;
  }

  .emoji-stack {
    font-size: 2.5rem;
  }

  .empty-title {
    font-size: var(--text-lg);
  }

  .empty-action-btn {
    padding: var(--space-2) var(--space-4);
    font-size: var(--text-sm);
  }
}
</style>