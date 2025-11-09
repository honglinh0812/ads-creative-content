-- =====================================================
-- Rollback for V22: Remove needs_review column from ad_contents
-- WARNING: This will delete the needs_review data
-- Use only if migration V22 needs to be reverted
-- =====================================================

-- Instructions:
-- To rollback V22 migration, run this script manually:
-- docker-compose exec -T postgres psql -U postgres fbadsautomation < backend/src/main/resources/db/rollback/U22__rollback_needs_review.sql

-- Step 1: Drop the review queue view first (depends on the column)
DROP VIEW IF EXISTS ad_contents_review_queue;

-- Step 2: Drop indexes
DROP INDEX IF EXISTS idx_ad_contents_user_needs_review;
DROP INDEX IF EXISTS idx_ad_contents_needs_review;

-- Step 3: Drop the needs_review column
ALTER TABLE ad_contents
DROP COLUMN IF EXISTS needs_review;

-- Step 4: Log rollback completion
DO $$
DECLARE
    total_content INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_content FROM ad_contents;

    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'Migration V22 rolled back successfully';
    RAISE NOTICE 'needs_review column removed from ad_contents';
    RAISE NOTICE 'ad_contents_review_queue view dropped';
    RAISE NOTICE 'Related indexes dropped';
    RAISE NOTICE 'Total ad_contents remaining: %', total_content;
    RAISE NOTICE '=====================================================';
END $$;

-- Step 5: Update Flyway schema history (optional - use with caution)
-- Uncomment the following lines ONLY if you want to remove V22 from migration history
-- This allows V22 to be re-applied in the future
-- WARNING: This is dangerous and should only be done in development/testing

-- UPDATE flyway_schema_history
-- SET success = false
-- WHERE version = '22';

-- DELETE FROM flyway_schema_history WHERE version = '22';
