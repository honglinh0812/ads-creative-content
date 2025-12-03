/**
 * Language Detection Utility for Ad Creative Content
 * Detects language of text using character-based heuristics and keyword matching
 */

/**
 * Detect language of text using character-based heuristics
 * @param {string} text - Text to analyze
 * @returns {string} - Detected language code ('vi' or 'en')
 */
export function detectLanguage(text) {
  if (!text || text.trim().length === 0) {
    return 'en' // default to English for empty text
  }

  // Count Vietnamese-specific characters (diacritics)
  const vietnameseChars = /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/gi
  const vietnameseMatches = (text.match(vietnameseChars) || []).length

  // Count English words
  const englishWords = /\b[a-zA-Z]+\b/g
  const englishMatches = (text.match(englishWords) || []).length

  // Vietnamese common keywords for additional scoring
  const vietnameseKeywords = [
    'của', 'và', 'có', 'là', 'được', 'trong', 'các', 'một', 'cho', 'với',
    'này', 'đó', 'những', 'tôi', 'bạn', 'để', 'khi', 'về', 'từ', 'người',
    'không', 'như', 'đã', 'sẽ', 'nếu', 'vì', 'rất', 'nhiều', 'phải', 'thể'
  ]

  let vietnameseKeywordCount = 0
  const lowerText = text.toLowerCase()
  vietnameseKeywords.forEach(keyword => {
    if (lowerText.includes(keyword)) {
      vietnameseKeywordCount++
    }
  })

  // Scoring algorithm
  // Vietnamese diacritics are strong indicators (weight: 2)
  // Vietnamese keywords are moderately strong (weight: 3)
  // English words are baseline (weight: 1)
  let vietnameseScore = vietnameseMatches * 2 + vietnameseKeywordCount * 3
  let englishScore = englishMatches

  return vietnameseScore > englishScore ? 'vi' : 'en'
}

/**
 * Get localized text templates for different sections
 * Supports: trending keywords, ad reference
 */
export const i18nTemplates = {
  trendingKeywords: {
    vi: {
      title: 'TỪ KHÓA THỊNH HÀNH',
      instruction: 'Hãy kết hợp các từ khóa này một cách tự nhiên vào nội dung quảng cáo.'
    },
    en: {
      title: 'TRENDING KEYWORDS',
      instruction: 'Incorporate these trending topics naturally into the ad content.'
    }
  },
  adReference: {
    vi: {
      title: 'THAM KHẢO TỪ QUẢNG CÁO',
      instruction: 'Học phong cách viết, KHÔNG sao chép nội dung.'
    },
    en: {
      title: 'AD REFERENCE',
      instruction: 'Learn the writing style, DO NOT copy content.'
    }
  }
}

/**
 * Get success message for keyword insertion
 * @param {number} count - Number of keywords added
 * @param {string} language - Language code ('vi' or 'en')
 * @returns {string} - Localized success message
 */
export function getKeywordSuccessMessage(count, language) {
  if (language === 'vi') {
    return `Đã thêm ${count} từ khóa thịnh hành vào prompt`
  }
  return `Added ${count} trending keyword(s) to prompt`
}

/**
 * Get the marker text for detecting existing sections
 * @param {string} sectionType - Type of section ('trending' or 'reference')
 * @param {string} language - Language code ('vi' or 'en')
 * @returns {string} - Marker text
 */
export function getSectionMarker(sectionType, language) {
  if (sectionType === 'trending') {
    return language === 'vi' ? 'TỪ KHÓA THỊNH HÀNH' : 'TRENDING KEYWORDS'
  } else if (sectionType === 'reference') {
    return language === 'vi' ? 'THAM KHẢO TỪ QUẢNG CÁO' : 'AD REFERENCE'
  }
  return ''
}
