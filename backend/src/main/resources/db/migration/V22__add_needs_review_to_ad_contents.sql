-- =====================================================
-- Migration V22: Add needs_review flag to ad_contents table
-- Purpose: Track ad content that was auto-truncated to meet Facebook limits
-- Issue: Headline length validation (40 chars max for Facebook)
-- =====================================================

-- Step 1: Add needs_review column (nullable initially for existing records)
ALTER TABLE ad_contents
ADD COLUMN needs_review BOOLEAN;

-- Step 2: Set default value for existing records based on validation warnings
-- Logic: Content with warnings or low quality should be reviewed
UPDATE ad_contents
SET needs_review = CASE
    WHEN has_warnings = TRUE THEN TRUE
    WHEN quality_score IS NOT NULL AND quality_score < 50 THEN TRUE
    ELSE FALSE
END;

-- Step 3: Make needs_review NOT NULL with default FALSE for new records
ALTER TABLE ad_contents
ALTER COLUMN needs_review SET DEFAULT FALSE;

ALTER TABLE ad_contents
ALTER COLUMN needs_review SET NOT NULL;

-- Step 4: Create index for filtering content needing review
-- Partial index: only index rows where needs_review = TRUE (saves space)
CREATE INDEX idx_ad_contents_needs_review
ON ad_contents(needs_review, created_date DESC)
WHERE needs_review = TRUE;

-- Step 5: Create composite index for user-specific review queries
CREATE INDEX idx_ad_contents_user_needs_review
ON ad_contents(user_id, needs_review, created_date DESC);

-- Step 6: Add column comment explaining the field
COMMENT ON COLUMN ad_contents.needs_review IS 'Flag indicating if content requires human review. Set to TRUE when content is auto-truncated to meet Facebook limits (headline 40 chars, description 125 chars, primary text 1000 chars) or has validation warnings. Default: FALSE';

-- Step 7: Create view for easy review queue access
CREATE OR REPLACE VIEW ad_contents_review_queue AS
SELECT
    ac.id,
    ac.user_id,
    ac.ad_id,
    ac.content_type,
    ac.primary_text,
    ac.headline,
    ac.description,
    ac.quality_score,
    ac.has_warnings,
    ac.validation_warnings,
    ac.needs_review,
    ac.created_date,
    a.name as ad_name,
    u.email as user_email
FROM ad_contents ac
JOIN ads a ON ac.ad_id = a.id
JOIN users u ON ac.user_id = u.id
WHERE ac.needs_review = TRUE
ORDER BY ac.created_date DESC;

-- Step 8: Add helpful statistics query
COMMENT ON VIEW ad_contents_review_queue IS 'View of all ad contents requiring review, with user and ad context. Use for review dashboard. Content appears here when auto-truncated to meet Facebook limits or flagged by validation.';

-- Step 9: Log migration completion with statistics
DO $$
DECLARE
    total_content INTEGER;
    needs_review_count INTEGER;
    review_percentage NUMERIC(5,2);
BEGIN
    SELECT COUNT(*) INTO total_content FROM ad_contents;
    SELECT COUNT(*) INTO needs_review_count FROM ad_contents WHERE needs_review = TRUE;

    IF total_content > 0 THEN
        review_percentage := (needs_review_count::NUMERIC / total_content::NUMERIC) * 100;
    ELSE
        review_percentage := 0;
    END IF;

    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'Migration V22 completed successfully';
    RAISE NOTICE 'Total ad_contents: %', total_content;
    RAISE NOTICE 'Content needing review: % (%.2f%%)', needs_review_count, review_percentage;
    RAISE NOTICE 'Review queue view created: ad_contents_review_queue';
    RAISE NOTICE '=====================================================';
END $$;
