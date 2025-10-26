/**
 * CTA (Call-to-Action) Utility Helper Functions
 *
 * Provides reusable helper functions for CTA formatting and manipulation.
 * Can be used standalone or in conjunction with Vuex CTA store.
 */

/**
 * Format CTA value to display label
 *
 * @param {string} ctaValue - CTA enum value (e.g., 'LEARN_MORE')
 * @param {array} ctasList - Array of CTA objects with {value, label}
 * @param {boolean} fallbackToValue - Return raw value if not found (default: true)
 * @returns {string} - Display label or fallback value
 *
 * @example
 * formatCTA('LEARN_MORE', ctas) // Returns 'Tìm hiểu thêm'
 * formatCTA('INVALID', ctas) // Returns 'INVALID' (fallback)
 * formatCTA('INVALID', ctas, false) // Returns '' (no fallback)
 */
export function formatCTA(ctaValue, ctasList, fallbackToValue = true) {
  if (!ctaValue) return ''
  if (!ctasList || ctasList.length === 0) {
    return fallbackToValue ? ctaValue : ''
  }

  const cta = ctasList.find(c => c.value === ctaValue)
  return cta ? cta.label : (fallbackToValue ? ctaValue : '')
}

/**
 * Get CTA object by value
 *
 * @param {string} ctaValue - CTA enum value
 * @param {array} ctasList - Array of CTA objects
 * @returns {object|null} - CTA object {value, label} or null if not found
 *
 * @example
 * getCTAByValue('LEARN_MORE', ctas) // Returns {value: 'LEARN_MORE', label: 'Tìm hiểu thêm'}
 */
export function getCTAByValue(ctaValue, ctasList) {
  if (!ctaValue || !ctasList) return null
  return ctasList.find(c => c.value === ctaValue) || null
}

/**
 * Get CTA object by label (reverse lookup)
 *
 * @param {string} label - CTA display label
 * @param {array} ctasList - Array of CTA objects
 * @returns {object|null} - CTA object or null
 *
 * @example
 * getCTAByLabel('Tìm hiểu thêm', ctas) // Returns {value: 'LEARN_MORE', label: 'Tìm hiểu thêm'}
 */
export function getCTAByLabel(label, ctasList) {
  if (!label || !ctasList) return null
  return ctasList.find(c => c.label === label) || null
}

/**
 * Check if a CTA value is valid
 *
 * @param {string} ctaValue - CTA enum value
 * @param {array} ctasList - Array of CTA objects
 * @returns {boolean} - True if valid, false otherwise
 *
 * @example
 * isValidCTA('LEARN_MORE', ctas) // Returns true
 * isValidCTA('INVALID_CTA', ctas) // Returns false
 */
export function isValidCTA(ctaValue, ctasList) {
  if (!ctaValue || !ctasList) return false
  return ctasList.some(c => c.value === ctaValue)
}

/**
 * Get all CTA values (enum values only)
 *
 * @param {array} ctasList - Array of CTA objects
 * @returns {array} - Array of CTA value strings
 *
 * @example
 * getCTAValues(ctas) // Returns ['SHOP_NOW', 'LEARN_MORE', ...]
 */
export function getCTAValues(ctasList) {
  if (!ctasList) return []
  return ctasList.map(c => c.value)
}

/**
 * Get all CTA labels (display labels only)
 *
 * @param {array} ctasList - Array of CTA objects
 * @returns {array} - Array of CTA label strings
 *
 * @example
 * getCTALabels(ctas) // Returns ['Mua ngay', 'Tìm hiểu thêm', ...]
 */
export function getCTALabels(ctasList) {
  if (!ctasList) return []
  return ctasList.map(c => c.label)
}

/**
 * Filter CTAs by search term (searches both value and label)
 *
 * @param {string} searchTerm - Search term
 * @param {array} ctasList - Array of CTA objects
 * @returns {array} - Filtered CTA objects
 *
 * @example
 * filterCTAs('tìm', ctas) // Returns CTAs with 'tìm' in value or label
 */
export function filterCTAs(searchTerm, ctasList) {
  if (!searchTerm || !ctasList) return ctasList || []

  const term = searchTerm.toLowerCase()
  return ctasList.filter(cta =>
    cta.value.toLowerCase().includes(term) ||
    cta.label.toLowerCase().includes(term)
  )
}

/**
 * Sort CTAs by label alphabetically
 *
 * @param {array} ctasList - Array of CTA objects
 * @param {boolean} ascending - Sort ascending (default: true)
 * @returns {array} - Sorted CTA objects
 */
export function sortCTAsByLabel(ctasList, ascending = true) {
  if (!ctasList) return []

  return [...ctasList].sort((a, b) => {
    const comparison = a.label.localeCompare(b.label)
    return ascending ? comparison : -comparison
  })
}

/**
 * Create a map of CTA values to labels for O(1) lookup
 *
 * @param {array} ctasList - Array of CTA objects
 * @returns {object} - Map of {value: label}
 *
 * @example
 * const ctaMap = createCTAMap(ctas)
 * ctaMap['LEARN_MORE'] // Returns 'Tìm hiểu thêm'
 */
export function createCTAMap(ctasList) {
  if (!ctasList) return {}

  return ctasList.reduce((map, cta) => {
    map[cta.value] = cta.label
    return map
  }, {})
}

/**
 * Validate and sanitize CTA value
 * Ensures the value is a valid CTA enum or returns a default
 *
 * @param {string} ctaValue - CTA value to validate
 * @param {array} ctasList - Array of CTA objects
 * @param {string} defaultValue - Default CTA value if invalid (default: null)
 * @returns {string|null} - Valid CTA value or default
 *
 * @example
 * validateCTA('LEARN_MORE', ctas) // Returns 'LEARN_MORE'
 * validateCTA('INVALID', ctas) // Returns null
 * validateCTA('INVALID', ctas, 'LEARN_MORE') // Returns 'LEARN_MORE'
 */
export function validateCTA(ctaValue, ctasList, defaultValue = null) {
  if (!ctaValue) return defaultValue

  const isValid = ctasList && ctasList.some(c => c.value === ctaValue)
  return isValid ? ctaValue : defaultValue
}

/**
 * Batch format multiple CTA values to labels
 *
 * @param {array} ctaValues - Array of CTA enum values
 * @param {array} ctasList - Array of CTA objects
 * @param {boolean} fallbackToValue - Fallback to value if not found
 * @returns {array} - Array of formatted labels
 *
 * @example
 * batchFormatCTAs(['LEARN_MORE', 'SHOP_NOW'], ctas)
 * // Returns ['Tìm hiểu thêm', 'Mua ngay']
 */
export function batchFormatCTAs(ctaValues, ctasList, fallbackToValue = true) {
  if (!ctaValues || !Array.isArray(ctaValues)) return []

  return ctaValues.map(value => formatCTA(value, ctasList, fallbackToValue))
}

export default {
  formatCTA,
  getCTAByValue,
  getCTAByLabel,
  isValidCTA,
  getCTAValues,
  getCTALabels,
  filterCTAs,
  sortCTAsByLabel,
  createCTAMap,
  validateCTA,
  batchFormatCTAs
}
