-- Migration V12: Fix Cascade Deletion Constraints
-- This migration ensures proper cascade deletion behavior at the database level

-- =====================================================
-- FOREIGN KEY CONSTRAINT FIXES
-- =====================================================

-- First, we need to drop existing foreign key constraints if they exist
-- and recreate them with proper CASCADE DELETE behavior

-- Drop existing foreign key constraints (if they exist)
DO $$
BEGIN
    -- Drop campaigns.user_id constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_campaigns_user_id'
        AND table_name = 'campaigns'
    ) THEN
        ALTER TABLE campaigns DROP CONSTRAINT fk_campaigns_user_id;
    END IF;

    -- Drop ads.campaign_id constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ads_campaign_id'
        AND table_name = 'ads'
    ) THEN
        ALTER TABLE ads DROP CONSTRAINT fk_ads_campaign_id;
    END IF;

    -- Drop ads.user_id constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ads_user_id'
        AND table_name = 'ads'
    ) THEN
        ALTER TABLE ads DROP CONSTRAINT fk_ads_user_id;
    END IF;

    -- Drop ad_contents.ad_id constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ad_contents_ad_id'
        AND table_name = 'ad_contents'
    ) THEN
        ALTER TABLE ad_contents DROP CONSTRAINT fk_ad_contents_ad_id;
    END IF;

    -- Drop ad_contents.user_id constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ad_contents_user_id'
        AND table_name = 'ad_contents'
    ) THEN
        ALTER TABLE ad_contents DROP CONSTRAINT fk_ad_contents_user_id;
    END IF;
END $$;

-- =====================================================
-- CREATE PROPER FOREIGN KEY CONSTRAINTS WITH CASCADE
-- =====================================================

-- Campaigns reference Users
ALTER TABLE campaigns
ADD CONSTRAINT fk_campaigns_user_id
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- Ads reference Campaigns (CASCADE DELETE when campaign is deleted)
ALTER TABLE ads
ADD CONSTRAINT fk_ads_campaign_id
FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- Ads reference Users (CASCADE DELETE when user is deleted)
ALTER TABLE ads
ADD CONSTRAINT fk_ads_user_id
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- Ad Contents reference Ads (CASCADE DELETE when ad is deleted)
ALTER TABLE ad_contents
ADD CONSTRAINT fk_ad_contents_ad_id
FOREIGN KEY (ad_id) REFERENCES ads(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- Ad Contents reference Users (CASCADE DELETE when user is deleted)
ALTER TABLE ad_contents
ADD CONSTRAINT fk_ad_contents_user_id
FOREIGN KEY (user_id) REFERENCES users(id)
ON DELETE CASCADE ON UPDATE CASCADE;

-- =====================================================
-- ASYNC JOB STATUS FOREIGN KEY CONSTRAINTS
-- =====================================================

-- Add foreign key constraint for async_job_status.user_id if table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        -- Drop existing constraint if it exists
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_async_job_status_user_id'
            AND table_name = 'async_job_status'
        ) THEN
            ALTER TABLE async_job_status DROP CONSTRAINT fk_async_job_status_user_id;
        END IF;

        -- Add new constraint with CASCADE DELETE
        ALTER TABLE async_job_status
        ADD CONSTRAINT fk_async_job_status_user_id
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE;
    END IF;
END $$;

-- =====================================================
-- ADDITIONAL REFERENTIAL INTEGRITY CHECKS
-- =====================================================

-- Add constraint to ensure ads.selected_content_id references valid ad_contents
-- Only if selected_content_id is not null
DO $$
BEGIN
    -- Drop existing constraint if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_ads_selected_content_id'
        AND table_name = 'ads'
    ) THEN
        ALTER TABLE ads DROP CONSTRAINT fk_ads_selected_content_id;
    END IF;

    -- Add new constraint with SET NULL on delete (so ad can exist without selected content)
    ALTER TABLE ads
    ADD CONSTRAINT fk_ads_selected_content_id
    FOREIGN KEY (selected_content_id) REFERENCES ad_contents(id)
    ON DELETE SET NULL ON UPDATE CASCADE;
END $$;

-- =====================================================
-- CLEANUP ORPHANED RECORDS
-- =====================================================

-- Clean up any orphaned records that might exist
-- This should be run before adding the constraints

-- Delete ad_contents that reference non-existent ads
DELETE FROM ad_contents
WHERE ad_id NOT IN (SELECT id FROM ads);

-- Delete ad_contents that reference non-existent users
DELETE FROM ad_contents
WHERE user_id NOT IN (SELECT id FROM users);

-- Delete ads that reference non-existent campaigns
DELETE FROM ads
WHERE campaign_id NOT IN (SELECT id FROM campaigns);

-- Delete ads that reference non-existent users
DELETE FROM ads
WHERE user_id NOT IN (SELECT id FROM users);

-- Delete campaigns that reference non-existent users
DELETE FROM campaigns
WHERE user_id NOT IN (SELECT id FROM users);

-- Clean up selected_content_id references that point to non-existent ad_contents
UPDATE ads
SET selected_content_id = NULL
WHERE selected_content_id IS NOT NULL
AND selected_content_id NOT IN (SELECT id FROM ad_contents);

-- Clean up async_job_status that reference non-existent users (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        DELETE FROM async_job_status
        WHERE user_id NOT IN (SELECT id FROM users);
    END IF;
END $$;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Create view to monitor referential integrity
CREATE OR REPLACE VIEW v_referential_integrity_check AS
SELECT
    'campaigns' as table_name,
    'user_id' as foreign_key_column,
    COUNT(*) as total_records,
    COUNT(CASE WHEN user_id NOT IN (SELECT id FROM users) THEN 1 END) as orphaned_records
FROM campaigns
UNION ALL
SELECT
    'ads' as table_name,
    'campaign_id' as foreign_key_column,
    COUNT(*) as total_records,
    COUNT(CASE WHEN campaign_id NOT IN (SELECT id FROM campaigns) THEN 1 END) as orphaned_records
FROM ads
UNION ALL
SELECT
    'ads' as table_name,
    'user_id' as foreign_key_column,
    COUNT(*) as total_records,
    COUNT(CASE WHEN user_id NOT IN (SELECT id FROM users) THEN 1 END) as orphaned_records
FROM ads
UNION ALL
SELECT
    'ad_contents' as table_name,
    'ad_id' as foreign_key_column,
    COUNT(*) as total_records,
    COUNT(CASE WHEN ad_id NOT IN (SELECT id FROM ads) THEN 1 END) as orphaned_records
FROM ad_contents
UNION ALL
SELECT
    'ad_contents' as table_name,
    'user_id' as foreign_key_column,
    COUNT(*) as total_records,
    COUNT(CASE WHEN user_id NOT IN (SELECT id FROM users) THEN 1 END) as orphaned_records
FROM ad_contents;

-- =====================================================
-- CONSTRAINTS VERIFICATION VIEW
-- =====================================================

-- Create view to verify foreign key constraints
CREATE OR REPLACE VIEW v_foreign_key_constraints AS
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS referenced_table,
    ccu.column_name AS referenced_column,
    rc.delete_rule,
    rc.update_rule
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage ccu
    ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints rc
    ON tc.constraint_name = rc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;

-- =====================================================
-- UPDATE STATISTICS
-- =====================================================

-- Update table statistics after cleanup
ANALYZE users;
ANALYZE campaigns;
ANALYZE ads;
ANALYZE ad_contents;

-- Update async_job_status if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'async_job_status') THEN
        ANALYZE async_job_status;
    END IF;
END $$;

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================

COMMENT ON CONSTRAINT fk_campaigns_user_id ON campaigns IS 'Foreign key with CASCADE DELETE - when user is deleted, all campaigns are deleted';
COMMENT ON CONSTRAINT fk_ads_campaign_id ON ads IS 'Foreign key with CASCADE DELETE - when campaign is deleted, all ads are deleted';
COMMENT ON CONSTRAINT fk_ads_user_id ON ads IS 'Foreign key with CASCADE DELETE - when user is deleted, all ads are deleted';
COMMENT ON CONSTRAINT fk_ad_contents_ad_id ON ad_contents IS 'Foreign key with CASCADE DELETE - when ad is deleted, all ad contents are deleted';
COMMENT ON CONSTRAINT fk_ad_contents_user_id ON ad_contents IS 'Foreign key with CASCADE DELETE - when user is deleted, all ad contents are deleted';
COMMENT ON CONSTRAINT fk_ads_selected_content_id ON ads IS 'Foreign key with SET NULL - when ad content is deleted, selected_content_id is set to NULL';

-- Log completion (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'schema_migrations_log') THEN
        INSERT INTO public.schema_migrations_log (migration_version, applied_at, description)
        VALUES ('V12', NOW(), 'Fixed cascade deletion constraints and cleaned up orphaned records')
        ON CONFLICT DO NOTHING;
    END IF;
END $$;