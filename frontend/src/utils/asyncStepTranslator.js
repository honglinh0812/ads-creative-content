const STEP_TRANSLATION_KEYS = {
  'starting-ai-content-generation': 'common.asyncSteps.starting',
  'preparing-content-generation': 'common.asyncSteps.preparing',
  'generating-content-with-ai': 'common.asyncSteps.generating',
  'processing-generated-content': 'common.asyncSteps.processing',
  'validating-content': 'common.asyncSteps.validating',
  'completed': 'common.asyncSteps.completed',
  'failed': 'common.asyncSteps.failed'
}

const normalizeStep = step =>
  step
    .toString()
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')

export const getAsyncStepTranslationKey = step => {
  if (!step) {
    return null
  }
  const normalized = normalizeStep(step)
  return STEP_TRANSLATION_KEYS[normalized] || null
}
