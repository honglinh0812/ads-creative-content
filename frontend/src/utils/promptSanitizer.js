/* eslint-disable no-control-regex */
import { detectLanguage } from './languageDetector'

const CONTROL_REGEX = /[\u0000-\u001F\u007F]+/g
const MULTISPACE_REGEX = /\s{2,}/g
const MAX_LENGTH = 4000
const SUSPICIOUS_PATTERNS = [
  /ignore\s+all\s+previous/gi,
  /system\s*prompt/gi,
  /jailbreak/gi,
  /you\s+are\s+chatgpt/gi,
  /act\s+as\s+/gi,
  /<\/?script>/gi,
  /```/g
]

export function sanitizePromptInput(text = '') {
  if (!text) return ''
  let normalized = ''
  try {
    normalized = text.normalize ? text.normalize('NFKC') : text
  } catch (error) {
    normalized = text
  }
  normalized = normalized.replace(CONTROL_REGEX, ' ')
  SUSPICIOUS_PATTERNS.forEach(pattern => {
    normalized = normalized.replace(pattern, '[blocked]')
  })
  normalized = normalized.replace(MULTISPACE_REGEX, ' ').trim()
  if (normalized.length > MAX_LENGTH) {
    normalized = normalized.slice(0, MAX_LENGTH)
  }
  return normalized
}

export function buildPromptEnvelope(text = '') {
  const sanitized = sanitizePromptInput(text)
  const language = detectLanguage(sanitized || '')
  const wrapped = sanitized
    ? `<<USER_PROMPT_${language.toUpperCase()}>>\n${sanitized}\n<<END_USER_PROMPT>>`
    : ''
  return { sanitized, wrapped, language }
}

export function sanitizeRequestPayload(payload = {}) {
  if (!payload) return {}
  const clone = { ...payload }
  if (clone.prompt !== undefined) {
    const { sanitized } = buildPromptEnvelope(clone.prompt)
    clone.prompt = sanitized
  }
  return clone
}
