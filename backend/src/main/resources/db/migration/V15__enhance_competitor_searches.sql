-- ===========================================================================
-- Migration V15: Enhance Competitor Searches
-- ===========================================================================
-- Description: Enhance competitor_searches table with additional fields
--              for better search tracking, regional support, and analytics
--
-- Author: AI Panel - Senior Engineers
-- Date: 2025-10-10
-- Phase: Phase 3 - Competitor Insights
--
-- Changes:
-- 1. Add new columns to competitor_searches table (if not exists)
-- 2. Create indexes for query optimization
-- 3. Add constraints for data integrity
-- 4. Backfill default values for existing records
--
-- Security: All operations are idempotent and backwards-compatible
-- Performance: Indexes created to support common query patterns
-- ===========================================================================

-- Check if table exists, create if not
CREATE TABLE IF NOT EXISTS competitor_searches (
    id BIGSERIAL PRIMARY KEY,
    brand_name VARCHAR(100) NOT NULL,
    industry VARCHAR(50),
    user_id BIGINT NOT NULL,
    search_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_competitor_search_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Add new columns if they don't exist
-- Region column for geographic search filtering
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'competitor_searches'
        AND column_name = 'region'
    ) THEN
        ALTER TABLE competitor_searches
        ADD COLUMN region VARCHAR(10) DEFAULT 'US';

        COMMENT ON COLUMN competitor_searches.region IS
            'ISO 3166-1 alpha-2 region code (US, UK, VN, etc.)';
    END IF;
END $$;

-- Result count for analytics
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'competitor_searches'
        AND column_name = 'result_count'
    ) THEN
        ALTER TABLE competitor_searches
        ADD COLUMN result_count INTEGER DEFAULT 0;

        COMMENT ON COLUMN competitor_searches.result_count IS
            'Number of results returned by the search';
    END IF;
END $$;

-- Search type classification
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'competitor_searches'
        AND column_name = 'search_type'
    ) THEN
        ALTER TABLE competitor_searches
        ADD COLUMN search_type VARCHAR(20) DEFAULT 'BRAND'
        CHECK (search_type IN ('BRAND', 'URL', 'INDUSTRY', 'KEYWORD'));

        COMMENT ON COLUMN competitor_searches.search_type IS
            'Type of search performed: BRAND, URL, INDUSTRY, or KEYWORD';
    END IF;
END $$;

-- Success flag for tracking failed searches
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'competitor_searches'
        AND column_name = 'success'
    ) THEN
        ALTER TABLE competitor_searches
        ADD COLUMN success BOOLEAN DEFAULT TRUE;

        COMMENT ON COLUMN competitor_searches.success IS
            'Whether the search completed successfully';
    END IF;
END $$;

-- ===========================================================================
-- Create Indexes for Query Optimization
-- ===========================================================================

-- Index for user-scoped queries (most common pattern)
CREATE INDEX IF NOT EXISTS idx_competitor_search_user
    ON competitor_searches(user_id);

-- Index for date-based sorting and filtering
CREATE INDEX IF NOT EXISTS idx_competitor_search_date
    ON competitor_searches(search_date DESC);

-- Composite index for autocomplete queries (brand name + user)
CREATE INDEX IF NOT EXISTS idx_competitor_search_brand_user
    ON competitor_searches(brand_name, user_id);

-- Index for region-based filtering
CREATE INDEX IF NOT EXISTS idx_competitor_search_region
    ON competitor_searches(region);

-- Composite index for analytics queries (user + date + success)
CREATE INDEX IF NOT EXISTS idx_competitor_search_analytics
    ON competitor_searches(user_id, search_date DESC, success);

-- ===========================================================================
-- Data Migration: Backfill Default Values
-- ===========================================================================

-- Set default region for existing records without region
UPDATE competitor_searches
SET region = 'US'
WHERE region IS NULL;

-- Set default search_type for existing records
UPDATE competitor_searches
SET search_type = 'BRAND'
WHERE search_type IS NULL;

-- Set default success flag for existing records
UPDATE competitor_searches
SET success = TRUE
WHERE success IS NULL;

-- ===========================================================================
-- Add Table Comments for Documentation
-- ===========================================================================

COMMENT ON TABLE competitor_searches IS
    'Stores user search history for competitor ad lookups. Used for autocomplete suggestions and usage analytics.';

COMMENT ON COLUMN competitor_searches.brand_name IS
    'Brand or company name searched. Sanitized before storage to prevent injection attacks.';

COMMENT ON COLUMN competitor_searches.industry IS
    'Optional industry category. May also be used to store additional context.';

COMMENT ON COLUMN competitor_searches.user_id IS
    'Foreign key to users table. Enforces ownership and enables user-scoped queries.';

COMMENT ON COLUMN competitor_searches.search_date IS
    'Timestamp when search was performed. Indexed for sorting and date range queries.';

-- ===========================================================================
-- Performance Statistics Update
-- ===========================================================================

-- Update table statistics for query planner
ANALYZE competitor_searches;

-- ===========================================================================
-- Migration Verification
-- ===========================================================================

-- Verify all expected columns exist
DO $$
DECLARE
    expected_columns TEXT[] := ARRAY[
        'id', 'brand_name', 'industry', 'user_id', 'search_date',
        'region', 'result_count', 'search_type', 'success'
    ];
    col_name TEXT;
    column_count INTEGER;
BEGIN
    -- Count how many expected columns exist
    SELECT COUNT(*) INTO column_count
    FROM information_schema.columns c
    WHERE c.table_name = 'competitor_searches'
    AND c.column_name = ANY(expected_columns);

    -- Log verification result
    RAISE NOTICE 'Migration V15 verification: % of % expected columns exist',
                 column_count, array_length(expected_columns, 1);

    IF column_count < array_length(expected_columns, 1) THEN
        RAISE WARNING 'Some columns are missing from competitor_searches table';
    END IF;
END $$;

-- ===========================================================================
-- Performance Baseline (for future comparison)
-- ===========================================================================

-- Log current table statistics
DO $$
DECLARE
    row_count BIGINT;
    table_size TEXT;
BEGIN
    -- Get row count
    SELECT COUNT(*) INTO row_count FROM competitor_searches;

    -- Get table size
    SELECT pg_size_pretty(pg_total_relation_size('competitor_searches')) INTO table_size;

    RAISE NOTICE 'Competitor searches table: % rows, size: %', row_count, table_size;
END $$;

-- ===========================================================================
-- End of Migration V15
-- ===========================================================================
