<template>
  <div class="creative-error-page" :class="`error-${errorType}`">
    <div class="error-container">
      <!-- Creative ASCII Art Error Display -->
      <div class="error-art">
        <div class="error-code">
          <span class="code-number">{{ errorCode }}</span>
          <div class="code-decoration">
            <span class="bracket">{</span>
            <span class="error-emoji">{{ getErrorEmoji() }}</span>
            <span class="bracket">}</span>
          </div>
        </div>

        <!-- Interactive ASCII Character -->
        <div class="ascii-character" @click="animateCharacter">
          <pre class="ascii-art">{{ getAsciiArt() }}</pre>
          <div class="character-speech-bubble" v-if="speechBubbleVisible">
            {{ speechBubbleMessage }}
          </div>
        </div>
      </div>

      <!-- Student Developer Error Messages -->
      <div class="error-content">
        <h1 class="error-title">{{ getErrorTitle() }}</h1>
        <p class="error-message">{{ getErrorMessage() }}</p>
        <p class="error-explanation">{{ getErrorExplanation() }}</p>

        <!-- Developer's Personal Touch -->
        <div class="developer-note">
          <div class="note-header">
            <span class="note-emoji">💭</span>
            <span class="note-title">Developer's Note:</span>
          </div>
          <p class="note-content">{{ getDeveloperNote() }}</p>
        </div>

        <!-- Action Buttons -->
        <div class="error-actions">
          <button
            v-if="showHomeButton"
            @click="goHome"
            class="error-btn primary"
          >
            🏠 Take Me Home
          </button>

          <button
            v-if="showBackButton"
            @click="goBack"
            class="error-btn secondary"
          >
            ⏮️ Go Back
          </button>

          <button
            v-if="showRefreshButton"
            @click="refresh"
            class="error-btn secondary"
          >
            🔄 Try Again
          </button>

          <button
            @click="reportIssue"
            class="error-btn ghost"
          >
            🐛 Report This (Please!)
          </button>
        </div>

        <!-- Error Details for Debugging (Hidden by default) -->
        <div class="debug-section">
          <button
            @click="showDebugInfo = !showDebugInfo"
            class="debug-toggle"
          >
            🔧 Debug Info (For Nerds)
          </button>

          <div v-if="showDebugInfo" class="debug-info">
            <pre class="debug-content">{{ debugInfo }}</pre>
            <p class="debug-note">
              Copy this and send it to the developer. They'll probably fix it faster than you can say "Stack Overflow". ☕
            </p>
          </div>
        </div>
      </div>

      <!-- Easter Egg Section -->
      <div class="easter-egg-section">
        <button
          @click="triggerEasterEgg"
          class="easter-egg-button"
          :class="{ 'rainbow': easterEggActive }"
        >
          {{ easterEggText }}
        </button>

        <div v-if="easterEggActive" class="easter-egg-content">
          <div class="konami-code">
            <p>🎮 Konami Code Achievement Unlocked!</p>
            <p>{{ getEasterEggMessage() }}</p>
            <div class="mini-game">
              <button @click="playMiniGame" class="mini-game-btn">
                Play "Guess the HTTP Status" 🎯
              </button>
              <div v-if="miniGameActive" class="mini-game-content">
                <p>{{ miniGameQuestion }}</p>
                <div class="mini-game-options">
                  <button
                    v-for="option in miniGameOptions"
                    :key="option.code"
                    @click="answerMiniGame(option)"
                    class="mini-game-option"
                  >
                    {{ option.code }} - {{ option.message }}
                  </button>
                </div>
                <p v-if="miniGameResult" class="mini-game-result">{{ miniGameResult }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Developer Signature -->
      <div class="developer-signature">
        <p>{{ getDeveloperSignature() }}</p>
        <small>Error page crafted with ❤️ (and lots of Stack Overflow)</small>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreativeErrorPage',
  props: {
    errorCode: {
      type: [String, Number],
      default: 404
    },
    errorType: {
      type: String,
      default: 'not-found',
      validator: value => [
        'not-found', 'server-error', 'network-error', 'permission-denied',
        'timeout', 'bad-request', 'maintenance'
      ].includes(value)
    },
    customTitle: String,
    customMessage: String,
    showHomeButton: {
      type: Boolean,
      default: true
    },
    showBackButton: {
      type: Boolean,
      default: true
    },
    showRefreshButton: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      speechBubbleVisible: false,
      speechBubbleMessage: '',
      showDebugInfo: false,
      easterEggActive: false,
      easterEggText: 'Click here if you\'re a developer 🤓',
      easterEggClicks: 0,
      miniGameActive: false,
      miniGameQuestion: '',
      miniGameOptions: [],
      miniGameResult: ''
    }
  },

  computed: {
    debugInfo() {
      return {
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
        url: window.location.href,
        errorCode: this.errorCode,
        errorType: this.errorType,
        viewport: `${window.innerWidth}x${window.innerHeight}`,
        localStorage: Object.keys(localStorage).length,
        developer_message: "Thanks for checking the debug info! You're the kind of user developers love. 🥰"
      }
    }
  },

  methods: {
    getErrorEmoji() {
      const emojiMap = {
        'not-found': '🔍',
        'server-error': '🤖',
        'network-error': '🌐',
        'permission-denied': '🚫',
        'timeout': '⏰',
        'bad-request': '❓',
        'maintenance': '🔧'
      }
      return emojiMap[this.errorType] || '😅'
    },

    getAsciiArt() {
      const artMap = {
        404: `    ╭─────────────╮
    │ (⌐■_■)     │
    │             │
    │  Not here   │
    │   buddy     │
    ╰─────────────╯`,
        500: `    ╭─────────────╮
    │  (╯°□°)╯    │
    │             │
    │   Server    │
    │   had a     │
    │   moment    │
    ╰─────────────╯`,
        403: `    ╭─────────────╮
    │    ಠ_ಠ      │
    │             │
    │  Access     │
    │  Denied     │
    │   Sorry!    │
    ╰─────────────╯`
      }
      return artMap[this.errorCode] || artMap[404]
    },

    getErrorTitle() {
      if (this.customTitle) return this.customTitle

      const titles = {
        'not-found': this.getRandomTitle([
          "Oops! This page went on vacation",
          "404: Page Not Found (But Your Humor Is Intact)",
          "This page is playing hide and seek... and winning",
          "Well, this is awkward...",
          "Houston, we have a problem"
        ]),
        'server-error': this.getRandomTitle([
          "The server had a coffee break",
          "Something went wrong on our end",
          "The hamsters stopped running",
          "Our code threw a tantrum",
          "Error 500: Technology is hard"
        ]),
        'network-error': this.getRandomTitle([
          "Your internet is taking a nap",
          "Network says 'nope'",
          "Connection lost in the digital void",
          "The internet is having trust issues",
          "WiFi went on strike"
        ]),
        'permission-denied': this.getRandomTitle([
          "Access denied (it's not personal)",
          "You shall not pass!",
          "Members only, buddy",
          "VIP area - ID required",
          "Permission level: Not quite there yet"
        ])
      }

      return titles[this.errorType] || "Something unexpected happened"
    },

    getErrorMessage() {
      if (this.customMessage) return this.customMessage

      const messages = {
        'not-found': this.getRandomMessage([
          "The page you're looking for has vanished into the digital ether. Maybe it never existed, maybe it's on a different server, or maybe it's just being shy.",
          "Either this page doesn't exist, or it's really good at playing hide and seek. We're betting on the former.",
          "This URL leads to a place that's emptier than a developer's coffee cup at 3 AM.",
          "The page you requested is like my motivation on Monday mornings: nowhere to be found."
        ]),
        'server-error': this.getRandomMessage([
          "Our servers are having an existential crisis right now. They'll be back once they figure out the meaning of life.",
          "Something went wrong on our end. Don't worry, it's not you, it's definitely us.",
          "The server encountered an error and decided to take a mental health day. We respect that.",
          "Our code is currently debugging itself. It's a very philosophical process."
        ]),
        'network-error': this.getRandomMessage([
          "Your connection to our servers is weaker than decaf coffee. Check your internet and try again.",
          "The network connection is more unreliable than a weather forecast. Please check your connectivity.",
          "It seems like you're offline, or the internet is having one of those days.",
          "Network error: The tubes of the internet are temporarily clogged."
        ])
      }

      return messages[this.errorType] || "An unexpected error occurred, but hey, at least you found this creative error page!"
    },

    getErrorExplanation() {
      const explanations = {
        'not-found': "This usually means the URL is misspelled, the page was moved, or it never existed in the first place. Kind of like my social skills in high school.",
        'server-error': "Our server encountered an unexpected condition that prevented it from fulfilling your request. Think of it as a digital sneeze.",
        'network-error': "There seems to be an issue with your internet connection or our servers are unreachable. Time to blame the WiFi router.",
        'permission-denied': "You don't have the necessary permissions to access this resource. It's like trying to get into an exclusive club wearing flip-flops.",
        'timeout': "The request took too long to process. Even our servers have attention spans, apparently.",
        'bad-request': "The request couldn't be understood. Our server is confused, and frankly, so are we.",
        'maintenance': "We're currently performing maintenance. Think of it as spa day for our servers."
      }

      return explanations[this.errorType] || "Something went wrong, but we're not entirely sure what."
    },

    getDeveloperNote() {
      const notes = [
        "I've spent more time on this error page than most people spend on their actual websites. At least it's entertaining!",
        "Fun fact: This error page has more personality than 90% of corporate websites. You're welcome.",
        "If you're seeing this, something broke. But hey, at least the error page works perfectly! Silver lining, right?",
        "Real talk: I actually test these error pages because I care about your experience, even when things go wrong.",
        "This error page was coded at 2 AM fueled by coffee and determination. Quality may vary, entertainment guaranteed.",
        "Most developers hate writing error pages. I embraced it. This is what happens when you give a creative person debugging tasks.",
        "I could have made this a boring white page with black text. Instead, you get ASCII art and dad jokes. You're welcome.",
        "Error pages are like error messages in code: they should be helpful, informative, and occasionally amusing."
      ]
      return notes[Math.floor(Math.random() * notes.length)]
    },

    getRandomTitle(titles) {
      return titles[Math.floor(Math.random() * titles.length)]
    },

    getRandomMessage(messages) {
      return messages[Math.floor(Math.random() * messages.length)]
    },

    getDeveloperSignature() {
      const signatures = [
        "Handcrafted error pages since 2024",
        "Making 404s fun, one ASCII character at a time",
        "Error page artisan and coffee enthusiast",
        "Professional mistake handler and creative problem solver",
        "Turning bugs into features since the beginning of time"
      ]
      return signatures[Math.floor(Math.random() * signatures.length)]
    },

    animateCharacter() {
      const messages = [
        "Hey there! 👋",
        "Still looking for that page?",
        "I'm just decorative, sorry!",
        "Try the buttons below! 👇",
        "Having fun yet? 😄",
        "Error pages > regular pages",
        "Blame the developer! 🤷‍♂️",
        "At least I'm here for you! ❤️"
      ]

      this.speechBubbleMessage = messages[Math.floor(Math.random() * messages.length)]
      this.speechBubbleVisible = true

      setTimeout(() => {
        this.speechBubbleVisible = false
      }, 3000)
    },

    goHome() {
      this.$router.push('/')
    },

    goBack() {
      window.history.back()
    },

    refresh() {
      window.location.reload()
    },

    reportIssue() {
      const subject = `Error Report: ${this.errorCode} ${this.errorType}`
      const body = `
Hi! I encountered an error on your website:

Error Code: ${this.errorCode}
Error Type: ${this.errorType}
URL: ${window.location.href}
Time: ${new Date().toISOString()}

Debug Info:
${JSON.stringify(this.debugInfo, null, 2)}

Additional Details:
(Please describe what you were trying to do when this error occurred)

P.S. Nice error page! 😄
      `

      const mailtoLink = `mailto:developer@example.com?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
      window.open(mailtoLink)
    },

    triggerEasterEgg() {
      this.easterEggClicks++

      if (this.easterEggClicks >= 3) {
        this.easterEggActive = !this.easterEggActive
        this.easterEggText = this.easterEggActive ?
          'Welcome to the developer zone! 🎉' :
          'Click here if you\'re a developer 🤓'
      }
    },

    getEasterEggMessage() {
      const messages = [
        "You found the secret developer area! Welcome to the club of people who click random buttons. 🎖️",
        "Achievement unlocked: Curiosity Level 100. You're the kind of user we build software for! 🏆",
        "Secret developer handshake: You clicked the button, so you're clearly one of us. Here's some bonus content! 🤝",
        "Fun fact: Only 0.03% of users ever see this message. You're special! ⭐"
      ]
      return messages[Math.floor(Math.random() * messages.length)]
    },

    playMiniGame() {
      this.miniGameActive = true
      this.miniGameResult = ''

      const questions = [
        {
          question: "What does HTTP status 418 mean?",
          options: [
            { code: 418, message: "I'm a teapot", correct: true },
            { code: 418, message: "Server Error", correct: false },
            { code: 418, message: "Not Found", correct: false }
          ]
        },
        {
          question: "Which status code means 'Payment Required'?",
          options: [
            { code: 401, message: "Unauthorized", correct: false },
            { code: 402, message: "Payment Required", correct: true },
            { code: 403, message: "Forbidden", correct: false }
          ]
        },
        {
          question: "What's the most creative HTTP status code?",
          options: [
            { code: 200, message: "OK", correct: false },
            { code: 451, message: "Unavailable For Legal Reasons", correct: false },
            { code: 418, message: "I'm a teapot", correct: true }
          ]
        }
      ]

      const randomQuestion = questions[Math.floor(Math.random() * questions.length)]
      this.miniGameQuestion = randomQuestion.question
      this.miniGameOptions = randomQuestion.options
    },

    answerMiniGame(selectedOption) {
      if (selectedOption.correct) {
        this.miniGameResult = "🎉 Correct! You clearly know your HTTP status codes. Developer level: Expert!"
      } else {
        this.miniGameResult = "🤔 Not quite, but hey, now you learned something new! That's what matters."
      }

      setTimeout(() => {
        this.miniGameActive = false
        this.miniGameResult = ''
      }, 4000)
    }
  }
}
</script>

<style scoped>
.creative-error-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
  font-family: var(--font-family-base);
}

.error-container {
  max-width: 800px;
  width: 100%;
  text-align: center;
}

/* Error Art Section */
.error-art {
  margin-bottom: var(--space-8);
}

.error-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.code-number {
  font-size: 8rem;
  font-weight: var(--font-black);
  color: var(--brand-primary);
  line-height: 1;
  text-shadow: 2px 2px 4px rgba(45, 90, 160, 0.2);
}

.code-decoration {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.bracket {
  font-size: 2rem;
  color: var(--color-text-secondary);
  font-weight: var(--font-bold);
}

.error-emoji {
  font-size: 3rem;
  animation: emoji-bounce 2s ease-in-out infinite;
}

.ascii-character {
  position: relative;
  display: inline-block;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.ascii-character:hover {
  transform: scale(1.05);
}

.ascii-art {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.2;
  color: var(--color-text);
  background: var(--color-bg-secondary);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 2px solid var(--primary-200);
  display: inline-block;
}

.character-speech-bubble {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--brand-primary);
  color: white;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  white-space: nowrap;
  z-index: 10;
  animation: bubble-appear 0.3s ease-out;
}

.character-speech-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 8px solid transparent;
  border-top-color: var(--brand-primary);
}

/* Error Content */
.error-content {
  margin-bottom: var(--space-8);
}

.error-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--color-text);
  margin: 0 0 var(--space-4) 0;
  line-height: var(--leading-tight);
}

.error-message {
  font-size: var(--text-lg);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-4) 0;
  line-height: var(--leading-relaxed);
}

.error-explanation {
  font-size: var(--text-base);
  color: var(--color-text-muted);
  margin: 0 0 var(--space-6) 0;
  font-style: italic;
}

/* Developer Note */
.developer-note {
  background: var(--primary-50);
  border: 2px solid var(--primary-200);
  border-radius: var(--radius-xl);
  padding: var(--space-5);
  margin: var(--space-6) 0;
  text-align: left;
}

.note-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.note-emoji {
  font-size: 1.5rem;
}

.note-title {
  font-weight: var(--font-semibold);
  color: var(--brand-primary);
}

.note-content {
  color: var(--color-text);
  margin: 0;
  line-height: var(--leading-relaxed);
}

/* Action Buttons */
.error-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  justify-content: center;
  margin-bottom: var(--space-8);
}

.error-btn {
  padding: var(--space-3) var(--space-6);
  border-radius: var(--radius-xl);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  font-size: var(--text-base);
}

.error-btn.primary {
  background: var(--brand-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(45, 90, 160, 0.3);
}

.error-btn.primary:hover {
  background: #274d89;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(45, 90, 160, 0.4);
}

.error-btn.secondary {
  background: var(--accent-orange);
  color: white;
  box-shadow: 0 4px 12px rgba(244, 162, 97, 0.3);
}

.error-btn.secondary:hover {
  background: #e89761;
  transform: translateY(-2px);
}

.error-btn.ghost {
  background: transparent;
  border: 2px solid var(--primary-300);
  color: var(--brand-primary);
}

.error-btn.ghost:hover {
  background: var(--primary-50);
  border-color: var(--brand-primary);
}

/* Debug Section */
.debug-section {
  margin-bottom: var(--space-6);
}

.debug-toggle {
  background: var(--color-bg-tertiary);
  border: 1px solid var(--primary-200);
  color: var(--color-text);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: var(--text-sm);
  transition: all 0.3s ease;
}

.debug-toggle:hover {
  background: var(--primary-50);
  border-color: var(--brand-primary);
}

.debug-info {
  margin-top: var(--space-4);
  text-align: left;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.debug-content {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: var(--text-xs);
  color: var(--color-text);
  background: var(--gray-900);
  color: var(--green-400);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  overflow-x: auto;
}

.debug-note {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-top: var(--space-2);
  margin-bottom: 0;
  font-style: italic;
}

/* Easter Egg Section */
.easter-egg-section {
  margin-bottom: var(--space-6);
}

.easter-egg-button {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: var(--text-xs);
  cursor: pointer;
  font-style: italic;
  transition: all 0.3s ease;
}

.easter-egg-button:hover {
  color: var(--brand-primary);
}

.easter-egg-button.rainbow {
  background: linear-gradient(45deg, #ff0000, #ff8800, #ffff00, #88ff00, #00ff00, #00ff88, #00ffff, #0088ff, #0000ff, #8800ff, #ff00ff, #ff0088);
  background-size: 200% 200%;
  animation: rainbow-shift 2s ease-in-out infinite;
  color: white;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  font-weight: var(--font-bold);
}

.easter-egg-content {
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--primary-50);
  border-radius: var(--radius-lg);
  border: 2px solid var(--primary-200);
}

.konami-code {
  text-align: center;
}

.mini-game {
  margin-top: var(--space-4);
}

.mini-game-btn {
  background: var(--accent-orange);
  color: white;
  border: none;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: var(--font-semibold);
  transition: all 0.3s ease;
}

.mini-game-btn:hover {
  background: #e89761;
  transform: translateY(-1px);
}

.mini-game-content {
  margin-top: var(--space-4);
}

.mini-game-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin: var(--space-3) 0;
}

.mini-game-option {
  background: var(--color-bg-secondary);
  border: 1px solid var(--primary-200);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.mini-game-option:hover {
  background: var(--primary-50);
  border-color: var(--brand-primary);
}

.mini-game-result {
  font-weight: var(--font-semibold);
  color: var(--brand-primary);
  margin-top: var(--space-3);
}

/* Developer Signature */
.developer-signature {
  border-top: 1px solid var(--primary-200);
  padding-top: var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

/* Animations */
@keyframes emoji-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes bubble-appear {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.8);
  }
  100% {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
}

@keyframes rainbow-shift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-error-page {
    padding: var(--space-4);
  }

  .code-number {
    font-size: 4rem;
  }

  .error-code {
    flex-direction: column;
    gap: var(--space-2);
  }

  .error-title {
    font-size: var(--text-2xl);
  }

  .error-actions {
    flex-direction: column;
    align-items: center;
  }

  .error-btn {
    width: 100%;
    max-width: 300px;
  }

  .mini-game-options {
    align-items: center;
  }

  .mini-game-option {
    max-width: 400px;
    width: 100%;
  }

  .ascii-art {
    font-size: 0.7rem;
  }

  .character-speech-bubble {
    top: -50px;
    font-size: var(--text-xs);
  }
}

@media (max-width: 480px) {
  .code-number {
    font-size: 3rem;
  }

  .error-title {
    font-size: var(--text-xl);
  }

  .ascii-art {
    font-size: 0.6rem;
  }
}
</style>