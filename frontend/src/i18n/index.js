import { createI18n } from 'vue-i18n'
import en from './locales/en.json'
import vi from './locales/vi.json'

// Get saved language preference or default to Vietnamese (Issue: I18n Phase 1)
const savedLocale = localStorage.getItem('locale') || 'vi'

const i18n = createI18n({
  legacy: false, // Use Composition API mode
  locale: savedLocale,
  fallbackLocale: 'en',
  messages: {
    en,
    vi
  },
  globalInjection: true,
  // Date and number formatting (Issue: I18n Phase 1)
  datetimeFormats: {
    vi: {
      short: {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      },
      long: {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      },
      monthYear: {
        year: 'numeric',
        month: 'long'
      }
    },
    en: {
      short: {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      },
      long: {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
        hour12: true
      },
      monthYear: {
        year: 'numeric',
        month: 'long'
      }
    }
  },
  numberFormats: {
    vi: {
      currency: {
        style: 'currency',
        currency: 'VND',
        currencyDisplay: 'symbol',
        minimumFractionDigits: 0
      },
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      },
      percent: {
        style: 'percent',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    },
    en: {
      currency: {
        style: 'currency',
        currency: 'USD',
        currencyDisplay: 'symbol',
        minimumFractionDigits: 2
      },
      decimal: {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      },
      percent: {
        style: 'percent',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
      }
    }
  }
})

export default i18n
