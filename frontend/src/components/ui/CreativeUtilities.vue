<template>
  <div class="creative-utilities">
    <!-- Advanced CSS Grid Mastery -->
    <div class="grid-mastery-demo" v-if="showDemo">
      <div class="grid-container">
        <div class="grid-item featured">Featured Content</div>
        <div class="grid-item secondary">Secondary</div>
        <div class="grid-item tertiary">Extra</div>
        <div class="grid-item span-two">Spanning Two Columns</div>
        <div class="grid-item auto-fit">Auto-fit</div>
      </div>
    </div>

    <!-- CSS Custom Properties Showcase -->
    <div class="dynamic-theming" :style="dynamicTheme">
      <h3 class="theme-title">Dynamic Theming</h3>
      <p class="theme-text">Student developer flexing CSS custom properties!</p>
      <div class="theme-controls">
        <input
          type="range"
          min="0"
          max="360"
          v-model="hue"
          @input="updateTheme"
          class="hue-slider"
        />
        <label>{{ hue }}° Hue</label>
      </div>
    </div>

    <!-- Advanced Animation Patterns -->
    <div class="animation-showcase">
      <div class="morphing-button" @click="triggerMorph" :class="{ morphed: isMorphed }">
        <span class="button-text">{{ isMorphed ? 'Reset' : 'Morph Me!' }}</span>
        <div class="button-background"></div>
      </div>

      <div class="floating-elements">
        <div
          v-for="n in 5"
          :key="n"
          class="floating-particle"
          :style="getParticleStyle(n)"
        ></div>
      </div>
    </div>

    <!-- Container Queries (Future CSS) -->
    <div class="container-query-demo" ref="containerDemo">
      <div class="responsive-card">
        <h4>Container Query Card</h4>
        <p>I adapt based on my container size, not the viewport!</p>
      </div>
    </div>

    <!-- Creative Typography -->
    <div class="typography-art">
      <h2 class="gradient-text">Gradient Text Effect</h2>
      <div class="text-shadow-art">Shadow Art</div>
      <div class="outlined-text">Outlined Text</div>
      <div class="glitch-text" @mouseenter="startGlitch" @mouseleave="stopGlitch">
        {{ glitchText }}
      </div>
    </div>

    <!-- CSS Shapes and Clip-Path -->
    <div class="shape-gallery">
      <div class="shape hexagon"></div>
      <div class="shape triangle"></div>
      <div class="shape star"></div>
      <div class="shape blob"></div>
    </div>

    <!-- Scroll-Driven Animations -->
    <div class="scroll-animations" ref="scrollDemo">
      <div class="scroll-indicator">
        <div class="indicator-fill" :style="{ width: scrollProgress + '%' }"></div>
      </div>
      <div class="parallax-element" :style="parallaxStyle">
        Parallax Element
      </div>
    </div>

    <!-- Student Developer Comments -->
    <div class="developer-comments">
      <div class="comment-block">
        <span class="comment-marker">// Developer Notes:</span>
        <p class="comment-text">{{ getCurrentDeveloperComment() }}</p>
      </div>

      <div class="code-quality-badge" @click="toggleQualityBadge">
        <span class="badge-text">{{ qualityBadgeText }}</span>
        <div class="badge-shimmer"></div>
      </div>
    </div>

    <!-- Advanced Flexbox Patterns -->
    <div class="flexbox-mastery">
      <div class="flex-container holy-grail">
        <header class="flex-header">Header</header>
        <div class="flex-body">
          <nav class="flex-nav">Nav</nav>
          <main class="flex-main">Main Content</main>
          <aside class="flex-aside">Aside</aside>
        </div>
        <footer class="flex-footer">Footer</footer>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreativeUtilities',
  props: {
    showDemo: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      hue: 220,
      isMorphed: false,
      scrollProgress: 0,
      scrollY: 0,
      glitchText: 'Hover for Glitch',
      glitchInterval: null,
      qualityBadgeText: 'Code Quality: Student Level',
      qualityBadgeClicks: 0
    }
  },

  computed: {
    dynamicTheme() {
      return {
        '--dynamic-hue': `${this.hue}deg`,
        '--primary-color': `hsl(${this.hue}, 70%, 50%)`,
        '--secondary-color': `hsl(${this.hue + 60}, 70%, 60%)`,
        '--accent-color': `hsl(${this.hue + 120}, 70%, 70%)`
      }
    },

    parallaxStyle() {
      return {
        transform: `translateY(${this.scrollY * 0.5}px)`
      }
    }
  },

  mounted() {
    this.setupScrollAnimations()
    this.startFloatingAnimation()
  },

  beforeUnmount() {
    this.clearAnimations()
  },

  methods: {
    updateTheme() {
      // Dynamic CSS custom property updates
      document.documentElement.style.setProperty('--student-creativity', `${this.hue}deg`)
    },

    triggerMorph() {
      this.isMorphed = !this.isMorphed
    },

    getParticleStyle(index) {
      const delay = index * 0.5
      const scale = 0.5 + (index * 0.2)
      const hue = (this.hue + (index * 72)) % 360

      return {
        '--particle-delay': `${delay}s`,
        '--particle-scale': scale,
        '--particle-hue': `${hue}deg`,
        animationDelay: `${delay}s`
      }
    },

    setupScrollAnimations() {
      const updateScroll = () => {
        if (this.$refs.scrollDemo) {
          const rect = this.$refs.scrollDemo.getBoundingClientRect()
          const progress = Math.max(0, Math.min(100, (window.innerHeight - rect.top) / window.innerHeight * 100))
          this.scrollProgress = progress
          this.scrollY = window.scrollY
        }
      }

      window.addEventListener('scroll', updateScroll)
      this._scrollHandler = updateScroll
    },

    startFloatingAnimation() {
      // Advanced CSS animations are handled in CSS
      // This method can be used for JavaScript-driven animations if needed
    },

    startGlitch() {
      const glitchChars = '!@#$%^&*()_+-=[]{}|;:,.<>?'
      const originalText = 'Hover for Glitch'

      this.glitchInterval = setInterval(() => {
        let glitchedText = ''
        for (let i = 0; i < originalText.length; i++) {
          if (Math.random() > 0.8) {
            glitchedText += glitchChars[Math.floor(Math.random() * glitchChars.length)]
          } else {
            glitchedText += originalText[i]
          }
        }
        this.glitchText = glitchedText
      }, 100)
    },

    stopGlitch() {
      if (this.glitchInterval) {
        clearInterval(this.glitchInterval)
        this.glitchInterval = null
      }
      this.glitchText = 'Hover for Glitch'
    },

    toggleQualityBadge() {
      this.qualityBadgeClicks++
      const badges = [
        'Code Quality: Student Level',
        'Code Quality: Junior Developer',
        'Code Quality: Stack Overflow Dependent',
        'Code Quality: Caffeinated Excellence',
        'Code Quality: Late Night Genius',
        'Code Quality: Professional (Sometimes)',
        'Code Quality: Debugs by Removing Code',
        'Code Quality: Works on My Machine™'
      ]
      this.qualityBadgeText = badges[this.qualityBadgeClicks % badges.length]
    },

    getCurrentDeveloperComment() {
      const comments = [
        'This component showcases CSS techniques I learned from YouTube tutorials and documentation diving. No frameworks needed - just pure CSS creativity!',
        'Fun fact: Every animation here is CSS-driven. JavaScript is just for interactivity. I believe in letting CSS do what CSS does best.',
        'These patterns demonstrate grid mastery, custom properties, clip-path, and modern layout techniques. Built by someone who actually reads the CSS specs.',
        'I spent way too much time perfecting these animations. But hey, smooth 60fps interactions are worth it!',
        'Container queries aren\'t fully supported yet, but I\'m future-proofing this code. That\'s what student developers do - we experiment!',
        'Each CSS technique here solves a real-world design challenge. No bloat, no unnecessary complexity - just creative solutions.',
        'This is what happens when you give a developer CSS Grid, custom properties, and too much free time. I regret nothing.',
        'Built with love, caffeine, and a deep appreciation for what modern CSS can achieve. No preprocessors harmed in the making.'
      ]
      return comments[Math.floor(Math.random() * comments.length)]
    },

    clearAnimations() {
      if (this.glitchInterval) {
        clearInterval(this.glitchInterval)
      }
      if (this._scrollHandler) {
        window.removeEventListener('scroll', this._scrollHandler)
      }
    }
  }
}
</script>

<style scoped>
.creative-utilities {
  padding: var(--space-8);
  max-width: 1200px;
  margin: 0 auto;
}

/* Advanced CSS Grid Mastery */
.grid-mastery-demo {
  margin-bottom: var(--space-10);
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  grid-template-rows: auto auto auto;
  gap: var(--space-4);
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  padding: var(--space-6);
  border-radius: var(--radius-xl);
}

.grid-item {
  background: white;
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  border: 1px solid var(--primary-200);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-semibold);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.grid-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(45, 90, 160, 0.15);
}

.grid-item.featured {
  grid-column: span 2;
  background: linear-gradient(135deg, var(--brand-primary), var(--accent-orange));
  color: white;
  font-size: var(--text-lg);
}

.grid-item.span-two {
  grid-column: span 2;
  background: var(--success-50);
  color: var(--success-800);
}

/* Dynamic Theming with CSS Custom Properties */
.dynamic-theming {
  background: linear-gradient(135deg,
    var(--primary-color, var(--brand-primary)) 0%,
    var(--secondary-color, var(--accent-orange)) 100%);
  padding: var(--space-6);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-8);
  color: white;
  text-align: center;
}

.theme-title {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.theme-text {
  margin: 0 0 var(--space-4) 0;
  opacity: 0.9;
}

.theme-controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
}

.hue-slider {
  width: 200px;
  height: 8px;
  border-radius: var(--radius-full);
  background: linear-gradient(to right,
    hsl(0, 70%, 50%),
    hsl(60, 70%, 50%),
    hsl(120, 70%, 50%),
    hsl(180, 70%, 50%),
    hsl(240, 70%, 50%),
    hsl(300, 70%, 50%),
    hsl(360, 70%, 50%));
  cursor: pointer;
  appearance: none;
}

.hue-slider::-webkit-slider-thumb {
  appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: pointer;
}

/* Advanced Animation Patterns */
.animation-showcase {
  margin-bottom: var(--space-8);
  position: relative;
  min-height: 200px;
}

.morphing-button {
  position: relative;
  display: inline-block;
  padding: var(--space-4) var(--space-8);
  border: none;
  border-radius: var(--radius-xl);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  margin-bottom: var(--space-6);
}

.morphing-button:hover {
  transform: translateY(-2px);
}

.morphing-button.morphed {
  transform: scale(1.1) rotate(5deg);
  border-radius: var(--radius-md);
}

.button-text {
  position: relative;
  z-index: 2;
  color: white;
  font-weight: var(--font-bold);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.button-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, var(--brand-primary), var(--accent-orange));
  transition: all 0.4s ease;
}

.morphing-button.morphed .button-background {
  background: linear-gradient(225deg, var(--success-500), var(--warning-500));
  transform: scale(1.2);
}

.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.floating-particle {
  position: absolute;
  width: 12px;
  height: 12px;
  background: hsl(var(--particle-hue, 220deg), 70%, 60%);
  border-radius: 50%;
  animation: float-around 8s ease-in-out infinite;
  transform: scale(var(--particle-scale, 1));
  opacity: 0.7;
}

.floating-particle:nth-child(1) { left: 10%; top: 20%; }
.floating-particle:nth-child(2) { left: 80%; top: 30%; }
.floating-particle:nth-child(3) { left: 60%; top: 60%; }
.floating-particle:nth-child(4) { left: 20%; top: 80%; }
.floating-particle:nth-child(5) { left: 90%; top: 70%; }

/* Container Query Demo */
.container-query-demo {
  container-type: inline-size;
  background: var(--primary-50);
  padding: var(--space-4);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-8);
  resize: horizontal;
  overflow: auto;
  min-width: 200px;
  max-width: 100%;
  border: 2px solid var(--primary-200);
}

.responsive-card {
  background: white;
  padding: var(--space-4);
  border-radius: var(--radius-md);
  text-align: center;
}

/* Typography Art */
.typography-art {
  margin-bottom: var(--space-8);
  text-align: center;
}

.gradient-text {
  font-size: var(--text-4xl);
  font-weight: var(--font-black);
  background: linear-gradient(45deg, var(--brand-primary), var(--accent-orange), var(--error-500));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-size: 200% 200%;
  animation: gradient-shift 3s ease-in-out infinite;
  margin-bottom: var(--space-4);
}

.text-shadow-art {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--brand-primary);
  text-shadow:
    0 0 5px rgba(45, 90, 160, 0.5),
    0 0 10px rgba(45, 90, 160, 0.3),
    0 0 15px rgba(45, 90, 160, 0.2),
    0 0 20px rgba(45, 90, 160, 0.1);
  margin-bottom: var(--space-4);
}

.outlined-text {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: transparent;
  -webkit-text-stroke: 2px var(--brand-primary);
  text-stroke: 2px var(--brand-primary);
  margin-bottom: var(--space-4);
}

.glitch-text {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--error-500);
  cursor: pointer;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  transition: all 0.3s ease;
}

.glitch-text:hover {
  animation: glitch-shake 0.3s ease-in-out infinite;
}

/* CSS Shapes and Clip-Path */
.shape-gallery {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: var(--space-8);
  flex-wrap: wrap;
  gap: var(--space-4);
}

.shape {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--brand-primary), var(--accent-orange));
  transition: transform 0.3s ease;
}

.shape:hover {
  transform: scale(1.2) rotate(10deg);
}

.shape.hexagon {
  clip-path: polygon(30% 0%, 70% 0%, 100% 50%, 70% 100%, 30% 100%, 0% 50%);
}

.shape.triangle {
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
}

.shape.star {
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}

.shape.blob {
  border-radius: 50% 20% 80% 30%;
  animation: blob-morph 4s ease-in-out infinite;
}

/* Scroll-Driven Animations */
.scroll-animations {
  position: relative;
  height: 200px;
  margin-bottom: var(--space-8);
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.scroll-indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(45, 90, 160, 0.1);
}

.indicator-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--brand-primary), var(--accent-orange));
  transition: width 0.3s ease;
}

.parallax-element {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: var(--brand-primary);
  color: white;
  padding: var(--space-4) var(--space-6);
  border-radius: var(--radius-lg);
  font-weight: var(--font-bold);
  will-change: transform;
}

/* Developer Comments */
.developer-comments {
  background: var(--gray-900);
  color: var(--gray-100);
  padding: var(--space-6);
  border-radius: var(--radius-lg);
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  margin-bottom: var(--space-8);
  position: relative;
}

.comment-marker {
  color: var(--green-400);
  font-weight: var(--font-bold);
}

.comment-text {
  color: var(--gray-300);
  margin-top: var(--space-2);
  line-height: var(--leading-relaxed);
  font-size: var(--text-sm);
}

.code-quality-badge {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  background: var(--warning-600);
  color: white;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.badge-shimmer {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: shimmer 2s ease-in-out infinite;
}

/* Advanced Flexbox Patterns */
.flexbox-mastery {
  margin-bottom: var(--space-8);
}

.holy-grail {
  display: flex;
  flex-direction: column;
  min-height: 300px;
  background: var(--primary-50);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.flex-header,
.flex-footer {
  background: var(--brand-primary);
  color: white;
  padding: var(--space-3);
  text-align: center;
  font-weight: var(--font-semibold);
}

.flex-body {
  display: flex;
  flex: 1;
}

.flex-nav,
.flex-aside {
  background: var(--primary-100);
  padding: var(--space-3);
  flex: 0 0 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
}

.flex-main {
  flex: 1;
  background: white;
  padding: var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-semibold);
}

/* Keyframe Animations */
@keyframes float-around {
  0%, 100% {
    transform: translate(0, 0) scale(var(--particle-scale, 1));
  }
  25% {
    transform: translate(30px, -20px) scale(calc(var(--particle-scale, 1) * 1.2));
  }
  50% {
    transform: translate(-10px, -40px) scale(var(--particle-scale, 1));
  }
  75% {
    transform: translate(-30px, -10px) scale(calc(var(--particle-scale, 1) * 0.8));
  }
}

@keyframes gradient-shift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes glitch-shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

@keyframes blob-morph {
  0%, 100% {
    border-radius: 50% 20% 80% 30%;
  }
  25% {
    border-radius: 20% 50% 30% 80%;
  }
  50% {
    border-radius: 80% 30% 50% 20%;
  }
  75% {
    border-radius: 30% 80% 20% 50%;
  }
}

@keyframes shimmer {
  from {
    left: -100%;
  }
  to {
    left: 100%;
  }
}

/* Container Queries (Future CSS) */
@container (min-width: 300px) {
  .responsive-card {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-4);
    text-align: left;
  }

  .responsive-card::before {
    content: '🎯';
    font-size: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

@container (min-width: 500px) {
  .responsive-card {
    grid-template-columns: auto 1fr auto;
    text-align: center;
  }

  .responsive-card::after {
    content: '✨';
    font-size: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .creative-utilities {
    padding: var(--space-4);
  }

  .grid-container {
    grid-template-columns: 1fr;
    gap: var(--space-2);
  }

  .grid-item.featured,
  .grid-item.span-two {
    grid-column: span 1;
  }

  .shape-gallery {
    justify-content: center;
  }

  .theme-controls {
    flex-direction: column;
  }

  .hue-slider {
    width: 250px;
  }

  .flex-body {
    flex-direction: column;
  }

  .flex-nav,
  .flex-aside {
    flex: 0 0 auto;
  }

  .typography-art {
    word-break: break-word;
  }

  .gradient-text {
    font-size: var(--text-2xl);
  }
}

/* Reduced Motion Support */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }

  .floating-particle {
    animation: none;
  }

  .morphing-button {
    transition: none;
  }

  .parallax-element {
    transform: translate(-50%, -50%) !important;
  }
}

/* High Contrast Mode */
@media (prefers-contrast: high) {
  .grid-item,
  .responsive-card,
  .flex-main {
    border: 2px solid;
  }

  .button-background {
    background: #000 !important;
  }

  .button-text {
    color: #fff !important;
  }
}
</style>