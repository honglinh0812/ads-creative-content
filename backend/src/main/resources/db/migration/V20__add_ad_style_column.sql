-- =====================================================
-- Migration V20: Add ad_style column to ads table
-- Issue #8: Creative style selection for ad generation
-- =====================================================

-- Step 1: Add ad_style column (nullable - optional field)
ALTER TABLE ads
ADD COLUMN ad_style VARCHAR(50);

-- Step 2: Add index for filtering by style (partial index for non-null values)
CREATE INDEX idx_ads_ad_style ON ads(ad_style)
WHERE ad_style IS NOT NULL;

-- Step 3: Add comment explaining the field
COMMENT ON COLUMN ads.ad_style IS 'Creative style/tone for ad content: PROFESSIONAL, CASUAL, HUMOROUS, URGENT, LUXURY, EDUCATIONAL, INSPIRATIONAL, MINIMALIST. Optional field - NULL means no specific style preference.';

-- Step 4: Log completion
DO $$
BEGIN
    RAISE NOTICE 'Migration V20 completed: ad_style column added to ads table';
    RAISE NOTICE 'Available styles: PROFESSIONAL, CASUAL, HUMOROUS, URGENT, LUXURY, EDUCATIONAL, INSPIRATIONAL, MINIMALIST';
END $$;
