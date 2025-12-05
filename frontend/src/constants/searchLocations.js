const LINKEDIN_PRESETS = [
  {
    key: 'global',
    location: '',
    country: '',
    gl: '',
    hl: '',
    labelKey: 'competitors.locations.global',
    fallback: 'Global'
  },
  {
    key: 'us',
    location: 'United States',
    country: 'US',
    gl: 'us',
    hl: 'en',
    labelKey: 'competitors.locations.us',
    fallback: 'United States'
  },
  {
    key: 'gb',
    location: 'United Kingdom',
    country: 'GB',
    gl: 'gb',
    hl: 'en',
    labelKey: 'competitors.locations.gb',
    fallback: 'United Kingdom'
  },
  {
    key: 'vn',
    location: 'Viet Nam',
    country: 'VN',
    gl: 'vn',
    hl: 'vi',
    labelKey: 'competitors.locations.vn',
    fallback: 'Vietnam'
  },
  {
    key: 'au',
    location: 'Australia',
    country: 'AU',
    gl: 'au',
    hl: 'en',
    labelKey: 'competitors.locations.au',
    fallback: 'Australia'
  },
  {
    key: 'de',
    location: 'Germany',
    country: 'DE',
    gl: 'de',
    hl: 'de',
    labelKey: 'competitors.locations.de',
    fallback: 'Germany'
  },
  {
    key: 'fr',
    location: 'France',
    country: 'FR',
    gl: 'fr',
    hl: 'fr',
    labelKey: 'competitors.locations.fr',
    fallback: 'France'
  },
  {
    key: 'jp',
    location: 'Japan',
    country: 'JP',
    gl: 'jp',
    hl: 'ja',
    labelKey: 'competitors.locations.jp',
    fallback: 'Japan'
  },
  {
    key: 'sg',
    location: 'Singapore',
    country: 'SG',
    gl: 'sg',
    hl: 'en',
    labelKey: 'competitors.locations.sg',
    fallback: 'Singapore'
  }
]

const TIKTOK_PRESETS = [
  { key: 'all', location: '', country: 'all', labelKey: 'competitors.locations.global', fallback: 'All Regions' },
  { key: 'at', location: 'Austria', country: 'AT', labelKey: '', fallback: 'Austria' },
  { key: 'be', location: 'Belgium', country: 'BE', labelKey: '', fallback: 'Belgium' },
  { key: 'bg', location: 'Bulgaria', country: 'BG', labelKey: '', fallback: 'Bulgaria' },
  { key: 'hr', location: 'Croatia', country: 'HR', labelKey: '', fallback: 'Croatia' },
  { key: 'cy', location: 'Cyprus', country: 'CY', labelKey: '', fallback: 'Cyprus' },
  { key: 'cz', location: 'Czech Republic', country: 'CZ', labelKey: '', fallback: 'Czech Republic' },
  { key: 'dk', location: 'Denmark', country: 'DK', labelKey: '', fallback: 'Denmark' },
  { key: 'ee', location: 'Estonia', country: 'EE', labelKey: '', fallback: 'Estonia' },
  { key: 'fi', location: 'Finland', country: 'FI', labelKey: '', fallback: 'Finland' },
  { key: 'fr', location: 'France', country: 'FR', labelKey: 'competitors.locations.fr', fallback: 'France' },
  { key: 'de', location: 'Germany', country: 'DE', labelKey: 'competitors.locations.de', fallback: 'Germany' },
  { key: 'gr', location: 'Greece', country: 'GR', labelKey: '', fallback: 'Greece' },
  { key: 'hu', location: 'Hungary', country: 'HU', labelKey: '', fallback: 'Hungary' },
  { key: 'is', location: 'Iceland', country: 'IS', labelKey: '', fallback: 'Iceland' },
  { key: 'ie', location: 'Ireland', country: 'IE', labelKey: '', fallback: 'Ireland' },
  { key: 'it', location: 'Italy', country: 'IT', labelKey: '', fallback: 'Italy' },
  { key: 'lv', location: 'Latvia', country: 'LV', labelKey: '', fallback: 'Latvia' },
  { key: 'li', location: 'Liechtenstein', country: 'LI', labelKey: '', fallback: 'Liechtenstein' },
  { key: 'lt', location: 'Lithuania', country: 'LT', labelKey: '', fallback: 'Lithuania' },
  { key: 'lu', location: 'Luxembourg', country: 'LU', labelKey: '', fallback: 'Luxembourg' },
  { key: 'mt', location: 'Malta', country: 'MT', labelKey: '', fallback: 'Malta' },
  { key: 'nl', location: 'Netherlands', country: 'NL', labelKey: '', fallback: 'Netherlands' },
  { key: 'no', location: 'Norway', country: 'NO', labelKey: '', fallback: 'Norway' },
  { key: 'pl', location: 'Poland', country: 'PL', labelKey: '', fallback: 'Poland' },
  { key: 'pt', location: 'Portugal', country: 'PT', labelKey: '', fallback: 'Portugal' },
  { key: 'ro', location: 'Romania', country: 'RO', labelKey: '', fallback: 'Romania' },
  { key: 'sk', location: 'Slovakia', country: 'SK', labelKey: '', fallback: 'Slovakia' },
  { key: 'si', location: 'Slovenia', country: 'SI', labelKey: '', fallback: 'Slovenia' },
  { key: 'es', location: 'Spain', country: 'ES', labelKey: '', fallback: 'Spain' },
  { key: 'se', location: 'Sweden', country: 'SE', labelKey: '', fallback: 'Sweden' },
  { key: 'ch', location: 'Switzerland', country: 'CH', labelKey: '', fallback: 'Switzerland' },
  { key: 'tr', location: 'Turkey', country: 'TR', labelKey: '', fallback: 'Turkey' },
  { key: 'gb', location: 'United Kingdom', country: 'GB', labelKey: 'competitors.locations.gb', fallback: 'United Kingdom' }
]

const DEFAULT_ENGINE = 'linkedin_ad_library'

const PRESET_MAP = {
  linkedin_ad_library: LINKEDIN_PRESETS,
  tiktok_ads_library: TIKTOK_PRESETS
}

export const SEARCH_LOCATION_PRESETS = LINKEDIN_PRESETS

export const getLocationPresets = (engine = DEFAULT_ENGINE) => {
  return PRESET_MAP[engine] || LINKEDIN_PRESETS
}

export const findLocationPreset = (value, engine = DEFAULT_ENGINE) => {
  const presets = getLocationPresets(engine)
  if (!value) {
    return presets[0]
  }
  const normalized = String(value).toLowerCase()
  return presets.find(preset =>
    preset.key === normalized ||
    (preset.country && preset.country.toLowerCase() === normalized) ||
    (preset.location && preset.location.toLowerCase() === normalized)
  ) || presets[0]
}
