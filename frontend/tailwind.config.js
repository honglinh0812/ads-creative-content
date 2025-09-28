module.exports = {
  darkMode: 'class',
  content: [
    "./public/index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        // Primary - Deep blue instead of generic blue
        primary: {
          50: '#f0f4f7',
          100: '#dae4eb',
          200: '#b8c9d4',
          300: '#8fa6b7',
          400: '#6b8499',
          500: '#2d5aa0', // Main primary color - more opinionated
          600: '#274d89',
          700: '#1f3d6b',
          800: '#19304f',
          900: '#13253a'
        },
        // Accent - Warm orange instead of purple/pink
        accent: {
          50: '#fef6f0',
          100: '#fdebd7',
          200: '#fad4ab',
          300: '#f6b373',
          400: '#f4a261', // Main accent - warm and creative
          500: '#e8964d',
          600: '#d4843a',
          700: '#b06c2e',
          800: '#8d5623',
          900: '#6b411b'
        },
        // Secondary - Terracotta red for character
        secondary: {
          50: '#fdf2f0',
          100: '#fae2dc',
          200: '#f4c2b4',
          300: '#ed9b84',
          400: '#e76f51', // Terracotta main
          500: '#d65d42',
          600: '#c14a32',
          700: '#9d3b28',
          800: '#7a2e1e',
          900: '#5c2217'
        },
        // Success - More natural green
        success: {
          50: '#f2f8f2',
          100: '#e1f0e1',
          200: '#c3e0c3',
          300: '#98c998',
          400: '#6ba66b',
          500: '#4a8c4a',
          600: '#3d743d',
          700: '#325c32',
          800: '#284628',
          900: '#1f361f'
        },
        // Warning - Muted amber
        warning: {
          50: '#faf7f0',
          100: '#f3ecd9',
          200: '#e6d6b0',
          300: '#d4b97d',
          400: '#c49c52',
          500: '#b8873d',
          600: '#9e7232',
          700: '#7f5a28',
          800: '#634420',
          900: '#4a3318'
        },
        // Error - Less aggressive red
        error: {
          50: '#fdf2f2',
          100: '#fbe2e2',
          200: '#f6c8c8',
          300: '#eea3a3',
          400: '#e07575',
          500: '#c85a5a',
          600: '#a64545',
          700: '#853737',
          800: '#662b2b',
          900: '#4d2020'
        },
        // Neutrals - Slightly warmer grays
        neutral: {
          50: '#fafaf9',
          100: '#f4f4f2',
          200: '#e8e8e4',
          300: '#d6d6d1',
          400: '#a8a8a1',
          500: '#7a7a70',
          600: '#5a5a52',
          700: '#46463f',
          800: '#2a2a26',
          900: '#1a1a18'
        },
        // Background colors - softer than pure white
        background: '#fafafa', // Slight warmth instead of pure white
        surface: '#ffffff',
        text: '#2a2a26' // Softer than pure black
      },
      fontFamily: {
        'sans': ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'system-ui', 'sans-serif'],
        'display': ['Inter', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'system-ui'],
        'body': ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'system-ui'],
        'mono': ['SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', 'monospace']
      },
      fontSize: {
        'tiny': ['0.6875rem', { lineHeight: '1rem', fontWeight: '400' }],      // 11px - authentic small text
        'xs': ['0.8125rem', { lineHeight: '1.125rem', fontWeight: '400' }],    // 13px - slightly off-grid
        'sm': ['0.9375rem', { lineHeight: '1.375rem', fontWeight: '400' }],    // 15px - organic sizing
        'base': ['1.0625rem', { lineHeight: '1.625rem', fontWeight: '400' }],  // 17px - reader-friendly
        'lg': ['1.1875rem', { lineHeight: '1.75rem', fontWeight: '500' }],     // 19px - varied weights
        'xl': ['1.375rem', { lineHeight: '1.875rem', fontWeight: '600' }],     // 22px
        '2xl': ['1.625rem', { lineHeight: '2.125rem', fontWeight: '700' }],    // 26px
        '3xl': ['1.9375rem', { lineHeight: '2.375rem', fontWeight: '700' }],   // 31px
        '4xl': ['2.4375rem', { lineHeight: '2.75rem', fontWeight: '800' }],    // 39px
        '5xl': ['3.0625rem', { lineHeight: '3.25rem', fontWeight: '800' }],    // 49px
        '6xl': ['3.875rem', { lineHeight: '4rem', fontWeight: '900' }]         // 62px
      },
      spacing: {
        // Organic spacing instead of perfect mathematical spacing
        '15': '3.75rem',   // 60px - between 14 (56px) and 16 (64px)
        '18': '4.5rem',    // 72px
        '22': '5.5rem',    // 88px - between 20 (80px) and 24 (96px)
        '26': '6.5rem',    // 104px
        '30': '7.5rem',    // 120px
        '34': '8.5rem',    // 136px
        '88': '22rem',     // 352px
        '128': '32rem',    // 512px
        'content': '42rem' // 672px - content width
      },
      borderRadius: {
        'xl': '0.75rem',
        '2xl': '1rem',
        '3xl': '1.5rem'
      },
      boxShadow: {
        'soft': '0 2px 15px -3px rgba(0, 0, 0, 0.07), 0 10px 20px -2px rgba(0, 0, 0, 0.04)',
        'medium': '0 4px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
        'strong': '0 10px 40px -10px rgba(0, 0, 0, 0.15), 0 4px 25px -5px rgba(0, 0, 0, 0.1)'
      },
      animation: {
        'fade-in': 'fadeIn 0.5s ease-in-out',
        'slide-up': 'slideUp 0.3s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out'
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideUp: {
          '0%': { transform: 'translateY(10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        slideDown: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        scaleIn: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        }
      }
    },
  },
  plugins: [],
}
