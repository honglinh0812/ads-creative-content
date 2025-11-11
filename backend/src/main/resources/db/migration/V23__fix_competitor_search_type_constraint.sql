-- Fix competitor_searches table constraint to support platform-based search types
-- Issue: Current constraint only allows ('BRAND', 'URL', 'INDUSTRY', 'KEYWORD')
-- Need: Support platform values ('FACEBOOK', 'GOOGLE', 'TIKTOK', 'MANUAL')

-- Drop existing constraint
ALTER TABLE competitor_searches
DROP CONSTRAINT IF EXISTS competitor_searches_search_type_check;

-- Add new constraint with platform support
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
    'MANUAL'
));
