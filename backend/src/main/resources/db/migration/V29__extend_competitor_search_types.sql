-- Extend competitor search type constraint to support new SearchAPI engines
-- Adds LinkedIn and TikTok ads library identifiers alongside existing values

ALTER TABLE competitor_searches
DROP CONSTRAINT IF EXISTS competitor_searches_search_type_check;

ALTER TABLE competitor_searches
ADD CONSTRAINT competitor_searches_search_type_check
CHECK (search_type IN (
    'BRAND',
    'URL',
    'INDUSTRY',
    'KEYWORD',
    'FACEBOOK',
    'GOOGLE',
    'TIKTOK',
    'MANUAL',
    'LINKEDIN_AD_LIBRARY',
    'TIKTOK_ADS_LIBRARY'
));
