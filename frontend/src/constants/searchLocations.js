export const SEARCH_LOCATION_PRESETS = [
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

export const findLocationPreset = (value) => {
  if (!value) {
    return SEARCH_LOCATION_PRESETS[0]
  }
  const normalized = String(value).toLowerCase()
  return SEARCH_LOCATION_PRESETS.find(preset =>
    preset.key === normalized || (preset.country && preset.country.toLowerCase() === normalized)
  ) || SEARCH_LOCATION_PRESETS[0]
}
